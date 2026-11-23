import { Pipe, PipeTransform } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/**
 * Turns a control's validation errors into a single readable message for <mat-error>.
 * Impure because the control instance stays the same while its errors change.
 */
@Pipe({ name: 'formError', pure: false, standalone: false })
export class FormErrorPipe implements PipeTransform {
  transform(control: AbstractControl | null | undefined, label = 'This field'): string {
    const e = control?.errors;
    if (!e) return '';
    if (e['server']) return e['server'];
    if (e['required']) return `${label} is required`;
    if (e['email']) return 'Enter a valid email address';
    if (e['minlength']) return `${label} must be at least ${e['minlength'].requiredLength} characters`;
    if (e['maxlength']) return `${label} must be at most ${e['maxlength'].requiredLength} characters`;
    if (e['min']) return `${label} must be at least ${e['min'].min}`;
    if (e['max']) return `${label} must be at most ${e['max'].max}`;
    if (e['pattern']) return `${label} has an invalid format`;
    if (e['weakPassword']) return 'Use 8+ characters with upper-case, lower-case and a number';
    if (e['passwordMismatch']) return 'Passwords do not match';
    if (e['matDatepickerParse']) return 'Enter a valid date';
    if (e['matDatepickerMax']) return 'Date is too late';
    if (e['matDatepickerMin']) return 'Date is too early';
    if (e['unsafeTarget']) return e['unsafeTarget'];
    return `${label} is invalid`;
  }
}
