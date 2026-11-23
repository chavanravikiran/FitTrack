import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AdminLayoutComponent } from './admin-layout.component';
import { AdminOverviewComponent } from './overview/admin-overview.component';
import { AdminPaymentsComponent } from './payments/admin-payments.component';
import { AdminPlansComponent } from './plans/admin-plans.component';
import { AdminUsersComponent } from './users/admin-users.component';
import { AdminVideosComponent } from './videos/admin-videos.component';
import { VideoEditDialogComponent } from './videos/video-edit-dialog.component';
import { GrantDialogComponent } from './users/grant-dialog.component';
import { UserDetailDialogComponent } from './users/user-detail-dialog.component';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      { path: 'overview', component: AdminOverviewComponent, title: 'Admin · Overview' },
      { path: 'users', component: AdminUsersComponent, title: 'Admin · Users' },
      { path: 'plans', component: AdminPlansComponent, title: 'Admin · Plans' },
      { path: 'payments', component: AdminPaymentsComponent, title: 'Admin · Payments' },
      { path: 'videos', component: AdminVideosComponent, title: 'Admin · Videos' },
    ],
  },
];

@NgModule({
  declarations: [
    AdminLayoutComponent,
    AdminOverviewComponent,
    AdminUsersComponent,
    GrantDialogComponent,
    UserDetailDialogComponent,
    AdminPlansComponent,
    AdminPaymentsComponent,
    AdminVideosComponent,
    VideoEditDialogComponent,
  ],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class AdminModule {}
