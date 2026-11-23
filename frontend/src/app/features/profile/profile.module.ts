import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AssessmentEditComponent } from './assessment-edit/assessment-edit.component';
import { GoalDialogComponent } from './goals/goal-dialog.component';
import { GoalsComponent } from './goals/goals.component';
import { ProfileDetailsComponent } from './profile-details/profile-details.component';
import { ProfilePageComponent } from './profile-page.component';
import { SecurityComponent } from './security/security.component';

@NgModule({
  declarations: [
    ProfilePageComponent,
    ProfileDetailsComponent,
    AssessmentEditComponent,
    GoalsComponent,
    GoalDialogComponent,
    SecurityComponent,
  ],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: ProfilePageComponent }])],
})
export class ProfileModule {}
