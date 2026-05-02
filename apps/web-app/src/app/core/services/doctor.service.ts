import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DoctorInfo, PatientInfo, DoctorResponse } from '../../shared/models/doctor.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class DoctorService {

  private readonly API_URL = `${environment.apiUrl}/doctors`;
  private readonly MONITORING_URL = `${environment.monitoringUrl}/monitoring/doctors`;

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  /**
   * Get all doctors.
   */
  getDoctors(): Observable<DoctorResponse[]> {
    return this.http.get<DoctorResponse[]>(this.API_URL);
  }

  /**
   * Get a doctor by ID.
   */
  getDoctorById(id: number): Observable<DoctorResponse> {
    return this.http.get<DoctorResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * Verify a doctor.
   */
  verifyDoctor(doctorId: number): Observable<any> {
    return this.http.put(`${this.API_URL}/VerifyDoctor/${doctorId}`, {});
  }

  /**
   * Delete a doctor.
   */
  deleteDoctor(doctorId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${doctorId}`);
  }

  /**
   * Update doctor information with file upload support.
   */
  updateDoctor(doctorId: number, formData: FormData): Observable<any> {
    return this.http.put(
      `${this.API_URL}/${doctorId}`,
      formData,
      { headers: { Authorization: `Bearer ${this.authService.getToken()}` } }
    );
  }

  /**
   * Get all patients assigned to a doctor (doctor sees names, not IDs).
   */
  getPatientsForDoctor(doctorId: number): Observable<PatientInfo[]> {
    return this.http.get<PatientInfo[]>(`${this.MONITORING_URL}/${doctorId}/patients`);
  }

  /**
   * Get the doctor responsible for a patient.
   */
  getDoctorForPatient(patientId: number): Observable<DoctorInfo> {
    return this.http.get<DoctorInfo>(`${this.MONITORING_URL}/patients/${patientId}/doctor`);
  }

  /**
   * Assign/reassign doctor to patient (optional helper for future UI flows).
   */
  assignDoctorToPatient(doctorId: number, patientId: number): Observable<PatientInfo> {
    return this.http.post<PatientInfo>(`${this.MONITORING_URL}/${doctorId}/patients/${patientId}`, {});
  }
}
