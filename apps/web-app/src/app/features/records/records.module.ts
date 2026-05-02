import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RecordListComponent } from './record-list/record-list.component';
import { RecordFormComponent } from './record-form/record-form.component';

const routes: Routes = [
  { path: '', component: RecordListComponent },
  { path: 'new', component: RecordFormComponent },
  { path: ':recordId/edit', component: RecordFormComponent },
  {
    path: ':recordId/prescriptions',
    loadChildren: () => import('../prescriptions/prescriptions.module').then((m) => m.PrescriptionsModule)
  }
];

@NgModule({
  declarations: [RecordListComponent, RecordFormComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class RecordsModule {}
