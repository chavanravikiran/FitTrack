import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthLayoutComponent } from './auth-layout/auth-layout.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';

const routes: Routes = [
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'login' },
      { path: 'login', component: LoginComponent, title: 'Sign in · FitTrack' },
      { path: 'register', component: RegisterComponent, title: 'Create account · FitTrack' },
      { path: 'forgot-password', component: ForgotPasswordComponent, title: 'Forgot password · FitTrack' },
      { path: 'reset-password', component: ResetPasswordComponent, title: 'Reset password · FitTrack' },
    ],
  },
];

@NgModule({
  declarations: [AuthLayoutComponent, LoginComponent, RegisterComponent, ForgotPasswordComponent, ResetPasswordComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class AuthModule {}
