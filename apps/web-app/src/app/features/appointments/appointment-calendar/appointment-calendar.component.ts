import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CalendarBusySlot } from '../../../shared/models/appointment.model';
import {
  appointmentDateToYmd,
  formatSlotRange as formatSlotRangeUtil,
  normalizeTimeSortKey
} from '../../../shared/utils/appointment-scheduling.utils';

export interface CalendarDaySlotLine {
  source: 'DOCTOR' | 'PATIENT';
  /** e.g. "09:00–10:30" */
  rangeLabel: string;
}

export interface CalendarDayCell {
  dateStr: string;
  inMonth: boolean;
  dayNum: number;
  doctorBusy: boolean;
  patientBusy: boolean;
  /** Shown inside the day cell (rest in detail / +N). */
  slotLines: CalendarDaySlotLine[];
  slotOverflow: number;
}

@Component({
  selector: 'app-appointment-calendar',
  templateUrl: './appointment-calendar.component.html',
  styleUrls: ['./appointment-calendar.component.scss']
})
export class AppointmentCalendarComponent implements OnChanges {
  @Input() busySlots: CalendarBusySlot[] = [];
  @Input() selectedDate = '';
  /** 1–12 */
  @Input() viewMonth = new Date().getMonth() + 1;
  @Input() viewYear = new Date().getFullYear();
  @Input() legendDoctor = 'Doctor already has a visit';
  /** If empty, the second legend row is hidden (e.g. hub view shows only your bookings). */
  @Input() legendPatient = 'Patient / you already booked elsewhere';
  @Input() showDurationNote = true;
  /** Block length used to show end time (e.g. 90 → 1h 30min). */
  @Input() slotDurationMinutes = 90;
  /** Max time rows shown inside each day cell before "+N". */
  @Input() maxSlotLinesInCell = 3;
  /** If false, day cells show only the day number (busy days still tinted). */
  @Input() showTimeChipsInCells = false;

  @Output() selectedDateChange = new EventEmitter<string>();
  @Output() viewMonthChange = new EventEmitter<{ year: number; month: number }>();

  weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  grid: CalendarDayCell[] = [];

  get monthTitle(): string {
    return new Date(this.viewYear, this.viewMonth - 1, 1).toLocaleString(undefined, {
      month: 'long',
      year: 'numeric'
    });
  }

  /** Human label for legend (default 90 → "1h 30min"). */
  get slotDurationLabel(): string {
    const m = this.slotDurationMinutes;
    const h = Math.floor(m / 60);
    const min = m % 60;
    if (h === 0) {
      return `${min} min`;
    }
    if (min === 0) {
      return h === 1 ? '1 hour' : `${h} hours`;
    }
    return `${h}h ${min}min`;
  }

  private doctorDays = new Set<string>();
  private patientDays = new Set<string>();
  private slotsByDate = new Map<string, CalendarBusySlot[]>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['busySlots']) {
      this.doctorDays.clear();
      this.patientDays.clear();
      this.slotsByDate.clear();
      for (const s of this.busySlots || []) {
        const d = this.normalizeDateKey(s.appointmentDate);
        if (!d) {
          continue;
        }
        if (s.source === 'DOCTOR') {
          this.doctorDays.add(d);
        } else {
          this.patientDays.add(d);
        }
        if (!this.slotsByDate.has(d)) {
          this.slotsByDate.set(d, []);
        }
        this.slotsByDate.get(d)!.push(s);
      }
      for (const arr of this.slotsByDate.values()) {
        arr.sort((a, b) => normalizeTimeSortKey(a.timeSlot).localeCompare(normalizeTimeSortKey(b.timeSlot)));
      }
    }
    if (
      changes['busySlots'] ||
      changes['viewMonth'] ||
      changes['viewYear'] ||
      changes['slotDurationMinutes'] ||
      changes['maxSlotLinesInCell'] ||
      changes['showTimeChipsInCells']
    ) {
      this.buildGrid();
    }
  }

  prevMonth(): void {
    let m = this.viewMonth - 1;
    let y = this.viewYear;
    if (m < 1) {
      m = 12;
      y -= 1;
    }
    this.viewMonthChange.emit({ year: y, month: m });
  }

  nextMonth(): void {
    let m = this.viewMonth + 1;
    let y = this.viewYear;
    if (m > 12) {
      m = 1;
      y += 1;
    }
    this.viewMonthChange.emit({ year: y, month: m });
  }

  pickDay(cell: CalendarDayCell): void {
    if (!cell.inMonth) {
      return;
    }
    this.selectedDateChange.emit(cell.dateStr);
  }

  isSelected(cell: CalendarDayCell): boolean {
    return !!this.selectedDate && cell.dateStr === this.selectedDate;
  }

  formatSlotRange(timeSlot: string): string {
    return formatSlotRangeUtil(timeSlot, this.slotDurationMinutes);
  }

  private normalizeDateKey(v: string | unknown): string | null {
    return appointmentDateToYmd(v);
  }

  private buildGrid(): void {
    const y = this.viewYear;
    const m = this.viewMonth;
    const first = new Date(y, m - 1, 1);
    const startPad = (first.getDay() + 6) % 7;
    const daysInMonth = new Date(y, m, 0).getDate();
    const cells: CalendarDayCell[] = [];

    const pad = (n: number): string => String(n).padStart(2, '0');
    const toLocalYmd = (date: Date): string =>
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

    const pushCell = (date: Date, inMonth: boolean): void => {
      const dateStr = toLocalYmd(date);
      const dk = dateStr;
      const raw = this.slotsByDate.get(dk) || [];
      const allLines: CalendarDaySlotLine[] = this.showTimeChipsInCells
        ? raw.map((s) => ({
            source: s.source,
            rangeLabel: this.formatSlotRange(s.timeSlot)
          }))
        : [];
      const max = Math.max(1, this.maxSlotLinesInCell);
      const slotLines = this.showTimeChipsInCells ? allLines.slice(0, max) : [];
      const slotOverflow = this.showTimeChipsInCells ? Math.max(0, allLines.length - max) : 0;
      cells.push({
        dateStr: dk,
        inMonth,
        dayNum: date.getDate(),
        doctorBusy: this.doctorDays.has(dk),
        patientBusy: this.patientDays.has(dk),
        slotLines,
        slotOverflow
      });
    };

    for (let i = 0; i < startPad; i++) {
      const d = new Date(y, m - 1, 1 - (startPad - i));
      pushCell(d, false);
    }
    for (let day = 1; day <= daysInMonth; day++) {
      pushCell(new Date(y, m - 1, day), true);
    }
    let trailing = 1;
    while (cells.length < 42) {
      pushCell(new Date(y, m - 1, daysInMonth + trailing), false);
      trailing++;
    }
    this.grid = cells;
  }
}
