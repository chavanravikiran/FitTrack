import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Feature, Plan, PlanFeature } from '../../../core/models/billing.models';
import { AdminService } from '../../../core/services/admin.service';
import { NotifyService } from '../../../core/services/notify.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { ALL_FEATURES, FEATURE_INFO } from '../../../shared/constants/features';

type FeatureForm = FormGroup<{
  enabled: FormControl<boolean>;
  unlimited: FormControl<boolean>;
  limit: FormControl<number | null>;
}>;

interface PlanEditor {
  plan: Plan;
  form: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    priceMonthly: FormControl<number>;
    priceYearly: FormControl<number>;
    active: FormControl<boolean>;
  }>;
  features: Record<Feature, FeatureForm>;
  saving: boolean;
  error: string | null;
}

/** Edit plan names, prices (entered in rupees, stored in paise) and feature entitlements. */
@Component({
  selector: 'ft-admin-plans',
  standalone: false,
  templateUrl: './admin-plans.component.html',
  styleUrl: './admin-plans.component.scss',
})
export class AdminPlansComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly allFeatures = ALL_FEATURES;
  readonly info = FEATURE_INFO;
  editors: PlanEditor[] = [];

  constructor(
    private readonly admin: AdminService,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.admin.plans().subscribe((plans) => (this.editors = plans.map((p) => this.editorFor(p))));
  }

  save(e: PlanEditor): void {
    if (e.form.invalid) {
      e.form.markAllAsTouched();
      return;
    }
    const v = e.form.getRawValue();
    const features: PlanFeature[] = this.allFeatures
      .filter((code) => e.features[code].controls.enabled.value)
      .map((code) => {
        const f = e.features[code].getRawValue();
        return { code, limit: f.unlimited ? null : Number(f.limit ?? 0) };
      });
    e.saving = true;
    e.error = null;
    this.admin
      .updatePlan(e.plan.code, {
        name: v.name.trim(),
        description: v.description.trim() || null,
        priceMonthlyMinor: Math.round(Number(v.priceMonthly) * 100),
        priceYearlyMinor: Math.round(Number(v.priceYearly) * 100),
        active: v.active,
        features,
      })
      .pipe(finalize(() => (e.saving = false)))
      .subscribe({
        next: (plan) => {
          Object.assign(e, this.editorFor(plan));
          this.notify.success(`${plan.name} saved`);
        },
        error: (err) => (e.error = apiErrorMessage(err)),
      });
  }

  private editorFor(plan: Plan): PlanEditor {
    const features = {} as Record<Feature, FeatureForm>;
    for (const code of this.allFeatures) {
      const existing = plan.features.find((f) => f.code === code);
      const group: FeatureForm = this.fb.nonNullable.group({
        enabled: this.fb.nonNullable.control(!!existing),
        unlimited: this.fb.nonNullable.control(existing ? existing.limit === null : true),
        limit: this.fb.control<number | null>(existing?.limit ?? 3, [Validators.min(0)]),
      });
      features[code] = group;
    }
    return {
      plan,
      form: this.fb.nonNullable.group({
        name: [plan.name, [Validators.required, Validators.maxLength(60)]],
        description: [plan.description ?? '', Validators.maxLength(500)],
        priceMonthly: [{ value: plan.priceMonthlyMinor / 100, disabled: plan.free }, [Validators.required, Validators.min(0)]],
        priceYearly: [{ value: plan.priceYearlyMinor / 100, disabled: plan.free }, [Validators.required, Validators.min(0)]],
        active: [{ value: plan.active, disabled: plan.free }],
      }),
      features,
      saving: false,
      error: null,
    };
  }
}
