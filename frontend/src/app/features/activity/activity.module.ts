import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ActivityPageComponent } from './activity-page.component';
import { SleepComponent } from './sleep/sleep.component';
import { StepsComponent } from './steps/steps.component';
import { WaterComponent } from './water/water.component';

@NgModule({
  declarations: [ActivityPageComponent, StepsComponent, WaterComponent, SleepComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: ActivityPageComponent }])],
})
export class ActivityModule {}
