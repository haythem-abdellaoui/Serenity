import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { DoctorsManagementComponent } from './doctors-management/doctors-management.component';
import { RoleGuard } from 'src/app/core/guards/role.guard';


const routes: Routes = [
  { path: '', 
    component: DoctorsManagementComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] },
  },
]

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    RouterModule.forChild(routes)
  ]
})
export class DoctorsManagementModule { }
