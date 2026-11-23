import { Pipe, PipeTransform } from '@angular/core';

/** Formats an amount in minor units (paise) as currency, e.g. 19900 -> ₹199. */
@Pipe({ name: 'money', standalone: false })
export class MoneyPipe implements PipeTransform {
  transform(minor: number | null | undefined, currency = 'INR'): string {
    if (minor == null) return '—';
    const major = minor / 100;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      minimumFractionDigits: Number.isInteger(major) ? 0 : 2,
      maximumFractionDigits: 2,
    }).format(major);
  }
}
