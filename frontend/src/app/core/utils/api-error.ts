import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiResponse, FieldError } from '../models/api.models';

/** Extracts a user-friendly message from an API error response. */
export function apiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Please check your connection.';
    }
    const body = error.error as Partial<ApiResponse<unknown>> | null;
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}

export function apiFieldErrors(error: unknown): FieldError[] {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiResponse<unknown>> | null;
    return body?.errors ?? [];
  }
  return [];
}

/**
 * Maps server-side validation errors (e.g. "assessment.sleepHours") onto matching form controls
 * so they render inline under the field. Returns the errors that could not be mapped.
 */
export function applyServerErrors(form: FormGroup, error: unknown): FieldError[] {
  const unmapped: FieldError[] = [];
  for (const fe of apiFieldErrors(error)) {
    const control: AbstractControl | null = fe.field ? form.get(fe.field) : null;
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: fe.message });
      control.markAsTouched();
    } else {
      unmapped.push(fe);
    }
  }
  return unmapped;
}
