import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AddFoodDialogComponent } from './add-food-dialog.component';
import { NutritionPageComponent } from './nutrition-page.component';

@NgModule({
  declarations: [NutritionPageComponent, AddFoodDialogComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: NutritionPageComponent }])],
})
export class NutritionModule {}
