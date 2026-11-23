import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AssessmentFormComponent } from './components/assessment-form/assessment-form.component';
import { ComingSoonComponent } from './components/coming-soon.component';
import { DisclaimerComponent } from './components/disclaimer.component';
import { EmptyStateComponent } from './components/empty-state.component';
import { PageHeaderComponent } from './components/page-header.component';
import { StatCardComponent } from './components/stat-card.component';
import { MaterialModule } from './material.module';
import { PlanCardsComponent } from './components/plan-cards/plan-cards.component';
import { BarChartComponent } from './components/bar-chart.component';
import { ProgressRingComponent } from './components/progress-ring.component';
import { ExerciseDetailDialogComponent } from './components/exercise-detail-dialog.component';
import { FormErrorPipe } from './pipes/form-error.pipe';
import { MoneyPipe } from './pipes/money.pipe';

const DECLARATIONS = [
  PageHeaderComponent,
  StatCardComponent,
  EmptyStateComponent,
  DisclaimerComponent,
  ComingSoonComponent,
  AssessmentFormComponent,
  PlanCardsComponent,
  BarChartComponent,
  ProgressRingComponent,
  ExerciseDetailDialogComponent,
  FormErrorPipe,
  MoneyPipe,
];

/** Reusable presentational building blocks. Import into every feature module. */
@NgModule({
  declarations: DECLARATIONS,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MaterialModule],
  exports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule, ...DECLARATIONS],
})
export class SharedModule {}
