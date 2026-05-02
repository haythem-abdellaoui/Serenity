import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { DashboardStats } from '../../models/dashboard.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly base = environment.medicalApiUrl;

  constructor(private readonly http: HttpClient) {}

  getStats(): Observable<DashboardStats> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<DashboardStats>>(`${this.base}/dashboard`)
    );
  }
}
