import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DoctorRoutingModule } from './doctor-routing.module';
import { DoctorPatientsComponent } from './doctor-patients/doctor-patients.component';

@NgModule({
  declarations: [DoctorPatientsComponent],
  imports: [CommonModule, RouterModule, DoctorRoutingModule]
})
export class DoctorModule {}
