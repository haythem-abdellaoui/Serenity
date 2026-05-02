import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppointmentService, GoogleCalendarStatusDto } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { formatUserLookupName, UserLookup } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  appointmentParticipantDisplayName,
  CalendarBusySlot
} from '../../../shared/models/appointment.model';
import {
  APPOINTMENT_SLOT_DURATION_MINUTES,
  appointmentDateToYmd,
  getNextRelevantAppointment,
  parseAppointmentLocalStart
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-appointment-list',
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss']
})
export class AppointmentListComponent implements OnInit, OnDestroy {
  appointments: AppointmentResponse[] = [];
  /** Sorted list, optionally filtered by selected calendar day. */
  displayedAppointments: AppointmentResponse[] = [];
  loading = true;
  errorMessage = '';
  infoMessage = '';
  isAdmin = false;
  googleCalendarStatus: GoogleCalendarStatusDto | null = null;
  googleCalendarSyncing = false;
  /** When true, admin sees every appointment (query ?scope=all). */
  scopeAll = false;
  /** Admin module route: show only the full table + stats (no calendar / hub). */
  adminAppointmentsShell = false;
  /** Summary counts for admin dashboard (all appointments). */
  adminStats: {
    total: number;
    pending: number;
    confirmed: number;
    completed: number;
    cancelled: number;
    tele: number;
    inPerson: number;
  } | null = null;
  private readonly lookupById = new Map<number, UserLookup>();

  nextAppointment: AppointmentResponse | null = null;
  /** Live countdown segments until the next visit starts (null when none or in-progress). */
  /** Remaining time until start (sec = 0–59 seconds component, not total seconds). */
  countdownParts: { d: number; h: number; m: number; sec: number } | null = null;
  /** True while the next visit is currently in its time window. */
  nextVisitInProgress = false;
  private countdownInterval: ReturnType<typeof setInterval> | undefined;
  private querySub?: Subscription;

  /** Slots for the month calendar (teal = has at least one booking that day). */
  calendarBusySlots: CalendarBusySlot[] = [];
  viewMonth = new Date().getMonth() + 1;
  viewYear = new Date().getFullYear();
  /** When set, the table below shows only this YYYY-MM-DD. */
  selectedFilterDate: string | null = null;

  readonly slotDurationMin = APPOINTMENT_SLOT_DURATION_MINUTES;

