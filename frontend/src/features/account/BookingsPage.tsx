'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useTranslation, useCurrency } from '../../shared/i18n/hooks';

// ─── Types ───────────────────────────────────────────────────────────────────

type Room = {
  id: number;
  roomNumber: string;
  imageUrl?: string;
  roomType?: { displayName?: string };
  pricePerNight?: number;
};

type Booking = {
  id: number;
  room?: Room;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
  status: string;
  guestName: string;
  guestPhone?: string;
  guestEmail?: string;
  guests?: number;
  notes?: string;
  paymentDeadline?: string;
  paidAt?: string;
  approvedAt?: string;
  createdAt?: string;
  rejectionReason?: string;
  refundAmount?: number;
  refundPercentage?: number;
  appliedVoucherCode?: string;
  discountAmount?: number;
};

type TabKey = 'all' | 'upcoming' | 'current' | 'completed' | 'cancelled';

// ─── Helpers ─────────────────────────────────────────────────────────────────

// Removed local fmt

function fmtDate(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleDateString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

function nights(checkIn: string, checkOut: string): number {
  const a = new Date(checkIn).getTime();
  const b = new Date(checkOut).getTime();
  return Math.max(1, Math.round((b - a) / 86400000));
}

const STATUS_CONFIG: Record<string, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: 'Pending Payment', color: 'bg-amber-100 text-amber-700' },
  PAID:            { label: 'Paid',            color: 'bg-brand-sage/20 text-brand-forest' },
  CHECKED_IN:      { label: 'Checked In',      color: 'bg-brand-forest/15 text-brand-forest-deep' },
  CHECKED_OUT:     { label: 'Checked Out',     color: 'bg-brand-sage/15 text-brand-forest' },
  COMPLETED:       { label: 'Completed',       color: 'bg-brand-sage/20 text-brand-forest' },
  CANCELLED:       { label: 'Cancelled',       color: 'bg-red-100 text-red-700' },
  EXPIRED:         { label: 'Expired',         color: 'bg-red-100 text-red-700' },
  NO_SHOW:         { label: 'No Show',         color: 'bg-red-100 text-red-700' },
};

function StatusBadge({ status, t }: { status: string; t: any }) {
  const cfg = STATUS_CONFIG[status] ?? { label: status, color: 'bg-gray-100 text-gray-600' };
  const translatedLabel = t(`bookings.status.${status}`) || cfg.label;
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-[0.65rem] font-semibold uppercase tracking-[0.1em] ${cfg.color}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
      {translatedLabel !== `bookings.status.${status}` ? translatedLabel : cfg.label}
    </span>
  );
}

function categorizeTabs(bookings: Booking[]): Record<TabKey, Booking[]> {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const result: Record<TabKey, Booking[]> = { all: [], upcoming: [], current: [], completed: [], cancelled: [] };
  for (const b of bookings) {
    result.all.push(b);
    const cin  = new Date(b.checkInDate); cin.setHours(0, 0, 0, 0);
    const cout = new Date(b.checkOutDate); cout.setHours(0, 0, 0, 0);
    const st = b.status;
    if (st === 'CANCELLED' || st === 'EXPIRED' || st === 'NO_SHOW') {
      result.cancelled.push(b);
    } else if (st === 'COMPLETED' || st === 'CHECKED_OUT') {
      result.completed.push(b);
    } else if (st === 'CHECKED_IN' || (cin <= today && cout > today)) {
      result.current.push(b);
    } else {
      result.upcoming.push(b);
    }
  }
  return result;
}

// ─── Booking Actions ──────────────────────────────────────────────────────────

