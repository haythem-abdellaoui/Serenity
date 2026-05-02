import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { PharmacyApplicationsComponent } from './pharmacy-applications/pharmacy-applications.component';


const routes: Routes = [
  { path: '', component: AdminDashboardComponent },
  {
    path: 'users',
    loadChildren: () => import('../users/users.module').then(m => m.UsersModule)
  },
  {
    path: 'doctors',
    loadChildren: () => import('../doctors-management/doctors-management.module').then(m => m.DoctorsManagementModule)
  },
  {
    path: 'insurance',
    loadChildren: () => import('../insurance/insurance.module').then(m => m.InsuranceModule)
  },
  {
    path: 'marketplace',
    loadChildren: () => import('../marketplace-admin/marketplace-admin.module').then(m => m.MarketplaceAdminModule)
  },
  {
    path: 'appointments',
    loadChildren: () => import('../appointments/appointments.module').then(m => m.AppointmentsModule)
  },
  {
    path: 'pharmacy-applications',
    component: PharmacyApplicationsComponent
  }
];

@NgModule({
  declarations: [AdminDashboardComponent, PharmacyApplicationsComponent],
  imports: [
    SharedModule,
    RouterModule.forChild(routes)
  ]
})
export class AdminModule {}
