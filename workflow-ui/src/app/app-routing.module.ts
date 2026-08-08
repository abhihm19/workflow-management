import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CancelLeaveComponent } from './pages/cancel-leave/cancel-leave.component';
import { HomeComponent } from './pages/home/home.component';
import { SubmitLeaveComponent } from './pages/submit-leave/submit-leave.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'submit-leave', component: SubmitLeaveComponent },
  { path: 'leaves/:id/cancel', component: CancelLeaveComponent },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
