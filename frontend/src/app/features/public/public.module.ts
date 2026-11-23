import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { signedOutOnly } from '../../core/guards/auth.guards';
import { SharedModule } from '../../shared/shared.module';
import { LandingComponent } from './landing/landing.component';
import { LegalComponent } from './legal/legal.component';
import { PricingComponent } from './pricing/pricing.component';
import { PublicLayoutComponent } from './public-layout.component';
import { VerifyEmailComponent } from './verify-email/verify-email.component';

const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      // Signed-in visitors at "/" don't match here and fall through to the app shell (dashboard).
      { path: '', pathMatch: 'full', canMatch: [signedOutOnly], component: LandingComponent, title: 'FitTrack - your personal fitness coach' },
      { path: 'pricing', component: PricingComponent, title: 'Pricing · FitTrack' },
      { path: 'terms', component: LegalComponent, data: { doc: 'terms' }, title: 'Terms of Service · FitTrack' },
      { path: 'privacy', component: LegalComponent, data: { doc: 'privacy' }, title: 'Privacy Policy · FitTrack' },
      { path: 'verify-email', component: VerifyEmailComponent, title: 'Verify email · FitTrack' },
    ],
  },
];

@NgModule({
  declarations: [PublicLayoutComponent, LandingComponent, PricingComponent, LegalComponent, VerifyEmailComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class PublicModule {}
