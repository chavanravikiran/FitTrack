import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ReminderDialogComponent } from './reminder-dialog.component';
import { RemindersPageComponent } from './reminders-page.component';

@NgModule({
  declarations: [RemindersPageComponent, ReminderDialogComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: RemindersPageComponent }])],
})
export class RemindersModule {}
