import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AppointmentsRoutingModule } from './appointments-routing.module';
import { AppointmentEntryComponent } from './appointment-entry/appointment-entry.component';
import { PatientAppointmentsHubComponent } from './patient-appointments-hub/patient-appointments-hub.component';
import { AppointmentListComponent } from './appointment-list/appointment-list.component';
import { AppointmentBookComponent } from './appointment-book/appointment-book.component';
import { AppointmentScheduleComponent } from './appointment-schedule/appointment-schedule.component';
import { AppointmentDetailComponent } from './appointment-detail/appointment-detail.component';
import { AppointmentCalendarComponent } from './appointment-calendar/appointment-calendar.component';
import { AppointmentBusyTimesPanelComponent } from './appointment-busy-times-panel/appointment-busy-times-panel.component';
import { GoogleCalendarOAuthCallbackComponent } from './google-calendar-oauth-callback/google-calendar-oauth-callback.component';

@NgModule({
  declarations: [
    AppointmentEntryComponent,
    PatientAppointmentsHubComponent,
    AppointmentListComponent,
    AppointmentBookComponent,
    AppointmentScheduleComponent,
    AppointmentDetailComponent,
    AppointmentCalendarComponent,
    AppointmentBusyTimesPanelComponent,
    GoogleCalendarOAuthCallbackComponent
  ],
  imports: [
    SharedModule,
    AppointmentsRoutingModule
  ]
})
export class AppointmentsModule {}
