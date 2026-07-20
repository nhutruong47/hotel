'use client';

import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { api, API_PATHS } from '../../shared/api/client';
import { notFound } from 'next/navigation';
import { useState } from 'react';
import { ReviewForm } from './components/ReviewForm';

function fmt(value: number | string | undefined): string {
  if (value == null) return '—';
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(num);
}

function fmtDateTime(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  } catch {
    return dateStr;
  }
}

function StatusBadge({ status, isPayment = false }: { status: string, isPayment?: boolean }) {
  let color = 'bg-gray-100 text-gray-600';
  if (isPayment) {
    const isOk = ['PAID', 'COMPLETED', 'REFUNDED'].includes(status);
    const isErr = ['FAILED'].includes(status);
    color = isOk ? 'bg-brand-sage/20 text-brand-forest' : isErr ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700';
  } else {
    const isOk = ['PAID', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED'].includes(status);
    const isErr = ['CANCELLED', 'EXPIRED', 'NO_SHOW'].includes(status);
    const isWarn = ['PENDING_PAYMENT'].includes(status);
    color = isOk ? 'bg-brand-sage/20 text-brand-forest' : isErr ? 'bg-red-100 text-red-700' : isWarn ? 'bg-amber-100 text-amber-700' : 'bg-blue-100 text-blue-700';
  }

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[0.65rem] font-semibold uppercase tracking-[0.1em] ${color}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
      {status.replace(/_/g, ' ')}
    </span>
  );
}

const BOOKING_STEPS = [
  'PENDING_PAYMENT',
  'PAID',
  'CHECKED_IN',
  'CHECKED_OUT',
  'COMPLETED'
];

function getStepIndex(status: string) {
  return BOOKING_STEPS.indexOf(status);
}

