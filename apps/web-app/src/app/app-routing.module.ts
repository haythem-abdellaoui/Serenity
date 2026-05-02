import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { StatisticsComponent } from './features/statistics/statistics.component';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { ContratComponent } from './features/contrat/contrat.component';


const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule)
      }
    ]
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: DashboardComponent },
      { path: 'statistics', component: StatisticsComponent },
      {
        path: 'pharmacy',
        loadChildren: () => import('./features/pharmacy/pharmacy.module').then(m => m.PharmacyModule)
      },
      {
        path: 'profile',
        loadChildren: () => import('./features/profile/profile.module').then(m => m.ProfileModule)
      },
      {
        path: 'users',
        loadChildren: () => import('./features/users/users.module').then(m => m.UsersModule)
      },
      {
        path: 'messagerie',
        loadChildren: () => import('./features/messagerie/messagerie.module').then(m => m.MessagerieModule)
      },
      {
        path: 'insurance',
        loadChildren: () => import('./features/insurance/insurance.module').then(m => m.InsuranceModule)
      },
      {
        path: 'monitoring',
        canActivate: [RoleGuard],
        data: { roles: ['PATIENT', 'DOCTOR'] },
        loadChildren: () => import('./features/monitoring/monitoring.module').then(m => m.MonitoringModule)
      },
      {
        path: 'patients',
        loadChildren: () => import('./features/patients/patients.module').then(m => m.PatientsModule)
      },
      {
        path: 'marketplace',
        loadChildren: () => import('./features/marketplace/marketplace.module').then(m => m.MarketplaceModule)
      },
      {
        path: 'articles',
        loadChildren: () => import('./features/marketplace/marketplace.module').then(m => m.MarketplaceModule)
      },
      {
        path: 'appointments',
        loadChildren: () => import('./features/appointments/appointments.module').then(m => m.AppointmentsModule)
      }
    ]
  },
  { path: 'contrat', component: ContratComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
