import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MessagerieComponent } from './messagerie/messagerie.component';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

const routes: Routes = [
  {
    path: '',
    component: MessagerieComponent
  }
]
@NgModule({
  declarations: [
    MessagerieComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    FormsModule
  ]
})
export class MessagerieModule { }
