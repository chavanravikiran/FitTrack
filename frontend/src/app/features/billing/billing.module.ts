import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { BillingPageComponent } from './billing-page.component';

@NgModule({
  declarations: [BillingPageComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: BillingPageComponent }])],
})
export class BillingModule {}
