import { Feature, PlanFeature } from '../../core/models/billing.models';

export const FEATURE_INFO: Record<Feature, { label: string; icon: string }> = {
  ACTIVE_GOALS: { label: 'Fitness goals', icon: 'flag' },
  ADVANCED_ANALYTICS: { label: 'Advanced analytics & insights', icon: 'insights' },
  PDF_REPORTS: { label: 'Monthly PDF progress reports', icon: 'picture_as_pdf' },
  PROGRESS_PHOTOS: { label: 'Before/after progress photos', icon: 'photo_library' },
  CUSTOM_WORKOUTS: { label: 'Custom workout plans', icon: 'edit_note' },
  DATA_EXPORT: { label: 'Export your data', icon: 'download' },
  AI_ASSISTANT: { label: 'AI fitness assistant', icon: 'smart_toy' },
};

export const ALL_FEATURES = Object.keys(FEATURE_INFO) as Feature[];

/** Included in every plan - the core product. */
export const CORE_INCLUDED = [
  'Personalised workout plan',
  'Step, water, sleep & weight tracking',
  'Indian food database & meal log',
  'Exercise video library & reminders',
];

export function featureLine(f: PlanFeature): string {
  const info = FEATURE_INFO[f.code];
  if (f.code === 'ACTIVE_GOALS') {
    return f.limit == null ? 'Unlimited fitness goals' : `Up to ${f.limit} active goals`;
  }
  return f.limit == null ? info.label : `${info.label} (up to ${f.limit})`;
}
