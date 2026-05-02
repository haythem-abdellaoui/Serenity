import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MoodListComponent } from './mood/mood-list/mood-list.component';
import { MoodFormComponent } from './mood/mood-form/mood-form.component';
import { MoodDashboardComponent } from './mood/mood-dashboard/mood-dashboard.component';

const routes: Routes = [
  { path: '', component: MoodListComponent },
  { path: 'new', component: MoodFormComponent },
  { path: 'edit/:id', component: MoodFormComponent },
  { path: 'dashboard', component: MoodDashboardComponent }
];

@NgModule({
  declarations: [
    MoodListComponent,
    MoodFormComponent,
    MoodDashboardComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class MonitoringModule {}