  /** Indices 0–11 for twelve hour marks on the analog dial. */
  readonly clockTickIndexes = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.querySub = this.route.queryParamMap.subscribe((qm) => {
      const urlPath = this.router.url.split('?')[0].replace(/\/$/, '') || '/';
      this.adminAppointmentsShell =
        this.isAdmin && (urlPath === '/admin/appointments' || urlPath === '/admin/appointments/list');
      if (this.adminAppointmentsShell) {
        this.scopeAll = true;
        this.selectedFilterDate = null;
      } else {
        const scope = qm.get('scope');
        this.scopeAll = this.isAdmin && scope === 'all';
      }
      const calendarLinked = qm.get('calendarLinked');
      const calendarError = qm.get('calendarError');
      this.load(calendarLinked, calendarError);
    });
  }

  /** First connection or first successful sync in this browser (per user). */
  get showGcalMainButton(): boolean {
    return (
      (this.authService.hasRole('PATIENT') || this.authService.hasRole('DOCTOR')) &&
      !this.scopeAll &&
      (!this.googleCalendarStatus?.connected || !this.isGcalInitialSyncComplete())
    );
  }

  /** After at least one successful sync, show a small “update” action instead of the main CTA. */
  get showGcalResyncLink(): boolean {
    return (
      (this.authService.hasRole('PATIENT') || this.authService.hasRole('DOCTOR')) &&
      !this.scopeAll &&
      !!this.googleCalendarStatus?.connected &&
      this.isGcalInitialSyncComplete()
    );
  }

  onGoogleCalendarSync(): void {
    this.infoMessage = '';
    this.errorMessage = '';
    if (!this.googleCalendarStatus?.connected) {
      this.googleCalendarSyncing = true;
      const returnTo = this.router.url.split('?')[0].startsWith('/admin/')
        ? '/admin/appointments/list'
        : '/appointments/list';
      this.appointmentService.getGoogleCalendarAuthorizeUrl(returnTo).subscribe({
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
    this.querySub?.unsubscribe();
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  /** Admin: switch list API between mine and all; query string keeps deep links & return from detail. */
  setScope(all: boolean): void {
    if (!this.isAdmin) {
      return;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { scope: all ? 'all' : 'mine' },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  load(calendarLinked: string | null = null, calendarError: string | null = null): void {
    this.loading = true;
    this.errorMessage = '';
    this.refreshGoogleCalendarStatus();
    const req$ = this.scopeAll ? this.appointmentService.getAll() : this.appointmentService.getMine();
    req$.subscribe({
      next: (rows) => {
        this.appointments = rows;
        this.applyCalendarAndNext();
        this.applyTableFilter();
        const ids = [...new Set(rows.flatMap((a) => [a.patientUserId, a.doctorUserId]))];
        const finish = (): void => {
          this.loading = false;
          this.recomputeAdminStats();
          if (!this.adminAppointmentsShell) {
            this.startCountdownTicker();
          }
          if (calendarLinked) {
            this.infoMessage =
              'Google Calendar is connected. Use "Synchronize with Google Calendar" above to add your visits to the calendar.';
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
        this.adminStats = null;
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

  private recomputeAdminStats(): void {
    if (!this.adminAppointmentsShell) {
      this.adminStats = null;
      return;
    }
    const r = this.appointments;
    this.adminStats = {
      total: r.length,
      pending: r.filter((a) => a.status === 'PENDING').length,
      confirmed: r.filter((a) => a.status === 'CONFIRMED').length,
      completed: r.filter((a) => a.status === 'COMPLETED').length,
      cancelled: r.filter((a) => a.status === 'CANCELLED').length,
      tele: r.filter((a) => a.type === 'TELECONSULTATION').length,
      inPerson: r.filter((a) => a.type !== 'TELECONSULTATION').length
    };
  }

  private applyCalendarAndNext(): void {
    if (this.adminAppointmentsShell) {
      this.calendarBusySlots = [];
      this.nextAppointment = null;
      return;
    }
    this.calendarBusySlots = this.appointments
      .filter((a) => a.status !== 'CANCELLED')
      .map((a) => ({
        appointmentDate: appointmentDateToYmd(a.appointmentDate) ?? '',
        timeSlot: a.timeSlot,
        source: 'DOCTOR' as const
      }))
      .filter((s) => s.appointmentDate.length >= 10);

    this.nextAppointment = getNextRelevantAppointment(this.appointments);
    if (this.nextAppointment) {
      const start = parseAppointmentLocalStart(this.nextAppointment.appointmentDate, this.nextAppointment.timeSlot);
      if (start) {
        this.viewMonth = start.getMonth() + 1;
        this.viewYear = start.getFullYear();
      }
    }
    this.updateCountdownLabel();
  }

  private startCountdownTicker(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    this.countdownInterval = setInterval(() => this.updateCountdownLabel(), 1000);
  }

  private updateCountdownLabel(): void {
    let next = this.nextAppointment;
    this.nextVisitInProgress = false;
    this.countdownParts = null;
    if (!next) {
      return;
    }
    const start = parseAppointmentLocalStart(next.appointmentDate, next.timeSlot);
    if (!start) {
      return;
    }
    const dur = APPOINTMENT_SLOT_DURATION_MINUTES * 60 * 1000;
    const end = new Date(start.getTime() + dur);
    const now = new Date();
    if (now >= start && now < end) {
      this.nextVisitInProgress = true;
      return;
    }
    if (now >= end) {
      this.nextAppointment = getNextRelevantAppointment(this.appointments);
      next = this.nextAppointment;
      if (!next) {
        return;
      }
      return this.updateCountdownLabel();
    }
    const ms = start.getTime() - now.getTime();
    const totalSec = Math.floor(ms / 1000);
    const d = Math.floor(totalSec / 86400);
    const h = Math.floor((totalSec % 86400) / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const sec = totalSec % 60;
    // Do not use shorthand `{ d, h, m, s }` — `s` would bind to the wrong variable.
    this.countdownParts = { d, h, m, sec };
  }

  pad2(n: number): string {
    return String(n).padStart(2, '0');
  }

  /** Hour hand angle (degrees, 0 = 12 o’clock, clockwise). */
  appointmentHourAngle(): number {
    const a = this.nextAppointment;
    if (!a) {
      return 0;
    }
    const start = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot);
    if (!start) {
      return 0;
    }
    const h12 = start.getHours() % 12;
    const min = start.getMinutes();
    const secs = start.getSeconds();
    return h12 * 30 + min * 0.5 + secs * (0.5 / 60);
  }

  /** Minute hand angle (degrees). */
  appointmentMinuteAngle(): number {
    const a = this.nextAppointment;
    if (!a) {
      return 0;
    }
    const start = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot);
    if (!start) {
      return 0;
    }
    const min = start.getMinutes();
    const secs = start.getSeconds();
    return min * 6 + secs * 0.1;
  }

  /** Short label for the visit time (local). */
  nextAppointmentVisitTimeLabel(): string {
    const a = this.nextAppointment;
    if (!a) {
      return '';
    }
    const start = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot);
    if (!start) {
      return '';
    }
    return start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }

  onCalendarMonthChange(ev: { year: number; month: number }): void {
    this.viewYear = ev.year;
    this.viewMonth = ev.month;
  }

  onCalendarDaySelect(ymd: string): void {
    if (this.selectedFilterDate === ymd) {
      this.selectedFilterDate = null;
    } else {
      this.selectedFilterDate = ymd;
    }
    this.applyTableFilter();
  }

  private applyTableFilter(): void {
    const f = this.selectedFilterDate;
    const sorted = [...this.appointments].sort((a, b) => this.sortAppt(a, b));
    if (!f) {
      this.displayedAppointments = sorted;
      return;
    }
    const key = f.slice(0, 10);
    this.displayedAppointments = sorted.filter(
      (a) => (appointmentDateToYmd(a.appointmentDate) ?? '').slice(0, 10) === key
    );
  }

  /** Newest bookings first (by createdAt), then by id. */
  private sortAppt(a: AppointmentResponse, b: AppointmentResponse): number {
    const ca = Date.parse(a.createdAt);
    const cb = Date.parse(b.createdAt);
    if (!Number.isNaN(ca) && !Number.isNaN(cb)) {
      const byCreated = cb - ca;
      if (byCreated !== 0) {
        return byCreated;
      }
    }
    return b.id - a.id;
  }

  clearDateFilter(): void {
    this.selectedFilterDate = null;
    this.applyTableFilter();
  }

  nextAppointmentStartsAt(): string {
    const a = this.nextAppointment;
    if (!a) {
      return '';
    }
    const d = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot);
    if (!d) {
      return '';
    }
    return d.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  patientName(a: AppointmentResponse): string {
    const u = this.lookupById.get(a.patientUserId);
    if (u) {
      return formatUserLookupName(u);
    }
    return appointmentParticipantDisplayName(a, 'patient');
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
        return 'badge badge-success';
      case 'CANCELLED':
        return 'badge badge-danger';
      case 'COMPLETED':
        return 'badge badge-muted';
      default:
        return 'badge badge-primary';
    }
  }

  isDoctorFor(a: AppointmentResponse): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && a.doctorUserId === uid;
  }

  isPatientFor(a: AppointmentResponse): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && a.patientUserId === uid;
  }

  confirm(a: AppointmentResponse, ev: Event): void {
    ev.stopPropagation();
    this.appointmentService.confirm(a.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = err.error?.error || 'Confirm failed';
      }
    });
  }

  cancel(a: AppointmentResponse, ev: Event): void {
    ev.stopPropagation();
    this.appointmentService.cancel(a.id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.errorMessage = err.error?.error || 'Cancel failed';
      }
    });
  }

  openRow(a: AppointmentResponse): void {
    const base = this.router.url.split('?')[0].includes('/admin/appointments') ? '/admin/appointments' : '/appointments';
    this.router.navigate([base, a.id], { queryParams: this.route.snapshot.queryParams });
  }
}
