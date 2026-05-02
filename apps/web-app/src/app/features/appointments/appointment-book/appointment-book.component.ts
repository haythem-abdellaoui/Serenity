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
  selector: 'app-appointment-book',
  templateUrl: './appointment-book.component.html',
  styleUrls: ['./appointment-book.component.scss']
})
export class AppointmentBookComponent implements OnInit {
  doctors: UserLookup[] = [];
  selectedDoctorId: number | null = null;
  appointmentDate = '';
  timeSlot = '';
  type: AppointmentType = 'IN_PERSON';
  notes = '';
  submitting = false;
  errorMessage = '';
  loadingDoctors = true;

  busySlots: CalendarBusySlot[] = [];
  calMonth = new Date().getMonth() + 1;
  calYear = new Date().getFullYear();
  loadingCalendar = false;
  calendarLoadError = '';

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.userService.lookupDoctors().subscribe({
      next: (rows) => {
        this.doctors = rows;
        this.loadingDoctors = false;
      },
      error: () => {
        this.errorMessage = 'Could not load doctors.';
        this.loadingDoctors = false;
      }
    });
  }

  doctorLabel(d: UserLookup): string {
    return `${d.firstName} ${d.lastName}`.trim();
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
    if (this.selectedDoctorId == null || this.selectedDoctorId < 1) {
      this.busySlots = [];
      return;
    }
    const { from, to } = this.monthRange(this.calYear, this.calMonth);
    this.loadingCalendar = true;
    this.calendarLoadError = '';
    this.appointmentService
      .getCalendarHints(from, to, { doctorUserId: this.selectedDoctorId })
      .subscribe({
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

  onDoctorChange(): void {
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

  /** Working hours / past-time validation (null if incomplete or OK). */
  get schedulingWindowError(): string | null {
    if (!this.appointmentDate || !this.timeSlot?.trim()) {
      return null;
    }
    return validateAppointmentScheduling(this.appointmentDate, this.timeSlot);
  }

  submit(): void {
    this.errorMessage = '';
    if (this.selectedDoctorId == null || this.selectedDoctorId < 1) {
      this.errorMessage = 'Choose a doctor.';
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
      .patientRequest({
        doctorUserId: this.selectedDoctorId,
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
          this.errorMessage = err.error?.message || err.error?.error || 'Could not book appointment';
          this.submitting = false;
        }
      });
  }

  cancel(): void {
    const dest = this.router.url.startsWith('/admin') ? '/admin/appointments' : '/appointments';
    this.router.navigateByUrl(dest);
  }
}
