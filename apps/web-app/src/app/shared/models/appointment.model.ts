/** Busy slots for calendar (doctor + patient sources). */
export interface CalendarBusySlot {
  appointmentDate: string;
  timeSlot: string;
  source: 'DOCTOR' | 'PATIENT';
}

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
export type AppointmentType = 'IN_PERSON' | 'TELECONSULTATION';
export type TeleconsultationStatus = 'SCHEDULED' | 'LIVE' | 'COMPLETED' | 'CANCELLED';

export interface TeleconsultationResponse {
  id: number;
  meetingUrl: string;
  durationMinutes: number;
  startTime: string | null;
  endTime: string | null;
  recordingUrl: string | null;
  status: TeleconsultationStatus;
}

export interface AppointmentResponse {
  id: number;
  patientUserId: number;
  doctorUserId: number;
  patientFirstName?: string | null;
  patientLastName?: string | null;
  doctorFirstName?: string | null;
  doctorLastName?: string | null;
  appointmentDate: string;
  timeSlot: string;
  status: AppointmentStatus;
  type: AppointmentType;
  notes: string | null;
  createdAt: string;
  teleconsultation: TeleconsultationResponse | null;
}

export interface CreateAppointmentPatientRequest {
  doctorUserId: number;
  appointmentDate: string;
  timeSlot: string;
  type: AppointmentType;
  notes?: string;
}

export interface CreateAppointmentDoctorRequest {
  patientUserId: number;
  appointmentDate: string;
  timeSlot: string;
  type: AppointmentType;
  notes?: string;
}

/** Move an existing booking to a new date/time (same doctor & patient; overlap rules apply). */
export interface RescheduleAppointmentRequest {
  appointmentDate: string;
  timeSlot: string;
}

export interface AppointmentNotification {
  id: number;
  userId: number;
  appointmentId: number;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface AppointmentUnreadCountResponse {
  unreadCount: number;
}

/** Navbar bell: merged insurance + appointment notifications. */
export interface NavbarNotification {
  source: 'insurance' | 'appointment';
  id: number;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  claimId?: number | null;
  appointmentId?: number | null;
}

export function appointmentParticipantDisplayName(
  a: AppointmentResponse,
  role: 'patient' | 'doctor'
): string {
  const fn = role === 'patient' ? a.patientFirstName : a.doctorFirstName;
  const ln = role === 'patient' ? a.patientLastName : a.doctorLastName;
  const parts = [fn, ln].filter((x): x is string => typeof x === 'string' && x.trim().length > 0);
  return parts.length > 0 ? parts.join(' ') : '—';
}
