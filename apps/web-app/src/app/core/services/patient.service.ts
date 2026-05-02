import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { PageResponseDTO } from '../../models/page-response.model';
import { PageQuery } from '../../models/page-query.model';
import { Patient, PatientRequest } from '../../models/patient.model';
import { unwrapApiResponse } from '../../shared/utils/api-response.utils';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly base = environment.apiUrl + '/users';

  constructor(private readonly http: HttpClient) {}

  getAllPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.base}/patients`);
  }

  getPatientById(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.base}/${id}`);
  }

  searchPatients(name: string): Observable<Patient[]> {
    const params = new HttpParams().set('q', name);
    return this.http.get<Patient[]>(`${this.base}/search`, { params });
  }
}
