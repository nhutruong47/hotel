'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { formatCurrency, type BookingDraft } from './bookingUtils';
import { useSession } from '../../shared/auth/SessionProvider';
import { useTranslation } from '../../shared/i18n/hooks';

function readDraft(): BookingDraft | null {
  const raw = window.sessionStorage.getItem('nhu.bookingDraft');
  if (!raw) return null;
  try { return JSON.parse(raw) as BookingDraft; } catch { return null; }
}

type BookingResponse = {
  booking: {
    id: number;
  };
  reference: string;
};

export const CheckoutPage = () => {
  const { t } = useTranslation();
  const router = useRouter();
  const [draft, setDraft] = useState<BookingDraft | null>(null);
  const [guestName, setGuestName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('bank-transfer');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const isValid = guestName.trim().length >= 2 && email.includes('@') && phone.trim().length >= 8;

  useEffect(() => {
    setDraft(readDraft());
  }, []);

  const { user } = useSession();
  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.get<any>(API_PATHS.profile),
    enabled: !!user,
  });

  useEffect(() => {
    if (profile) {
      if (!guestName && profile.fullName) setGuestName(profile.fullName);
      if (!email && profile.email) setEmail(profile.email);
      if (!phone && profile.phone) setPhone(profile.phone);
    } else if (user) {
      if (!guestName && user.fullName) setGuestName(user.fullName);
      if (!email && user.email) setEmail(user.email);
    }
  }, [profile, user]);

  const mutation = useMutation<BookingResponse, ApiError, void>({
    mutationFn: async () => {
      if (!draft) throw new ApiError(0, 'NO_DRAFT', 'Không có booking draft');
      return api.post<BookingResponse>(API_PATHS.bookings, {
        roomId: draft.villaId,
        checkIn: draft.checkIn,
        checkOut: draft.checkOut,
        guestName,
        guestEmail: email,
        guestPhone: phone,
        notes: notes || undefined,
        voucherCode: draft.voucherCode,
        guests: draft.guests,
        paymentMethod,
      });
    },
    onSuccess: (data) => {
      const reference = data.reference || `NV-${data.booking.id}`;
      window.sessionStorage.setItem(
        'nhu.bookingSuccess',
        JSON.stringify({
          ...draft,
          reference,
          guestName,
          email,
          phone,
          notes,
          paymentMethod,
          bookingId: data.booking?.id,
          status: 'Pending confirmation',
        })
      );
      router.push(`/booking/success?ref=${reference}`);
    },
    onError: (err) => {
      setErrorMsg(err.message || t('errors.somethingWentWrong'));
    },
  });

  if (!draft) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-3xl rounded-[2rem] bg-brand-paper p-8 text-center">
          <h1 className="text-5xl text-brand-charcoal">{t('booking.noDraftTitle')}</h1>
          <p className="mt-4 text-brand-ink/62">{t('booking.noDraftDesc')}</p>
          <Link href="/villas" className="mt-8 inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            {t('booking.chooseVilla')}
          </Link>
        </div>
      </section>
    );
  }

  const isSubmitting = mutation.isPending;

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto grid max-w-[1180px] gap-8 lg:grid-cols-[0.95fr_0.65fr]">
        <div className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.1)] sm:p-8">
          <Link href={`/villas/${draft.villaId}`} className="inline-block mb-6 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage hover:text-brand-forest transition-colors">
            &larr; {t('booking.backToVilla')}
          </Link>
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('booking.checkout')}</p>
          <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-6xl">
            {t('booking.confirmTitle')}
          </h1>
          {errorMsg ? (
            <div className="mt-6 rounded-[1.25rem] border border-brand-coral/30 bg-brand-coral/10 p-4 text-sm text-brand-ink">
              {errorMsg}
            </div>
          ) : null}
          <div className="mt-10 grid gap-5">
            <label>
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.guestName')}</span>
              <input
                value={guestName}
                onChange={(event) => setGuestName(event.target.value)}
                className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              />
            </label>
            <div className="grid gap-5 sm:grid-cols-2">
              <label>
                <span className="text-sm font-semibold text-brand-charcoal">{t('forms.email')}</span>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                />
              </label>
              <label>
                <span className="text-sm font-semibold text-brand-charcoal">{t('forms.phone')}</span>
                <input
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5 outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                />
              </label>
            </div>
            <label>
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.notes')}</span>
              <textarea
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                rows={3}
                placeholder={t('forms.placeholder.notes')}
                className="mt-3 w-full rounded-[1.25rem] border border-brand-stone bg-brand-white p-5 outline-none focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              />
            </label>
            <fieldset className="rounded-[1.5rem] bg-brand-white p-5">
              <legend className="text-sm font-semibold text-brand-charcoal">{t('booking.paymentPref')}</legend>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {[
                  ['bank-transfer', t('booking.bankTransfer')],
                  ['qr', t('booking.qrCode')],
                ].map(([value, label]) => (
                  <label key={value} className="flex items-center gap-3 rounded-full border border-brand-stone px-4 py-3">
                    <input
                      type="radio"
                      name="payment"
                      checked={paymentMethod === value}
                      onChange={() => setPaymentMethod(value)}
                    />
                    <span className="text-sm text-brand-ink/72">{label}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          </div>
          <button
            type="button"
            onClick={() => { setErrorMsg(null); mutation.mutate(); }}
            disabled={!isValid || isSubmitting}
            className="mt-8 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
          >
            {isSubmitting ? t('booking.confirming') : t('booking.confirmBtn')}
          </button>
        </div>
        <aside className="h-fit rounded-[2rem] bg-brand-paper p-2 shadow-[0_24px_90px_rgba(32,52,43,0.1)] lg:sticky lg:top-28">
          <div className="rounded-[1.55rem] bg-brand-white p-6">
            <h2 className="text-3xl text-brand-charcoal">{draft.villaName}</h2>
            <dl className="mt-6 space-y-4 text-sm text-brand-ink/66">
              <div className="flex justify-between gap-4">
                <dt>{t('booking.dates')}</dt>
                <dd>{draft.checkIn} {t('common.to')} {draft.checkOut}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt>{t('booking.guests')}</dt>
                <dd>{draft.guests}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt>{t('booking.subtotal')}</dt>
                <dd>{formatCurrency(draft.subtotal)}</dd>
              </div>
              {draft.discount > 0 && (
                <div className="flex justify-between gap-4">
                  <dt>{t('booking.discount')}</dt>
                  <dd>-{formatCurrency(draft.discount)}</dd>
                </div>
              )}
              <div className="flex justify-between gap-4">
                <dt>{t('booking.serviceFee')}</dt>
                <dd>{formatCurrency(draft.serviceFee)}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt>{t('booking.taxes')}</dt>
                <dd>{formatCurrency(draft.taxAmount || 0)}</dd>
              </div>
              <div className="flex justify-between gap-4 border-t border-brand-stone pt-4 text-base font-semibold text-brand-charcoal">
                <dt>{t('booking.total')}</dt>
                <dd>{formatCurrency(draft.total)}</dd>
              </div>
            </dl>
          </div>
        </aside>
      </div>
    </section>
  );
};