function BookingActions({ booking, onCancelled }: { booking: Booking; onCancelled: () => void }) {
  const status = booking.status;
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const { formatCurrency } = useCurrency();

  const cancelMutation = useMutation<unknown, ApiError, void>({
    mutationFn: () => api.post(API_PATHS.bookings.cancel(booking.id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account-bookings'] });
      onCancelled();
    },
  });

  if (status === 'PENDING_PAYMENT') {
    return (
      <div className="flex flex-wrap gap-2">
        <Link
          href={`/checkout?bookingId=${booking.id}`}
          className="inline-flex items-center gap-2 rounded-full bg-brand-forest px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-white transition hover:bg-brand-forest-deep"
        >
          {t('bookings.payNow')}
        </Link>
        <button
          onClick={() => {
            if (confirm('Cancel this booking?')) cancelMutation.mutate();
          }}
          disabled={cancelMutation.isPending}
          className="inline-flex items-center rounded-full border border-red-200 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-red-600 transition hover:bg-red-50 disabled:opacity-50"
        >
          {t('bookings.cancel')}
        </button>
      </div>
    );
  }

  if (status === 'AWAITING_APPROVAL') {
    return (
      <div className="flex flex-wrap gap-2">
        <span className="inline-flex items-center rounded-full bg-blue-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-blue-500">
          Under Review
        </span>
        <button
          onClick={() => { if (confirm('Cancel this booking?')) cancelMutation.mutate(); }}
          disabled={cancelMutation.isPending}
          className="inline-flex items-center rounded-full border border-red-200 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-red-600 transition hover:bg-red-50 disabled:opacity-50"
        >
          {t('bookings.cancel')}
        </button>
      </div>
    );
  }

  if (status === 'PAID') {
    return (
      <div className="flex flex-wrap gap-2">
        <Link
          href={`/account/bookings/${booking.id}`}
          className="inline-flex items-center rounded-full bg-brand-forest/8 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-forest transition hover:bg-brand-forest/14"
        >
          {t('bookings.viewDetails')}
        </Link>
        <Link
          href={`/villas/${booking.room?.id || ''}?reviewBookingId=${booking.id}#reviews`}
          className="inline-flex items-center gap-1.5 rounded-full bg-brand-forest/8 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-forest transition hover:bg-brand-forest/14"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          {t('bookings.leaveReview')}
        </Link>
        <button
          onClick={() => { if (confirm('Cancel this booking? Cancellation policies apply.')) cancelMutation.mutate(); }}
          disabled={cancelMutation.isPending}
          className="inline-flex items-center rounded-full border border-red-200 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-red-600 transition hover:bg-red-50 disabled:opacity-50"
        >
          {t('bookings.cancel')}
        </button>
      </div>
    );
  }

  if (status === 'CHECKED_IN') {
    return (
      <div className="flex flex-wrap gap-2">
        <a
          href="tel:+84901234567"
          className="inline-flex items-center gap-1.5 rounded-full bg-brand-forest px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-white transition hover:bg-brand-forest-deep"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.41 2 2 0 0 1 3.6 1.2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.37a16 16 0 0 0 6.72 6.72l1.73-1.73a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
          Contact Reception
        </a>
        <Link
          href="/contact"
          className="inline-flex items-center rounded-full border border-brand-stone px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-ink transition hover:bg-brand-ink/5"
        >
          Request Service
        </Link>
      </div>
    );
  }

  if (status === 'COMPLETED' || status === 'CHECKED_OUT') {
    return (
      <div className="flex flex-wrap gap-2">
        <Link
          href={`/villas/${booking.room?.id || ''}?reviewBookingId=${booking.id}#reviews`}
          className="inline-flex items-center gap-1.5 rounded-full bg-brand-forest/8 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-forest transition hover:bg-brand-forest/14"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
          {t('bookings.leaveReview')}
        </Link>
        {booking.room?.id && (
          <Link
            href={`/villas/${booking.room.id}`}
            className="inline-flex items-center rounded-full border border-brand-stone px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-ink transition hover:bg-brand-ink/5"
          >
            {t('bookings.bookAgain')}
          </Link>
        )}
      </div>
    );
  }

  if (status === 'CANCELLED' || status === 'EXPIRED' || status === 'NO_SHOW') {
    return (
      <div className="flex flex-wrap gap-2">
        <Link
          href={`/account/bookings/${booking.id}`}
          className="inline-flex items-center rounded-full bg-brand-forest/8 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-brand-forest transition hover:bg-brand-forest/14"
        >
          {t('bookings.viewDetails')}
        </Link>
        {booking.refundAmount != null && Number(booking.refundAmount) > 0 && (
          <span className="inline-flex items-center rounded-full bg-amber-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.1em] text-amber-700">
            Refund: {formatCurrency(booking.refundAmount)}
          </span>
        )}
      </div>
    );
  }

  return null;
}

// ─── Booking Card ─────────────────────────────────────────────────────────────

function BookingCard({ booking }: { booking: Booking }) {
  const [cancelled, setCancelled] = useState(false);
  const nightsCount = nights(booking.checkInDate, booking.checkOutDate);
  const { t } = useTranslation();
  const { formatCurrency } = useCurrency();

  if (cancelled) {
    return (
      <div className="rounded-2xl border border-red-100 bg-red-50/50 p-5 text-sm text-red-700">
        Booking #{booking.id} has been cancelled.
      </div>
    );
  }

  return (
    <article className="rounded-2xl bg-brand-paper shadow-[0_4px_24px_rgba(32,52,43,0.07)] transition-shadow hover:shadow-[0_8px_40px_rgba(32,52,43,0.12)]">
      <div className="flex flex-col sm:flex-row">
        {/* Villa thumbnail */}
        <div className="relative h-44 w-full shrink-0 overflow-hidden rounded-t-2xl sm:h-auto sm:w-44 sm:rounded-l-2xl sm:rounded-tr-none">
          {booking.room?.id ? (
            <Link href={`/villas/${booking.room.id}`} className="block h-full w-full">
              {booking.room?.imageUrl ? (
                <img
                  src={booking.room.imageUrl}
                  alt={`Villa ${booking.room.roomNumber}`}
                  className="h-full w-full object-cover transition-transform hover:scale-105"
                />
              ) : (
                <div className="flex h-full w-full min-h-[110px] items-center justify-center bg-brand-forest/8 transition-colors hover:bg-brand-forest/15">
                  <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="text-brand-forest/30">
                    <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
                  </svg>
                </div>
              )}
            </Link>
          ) : (
            booking.room?.imageUrl ? (
              <img
                src={booking.room.imageUrl}
                alt={`Villa ${booking.room.roomNumber ?? '—'}`}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full min-h-[110px] items-center justify-center bg-brand-forest/8">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="text-brand-forest/30">
                  <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
                </svg>
              </div>
            )
          )}
          <div className="pointer-events-none absolute bottom-2 left-2">
            <StatusBadge status={booking.status} t={t} />
          </div>
        </div>

        {/* Content */}
        <div className="flex min-w-0 flex-1 flex-col p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[0.65rem] font-semibold uppercase tracking-[0.18em] text-brand-sage">
                #{booking.id} · {booking.room?.roomType?.displayName || 'Villa'}
              </p>
              <h3 className="mt-1 truncate text-xl font-semibold text-brand-charcoal">
                {booking.room?.id ? (
                  <Link href={`/villas/${booking.room.id}`} className="hover:text-brand-forest transition-colors">
                    Villa {booking.room.roomNumber}
                  </Link>
                ) : (
                  `Villa ${booking.room?.roomNumber ?? '—'}`
                )}
              </h3>
            </div>
            <div className="text-right">
              <p className="text-lg font-semibold text-brand-charcoal">{formatCurrency(booking.totalPrice)}</p>
              <p className="text-xs text-brand-ink/50">{nightsCount} {t('bookings.nights')}</p>
            </div>
          </div>

          {/* Dates row */}
          <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2 text-sm text-brand-ink/70">
            <span className="flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {fmtDate(booking.checkInDate)} → {fmtDate(booking.checkOutDate)}
            </span>
            {booking.guests && (
              <span className="flex items-center gap-1.5">
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                {booking.guests} {t('bookings.guests')}
              </span>
            )}
            <span className="flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              {booking.guestName}
            </span>
          </div>

          {/* Booking timeline */}
          <div className="mt-4 flex items-center gap-0">
            {[
              { label: 'Booked', done: true },
              { label: 'Paid', done: ['PAID','CHECKED_IN','CHECKED_OUT','COMPLETED'].includes(booking.status) || !!booking.paidAt },
              { label: 'Checked In', done: ['CHECKED_IN','CHECKED_OUT','COMPLETED'].includes(booking.status) },
            ].map((step, i) => (
              <div key={step.label} className="flex items-center">
                <div className="flex flex-col items-center">
                  <div className={`h-2.5 w-2.5 rounded-full border-2 ${step.done ? 'border-brand-forest bg-brand-forest' : 'border-brand-stone bg-brand-paper'}`} />
                  <span className={`mt-1 text-[0.55rem] font-semibold uppercase tracking-wide ${step.done ? 'text-brand-forest' : 'text-brand-ink/30'}`}>
                    {step.label}
                  </span>
                </div>
                {i < 3 && (
                  <div className={`mb-3 h-px w-10 ${step.done ? 'bg-brand-forest' : 'bg-brand-stone'}`} />
                )}
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="mt-4 border-t border-brand-ink/6 pt-4">
            <BookingActions booking={booking} onCancelled={() => setCancelled(true)} />
          </div>
        </div>
      </div>
    </article>
  );
}

// ─── Tab Bar ──────────────────────────────────────────────────────────────────

const TABS: { key: TabKey; label: string }[] = [
  { key: 'all',       label: 'All' },
  { key: 'upcoming',  label: 'Upcoming' },
  { key: 'current',   label: 'Current Stay' },
  { key: 'completed', label: 'Completed' },
  { key: 'cancelled', label: 'Cancelled' },
];

// ─── Main Page ────────────────────────────────────────────────────────────────

const PAGE_SIZE = 8;

export function BookingsPage() {
  const [tab, setTab] = useState<TabKey>('all');
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<'newest' | 'oldest'>('newest');
  const [page, setPage] = useState(1);
  const { t } = useTranslation();

  const { data, isLoading } = useQuery({
    queryKey: ['account-bookings'],
    queryFn: () => api.get<Booking[]>(API_PATHS.bookings.list),
    retry: false,
  });

  const tabs = useMemo(() => categorizeTabs(data ?? []), [data]);

  const filtered = useMemo(() => {
    let list = tabs[tab] ?? [];
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter(
        (b) =>
          String(b.id).includes(q) ||
          b.room?.roomNumber?.toLowerCase().includes(q) ||
          b.guestName?.toLowerCase().includes(q)
      );
    }
    list = [...list].sort((a, b) => {
      const da = new Date(a.checkInDate).getTime();
      const db = new Date(b.checkInDate).getTime();
      return sort === 'newest' ? db - da : da - db;
    });
    return list;
  }, [tabs, tab, search, sort]);

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  return (
    <div>
      {/* Header */}
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('settings.subtitle')}</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">{t('bookings.title')}</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 overflow-x-auto rounded-2xl bg-brand-paper p-1.5 shadow-[0_4px_20px_rgba(32,52,43,0.06)]">
        {TABS.map((t) => {
          const count = tabs[t.key]?.length ?? 0;
          const isActive = tab === t.key;
          return (
            <button
              key={t.key}
              onClick={() => { setTab(t.key); setPage(1); }}
              className={`flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition-all ${
                isActive
                  ? 'bg-brand-forest text-brand-white shadow-sm'
                  : 'text-brand-ink/60 hover:bg-brand-ink/4 hover:text-brand-ink'
              }`}
            >
              {t.label}
              {count > 0 && (
                <span className={`rounded-full px-2 py-0.5 text-[0.6rem] font-bold ${
                  isActive ? 'bg-brand-white/20 text-brand-white' : 'bg-brand-ink/8 text-brand-ink/60'
                }`}>
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Search + Sort */}
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <svg className="absolute left-4 top-1/2 -translate-y-1/2 text-brand-ink/40" xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input
            type="search"
            placeholder="Search by booking ID, villa, or guest name…"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            className="h-11 w-full rounded-full border border-brand-stone bg-brand-paper pl-10 pr-4 text-sm placeholder:text-brand-ink/40 focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest"
          />
        </div>
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as 'newest' | 'oldest')}
          className="h-11 rounded-full border border-brand-stone bg-brand-paper px-4 text-sm text-brand-ink focus:border-brand-forest focus:outline-none"
        >
          <option value="newest">Newest first</option>
          <option value="oldest">Oldest first</option>
        </select>
      </div>

      {/* Content */}
      <div className="mt-5 space-y-4">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-48 animate-pulse rounded-2xl bg-brand-paper" />
          ))
        ) : paged.length === 0 ? (
          <div className="rounded-2xl bg-brand-paper p-12 text-center shadow-[0_4px_24px_rgba(32,52,43,0.06)]">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-sage/10 text-brand-sage">
              <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/>
              </svg>
            </div>
            <h2 className="mt-5 text-2xl text-brand-charcoal">
              {search ? 'No results found' : t('bookings.noBookings')}
            </h2>
            <p className="mt-3 text-sm text-brand-ink/58">
              {search
                ? 'Try a different search term.'
                : tab === 'all'
                ? t('bookings.subtitle')
                : 'Nothing in this category yet.'}
            </p>
            {tab === 'all' && !search && (
              <Link
                href="/villas"
                className="mt-7 inline-flex rounded-full bg-brand-forest px-6 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
              >
                {t('bookings.explore')}
              </Link>
            )}
          </div>
        ) : (
          paged.map((b) => <BookingCard key={b.id} booking={b} />)
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            disabled={page === 1}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-brand-stone text-brand-ink transition hover:bg-brand-ink/5 disabled:opacity-30"
          >
            ‹
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              onClick={() => setPage(n)}
              className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-medium transition ${
                page === n
                  ? 'bg-brand-forest text-brand-white'
                  : 'border border-brand-stone text-brand-ink hover:bg-brand-ink/5'
              }`}
            >
              {n}
            </button>
          ))}
          <button
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            disabled={page === totalPages}
            className="flex h-9 w-9 items-center justify-center rounded-full border border-brand-stone text-brand-ink transition hover:bg-brand-ink/5 disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}
    </div>
  );
}
