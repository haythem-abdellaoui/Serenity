import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { PageResponseDTO } from '../../models/page-response.model';
import { MedicalRecord, MedicalRecordRequest } from '../../models/medical-record.model';
import { PageQuery } from '../../models/page-query.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class MedicalRecordService {
  private readonly base = environment.medicalApiUrl;

  constructor(private readonly http: HttpClient) {}

  getAllRecords(query: PageQuery = {}): Observable<PageResponseDTO<MedicalRecord>> {
    const params = this.buildPageParams(query);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<PageResponseDTO<MedicalRecord>>>(`${this.base}/records`, { params })
    );
  }

  getRecordById(id: number): Observable<MedicalRecord> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<MedicalRecord>>(`${this.base}/records/${id}`)
    );
  }

  getRecordsByPatientId(patientId: number): Observable<MedicalRecord[]> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<MedicalRecord[]>>(`${this.base}/records/patient/${patientId}`)
    );
  }

  createRecord(body: MedicalRecordRequest): Observable<MedicalRecord> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<MedicalRecord>>(`${this.base}/records`, body)
    );
  }

  updateRecord(id: number, body: MedicalRecordRequest): Observable<MedicalRecord> {
    return unwrapApiResponse(
      this.http.put<ApiResponseDTO<MedicalRecord>>(`${this.base}/records/${id}`, body)
    );
  }

  deleteRecord(id: number): Observable<void> {
    return this.http.delete<ApiResponseDTO<unknown>>(`${this.base}/records/${id}`).pipe(map(() => undefined));
  }

  searchRecords(diagnosis?: string, status?: string, severity?: string): Observable<MedicalRecord[]> {
    let params = new HttpParams();
    if (diagnosis) params = params.set('diagnosis', diagnosis);
    if (status) params = params.set('status', status);
    if (severity) params = params.set('severity', severity);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<MedicalRecord[]>>(`${this.base}/records/search`, { params })
    );
  }

  predictSeverity(diagnosis: string): Observable<{severity: string, confidence: number}> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<{severity: string, confidence: number}>>(
        `${this.base}/records/ai-severity/predict`, 
        { diagnosis }
      )
    );
  }

  // ── JPQL Complexe: Dossiers avec traitement actif ──
  getRecordsWithActiveTreatment(patientId: number): Observable<MedicalRecord[]> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<MedicalRecord[]>>(`${this.base}/records/patient/${patientId}/active-treatments`)
    );
  }

  private buildPageParams(q: PageQuery): HttpParams {
    let p = new HttpParams();
    if (q.page !== undefined) p = p.set('page', String(q.page));
    if (q.size !== undefined) p = p.set('size', String(q.size));
    if (q.sortBy !== undefined) p = p.set('sortBy', q.sortBy);
    if (q.direction !== undefined) p = p.set('direction', q.direction);
    return p;
  }
}
