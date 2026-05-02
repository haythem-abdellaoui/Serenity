import { AppointmentResponse, CalendarBusySlot } from '../models/appointment.model';

/** Default visit length used for end-time display and validation (must match backend). */
export const APPOINTMENT_SLOT_DURATION_MINUTES = 90;

/**
 * Normalize API date values (string ISO, Jackson [y,m,d] arrays, or {year,month,day}) to YYYY-MM-DD.
 */
export function appointmentDateToYmd(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  if (typeof value === 'string') {
    return value.length >= 10 ? value.slice(0, 10) : null;
  }
  if (Array.isArray(value) && value.length >= 3) {
    const y = Number(value[0]);
    const mo = Number(value[1]);
    const d = Number(value[2]);
    if (!Number.isFinite(y) || !Number.isFinite(mo) || !Number.isFinite(d)) {
      return null;
    }
    return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }
  if (typeof value === 'object' && value !== null) {
    const o = value as Record<string, unknown>;
    if ('year' in o && 'month' in o && 'day' in o) {
      const y = Number(o['year']);
      const mo = Number(o['month']);
      const d = Number(o['day']);
      if (!Number.isFinite(y) || !Number.isFinite(mo) || !Number.isFinite(d)) {
        return null;
      }
      return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    }
  }
  return null;
}

export function normalizeTimeHhMm(t: string): string {
  const p = t.trim().split(':');
  if (p.length !== 2) {
    return t.trim();
  }
  return `${p[0].padStart(2, '0')}:${p[1].padStart(2, '0')}`;
}

/** True if [aStart,aEnd) overlaps [bStart,bEnd) — half-open or closed intervals; using minute-of-day. */
export function intervalsOverlapMinutes(aStart: number, aEnd: number, bStart: number, bEnd: number): boolean {
  return aStart < bEnd && bStart < aEnd;
}

/**
 * True if proposed booking overlaps any busy slot on the same calendar day (same duration for all).
 */
export function hasAppointmentTimeOverlap(
  appointmentDateYmd: string,
  proposedTimeSlot: string,
  busySlots: CalendarBusySlot[] | undefined,
  durationMinutes: number = APPOINTMENT_SLOT_DURATION_MINUTES
): boolean {
  const dateKey = appointmentDateYmd.slice(0, 10);
  const newStart = parseTimeToMinutes(normalizeTimeHhMm(proposedTimeSlot));
  if (newStart == null) {
    return false;
  }
  const newEnd = newStart + durationMinutes;
  for (const s of busySlots || []) {
    const d = appointmentDateToYmd(s.appointmentDate);
    if (d !== dateKey) {
      continue;
    }
    const ex = parseTimeToMinutes(normalizeTimeHhMm(s.timeSlot));
    if (ex == null) {
      continue;
    }
    const exEnd = ex + durationMinutes;
    if (intervalsOverlapMinutes(newStart, newEnd, ex, exEnd)) {
      return true;
    }
  }
  return false;
}

/** Clinic window: first allowed start (minutes from midnight). */
export const WORK_DAY_START_MINUTES = 8 * 60;

/**
 * Last instant of the work day (minutes from midnight): 20:00 exclusive upper bound for *end* of visit.
 * So last allowed start = 20:00 - duration.
 */
export const WORK_DAY_END_MINUTES = 20 * 60;

export function parseTimeToMinutes(t: string): number | null {
  const p = t.trim().split(':');
  if (p.length !== 2) {
    return null;
  }
  const h = parseInt(p[0], 10);
  const m = parseInt(p[1], 10);
  if (Number.isNaN(h) || Number.isNaN(m) || h < 0 || h > 23 || m < 0 || m > 59) {
    return null;
  }
  return h * 60 + m;
}

export function formatSlotRange(timeSlot: string, durationMinutes: number = APPOINTMENT_SLOT_DURATION_MINUTES): string {
  const startMin = parseTimeToMinutes(normalizeTimeHhMm(timeSlot));
  if (startMin == null) {
    return timeSlot.trim();
  }
  const start = new Date(2000, 0, 1, Math.floor(startMin / 60), startMin % 60, 0, 0);
  const end = new Date(start.getTime() + durationMinutes * 60_000);
  const tf = (d: Date): string =>
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  return `${tf(start)}–${tf(end)}`;
}

