import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
  CrisisAlertPayload,
  DoctorRealtimeNotification,
  WeeklyDoctorDigestPayload
} from '../../shared/models/mood.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CrisisAlertService {

  private readonly API_URL = 'http://localhost:8085/api/monitoring/alerts/stream';

  private eventSource: EventSource | null = null;
  private connectedDoctorId: number | null = null;
  private readonly seenAlertKeys = new Set<string>();

  private readonly alertsSubject = new BehaviorSubject<DoctorRealtimeNotification[]>([]);
  readonly alerts$: Observable<DoctorRealtimeNotification[]> = this.alertsSubject.asObservable();

  private readonly newAlertSubject = new Subject<DoctorRealtimeNotification>();
  readonly newAlert$: Observable<DoctorRealtimeNotification> = this.newAlertSubject.asObservable();

  constructor(
    private readonly authService: AuthService,
    private readonly ngZone: NgZone
  ) {
    this.authService.onLogout(() => this.disconnect());
  }

  connect(doctorId: number): void {
    if (!doctorId) {
      return;
    }

    if (this.eventSource && this.connectedDoctorId === doctorId) {
      return;
    }

    this.disconnect();

    const token = this.authService.getToken();
    const streamUrl = token
      ? `${this.API_URL}/${doctorId}?token=${encodeURIComponent(token)}`
      : `${this.API_URL}/${doctorId}`;

    this.eventSource = new EventSource(streamUrl);
    this.connectedDoctorId = doctorId;

    this.eventSource.onopen = () => {
      console.info('[SSE] Connected for doctorId=', doctorId);
    };

    this.eventSource.addEventListener('connected', () => {
      console.info('[SSE] Handshake event received for doctorId=', doctorId);
    });

    this.eventSource.addEventListener('crisis-alert', (event: Event) => {
      this.processIncomingEvent(event, doctorId, 'crisis-alert');
    });

    this.eventSource.addEventListener('doctor-weekly-digest', (event: Event) => {
      this.processIncomingEvent(event, doctorId, 'doctor-weekly-digest');
    });

    // Fallback for servers that emit unnamed SSE messages.
    this.eventSource.onmessage = (event: MessageEvent) => {
      this.processIncomingEvent(event, doctorId, 'message');
    };

    this.eventSource.onerror = () => {
      console.warn('[SSE] Connection error for doctorId=', doctorId);
    };
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.connectedDoctorId = null;
    this.seenAlertKeys.clear();
  }

  clearAlerts(): void {
    this.alertsSubject.next([]);
  }

  private processIncomingEvent(
    event: Event,
    doctorId: number,
    channel: 'crisis-alert' | 'doctor-weekly-digest' | 'message'
  ): void {
    this.ngZone.run(() => {
      const data = (event as MessageEvent).data;
      const notification = this.parseNotification(data, channel);
      if (!notification) {
        return;
      }
      if (this.connectedDoctorId !== null && notification.doctorId !== this.connectedDoctorId) {
        console.warn('[SSE] Ignoring alert for mismatched doctorId', notification.doctorId, 'expected', this.connectedDoctorId);
        return;
      }

      const key = this.buildAlertKey(notification);
      if (this.seenAlertKeys.has(key)) {
        return;
      }
      this.seenAlertKeys.add(key);

      const current = this.alertsSubject.value;
      this.alertsSubject.next([notification, ...current]);
      this.newAlertSubject.next(notification);
      console.info('[SSE] Notification received via', channel, 'for doctorId=', doctorId, notification);
    });
  }

  private parseNotification(
    raw: unknown,
    channel: 'crisis-alert' | 'doctor-weekly-digest' | 'message'
  ): DoctorRealtimeNotification | null {
    try {
      if (!raw) {
        return null;
      }

      let parsed: unknown = raw;
      if (typeof raw === 'string') {
        parsed = JSON.parse(raw);
      }

      if (!parsed || typeof parsed !== 'object') {
        return null;
      }

      const payload = parsed as Record<string, unknown>;
      if (this.isCrisisPayload(payload)) {
        return {
          type: 'CRISIS',
          doctorId: payload.doctorId,
          patientId: payload.patientId,
          patientFullName: payload.patientFullName,
          moodLevel: payload.moodLevel,
          message: payload.message,
          timestamp: payload.timestamp
        };
      }

      if (this.isWeeklyDigestPayload(payload)) {
        return {
          type: 'WEEKLY_DIGEST',
          doctorId: payload.doctorId,
          message: payload.summaryMessage,
          timestamp: payload.generatedAt,
          weekStartDate: payload.weekStartDate,
          weekEndDate: payload.weekEndDate,
          crisisCount: payload.crisisCount,
          worseningPatients: payload.worseningPatients,
          noCheckinPatients: payload.noCheckinPatients
        };
      }

      if (channel === 'doctor-weekly-digest') {
        console.warn('[SSE] Digest payload shape mismatch:', parsed);
      }

      return null;
    } catch {
      console.warn('[SSE] Unable to parse SSE payload:', raw);
      return null;
    }
  }

  private isCrisisPayload(payload: any): payload is CrisisAlertPayload {
    return !!payload
      && typeof payload.doctorId === 'number'
      && typeof payload.patientId === 'number'
      && typeof payload.moodLevel === 'number'
      && typeof payload.message === 'string'
      && typeof payload.timestamp === 'string';
  }

  private isWeeklyDigestPayload(payload: any): payload is WeeklyDoctorDigestPayload {
    return !!payload
      && typeof payload.doctorId === 'number'
      && typeof payload.summaryMessage === 'string'
      && typeof payload.generatedAt === 'string'
      && typeof payload.weekStartDate === 'string'
      && typeof payload.weekEndDate === 'string';
  }

  private buildAlertKey(notification: DoctorRealtimeNotification): string {
    if (notification.type === 'CRISIS') {
      return [
        notification.type,
        notification.doctorId,
        notification.patientId,
        notification.moodLevel,
        notification.timestamp
      ].join('|');
    }

    return [
      notification.type,
      notification.doctorId,
      notification.weekStartDate,
      notification.weekEndDate,
      notification.timestamp
    ].join('|');
  }
}

