'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { formatCurrency, type BookingDraft } from './bookingUtils';
import { useTranslation } from '../../shared/i18n/hooks';
import { api, API_PATHS, ApiError } from '../../shared/api/client';

type BookingSuccess = BookingDraft & {
  reference: string;
  bookingId: number;
  guestName: string;
  email: string;
  phone: string;
  notes?: string;
  paymentMethod: string;
  status: string;
};

type BookingDetail = {
  id: number;
  status: string;
  totalPrice: number;
  paidAmount: number;
  paymentStatus: string;
};

function readSuccess(): BookingSuccess | null {
  const raw = window.sessionStorage.getItem('nhu.bookingSuccess');

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as BookingSuccess;
  } catch {
    return null;
  }
}

export const BookingSuccessPage = () => {
  const { t } = useTranslation();
  const params = useSearchParams();
  const initialBooking = useMemo(() => readSuccess(), []);
  const reference = params.get('ref') ?? initialBooking?.reference ?? 'NV-PENDING';

  const [bookingDetail, setBookingDetail] = useState<BookingDetail | null>(null);
  const [paymentMsg, setPaymentMsg] = useState<string | null>(null);

  useEffect(() => {
    const id = initialBooking?.bookingId;
    if (!id) return;
    api.get<BookingDetail>(API_PATHS.booking(id))
      .then((data) => setBookingDetail(data as BookingDetail))
      .catch(() => {
        // best-effort only; UI works even if backend is unreachable
      });
  }, [initialBooking?.bookingId]);

  const paymentMutation = useMutation<unknown, ApiError, void>({
    mutationFn: async () => {
      if (!initialBooking?.bookingId) {
        throw new ApiError(0, 'NO_BOOKING', t('errors.somethingWentWrong'));
      }
      return api.post<unknown>(API_PATHS.payments.create, {
        bookingId: initialBooking.bookingId,
        amount: initialBooking.total,
        method: 'BANK_TRANSFER',
        notes: 'Khách hàng thanh toán qua QR code.',
      });
    },
    onSuccess: () => setPaymentMsg(t('booking.paymentSent')),
    onError: (err) => setPaymentMsg(err.message || t('errors.somethingWentWrong')),
  });

  return (
    <section className="min-h-screen bg-brand-forest-deep px-5 pb-24 pt-32 text-brand-paper sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[980px] rounded-[2rem] border border-brand-white/12 bg-brand-white/8 p-6 shadow-[0_30px_110px_rgba(8,17,14,0.28)] backdrop-blur-xl sm:p-10">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">{t('booking.successEyebrow')}</p>
        <Link href="/" className="inline-block mb-6 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage hover:text-brand-white transition-colors">
          &larr; {t('auth.backToHome')}
        </Link>
        <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-white sm:text-7xl">
          {t('booking.successTitle')}
        </h1>
        <p className="mt-6 max-w-2xl text-base leading-8 text-brand-paper/72">
          {t('booking.successRef')} {reference}. {t('booking.successLead')}
        </p>

        {initialBooking ? (
          <div className="mt-10 rounded-[1.5rem] bg-brand-paper p-6 text-brand-ink">
            <h2 className="text-3xl text-brand-charcoal">{initialBooking.villaName}</h2>
            <dl className="mt-6 grid gap-4 text-sm sm:grid-cols-2">
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('booking.guestName')}</dt>
                <dd className="mt-1 text-brand-ink/62">{initialBooking.guestName}</dd>
              </div>
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('booking.status')}</dt>
                <dd className="mt-1 text-brand-ink/62">
                  {bookingDetail ? bookingDetail.status : initialBooking.status}
                </dd>
              </div>
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('booking.dates')}</dt>
                <dd className="mt-1 text-brand-ink/62">{initialBooking.checkIn} {t('common.to')} {initialBooking.checkOut}</dd>
              </div>
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('booking.total')}</dt>
                <dd className="mt-1 text-brand-ink/62">{formatCurrency(initialBooking.total)}</dd>
              </div>
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('forms.email')}</dt>
                <dd className="mt-1 text-brand-ink/62">{initialBooking.email || '—'}</dd>
              </div>
              <div>
                <dt className="font-semibold text-brand-charcoal">{t('forms.phone')}</dt>
                <dd className="mt-1 text-brand-ink/62">{initialBooking.phone || '—'}</dd>
              </div>
            </dl>

            {bookingDetail ? (
              <div className="mt-6 rounded-[1.25rem] border border-brand-stone bg-brand-sand/40 p-4">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-forest">
                  {t('booking.payment')}
                </p>
                <div className="mt-2 flex flex-wrap items-center justify-between gap-4 text-sm text-brand-ink/72">
                  <span>
                    {t('booking.paidAmount')} {formatCurrency(Number(bookingDetail.paidAmount ?? 0))} / {formatCurrency(Number(bookingDetail.totalPrice ?? initialBooking.total))}
                  </span>
                  <span className="rounded-full bg-brand-forest px-3 py-1 text-[11px] uppercase tracking-[0.18em] text-brand-white">
                    {String(bookingDetail.paymentStatus ?? 'PENDING')}
                  </span>
                </div>
              </div>
            ) : null}

            {initialBooking.bookingId ? (
              <div className="mt-6 flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => { setPaymentMsg(null); paymentMutation.mutate(); }}
                  disabled={paymentMutation.isPending}
                  className="inline-flex min-h-11 items-center justify-center rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-60"
                >
                  {paymentMutation.isPending ? t('booking.sendingPayment') : t('booking.notifyPayment')}
                </button>
                <button
                  type="button"
                  onClick={async () => {
                    setPaymentMsg(null);
                    try {
                      await api.post<{ message: string }>(API_PATHS.bookingSubmitPayment(initialBooking.bookingId), {
                        transactionRef: 'MOCK-PAY-' + Date.now(),
                        amount: initialBooking.total,
                        paymentMethod: 'CARD'
                      });
                      setPaymentMsg(t('booking.simulatedPaymentSuccess'));
                      setBookingDetail(prev => prev ? { ...prev, status: 'PAID', paymentStatus: 'PAID', paidAmount: initialBooking.total } : null);
                    } catch (err: any) {
                      setPaymentMsg(err.message || t('errors.somethingWentWrong'));
                    }
                  }}
                  className="inline-flex min-h-11 items-center justify-center rounded-full bg-brand-sage/20 border border-brand-sage px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-charcoal hover:bg-brand-sage/40 transition"
                >
                  {t('booking.simulatePayment')}
                </button>
                {paymentMsg ? <span className="self-center w-full text-xs text-brand-forest mt-2">{paymentMsg}</span> : null}
              </div>
            ) : null}
          </div>
        ) : (
          <div className="mt-10 rounded-[1.5rem] bg-brand-paper p-6 text-brand-ink">
            <p>
              {t('booking.noDetails')}
            </p>
          </div>
        )}

        <div className="mt-9 flex flex-col gap-3 sm:flex-row">
          <Link
            href="/my-bookings"
            className="inline-flex min-h-12 items-center justify-center rounded-full bg-brand-paper px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest-deep"
          >
            {t('booking.viewMyBookings')}
          </Link>
          <Link
            href="/villas"
            className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper/40 px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-paper"
          >
            {t('booking.exploreMoreVillas')}
          </Link>
        </div>
      </div>
    </section>
  );
};