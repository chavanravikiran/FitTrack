import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const STRONG_PASSWORD = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/;

/** Mirrors the backend @StrongPassword rule. */
export const strongPassword: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const v = control.value as string | null;
  return !v || STRONG_PASSWORD.test(v) ? null : { weakPassword: true };
};

/** Group validator: `confirm` must equal `password`. */
export function matchFields(field: string, confirmField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const a = group.get(field);
    const b = group.get(confirmField);
    if (!a || !b) return null;
    const mismatch = a.value !== b.value;
    const errors = { ...(b.errors ?? {}) };
    if (mismatch) {
      errors['passwordMismatch'] = true;
    } else {
      delete errors['passwordMismatch'];
    }
    b.setErrors(Object.keys(errors).length ? errors : null);
    return null;
  };
}

/** Formats a Date as yyyy-MM-dd in local time (what the API expects for LocalDate). */
export function toIsoDate(d: Date | string | null): string | null {
  if (!d) return null;
  if (typeof d === 'string') return d.substring(0, 10);
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/** Parses yyyy-MM-dd as a local date (avoids the UTC off-by-one of `new Date('yyyy-MM-dd')`). */
export function fromIsoDate(s: string | null): Date | null {
  if (!s) return null;
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function browserTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

/** BMI-based safety floor for a target weight (BMI 18.5), matching the backend rule. */
export function minHealthyWeight(heightCm: number): number {
  const m = heightCm / 100;
  return Math.ceil(18.5 * m * m * 10) / 10;
}
