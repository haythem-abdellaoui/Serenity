import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { Medicine, MedicineRequest, OpenFDAMedicine } from '../../models/medicine.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class MedicineService {
  private readonly base = environment.medicalApiUrl;

  constructor(private readonly http: HttpClient) {}

  getAll(name?: string): Observable<Medicine[]> {
    let params = new HttpParams();
    if (name) params = params.set('name', name);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Medicine[]>>(`${this.base}/medicines`, { params })
    );
  }

  getById(id: number): Observable<Medicine> {
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<Medicine>>(`${this.base}/medicines/${id}`)
    );
  }

  create(body: MedicineRequest): Observable<Medicine> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<Medicine>>(`${this.base}/medicines`, body)
    );
  }

  update(id: number, body: MedicineRequest): Observable<Medicine> {
    return unwrapApiResponse(
      this.http.put<ApiResponseDTO<Medicine>>(`${this.base}/medicines/${id}`, body)
    );
  }

  delete(id: number): Observable<void> {
    return unwrapApiResponse(
      this.http.delete<ApiResponseDTO<void>>(`${this.base}/medicines/${id}`)
    );
  }

  searchOpenFda(query: string): Observable<OpenFDAMedicine[]> {
    let params = new HttpParams().set('query', query);
    return unwrapApiResponse(
      this.http.get<ApiResponseDTO<OpenFDAMedicine[]>>(`${this.base}/medicines/external-search`, { params })
    );
  }

  getOrCreate(body: MedicineRequest): Observable<Medicine> {
    return unwrapApiResponse(
      this.http.post<ApiResponseDTO<Medicine>>(`${this.base}/medicines/get-or-create`, body)
    );
  }
}
