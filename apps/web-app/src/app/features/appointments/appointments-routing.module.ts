import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleGuard } from '../../core/guards/role.guard';
import { AppointmentEntryComponent } from './appointment-entry/appointment-entry.component';
import { AppointmentListComponent } from './appointment-list/appointment-list.component';
import { AppointmentBookComponent } from './appointment-book/appointment-book.component';
import { AppointmentScheduleComponent } from './appointment-schedule/appointment-schedule.component';
import { AppointmentDetailComponent } from './appointment-detail/appointment-detail.component';
import { GoogleCalendarOAuthCallbackComponent } from './google-calendar-oauth-callback/google-calendar-oauth-callback.component';

const routes: Routes = [
  { path: '', component: AppointmentEntryComponent },
  { path: 'list', component: AppointmentListComponent },
  { path: 'oauth/calendar', component: GoogleCalendarOAuthCallbackComponent },
  {
    path: 'book',
    component: AppointmentBookComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  },
  {
    path: 'schedule',
    component: AppointmentScheduleComponent,
    canActivate: [RoleGuard],
    data: { roles: ['DOCTOR'] }
  },
  { path: ':id', component: AppointmentDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AppointmentsRoutingModule {}
