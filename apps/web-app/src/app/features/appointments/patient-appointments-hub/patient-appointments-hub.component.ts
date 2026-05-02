import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppointmentService, GoogleCalendarStatusDto } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { formatUserLookupName, UserLookup } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  CalendarBusySlot,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';
import {
  appointmentDateToYmd,
  appointmentResponseStartMs,
  formatCountdownMs,
  formatSlotRange,
  minDateInputYmd,
  normalizeTimeHhMm
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-patient-appointments-hub',
  templateUrl: './patient-appointments-hub.component.html',
  styleUrls: ['./patient-appointments-hub.component.scss']
})
export class PatientAppointmentsHubComponent implements OnInit, OnDestroy {
  appointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage = '';
  infoMessage = '';
  googleCalendarStatus: GoogleCalendarStatusDto | null = null;
  googleCalendarSyncing = false;
  private readonly lookupById = new Map<number, UserLookup>();

  /** Calendar: all visits (any status) so the month reflects history + future. */
  calendarBusySlots: CalendarBusySlot[] = [];
  calMonth = new Date().getMonth() + 1;
  calYear = new Date().getFullYear();
  selectedDate = '';

  countdownText = '';
  userFirstName = '';

  private userSub?: Subscription;
  private querySub?: Subscription;
  private tickId: ReturnType<typeof setInterval> | undefined;

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.selectedDate = minDateInputYmd();
    this.userSub = this.userService.currentUser$.subscribe((u) => {
      this.userFirstName = u?.firstName?.trim() || '';
    });
    this.userService.getCurrentUser().subscribe({
      next: (u) => {
        this.userFirstName = u?.firstName?.trim() || '';
      },
      error: () => {
        /* keep greeting generic */
      }
    });
    this.querySub = this.route.queryParamMap.subscribe((qm) => {
      this.load(qm.get('calendarLinked'), qm.get('calendarError'));
    });
    this.tickId = setInterval(() => this.refreshCountdown(), 1000);
  }

  get showGcalMainButton(): boolean {
    return !this.googleCalendarStatus?.connected || !this.isGcalInitialSyncComplete();
  }

  get showGcalResyncLink(): boolean {
    return !!this.googleCalendarStatus?.connected && this.isGcalInitialSyncComplete();
  }

  onGoogleCalendarSync(): void {
    this.infoMessage = '';
    this.errorMessage = '';
    if (!this.googleCalendarStatus?.connected) {
      this.googleCalendarSyncing = true;
      this.appointmentService.getGoogleCalendarAuthorizeUrl('/appointments').subscribe({
        next: (r) => {
          window.location.href = r.authorizeUrl;
        },
        error: (err) => {
          this.googleCalendarSyncing = false;
          this.errorMessage =
            err.error?.message || err.error?.error || err.message || 'Could not start Google authorization';
        }
      });
      return;
    }
    this.googleCalendarSyncing = true;
    this.appointmentService.syncGoogleCalendar().subscribe({
      next: (r) => {
        this.googleCalendarSyncing = false;
        this.markGcalInitialSyncComplete();
        this.infoMessage = this.gcalSyncResultMessage(r.eventsUpserted, r.totalCandidates);
      },
      error: (err) => {
        this.googleCalendarSyncing = false;
        if (err.status === 412) {
          this.googleCalendarStatus = { configured: true, connected: false };
          this.errorMessage = 'Connect Google Calendar first, then try again.';
          return;
        }
        this.errorMessage =
          err.error?.message || err.error?.error || err.message || 'Google Calendar sync failed';
      }
    });
  }

  private gcalSyncStorageKey(): string | null {
    const id = this.authService.getUserId();
    return id != null ? `serenity_gcal_sync_v1_u${id}` : null;
  }

  private isGcalInitialSyncComplete(): boolean {
    const k = this.gcalSyncStorageKey();
    return k != null && localStorage.getItem(k) === '1';
  }

  private markGcalInitialSyncComplete(): void {
    const k = this.gcalSyncStorageKey();
    if (k) {
      localStorage.setItem(k, '1');
    }
  }

  private gcalSyncResultMessage(upserted: number, total: number | undefined): string {
    if (upserted > 0) {
      return `Updated Google Calendar: ${upserted} event(s).`;
    }
    if ((total ?? 0) === 0) {
      return 'No visits to add right now. Only pending or confirmed visits (from yesterday onward) are included.';
    }
    return 'Google Calendar did not change. If you expected new events, try again in a few seconds.';
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.querySub?.unsubscribe();
    if (this.tickId) {
      clearInterval(this.tickId);
    }
  }

  load(calendarLinked: string | null = null, calendarError: string | null = null): void {
    this.loading = true;
    this.errorMessage = '';
    this.refreshGoogleCalendarStatus();
    this.appointmentService.getMine().subscribe({
      next: (rows) => {
        this.appointments = rows;
        this.rebuildCalendarSlots();
        this.refreshCountdown();
        const ids = [...new Set(rows.flatMap((a) => [a.patientUserId, a.doctorUserId]))];
        const finish = (): void => {
          this.loading = false;
          if (calendarLinked) {
            this.infoMessage =
              'Google Calendar is connected. Tap "Synchronize with Google Calendar" to add your visits to the calendar.';
            void this.router.navigate([], {
              relativeTo: this.route,
              queryParams: { calendarLinked: null },
              queryParamsHandling: 'merge',
              replaceUrl: true
            });
          }
          if (calendarError) {
            this.errorMessage = 'Google Calendar: ' + calendarError;
            void this.router.navigate([], {
              relativeTo: this.route,
              queryParams: { calendarError: null },
              queryParamsHandling: 'merge',
              replaceUrl: true
            });
          }
        };
        if (ids.length === 0) {
          finish();
          return;
        }
        this.userService.lookupNamesByIds(ids).subscribe({
          next: (list) => {
            this.lookupById.clear();
            for (const u of list) {
              this.lookupById.set(u.id, u);
            }
            finish();
          },
          error: () => {
            finish();
          }
        });
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error?.error || err.message || 'Failed to load appointments';
        this.loading = false;
      }
    });
  }

  private refreshGoogleCalendarStatus(): void {
    this.appointmentService.getGoogleCalendarStatus().subscribe({
      next: (s) => {
        this.googleCalendarStatus = s;
      },
      error: () => {
        this.googleCalendarStatus = { configured: false, connected: false };
      }
    });
  }

  private rebuildCalendarSlots(): void {
    this.calendarBusySlots = this.appointments.map((a) => ({
      appointmentDate: appointmentDateToYmd(a.appointmentDate as unknown) ?? String(a.appointmentDate).slice(0, 10),
      timeSlot: normalizeTimeHhMm(a.timeSlot),
      source: 'PATIENT' as const
    }));
  }

  get greeting(): string {
    return this.userFirstName ? `Hi, ${this.userFirstName}` : 'Hi there';
  }

  /** Next PENDING or CONFIRMED visit strictly in the future. */
  get nextUpcoming(): AppointmentResponse | null {
    const now = Date.now();
    const upcoming = this.appointments
      .filter((a) => a.status === 'PENDING' || a.status === 'CONFIRMED')
      .map((a) => ({ a, t: appointmentResponseStartMs(a) }))
      .filter((x) => x.t > now)
      .sort((x, y) => x.t - y.t);
    return upcoming.length ? upcoming[0].a : null;
  }

  /** Confirmed or pending visits in the future (for "coming up" stat). */
  get upcomingActiveCount(): number {
    const now = Date.now();
    return this.appointments.filter(
      (a) =>
        (a.status === 'PENDING' || a.status === 'CONFIRMED') && appointmentResponseStartMs(a) > now
    ).length;
  }

  get completedCount(): number {
    return this.appointments.filter((a) => a.status === 'COMPLETED').length;
  }

  get hasVisits(): boolean {
    return this.appointments.length > 0;
  }

  /** Short line for patients: pending vs confirmed matters clinically. */
  statusPatientHint(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Your doctor still needs to confirm this visit.';
      case 'CONFIRMED':
        return 'Confirmed — you are on the schedule.';
      default:
        return '';
    }
  }

  private refreshCountdown(): void {
    const next = this.nextUpcoming;
    if (!next) {
      this.countdownText = '';
      return;
    }
    const target = appointmentResponseStartMs(next);
    const diff = target - Date.now();
    this.countdownText = formatCountdownMs(diff);
  }

  doctorName(a: AppointmentResponse): string {
    const u = this.lookupById.get(a.doctorUserId);
    if (u) {
      return formatUserLookupName(u);
    }
    return appointmentParticipantDisplayName(a, 'doctor');
  }

  statusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED':
        return 'hub-badge hub-badge--confirmed';
      case 'CANCELLED':
        return 'hub-badge hub-badge--cancelled';
      case 'COMPLETED':
        return 'hub-badge hub-badge--completed';
      default:
        return 'hub-badge hub-badge--pending';
    }
  }

  rangeLabel(timeSlot: string): string {
    return formatSlotRange(timeSlot);
  }

  get appointmentsOnSelectedDay(): AppointmentResponse[] {
    const key = this.selectedDate.slice(0, 10);
    return this.appointments
      .filter((a) => (appointmentDateToYmd(a.appointmentDate as unknown) ?? '').slice(0, 10) === key)
      .sort((a, b) => appointmentResponseStartMs(a) - appointmentResponseStartMs(b));
  }

  onCalMonthChange(ev: { year: number; month: number }): void {
    this.calYear = ev.year;
    this.calMonth = ev.month;
  }

  onDateChange(value: string): void {
    this.selectedDate = value;
    if (value && value.length >= 10) {
      const y = +value.slice(0, 4);
      const m = +value.slice(5, 7);
      if (y > 0 && m >= 1 && m <= 12) {
        if (this.calYear !== y || this.calMonth !== m) {
          this.calYear = y;
          this.calMonth = m;
        }
      }
    }
  }

  openAppointment(a: AppointmentResponse): void {
    this.router.navigate(['/appointments', a.id]);
  }

  onAppointmentItemKeydown(event: KeyboardEvent, a: AppointmentResponse): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.openAppointment(a);
    }
  }

  goBook(): void {
    this.router.navigate(['/appointments', 'book']);
  }

  goList(): void {
    this.router.navigate(['/appointments', 'list']);
  }
}
