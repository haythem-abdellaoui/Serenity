import { Component, OnInit } from '@angular/core';
import { DoctorService } from '../../../core/services/doctor.service';
import { AuthService } from '../../../core/services/auth.service';
import { PatientInfo } from '../../../shared/models/doctor.model';

@Component({
  selector: 'app-doctor-patients',
  templateUrl: './doctor-patients.component.html',
  styleUrls: ['./doctor-patients.component.scss']
})
export class DoctorPatientsComponent implements OnInit {

  patients: PatientInfo[] = [];
  loading = true;
  errorMessage = '';
  emptyState = false;

  constructor(
    private readonly doctorService: DoctorService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'Doctor not logged in.';
      this.loading = false;
      return;
    }

    this.doctorService.getPatientsForDoctor(currentUser.userId).subscribe({
      next: (patients) => {
        this.patients = patients;
        this.emptyState = patients.length === 0;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Could not load patients.';
        this.loading = false;
      }
    });
  }
}
