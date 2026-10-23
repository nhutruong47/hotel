import { describe, it, expect } from 'vitest';
import { calculateNights, formatCurrency, getTodayDate, getTomorrowDate } from '../bookingUtils';
import { findVillaById, getSimilarVillas } from '../../villas/villaSelectors';
import { fallbackVillas } from '../../villas/villaFallbacks';

describe('Nhu Villas Booking & Villa Selectors Engine', () => {
  it('calculates stay nights accurately across dates', () => {
    expect(calculateNights('2026-10-20', '2026-10-23')).toBe(3);
    expect(calculateNights('2026-10-20', '2026-10-20')).toBe(0);
    expect(calculateNights('2026-10-25', '2026-10-20')).toBe(0);
    expect(calculateNights('', '2026-10-23')).toBe(0);
  });

  it('formats USD currency consistently', () => {
    expect(formatCurrency(450)).toBe('$450');
    expect(formatCurrency(2500)).toBe('$2,500');
  });

  it('generates valid ISO check-in and check-out dates', () => {
    const today = getTodayDate();
    const tomorrow = getTomorrowDate();
    expect(today).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(tomorrow).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(calculateNights(today, tomorrow)).toBe(1);
  });

  it('resolves villas by numeric id, slug, or roomNumber', () => {
    const byId = findVillaById(fallbackVillas, '1');
    expect(byId?.roomNumber).toBe('GV-01');

    const bySlug = findVillaById(fallbackVillas, 'hillside-residence-private-pool');
    expect(bySlug?.roomNumber).toBe('HR-03');

    const byCode = findVillaById(fallbackVillas, 'SE-04');
    expect(byCode?.name).toContain('Signature');
  });

  it('recommends up to 3 similar villas excluding current villa', () => {
    const current = fallbackVillas[0];
    const similar = getSimilarVillas(current, fallbackVillas);
    expect(similar.length).toBe(3);
    expect(similar.every((v) => v.id !== current.id)).toBe(true);
  });
});
