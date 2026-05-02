import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InsuranceClaimRequest,
  InsuranceClaimResponse,
  InsuranceClaimTransition,
  ClaimRiskScoreResponse,
  ClaimRemittanceOcrSummary,
  InsuranceNotification,
  NotificationUnreadCountResponse
} from '../../shared/models/insurance.model';
import { PageResponseDTO } from '../../models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class InsuranceService {

  private readonly API_URL = `${environment.insuranceApiUrl}/insurance`;

  constructor(private readonly http: HttpClient) {}

  submitClaim(request: InsuranceClaimRequest, files: File[]): Observable<InsuranceClaimResponse> {
    const formData = new FormData();
    formData.append('description', request.description);
    formData.append('amount', request.amount.toString());
    formData.append('insuranceCompany', request.insuranceCompany);
    formData.append('insuranceGrade', request.insuranceGrade.toString());
    files.forEach(file => formData.append('files', file));
    return this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims`, formData);
  }

  getMyClaims(filters?: {
    status?: string;
    insuranceCompany?: string;
    fromDate?: string;
    toDate?: string;
    sortBy?: string;
    sortDir?: string;
  }): Observable<InsuranceClaimResponse[]> {
    return this.http.get<InsuranceClaimResponse[]>(`${this.API_URL}/claims/me`, {
      params: this.buildClaimQueryParams(filters)
    });
  }

  getAllClaims(filters?: {
    status?: string;
    insuranceCompany?: string;
    fromDate?: string;
    toDate?: string;
    sortBy?: string;
    sortDir?: string;
  }): Observable<InsuranceClaimResponse[]> {
    return this.http.get<InsuranceClaimResponse[]>(`${this.API_URL}/claims`, {
      params: this.buildClaimQueryParams(filters)
    });
  }

  getMyClaimsPaged(filters?: {
    status?: string;
    insuranceCompany?: string;
    fromDate?: string;
    toDate?: string;
    sortBy?: string;
    sortDir?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponseDTO<InsuranceClaimResponse>> {
    return this.http.get<PageResponseDTO<InsuranceClaimResponse>>(`${this.API_URL}/claims/me/paged`, {
      params: this.buildClaimQueryParams(filters)
    });
  }

  getAllClaimsPaged(filters?: {
    status?: string;
    insuranceCompany?: string;
    fromDate?: string;
    toDate?: string;
    sortBy?: string;
    sortDir?: string;
    userId?: number;
    page?: number;
    size?: number;
  }): Observable<PageResponseDTO<InsuranceClaimResponse>> {
    return this.http.get<PageResponseDTO<InsuranceClaimResponse>>(`${this.API_URL}/claims/paged`, {
      params: this.buildClaimQueryParams(filters)
    });
  }

  getClaimById(id: number): Observable<InsuranceClaimResponse> {
    return this.http.get<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}`);
  }

  approveClaim(id: number, montant: number): Observable<InsuranceClaimResponse> {
    return this.http.patch<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/approve?montant=${montant}`, {});
  }

  rejectClaim(id: number): Observable<InsuranceClaimResponse> {
    return this.http.patch<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/reject`, {});
  }

  requestAdditionalDocuments(id: number, payload: { reason: string; deadline: string }): Observable<InsuranceClaimResponse> {
    return this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/request-documents`, payload);
  }

  submitAdditionalDocuments(
    id: number,
    payload: { message?: string; description?: string; amount?: number; insuranceGrade?: number },
    files: File[]
  ): Observable<InsuranceClaimResponse> {
    const formData = new FormData();
    if (payload.message?.trim()) {
      formData.append('message', payload.message.trim());
    }
    if (payload.description != null) {
      formData.append('description', payload.description);
    }
    if (payload.amount != null) {
      formData.append('amount', String(payload.amount));
    }
    if (payload.insuranceGrade != null) {
      formData.append('insuranceGrade', String(payload.insuranceGrade));
    }
    files.forEach((file) => formData.append('files', file));
    return this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/documents-response`, formData);
  }

  getClaimTimeline(id: number): Observable<InsuranceClaimTransition[]> {
    return this.http.get<InsuranceClaimTransition[]>(`${this.API_URL}/claims/${id}/timeline`);
  }

  getClaimRiskScore(id: number): Observable<ClaimRiskScoreResponse> {
    return this.http.get<ClaimRiskScoreResponse>(`${this.API_URL}/claims/${id}/risk-score`);
  }

  getRemittanceOcrSummaryReport(): Observable<ClaimRemittanceOcrSummary[]> {
    return this.http.get<ClaimRemittanceOcrSummary[]>(`${this.API_URL}/reports/remittance-ocr-summary`);
  }

  sendHeldClaimToPortal(id: number): Observable<InsuranceClaimResponse> {
    return this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/send-to-portal`, {});
  }

  rejectHeldClaim(id: number, reason?: string): Observable<InsuranceClaimResponse> {
    return this.http.post<InsuranceClaimResponse>(`${this.API_URL}/claims/${id}/reject-held`, {
      reason: reason || undefined
    });
  }

  deleteClaim(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/claims/${id}`);
  }

  getMyNotifications(): Observable<InsuranceNotification[]> {
    return this.http.get<InsuranceNotification[]>(`${this.API_URL}/notifications/me`);
  }

  getAllNotificationsForAdmin(): Observable<InsuranceNotification[]> {
    return this.http.get<InsuranceNotification[]>(`${this.API_URL}/notifications`);
  }

  getUnreadNotificationsCount(): Observable<NotificationUnreadCountResponse> {
    return this.http.get<NotificationUnreadCountResponse>(`${this.API_URL}/notifications/me/unread-count`);
  }

  markNotificationAsRead(notificationId: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/notifications/me/${notificationId}/read`, {});
  }

  markAllNotificationsAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/notifications/me/read-all`, {});
  }

  private buildClaimQueryParams(filters?: {
    status?: string;
    insuranceCompany?: string;
    fromDate?: string;
    toDate?: string;
    sortBy?: string;
    sortDir?: string;
    userId?: number;
    page?: number;
    size?: number;
  }): HttpParams {
    let params = new HttpParams();
    if (!filters) return params;

    if (filters.status) params = params.set('status', filters.status);
    if (filters.insuranceCompany) params = params.set('insuranceCompany', filters.insuranceCompany);
    if (filters.fromDate) params = params.set('fromDate', filters.fromDate);
    if (filters.toDate) params = params.set('toDate', filters.toDate);
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.sortDir) params = params.set('sortDir', filters.sortDir);
    if (filters.userId != null) params = params.set('userId', String(filters.userId));
    if (filters.page != null) params = params.set('page', String(filters.page));
    if (filters.size != null) params = params.set('size', String(filters.size));
    return params;
  }
}
