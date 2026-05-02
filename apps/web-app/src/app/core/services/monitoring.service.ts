import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  EmotionalTriggerRequest,
  EmotionalTriggerResponse,
  MoodEntry,
  MoodEntryRequest,
  MoodEntryResponse
} from '../../shared/models/mood.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class MonitoringService {

  private readonly API_URL = 'http://localhost:8085/api/monitoring/mood';
  private readonly TRIGGER_API_URL = 'http://localhost:8085/api/monitoring/triggers';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  /**
   * Create a new mood entry
   */
  createMoodEntry(request: MoodEntryRequest): Observable<MoodEntryResponse> {
    return this.http.post<MoodEntryResponse>(this.API_URL, request);
  }

  /**
   * Get all mood entries for the logged-in user (patient)
   */
  getMoodEntries(patientId: number): Observable<MoodEntryResponse[]> {
    return this.http.get<MoodEntryResponse[]>(`${this.API_URL}?patientId=${patientId}`);
  }

  /**
   * Get all mood entries assigned to a doctor.
   */
  getMoodEntriesForDoctor(doctorId: number): Observable<MoodEntryResponse[]> {
    return this.http.get<MoodEntryResponse[]>(`${this.API_URL}/doctor/${doctorId}`);
  }

  /**
   * Get a specific mood entry by ID
   */
  getMoodEntryById(id: number): Observable<MoodEntryResponse> {
    return this.http.get<MoodEntryResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * Update an existing mood entry
   */
  updateMoodEntry(id: number, request: MoodEntryRequest): Observable<MoodEntryResponse> {
    return this.http.put<MoodEntryResponse>(`${this.API_URL}/${id}`, request);
  }

  /**
   * Delete a mood entry
   */
  deleteMoodEntry(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  /**
   * Export one patient's full mental health record as a PDF for the assigned doctor.
   */
  exportPatientRecordPdf(doctorId: number, patientId: number): Observable<Blob> {
    const token = this.authService.getToken();
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : undefined;

    return this.http.get(`${this.API_URL}/doctor/${doctorId}/patient/${patientId}/record-pdf`, {
      headers,
      responseType: 'blob'
    });
  }

  /**
   * List emotional triggers for one mood entry.
   */
  getTriggersByMoodEntryId(moodEntryId: number): Observable<EmotionalTriggerResponse[]> {
    return this.http
      .get<ApiEnvelope<EmotionalTriggerResponse[]>>(`${this.API_URL}/${moodEntryId}/triggers`)
      .pipe(map(res => res.data));
  }

  /**
   * Create emotional trigger for a mood entry.
   */
  createTrigger(moodEntryId: number, request: EmotionalTriggerRequest): Observable<EmotionalTriggerResponse> {
    return this.http
      .post<ApiEnvelope<EmotionalTriggerResponse>>(`${this.API_URL}/${moodEntryId}/triggers`, request)
      .pipe(map(res => res.data));
  }

  /**
   * Update emotional trigger.
   */
  updateTrigger(id: number, request: EmotionalTriggerRequest): Observable<EmotionalTriggerResponse> {
    return this.http
      .put<ApiEnvelope<EmotionalTriggerResponse>>(`${this.TRIGGER_API_URL}/${id}`, request)
      .pipe(map(res => res.data));
  }

  /**
   * Delete emotional trigger.
   */
  deleteTrigger(id: number): Observable<void> {
    return this.http
      .delete<ApiEnvelope<void>>(`${this.TRIGGER_API_URL}/${id}`)
      .pipe(map(() => undefined));
  }
}

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
