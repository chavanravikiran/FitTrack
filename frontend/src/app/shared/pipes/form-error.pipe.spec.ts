import { FormControl, Validators } from '@angular/forms';
import { FormErrorPipe } from './form-error.pipe';

describe('FormErrorPipe', () => {
  const pipe = new FormErrorPipe();

  it('returns empty string for a valid control', () => {
    expect(pipe.transform(new FormControl('ok', Validators.required))).toBe('');
  });

  it('describes required and range errors with the label', () => {
    expect(pipe.transform(new FormControl('', Validators.required), 'Email')).toBe('Email is required');
    expect(pipe.transform(new FormControl(5, Validators.min(10)), 'Duration')).toBe('Duration must be at least 10');
  });

  it('prefers server-side messages', () => {
    const c = new FormControl('x');
    c.setErrors({ server: 'An account with this email already exists', required: true });
    expect(pipe.transform(c)).toBe('An account with this email already exists');
  });
});
