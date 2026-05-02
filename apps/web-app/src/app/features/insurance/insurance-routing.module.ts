import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClaimListComponent } from './claim-list/claim-list.component';
import { ClaimFormComponent } from './claim-form/claim-form.component';
import { ClaimDetailComponent } from './claim-detail/claim-detail.component';
import { InsuranceStatisticsComponent } from './insurance-statistics/insurance-statistics.component';
import { OcrNotificationsComponent } from './ocr-notifications/ocr-notifications.component';

const routes: Routes = [
  { path: '', component: ClaimListComponent },
  { path: 'statistics', component: InsuranceStatisticsComponent },
  { path: 'ocr-notifications', component: OcrNotificationsComponent },
  { path: 'new', component: ClaimFormComponent },
  { path: ':id', component: ClaimDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class InsuranceRoutingModule {}