export function normalizeTimeSortKey(t: string): string {
  const m = parseTimeToMinutes(normalizeTimeHhMm(t));
  if (m == null) {
    return t.trim();
  }
  const h = Math.floor(m / 60);
  const min = m % 60;
  return `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;
}

export function slotsForDate(slots: CalendarBusySlot[] | undefined, dateYmd: string): CalendarBusySlot[] {
  if (!dateYmd || dateYmd.length < 10) {
    return [];
  }
  const key = dateYmd.slice(0, 10);
  const filtered = [...(slots || [])].filter((s) => appointmentDateToYmd(s.appointmentDate) === key);
  filtered.sort((a, b) => normalizeTimeSortKey(a.timeSlot).localeCompare(normalizeTimeSortKey(b.timeSlot)));
  const seenTimes = new Set<string>();
  const deduped: CalendarBusySlot[] = [];
  for (const s of filtered) {
    const tk = normalizeTimeSortKey(s.timeSlot);
    if (seenTimes.has(tk)) {
      continue;
    }
    seenTimes.add(tk);
    deduped.push(s);
  }
  return deduped;
}

function localYmd(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/**
 * Returns an error message, or null if valid.
 */
export function validateAppointmentScheduling(
  appointmentDateYmd: string,
  timeSlotRaw: string,
  now: Date = new Date(),
  durationMinutes: number = APPOINTMENT_SLOT_DURATION_MINUTES
): string | null {
  const timeSlot = timeSlotRaw?.trim() ?? '';
  if (!appointmentDateYmd || appointmentDateYmd.length < 10) {
    return 'Choose a date.';
  }
  const dateStr = appointmentDateYmd.slice(0, 10);
  const start = parseTimeToMinutes(normalizeTimeHhMm(timeSlot));
  if (start == null) {
    return 'Enter a valid time (HH:mm, 24h).';
  }
  const end = start + durationMinutes;
  if (start < WORK_DAY_START_MINUTES) {
    return 'Appointments cannot start before 08:00.';
  }
  if (end > WORK_DAY_END_MINUTES) {
    return 'Appointments must finish by 20:00 (latest start is 18:30 for a 1h30 visit).';
  }
  const today = localYmd(now);
  if (dateStr < today) {
    return 'Cannot schedule on a past date.';
  }
  if (dateStr === today) {
    const nowMin = now.getHours() * 60 + now.getMinutes();
    if (start <= nowMin) {
      return 'Choose a time later than the current time.';
    }
  }
  return null;
}

export function minDateInputYmd(now: Date = new Date()): string {
  return localYmd(now);
}

/** Local start instant for an appointment row (date + time slot). */
export function parseAppointmentLocalStart(
  appointmentDate: unknown,
  timeSlot: string | null | undefined,
  defaultHour = 9
): Date | null {
  const ymd = appointmentDateToYmd(appointmentDate);
  if (!ymd) {
    return null;
  }
  const raw = (timeSlot && String(timeSlot).trim()) || `${String(defaultHour).padStart(2, '0')}:00`;
  const t = normalizeTimeHhMm(raw);
  const p = t.split(':');
  const hh = parseInt(p[0], 10);
  const mm = parseInt(p[1], 10);
  if (Number.isNaN(hh) || Number.isNaN(mm)) {
    return null;
  }
  const [y, mo, d] = ymd.split('-').map(Number);
  return new Date(y, mo - 1, d, hh, mm, 0, 0);
}

/** Milliseconds timestamp for appointment start in local time (0 when invalid). */
export function appointmentResponseStartMs(a: AppointmentResponse, defaultHour = 9): number {
  const start = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot, defaultHour);
  return start ? start.getTime() : 0;
}

/** Human countdown label from remaining milliseconds. */
export function formatCountdownMs(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) {
    return 'Starting now';
  }

  const totalSeconds = Math.floor(ms / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (days > 0) {
    return `${days}d ${hours}h ${minutes}m`;
  }
  if (hours > 0) {
    return `${hours}h ${minutes}m ${seconds}s`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  }
  return `${seconds}s`;
}

/**
 * Earliest upcoming or in-progress visit (pending/confirmed, end time still in the future).
 */
export function getNextRelevantAppointment(
  appointments: AppointmentResponse[],
  now: Date = new Date(),
  durationMinutes: number = APPOINTMENT_SLOT_DURATION_MINUTES
): AppointmentResponse | null {
  const rows = appointments
    .filter((a) => a.status === 'PENDING' || a.status === 'CONFIRMED')
    .map((a) => {
      const start = parseAppointmentLocalStart(a.appointmentDate, a.timeSlot);
      if (!start) {
        return null;
      }
      const endMs = start.getTime() + durationMinutes * 60_000;
      return { a, start, endMs };
    })
    .filter((x): x is { a: AppointmentResponse; start: Date; endMs: number } => x != null && x.endMs > now.getTime())
    .sort((x, y) => x.start.getTime() - y.start.getTime());
  return rows[0]?.a ?? null;
}
