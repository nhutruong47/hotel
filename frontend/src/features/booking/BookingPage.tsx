'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter, useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';
import { fetchVillas, fetchBookedDates, fetchVillaById } from '../villas/villasApi';
import { fallbackVillas } from '../villas/villaFallbacks';
import { findVillaById } from '../villas/villaSelectors';
import {
  calculateNights,
  formatCurrency,
  getTodayDate,
  getTomorrowDate,
  type BookingDraft,
} from './bookingUtils';
import { useTranslation } from '../../shared/i18n/hooks';

export const BookingPage = () => {
  const { t } = useTranslation();
  const { villaId } = useParams() as { villaId: string };
  const router = useRouter();

  const villasQuery = useQuery({
    queryKey: ['villas'],
    queryFn: () => fetchVillas(),
    staleTime: 60_000,
  });

  const villaQuery = useQuery({
    queryKey: ['villa', villaId],
    queryFn: () => fetchVillaById(villaId!),
    enabled: Boolean(villaId),
    retry: false,
  });

  const availabilityQuery = useQuery({
    queryKey: ['booked-dates', villaId],
    queryFn: () => fetchBookedDates(villaId!),
    enabled: Boolean(villaId),
    retry: false,
  });

  const villas = villasQuery.data ?? (villasQuery.isError ? fallbackVillas : undefined);
  const villaFromList = findVillaById(villas, villaId);
  const villa = villaFromList ?? villaQuery.data ?? null;

  const [checkIn, setCheckIn] = useState(getTodayDate());
  const [checkOut, setCheckOut] = useState(getTomorrowDate());
  const [guests, setGuests] = useState(2);
  const [voucherCode, setVoucherCode] = useState('');
  const [appliedVoucher, setAppliedVoucher] = useState<{
    code: string;
    amount: number;
    percent: boolean;
  } | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);
  const bookedDates = availabilityQuery.data ?? [];

  const nights = calculateNights(checkIn, checkOut);

  const pricingQuery = useQuery({
    queryKey: ['pricing', villa?.id, checkIn, checkOut],
    queryFn: async () => {
      if (!villa || nights <= 0) return null;
      try {
        return await api.get<{
          subtotal: number;
          serviceFee: number;
          taxAmount: number;
          total: number;
        }>(`${API_PATHS.bookings}/pricing-preview?roomId=${villa.id}&checkIn=${checkIn}&checkOut=${checkOut}`);
      } catch (err) {
        // Fallback to local calculation if backend fails
        const subtotal = nights * villa.pricePerNight;
        const serviceFee = subtotal > 0 ? Math.round(subtotal * 0.08) : 0;
        const taxAmount = subtotal > 0 ? Math.round(subtotal * 0.10) : 0;
        return { subtotal, serviceFee, taxAmount, total: subtotal + serviceFee + taxAmount };
      }
    },
    enabled: Boolean(villa && nights > 0),
    staleTime: 60_000,
  });

  const basePricing = pricingQuery.data ?? {
    subtotal: villa ? nights * villa.pricePerNight : 0,
    serviceFee: villa && nights > 0 ? Math.round((nights * villa.pricePerNight) * 0.08) : 0,
    taxAmount: villa && nights > 0 ? Math.round((nights * villa.pricePerNight) * 0.10) : 0,
    total: villa ? nights * villa.pricePerNight + Math.round((nights * villa.pricePerNight) * 0.08) + Math.round((nights * villa.pricePerNight) * 0.10) : 0,
  };

  const discount = useMemo(() => {
    if (!appliedVoucher) return 0;
    if (appliedVoucher.percent) {
      return Math.round((basePricing.subtotal * appliedVoucher.amount) / 100);
    }
    return appliedVoucher.amount;
  }, [appliedVoucher, basePricing.subtotal]);

  const finalTotal = Math.max(0, basePricing.total - discount);
  const exceedsCapacity = villa ? guests > villa.capacity : false;
  const invalidDates = nights <= 0;
  const validationMessage = useMemo(() => {
    if (!villa) return t('validation.bookingVillaReq');
    if (invalidDates) return t('validation.bookingDatesInvalid');
    if (exceedsCapacity) return `${t('validation.bookingCapacity')} ${villa.capacity} ${t('booking.guests')}`;
    return '';
  }, [exceedsCapacity, invalidDates, villa, t]);

  const canContinue = Boolean(villa) && !invalidDates && !exceedsCapacity;

  const continueToCheckout = () => {
    setTouched(true);
    if (!villa || !canContinue) return;
    const draft: BookingDraft = {
      villaId: villa.id,
      villaName: villa.name,
      checkIn,
      checkOut,
      guests,
      nights,
      nightlyRate: villa.pricePerNight,
      subtotal: basePricing.subtotal,
      discount,
      serviceFee: basePricing.serviceFee,
      taxAmount: basePricing.taxAmount,
      total: finalTotal,
      voucherCode: appliedVoucher ? appliedVoucher.code : undefined,
    };
    window.sessionStorage.setItem('nhu.bookingDraft', JSON.stringify(draft));
    router.push('/checkout');
  };

  if ((villasQuery.isLoading || villaQuery.isLoading) && !villa) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-[1080px] rounded-[2rem] bg-brand-paper p-8">
          <div className="h-12 w-64 animate-pulse rounded-full bg-brand-stone" />
          <div className="mt-8 h-72 animate-pulse rounded-[1.5rem] bg-brand-stone" />
        </div>
      </section>
    );
  }

  if (!villa) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-3xl rounded-[2rem] bg-brand-paper p-8 text-center">
          <h1 className="text-5xl text-brand-charcoal">{t('booking.needsVillaTitle')}</h1>
          <Link href="/villas" className="mt-8 inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            {t('booking.chooseVilla')}
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1280px]">
        <Link href={`/villas/${villa.id}`} className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest">
          &larr; {t('booking.backToVilla')}
        </Link>
        <div className="mt-8 grid gap-8 lg:grid-cols-[0.92fr_0.58fr]">
          <div className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.1)] sm:p-8">
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('booking.bookingExp')}</p>
            <h1 className="max-w-[13ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-6xl">
              {t('booking.chooseDatesTitle')}
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-8 text-brand-ink/64">
              {t('booking.chooseDatesLead')}
            </p>

            <div className="mt-10 grid gap-5 sm:grid-cols-2">
              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">{t('booking.checkIn')}</span>
                <input
                  type="date"
                  value={checkIn}
                  min={getTodayDate()}
                  onChange={(event) => setCheckIn(event.target.value)}
                  className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-brand-ink outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                />
              </label>
              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">{t('booking.checkOut')}</span>
                <input
                  type="date"
                  value={checkOut}
                  min={checkIn || getTomorrowDate()}
                  onChange={(event) => setCheckOut(event.target.value)}
                  className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-brand-ink outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                />
              </label>
              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">{t('booking.guests')}</span>
                <input
                  type="number"
                  value={guests}
                  min={1}
                  max={villa.capacity}
                  onChange={(event) => setGuests(Number(event.target.value))}
                  className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-brand-ink outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                />
              </label>
              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">{t('booking.voucher')}</span>
                <div className="mt-3 flex gap-2">
                  <input
                    value={voucherCode}
                    onChange={(event) => { setVoucherCode(event.target.value); setAppliedVoucher(null); }}
                    placeholder={t('forms.placeholder.voucher')}
                    className="h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-brand-ink outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                  />
                  <button
                    type="button"
                    onClick={async () => {
                      setVoucherError(null);
                      setAppliedVoucher(null);
                      const code = voucherCode.trim();
                      if (!code) return;
                      try {
                        const res = await api.get<{ valid: boolean; message?: string; amount?: string; percent?: boolean }>(
                          `${API_PATHS.validateVoucher}?code=${encodeURIComponent(code)}`
                        );
                        if (res?.valid) {
                          setAppliedVoucher({
                            code,
                            amount: Number(res.amount ?? 0),
                            percent: Boolean(res.percent),
                          });
                        } else {
                          setVoucherError(res?.message || t('validation.voucherInvalid'));
                        }
                      } catch {
                        setVoucherError(t('errors.somethingWentWrong'));
                      }
                    }}
                    className="rounded-full border border-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest hover:bg-brand-forest hover:text-brand-white"
                  >
                    {t('booking.applyVoucher')}
                  </button>
                </div>
                {voucherError ? (
                  <p className="mt-2 text-xs text-brand-coral">{voucherError}</p>
                ) : null}
                {appliedVoucher ? (
                  <p className="mt-2 text-xs text-brand-forest">
                    {t('booking.appliedVoucher')} {appliedVoucher.code}: {appliedVoucher.percent ? `${appliedVoucher.amount}% ${t('booking.off')}` : `${formatCurrency(appliedVoucher.amount)} ${t('booking.off')}`}
                  </p>
                ) : null}
              </label>
            </div>

            {touched && validationMessage ? (
              <div className="mt-6 rounded-[1.25rem] border border-brand-coral/30 bg-brand-coral/10 p-4 text-sm text-brand-ink">
                {validationMessage}
              </div>
            ) : null}

            <div className="mt-8 rounded-[1.5rem] bg-brand-white p-5">
              <h2 className="text-2xl text-brand-charcoal">{t('booking.availability')}</h2>
              {availabilityQuery.isLoading ? (
                <p className="mt-3 text-sm text-brand-ink/58">{t('booking.checkingDates')}</p>
              ) : availabilityQuery.isError ? (
                <p className="mt-3 text-sm text-brand-ink/58">{t('booking.availOffline')}</p>
              ) : bookedDates.length === 0 ? (
                <p className="mt-3 text-sm text-brand-ink/58">{t('booking.noConflict')}</p>
              ) : (
                <ul className="mt-4 space-y-2 text-sm text-brand-ink/64">
                  {bookedDates.slice(0, 3).map((range) => (
                    <li key={`${range.checkIn}-${range.checkOut}`}>
                      {t('booking.unavailable')}: {range.checkIn} {t('common.to')} {range.checkOut}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <aside className="h-fit rounded-[2rem] bg-brand-paper p-2 shadow-[0_24px_90px_rgba(32,52,43,0.12)] lg:sticky lg:top-28">
            <div className="rounded-[1.55rem] bg-brand-white p-6">
              <img src={villa.imageUrl} alt={villa.name} loading="lazy" decoding="async" className="aspect-[4/3] w-full rounded-[1.25rem] object-cover" />
              <h2 className="mt-6 text-3xl text-brand-charcoal">{villa.name}</h2>
              <dl className="mt-6 space-y-4 text-sm text-brand-ink/66">
                <div className="flex justify-between gap-4">
                  <dt>{formatCurrency(villa.pricePerNight)} x {nights || 0} {t('booking.nights')}</dt>
                  <dd>{formatCurrency(basePricing.subtotal)}</dd>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between gap-4">
                    <dt>{t('booking.voucher')}</dt>
                    <dd>-{formatCurrency(discount)}</dd>
                  </div>
                )}
                <div className="flex justify-between gap-4">
                  <dt>{t('booking.serviceFee')}</dt>
                  <dd>{formatCurrency(basePricing.serviceFee)}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt>{t('booking.taxes')}</dt>
                  <dd>{formatCurrency(basePricing.taxAmount)}</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-brand-stone pt-4 text-base font-semibold text-brand-charcoal">
                  <dt>{t('booking.total')}</dt>
                  <dd>{formatCurrency(finalTotal)}</dd>
                </div>
              </dl>
              <button
                type="button"
                onClick={continueToCheckout}
                className="mt-7 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
              >
                {t('booking.continueToCheckout')}
              </button>
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
};