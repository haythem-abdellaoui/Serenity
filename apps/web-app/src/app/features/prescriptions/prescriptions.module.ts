import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { PrescriptionListComponent } from './prescription-list/prescription-list.component';
import { PrescriptionFormComponent } from './prescription-form/prescription-form.component';

const routes: Routes = [
  { path: '', component: PrescriptionListComponent },
  { path: 'new', component: PrescriptionFormComponent },
  { path: ':id/edit', component: PrescriptionFormComponent }
];

@NgModule({
  declarations: [PrescriptionListComponent, PrescriptionFormComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PrescriptionsModule {}
