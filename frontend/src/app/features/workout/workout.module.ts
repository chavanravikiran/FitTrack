import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AlternativesDialogComponent } from './dialogs/alternatives-dialog.component';
import { CompleteDialogComponent } from './dialogs/complete-dialog.component';
import { RescheduleDialogComponent } from './dialogs/reschedule-dialog.component';
import { WorkoutHistoryComponent } from './history/workout-history.component';
import { WorkoutPlanComponent } from './plan/workout-plan.component';
import { WorkoutTodayComponent } from './today/workout-today.component';
import { WorkoutWeekComponent } from './week/workout-week.component';
import { WorkoutPageComponent } from './workout-page.component';

@NgModule({
  declarations: [
    WorkoutPageComponent,
    WorkoutTodayComponent,
    WorkoutWeekComponent,
    WorkoutPlanComponent,
    WorkoutHistoryComponent,
    AlternativesDialogComponent,
    RescheduleDialogComponent,
    CompleteDialogComponent,
  ],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: WorkoutPageComponent }])],
})
export class WorkoutModule {}
