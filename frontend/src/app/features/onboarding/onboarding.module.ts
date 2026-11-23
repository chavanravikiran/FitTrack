import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { OnboardingComponent } from './onboarding.component';

@NgModule({
  declarations: [OnboardingComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{ path: '', component: OnboardingComponent, title: 'Get started · FitTrack' }]),
  ],
})
export class OnboardingModule {}