export const BookingDetailPage = ({ bookingId }: { bookingId: string }) => {
  const [showReviewModal, setShowReviewModal] = useState(false);

  const { data: booking, isLoading, error } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => api.get<any>(API_PATHS.bookings.booking(bookingId)),
  });

  const { data: timeline } = useQuery({
    queryKey: ['booking-timeline', bookingId],
    queryFn: () => api.get<any[]>(API_PATHS.bookings.timeline(bookingId)),
    enabled: !!booking,
  });

  const { data: paymentsRes } = useQuery({
    queryKey: ['booking-payments', bookingId],
    queryFn: () => api.get<any>(API_PATHS.payments.byBooking(bookingId)),
    enabled: !!booking,
  });

  const { data: reviewData } = useQuery({
    queryKey: ['booking-review', bookingId],
    queryFn: () => api.get<any>(API_PATHS.reviews.forBooking(bookingId)),
    enabled: !!booking && (booking.status === 'CHECKED_OUT' || booking.status === 'COMPLETED'),
  });

  const payments = paymentsRes?.payments || [];
  const latestPayment = payments.length > 0 ? payments[0] : null;
  const paymentStatus = latestPayment ? latestPayment.status : 'PENDING';

  if (isLoading) {
    return <div className="p-10 text-center text-brand-ink/50">Loading booking details...</div>;
  }

  if (error || !booking) {
    return notFound();
  }

  const currentIdx = getStepIndex(booking.status);
  const isErrStatus = ['CANCELLED', 'EXPIRED', 'NO_SHOW'].includes(booking.status);
  const canReview = !reviewData?.existing && (booking.status === 'CHECKED_OUT' || booking.status === 'COMPLETED');

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <Link href="/account/bookings" className="text-sm text-brand-forest hover:underline mb-2 inline-block">
            ← Back to Bookings
          </Link>
          <h1 className="text-3xl font-semibold text-brand-charcoal">Booking #{booking.id}</h1>
          <p className="text-sm text-brand-ink/60 mt-1">Placed on {fmtDateTime(booking.createdAt)}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex flex-col items-end">
             <span className="text-xs text-brand-ink/60 uppercase tracking-widest font-semibold mb-1">Booking Status</span>
             <StatusBadge status={booking.status} />
          </div>
          <div className="h-8 w-px bg-brand-stone mx-2"></div>
          <div className="flex flex-col items-end">
             <span className="text-xs text-brand-ink/60 uppercase tracking-widest font-semibold mb-1">Payment Status</span>
             <StatusBadge status={paymentStatus} isPayment={true} />
          </div>
        </div>
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          {/* Timeline Section */}
          <section className="bg-brand-paper rounded-[1.5rem] p-6 sm:p-8 shadow-[0_4px_24px_rgba(32,52,43,0.05)]">
            <h2 className="text-xl font-semibold text-brand-charcoal mb-6">Status Timeline</h2>
            
            <div className="relative border-l border-brand-stone ml-3 space-y-8">
              {BOOKING_STEPS.map((step, idx) => {
                 const isCompleted = currentIdx >= idx;
                 const isCurrent = currentIdx === idx;
                 
                 // if booking is in error state and we haven't reached this step, hide it
                 if (isErrStatus && idx > currentIdx && step !== 'PENDING_PAYMENT') return null;
                 
                 // Find matching timeline entry to show timestamp
                 const logEntry = timeline?.find(t => t.toStatus === step);
                 
                 return (
                   <div key={step} className={`relative pl-8 ${isCompleted ? 'opacity-100' : 'opacity-40'}`}>
                      <div className={`absolute -left-[9px] top-1 h-4 w-4 rounded-full border-2 border-brand-paper ${isCompleted ? 'bg-brand-forest' : 'bg-brand-stone'}`}></div>
                      <p className="text-sm font-bold text-brand-charcoal uppercase tracking-wider">
                        {step.replace(/_/g, ' ')}
                      </p>
                      {logEntry && (
                         <p className="text-xs text-brand-ink/60 mt-1">{fmtDateTime(logEntry.createdAt)}</p>
                      )}
                      {isCurrent && !isErrStatus && (
                         <p className="text-sm text-brand-ink/80 mt-2 bg-brand-sand p-3 rounded-xl border border-brand-stone/50">
                           {step === 'PENDING_PAYMENT' ? 'Awaiting payment confirmation.' :
                            step === 'PAID' ? 'Payment confirmed. Awaiting check in.' :
                            step === 'CHECKED_IN' ? 'Guest is currently checked in.' :
                            step === 'CHECKED_OUT' ? 'Guest checked out. Awaiting final completion.' :
                            'Stay is completed.'}
                         </p>
                      )}
                   </div>
                 );
              })}
              
              {isErrStatus && (
                 <div className="relative pl-8">
                    <div className="absolute -left-[9px] top-1 h-4 w-4 rounded-full border-2 border-brand-paper bg-red-500"></div>
                    <p className="text-sm font-bold text-red-600 uppercase tracking-wider">
                      {booking.status.replace(/_/g, ' ')}
                    </p>
                    <p className="text-sm text-brand-ink/80 mt-2 bg-red-50 p-3 rounded-xl border border-red-100">
                      Booking was marked as {booking.status.replace(/_/g, ' ')}.
                    </p>
                 </div>
              )}
            </div>
          </section>

          {/* Details Section */}
          <section className="bg-brand-paper rounded-[1.5rem] p-6 sm:p-8 shadow-[0_4px_24px_rgba(32,52,43,0.05)]">
            <h2 className="text-xl font-semibold text-brand-charcoal mb-6">Stay Information</h2>
            <div className="grid sm:grid-cols-2 gap-6 text-sm">
              <div>
                <dt className="text-brand-ink/60">Check-in</dt>
                <dd className="font-medium text-brand-charcoal mt-1">{booking.checkInDate}</dd>
              </div>
              <div>
                <dt className="text-brand-ink/60">Check-out</dt>
                <dd className="font-medium text-brand-charcoal mt-1">{booking.checkOutDate}</dd>
              </div>
              <div>
                <dt className="text-brand-ink/60">Guests</dt>
                <dd className="font-medium text-brand-charcoal mt-1">{booking.guests || '—'}</dd>
              </div>
              <div>
                <dt className="text-brand-ink/60">Guest Name</dt>
                <dd className="font-medium text-brand-charcoal mt-1">{booking.guestName}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-brand-ink/60">Notes</dt>
                <dd className="font-medium text-brand-charcoal mt-1">{booking.notes || 'No special requests.'}</dd>
              </div>
            </div>
          </section>
        </div>

        <div className="space-y-8">
          {/* Summary Section */}
          <section className="bg-brand-paper rounded-[1.5rem] p-6 shadow-[0_4px_24px_rgba(32,52,43,0.05)]">
            <h2 className="text-xl font-semibold text-brand-charcoal mb-6">Summary</h2>
            <dl className="space-y-4 text-sm">
              <div className="flex justify-between">
                <dt className="text-brand-ink/60">Subtotal</dt>
                <dd className="text-brand-charcoal">{fmt(booking.totalPrice)}</dd>
              </div>
              {booking.discountAmount > 0 && (
                <div className="flex justify-between text-brand-forest">
                  <dt>Discount</dt>
                  <dd>-{fmt(booking.discountAmount)}</dd>
                </div>
              )}
              <div className="flex justify-between border-t border-brand-stone pt-4 font-semibold text-base">
                <dt className="text-brand-charcoal">Total</dt>
                <dd className="text-brand-charcoal">{fmt(booking.totalPrice - (booking.discountAmount || 0))}</dd>
              </div>
            </dl>
            
            {booking.status !== 'CANCELLED' && booking.status !== 'EXPIRED' && (
              <div className="mt-6 border-t border-brand-stone pt-6 text-center">
                <a 
                  href={`/api/v1${API_PATHS.bookings.booking(booking.id)}/invoice`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-12 w-full items-center justify-center rounded-full border border-brand-forest text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-white"
                >
                  Download Invoice (PDF)
                </a>
              </div>
            )}
          </section>

          {canReview && (
            <section className="bg-brand-forest/5 rounded-[1.5rem] p-6 shadow-sm border border-brand-forest/20 text-center">
               <h3 className="text-brand-forest font-semibold mb-2">How was your stay?</h3>
               <p className="text-sm text-brand-ink/70 mb-4">Share your experience to help other guests make better choices.</p>
               <button
                  onClick={() => setShowReviewModal(true)}
                  className="w-full inline-flex h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
               >
                  Leave a Review
               </button>
            </section>
          )}
          {reviewData?.existing && (
            <section className="bg-brand-paper rounded-[1.5rem] p-6 shadow-[0_4px_24px_rgba(32,52,43,0.05)] border border-brand-stone text-center">
               <h3 className="text-brand-charcoal font-semibold mb-2">Thanks for your feedback!</h3>
               <p className="text-sm text-brand-ink/70 mb-4">You rated this stay {'★'.repeat(reviewData.existing.rating)}</p>
               <Link href="/account/reviews" className="text-sm font-semibold text-brand-forest hover:underline">View my reviews →</Link>
            </section>
          )}
        </div>
      </div>
      
      {showReviewModal && (
        <ReviewForm 
           bookingId={booking.id} 
           roomName={`Room ${booking.room?.roomNumber}`} 
           onClose={() => setShowReviewModal(false)} 
        />
      )}
    </div>
  );
};
