import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RoleGuard } from '../../core/guards/role.guard';
import { PharmacistDashboardComponent } from './pharmacist-dashboard/pharmacist-dashboard.component';
import { MyPharmacyComponent } from './my-pharmacy/my-pharmacy.component';
import { PrescriptionInboxComponent } from './prescription-inbox';
import { PharmacistPrescriptionDetailsComponent } from './pharmacist-prescription-details/pharmacist-prescription-details.component';
import { StockManagementComponent } from './stock-management/stock-management.component';
import { AddMedicineComponent } from './add-medicine/add-medicine.component';
import { PatientPharmacyComponent } from './patient-pharmacy/patient-pharmacy.component';
import { PatientPrescriptionDetailsComponent } from './patient-prescription-details/patient-prescription-details.component';
import { PharmacyApplicationComponent } from './pharmacy-application/pharmacy-application.component';

const routes: Routes = [
  {
    path: '',
    component: PharmacistDashboardComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'my-pharmacy',
    component: MyPharmacyComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'inbox',
    component: PrescriptionInboxComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'inbox/:id',
    component: PharmacistPrescriptionDetailsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'stock',
    component: StockManagementComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'stock/new',
    component: AddMedicineComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'patient/prescriptions/:id',
    component: PatientPrescriptionDetailsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  },
  {
    path: 'patient',
    component: PatientPharmacyComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  },
  {
    path: 'apply',
    component: PharmacyApplicationComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  }
];

@NgModule({
  declarations: [
    PharmacistDashboardComponent,
    MyPharmacyComponent,
    PrescriptionInboxComponent,
    PharmacistPrescriptionDetailsComponent,
    StockManagementComponent,
    AddMedicineComponent,
    PatientPharmacyComponent,
    PatientPrescriptionDetailsComponent,
    PharmacyApplicationComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PharmacyModule {}
