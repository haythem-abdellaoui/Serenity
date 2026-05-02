import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Patients see the care dashboard at /appointments; doctors and admins see the list.
 */
@Component({
  selector: 'app-appointment-entry',
  template: `
    <app-patient-appointments-hub *ngIf="showPatientHub" />
    <app-appointment-list *ngIf="!showPatientHub" />
  `
})
export class AppointmentEntryComponent {
  constructor(private readonly authService: AuthService) {}

  get showPatientHub(): boolean {
    return (
      this.authService.isPatient() &&
      !this.authService.isDoctor() &&
      !this.authService.isAdmin()
    );
  }
}
