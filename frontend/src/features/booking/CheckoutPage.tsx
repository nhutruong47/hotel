'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { formatCurrency, type BookingDraft } from './bookingUtils';
import { useCheckout } from './useCheckout';
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
    status: string;
    totalPrice: number;
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
  const [paymentMethod, setPaymentMethod] = useState<'stripe' | 'bank-transfer' | 'mock'>('stripe');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const {
    isProcessing,
    error,
    checkoutMode,
    setCheckoutMode,
    createBooking,
    initiateStripeCheckout,
    simulateMockPayment,
    clearError,
  } = useCheckout();

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
  }, [email, guestName, phone, profile, user]);

  // Handle error from checkout hook
  useEffect(() => {
    if (error) {
      setErrorMsg(error);
    }
  }, [error]);

  const handlePaymentMethodChange = (method: 'stripe' | 'bank-transfer' | 'mock') => {
    setPaymentMethod(method);
    setCheckoutMode(method === 'stripe' ? 'stripe' : method === 'mock' ? 'mock' : 'bank-transfer');
    clearError();
  };

  const handleSubmit = async () => {
    if (!draft) return;
    
    setErrorMsg(null);
    clearError();

    try {
      // Step 1: Create booking
      const bookingData = await createBooking({
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

      const bookingId = bookingData.booking.id;
      const reference = bookingData.reference || `NV-${bookingId}`;

      // Step 2: Process payment based on method
      if (paymentMethod === 'stripe') {
        // Initiate Stripe Checkout
        await initiateStripeCheckout(
          bookingId,
          `${window.location.origin}/booking/success?ref=${reference}`,
          `${window.location.origin}/checkout`
        );
      } else if (paymentMethod === 'mock') {
        // Simulate mock payment
        const successData = await simulateMockPayment(bookingId, draft.total);
        
        if (successData) {
          // Store success data and redirect
          window.sessionStorage.setItem(
            'nhu.bookingSuccess',
            JSON.stringify({
              ...draft,
              reference,
              guestName,
              email,
              phone,
              notes,
              paymentMethod: 'Mock Payment',
              bookingId,
              status: 'Confirmed',
            })
          );
          router.push(`/booking/success?ref=${reference}`);
        }
      } else {
        // Bank transfer - store and redirect
        window.sessionStorage.setItem(
          'nhu.bookingSuccess',
          JSON.stringify({
            ...draft,
            reference,
            guestName,
            email,
            phone,
            notes,
            paymentMethod: 'Bank Transfer',
            bookingId,
            status: 'Pending Confirmation',
          })
        );
        router.push(`/booking/success?ref=${reference}`);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMsg(err.message);
      } else {
        setErrorMsg(t('errors.somethingWentWrong'));
      }
    }
  };

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

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto grid max-w-[1180px] gap-8 lg:grid-cols-[0.95fr_0.65fr]">
        <div className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.1)] sm:p-8">
          <Link href={`/villas/${draft.villaId}`} className="mb-6 inline-block text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage transition-colors hover:text-brand-forest">
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
            
            {/* Payment Method Selection */}
            <fieldset className="rounded-[1.5rem] bg-brand-white p-5">
              <legend className="text-sm font-semibold text-brand-charcoal">{t('booking.paymentPref')}</legend>
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                {/* Stripe Card Payment */}
                <label className={`flex cursor-pointer items-center gap-3 rounded-full border px-4 py-3 transition-colors ${paymentMethod === 'stripe' ? 'border-brand-forest bg-brand-forest/5' : 'border-brand-stone hover:border-brand-sage'}`}>
                  <input
                    type="radio"
                    name="payment"
                    value="stripe"
                    checked={paymentMethod === 'stripe'}
                    onChange={() => handlePaymentMethodChange('stripe')}
                    className="sr-only"
                  />
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-indigo-600 to-purple-500 text-white text-xs font-bold">S</span>
                  <div className="flex-1">
                    <span className="block text-sm font-medium text-brand-charcoal">Stripe</span>
                    <span className="text-xs text-brand-ink/62">Credit Card</span>
                  </div>
                </label>
                
                {/* Bank Transfer */}
                <label className={`flex cursor-pointer items-center gap-3 rounded-full border px-4 py-3 transition-colors ${paymentMethod === 'bank-transfer' ? 'border-brand-forest bg-brand-forest/5' : 'border-brand-stone hover:border-brand-sage'}`}>
                  <input
                    type="radio"
                    name="payment"
                    value="bank-transfer"
                    checked={paymentMethod === 'bank-transfer'}
                    onChange={() => handlePaymentMethodChange('bank-transfer')}
                    className="sr-only"
                  />
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-forest/10 text-brand-forest text-sm">B</span>
                  <div className="flex-1">
                    <span className="block text-sm font-medium text-brand-charcoal">Bank Transfer</span>
                    <span className="text-xs text-brand-ink/62">Manual Confirm</span>
                  </div>
                </label>
                
                {/* Mock Payment (Dev only) */}
                <label className={`flex cursor-pointer items-center gap-3 rounded-full border px-4 py-3 transition-colors ${paymentMethod === 'mock' ? 'border-brand-forest bg-brand-forest/5' : 'border-brand-stone hover:border-brand-sage'}`}>
                  <input
                    type="radio"
                    name="payment"
                    value="mock"
                    checked={paymentMethod === 'mock'}
                    onChange={() => handlePaymentMethodChange('mock')}
                    className="sr-only"
                  />
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-amber/10 text-brand-amber text-sm">M</span>
                  <div className="flex-1">
                    <span className="block text-sm font-medium text-brand-charcoal">Mock</span>
                    <span className="text-xs text-brand-ink/62">Testing Only</span>
                  </div>
                </label>
              </div>
              
              {/* Payment Instructions */}
              {paymentMethod === 'bank-transfer' && (
                <div className="mt-4 rounded-xl bg-brand-sage/10 p-4 text-sm">
                  <p className="font-medium text-brand-charcoal">Bank Transfer Instructions:</p>
                  <p className="mt-2 text-brand-ink/72">
                    Please transfer to account: <strong>1234567890</strong><br />
                    Bank: Vietcombank<br />
                    Name: Nhu Villas Hotel<br />
                    <span className="text-brand-amber">* Please include booking reference in transfer note</span>
                  </p>
                </div>
              )}
              
              {paymentMethod === 'mock' && (
                <div className="mt-4 rounded-xl bg-brand-amber/10 p-4 text-sm">
                  <p className="font-medium text-brand-amber">Development Mode</p>
                  <p className="mt-1 text-brand-ink/72">
                    Mock payment will simulate a successful transaction.
                    <span className="block mt-1 text-brand-amber/80">Do not use in production!</span>
                  </p>
                </div>
              )}
            </fieldset>
          </div>
          
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!isValid || isProcessing}
            className="mt-8 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
          >
            {isProcessing ? (
              <>
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                {t('booking.confirming')}
              </>
            ) : (
              paymentMethod === 'stripe' ? t('booking.payWithStripe') : 
              paymentMethod === 'mock' ? t('booking.simulatePayment') :
              t('booking.confirmBtn')
            )}
          </button>
        </div>
        
        {/* Order Summary Sidebar */}
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
                  <dd className="text-brand-green">-{formatCurrency(draft.discount)}</dd>
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
                <dd className="text-xl">{formatCurrency(draft.total)}</dd>
              </div>
            </dl>
            
            {/* Payment Method Badge */}
            <div className="mt-6 flex items-center justify-center gap-2 rounded-full bg-brand-sage/10 px-4 py-2">
              {paymentMethod === 'stripe' && (
                <>
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gradient-to-br from-indigo-600 to-purple-500 text-white text-xs font-bold">S</span>
                  <span className="text-xs font-medium text-brand-charcoal">Secure payment via Stripe</span>
                </>
              )}
              {paymentMethod === 'bank-transfer' && (
                <>
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-forest/10 text-brand-forest text-sm">B</span>
                  <span className="text-xs font-medium text-brand-charcoal">Bank Transfer</span>
                </>
              )}
              {paymentMethod === 'mock' && (
                <>
                  <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-amber/10 text-brand-amber text-sm">M</span>
                  <span className="text-xs font-medium text-brand-amber">Mock Mode</span>
                </>
              )}
            </div>
          </div>
        </aside>
      </div>
    </section>
  );
};
