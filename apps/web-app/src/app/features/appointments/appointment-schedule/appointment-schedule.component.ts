import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { UserService } from '../../../core/services/user.service';
import { AppointmentType, CalendarBusySlot } from '../../../shared/models/appointment.model';
import { UserLookup } from '../../../shared/models/user.model';
import {
  hasAppointmentTimeOverlap,
  minDateInputYmd,
  validateAppointmentScheduling
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-appointment-schedule',
  templateUrl: './appointment-schedule.component.html',
  styleUrls: ['./appointment-schedule.component.scss']
})
export class AppointmentScheduleComponent implements OnInit {
  patients: UserLookup[] = [];
  selectedPatientId: number | null = null;
  loadingPatients = true;

  appointmentDate = '';
  timeSlot = '';
  type: AppointmentType = 'IN_PERSON';
  notes = '';
  submitting = false;
  errorMessage = '';

  busySlots: CalendarBusySlot[] = [];
  calMonth = new Date().getMonth() + 1;
  calYear = new Date().getFullYear();
  loadingCalendar = false;
  calendarLoadError = '';

  readonly legendDoctor = 'Your appointments';
  readonly legendPatient = "Patient's other visits";

  get busyPanelSubheading(): string {
    return this.selectedPatientId
      ? "Your bookings and this patient's other visits — avoid the same start time (HH:mm)."
      : 'Your bookings — select a patient to also see their other visits.';
  }

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.userService.lookupPatients().subscribe({
      next: (rows) => {
        this.patients = rows;
        this.loadingPatients = false;
        this.loadCalendarHints();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error?.error || 'Could not load patients.';
        this.loadingPatients = false;
      }
    });
  }

  patientLabel(p: UserLookup): string {
    return `${p.firstName} ${p.lastName} (${p.email})`;
  }

  private pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  private monthRange(y: number, m: number): { from: string; to: string } {
    const from = new Date(y, m - 1, 1);
    const to = new Date(y, m, 0);
    const fmt = (d: Date): string =>
      `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`;
    return { from: fmt(from), to: fmt(to) };
  }

  loadCalendarHints(): void {
    if (this.loadingPatients) {
      return;
    }
    const { from, to } = this.monthRange(this.calYear, this.calMonth);
    this.loadingCalendar = true;
    this.calendarLoadError = '';
    const opts: { patientUserId?: number } = {};
    if (this.selectedPatientId != null && this.selectedPatientId >= 1) {
      opts.patientUserId = this.selectedPatientId;
    }
    this.appointmentService.getCalendarHints(from, to, opts).subscribe({
      next: (rows) => {
        this.busySlots = rows;
        this.loadingCalendar = false;
      },
      error: (err) => {
        this.busySlots = [];
        this.loadingCalendar = false;
        this.calendarLoadError =
          err?.error?.message || err?.message || 'Could not load busy times. Check login and API.';
      }
    });
  }

  onPatientChange(): void {
    this.loadCalendarHints();
  }

  onCalMonthChange(ev: { year: number; month: number }): void {
    this.calYear = ev.year;
    this.calMonth = ev.month;
    this.loadCalendarHints();
  }

  onAppointmentDateChange(value: string): void {
    this.appointmentDate = value;
    if (value && value.length >= 10) {
      const y = +value.slice(0, 4);
      const m = +value.slice(5, 7);
      if (y > 0 && m >= 1 && m <= 12) {
        if (this.calYear !== y || this.calMonth !== m) {
          this.calYear = y;
          this.calMonth = m;
        }
        this.loadCalendarHints();
      }
    }
  }

  normalizeTime(t: string): string {
    const p = t.trim().split(':');
    if (p.length !== 2) {
      return t.trim();
    }
    return `${p[0].padStart(2, '0')}:${p[1].padStart(2, '0')}`;
  }

  hasSlotConflict(): boolean {
    if (!this.appointmentDate || !this.timeSlot?.trim()) {
      return false;
    }
    return hasAppointmentTimeOverlap(this.appointmentDate, this.timeSlot, this.busySlots);
  }

  get minDateStr(): string {
    return minDateInputYmd();
  }

  get schedulingWindowError(): string | null {
    if (!this.appointmentDate || !this.timeSlot?.trim()) {
      return null;
    }
    return validateAppointmentScheduling(this.appointmentDate, this.timeSlot);
  }

  submit(): void {
    this.errorMessage = '';
    if (this.selectedPatientId == null || this.selectedPatientId < 1) {
      this.errorMessage = 'Select a patient.';
      return;
    }
    if (!this.appointmentDate || !this.timeSlot) {
      this.errorMessage = 'Date and time are required.';
      return;
    }
    const schedErr = validateAppointmentScheduling(this.appointmentDate, this.timeSlot);
    if (schedErr) {
      this.errorMessage = schedErr;
      return;
    }
    if (this.hasSlotConflict()) {
      this.errorMessage =
        'That time overlaps an existing appointment (each visit blocks about 1h30).';
      return;
    }
    this.submitting = true;
    this.appointmentService
      .doctorSchedule({
        patientUserId: this.selectedPatientId,
        appointmentDate: this.appointmentDate,
        timeSlot: this.normalizeTime(this.timeSlot),
        type: this.type,
        notes: this.notes || undefined
      })
      .subscribe({
        next: () => {
          const dest = this.router.url.startsWith('/admin') ? '/admin/appointments' : '/appointments';
          this.router.navigateByUrl(dest);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || err.error?.error || 'Could not schedule';
          this.submitting = false;
        }
      });
  }

  cancel(): void {
    const dest = this.router.url.startsWith('/admin') ? '/admin/appointments' : '/appointments';
    this.router.navigateByUrl(dest);
  }
}
