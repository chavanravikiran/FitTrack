import { FormControl, FormGroup } from '@angular/forms';
import { fromIsoDate, matchFields, minHealthyWeight, strongPassword, toIsoDate } from './validators';

describe('validators', () => {
  it('strongPassword mirrors the backend rule', () => {
    expect(strongPassword(new FormControl('Secret123'))).toBeNull();
    expect(strongPassword(new FormControl('secret123'))).toEqual({ weakPassword: true });
    expect(strongPassword(new FormControl('Short1'))).toEqual({ weakPassword: true });
  });

  it('matchFields flags mismatched confirmation', () => {
    const group = new FormGroup(
      { password: new FormControl('Secret123'), confirm: new FormControl('Other123') },
      { validators: matchFields('password', 'confirm') },
    );
    expect(group.get('confirm')!.hasError('passwordMismatch')).toBeTrue();
    group.get('confirm')!.setValue('Secret123');
    expect(group.get('confirm')!.hasError('passwordMismatch')).toBeFalse();
  });

  it('round-trips ISO dates in local time without off-by-one', () => {
    const d = fromIsoDate('1995-04-12')!;
    expect(d.getDate()).toBe(12);
    expect(toIsoDate(d)).toBe('1995-04-12');
  });

  it('minHealthyWeight keeps BMI at 18.5 (matches backend)', () => {
    expect(minHealthyWeight(170)).toBe(53.5);
  });
});
