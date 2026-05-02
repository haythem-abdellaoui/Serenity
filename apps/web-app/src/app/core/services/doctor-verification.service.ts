import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DoctorVerification } from '../../shared/models/doctor-verification.model';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
@Injectable({
  providedIn: 'root'
})
export class DoctorVerificationService {
  private readonly API_URL = `${environment.apiUrl}/doctor-verifications`;

  constructor(private readonly http: HttpClient, private readonly authService: AuthService) {}

  getVerificationByDoctorId(doctorId: number): Observable<DoctorVerification | null> {
    return this.http.get<DoctorVerification[]>(`${this.API_URL}/FindByDoctorID/${doctorId}`).pipe(
      map(response => {
        console.log('API Response (raw):', response);
        // API returns an array, so we take the first element or return null
        return Array.isArray(response) && response.length > 0 ? response[0] : null;
      })
    );
  }

  getVerifications(): Observable<DoctorVerification[]> {
    return this.http.get<DoctorVerification[]>(this.API_URL);
  }

  getVerificationById(id: number): Observable<DoctorVerification> {
    return this.http.get<DoctorVerification>(`${this.API_URL}/${id}`);
  }

  approveVerification(verificationId: number, token: string): Observable<any> {
    const headers = { Authorization: `Bearer ${token}` };
    return this.http.put(`${this.API_URL}/Approve/${verificationId}`, {}, { headers });
  }

  rejectVerification(verificationId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/Reject/${verificationId}`, {});
  }

  deleteVerification(verificationId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${verificationId}`);
  }

  updateVerification(id: number, formData: FormData): Observable<DoctorVerification> {
  const token = this.authService.getToken();
  const headers = new HttpHeaders({
    Authorization: `Bearer ${token}`
  });

  return this.http.put<DoctorVerification>(
    `${this.API_URL}/update_verification/${id}`,
    formData,
    { headers }
  );
}

  updateVerificationWithFiles(
    id: number,
    licenseNumber: string,
    nationalId: string,
    cvFile?: File | null,
    diplomaFile?: File | null
  ): Observable<DoctorVerification> {
    const formData = new FormData();
    formData.append('licenseNumber', licenseNumber);
    formData.append('nationalId', nationalId);
    if (cvFile) {
      formData.append('cv', cvFile);
    }
    if (diplomaFile) {
      formData.append('diploma', diplomaFile);
    }

    return this.http.put<DoctorVerification>(
      `${this.API_URL}/update_verification/${id}`,
      formData
    );
  }

  approveContract(token: string): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/approve-contract`, null, { params: { token } });
  }

  getRejected() {
  const token = this.authService.getToken();
  return this.http.get<any[]>(
    `${this.API_URL}/rejected-keywords`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
}
}
