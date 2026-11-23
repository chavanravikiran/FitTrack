import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { ShellComponent } from './shell/shell.component';
import { NotificationBellComponent } from './notification-bell.component';

@NgModule({
  declarations: [ShellComponent, NotificationBellComponent],
  imports: [SharedModule],
  exports: [ShellComponent],
})
export class LayoutModule {}
