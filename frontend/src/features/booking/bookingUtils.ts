export type BookingDraft = {
  villaId: number;
  villaName: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  nights: number;
  nightlyRate: number;
  subtotal: number;
  discount: number;
  serviceFee: number;
  taxAmount: number;
  total: number;
  voucherCode?: string;
};

export function getTodayDate() {
  return new Date().toISOString().slice(0, 10);
}

export function getTomorrowDate() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

export function calculateNights(checkIn: string, checkOut: string) {
  if (!checkIn || !checkOut) {
    return 0;
  }

  const start = new Date(`${checkIn}T00:00:00`);
  const end = new Date(`${checkOut}T00:00:00`);
  const diff = end.getTime() - start.getTime();

  if (diff <= 0) {
    return 0;
  }

  return Math.ceil(diff / 86_400_000);
}

export function formatCurrency(value: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value);
}

export function getVoucherDiscount(_code: string, _subtotal: number) {
  // Discounts are now resolved server-side via /bookings/vouchers/validate.
  // This stub remains for any third-party callers; it always returns 0.
  return 0;
}
