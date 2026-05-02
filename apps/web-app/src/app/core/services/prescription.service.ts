import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { PageResponseDTO } from '../../models/page-response.model';
import { Prescription, PrescriptionRequest } from '../../models/prescription.model';
import { PageQuery } from '../../models/page-query.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private readonly base = environment.medicalApiUrl;

  constructor(private readonly http: HttpClient) {}

  getAllPrescriptions(query: PageQuery = {}): Observable<PageResponseDTO<Prescription>> {
    const params = this.buildPageParams(query);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<PageResponseDTO<Prescription>>>(`${this.base}/prescriptions`, { params })
    );
  }

  getPrescriptionById(id: number): Observable<Prescription> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Prescription>>(`${this.base}/prescriptions/${id}`)
    );
  }

  getPrescriptionsByRecordId(recordId: number): Observable<Prescription[]> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Prescription[]>>(`${this.base}/prescriptions/record/${recordId}`)
    );
  }

  createPrescription(body: PrescriptionRequest): Observable<Prescription> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<Prescription>>(`${this.base}/prescriptions`, body)
    );
  }

  updatePrescription(id: number, body: PrescriptionRequest): Observable<Prescription> {
    return unwrapApiResponse(
      this.http.put<ApiResponseDTO<Prescription>>(`${this.base}/prescriptions/${id}`, body)
    );
  }

  deletePrescription(id: number): Observable<void> {
    return this.http.delete<ApiResponseDTO<unknown>>(`${this.base}/prescriptions/${id}`).pipe(map(() => undefined));
  }

  searchPrescriptions(medicationName?: string, status?: string): Observable<Prescription[]> {
    let params = new HttpParams();
    if (medicationName) params = params.set('medicationName', medicationName);
    if (status) params = params.set('status', status);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Prescription[]>>(`${this.base}/prescriptions/search`, { params })
    );
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/prescriptions/${id}/pdf`, { responseType: 'blob' });
  }

  // ── Keywords Complexes: Prescriptions critiques ──
  getCriticalPrescriptions(patientId: number): Observable<Prescription[]> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Prescription[]>>(`${this.base}/prescriptions/patient/${patientId}/critical`)
    );
  }

  // ── AI Drug Recommendation ──
  recommendDrugs(diagnosis: string): Observable<{recommended_drugs: string[], error?: string}> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<{recommended_drugs: string[], error?: string}>>(`${this.base}/records/ai-prescriptions/recommend-drugs`, { diagnosis })
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
