import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats paise as rupees without decimals for whole amounts', () => {
    expect(pipe.transform(19900)).toBe('₹199');
    expect(pipe.transform(199900)).toBe('₹1,999');
  });

  it('keeps paise when present and handles null', () => {
    expect(pipe.transform(19950)).toBe('₹199.50');
    expect(pipe.transform(null)).toBe('—');
  });
});
