import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { PatientListComponent } from './patient-list/patient-list.component';
import { PatientDetailComponent } from './patient-detail/patient-detail.component';

const routes: Routes = [
  { path: '', component: PatientListComponent },
  {
    path: ':patientId/records',
    loadChildren: () => import('../records/records.module').then((m) => m.RecordsModule)
  },
  { path: ':id', component: PatientDetailComponent }
];

@NgModule({
  declarations: [PatientListComponent, PatientDetailComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PatientsModule {}
