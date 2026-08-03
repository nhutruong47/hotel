'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';

type AccountDashboard = {
  totalBookings: number;
  upcomingCount: number;
  currentCount: number;
  completedCount: number;
  cancelledCount: number;
  wishlistCount: number;
  unreadNotifications: number;
  totalSpent: number | string;
  nextStay?: {
    id: number;
    checkInDate?: string;
    checkOutDate?: string;
    status?: string;
    totalPrice?: number | string;
    room?: { roomNumber?: string; imageUrl?: string };
  } | null;
};

function formatVnd(value: number | string | undefined): string {
  if (value == null) return '-';
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return String(value);
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(num);
}

export function AccountDashboardPage() {
  const dashboard = useQuery({
    queryKey: ['account-dashboard'],
    queryFn: () => api.get<AccountDashboard>(API_PATHS.accountDashboard),
    retry: false,
  });

  if (dashboard.isLoading || !dashboard.data) {
    return <div className="h-72 animate-pulse rounded-2xl bg-brand-paper" />;
  }

  const data = dashboard.data;
  const metrics = [
    ['Bookings', data.totalBookings, '/account/bookings'],
    ['Upcoming', data.upcomingCount, '/account/bookings'],
    ['Wishlist', data.wishlistCount, '/account/wishlist'],
    ['Unread', data.unreadNotifications, '/account/notifications'],
  ];

  return (
    <div>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Account</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">Your stay overview</h1>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metrics.map(([label, value, href]) => (
          <Link key={String(label)} href={String(href)} className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)] transition hover:-translate-y-0.5">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/45">{label}</p>
            <p className="mt-3 text-3xl text-brand-charcoal">{value}</p>
          </Link>
        ))}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)]">
          <h2 className="text-xl font-semibold text-brand-charcoal">Next stay</h2>
          {data.nextStay ? (
            <div className="mt-5 rounded-xl bg-brand-white p-5">
              <p className="text-sm font-semibold uppercase tracking-[0.14em] text-brand-sage">
                Booking #{data.nextStay.id}
              </p>
              <p className="mt-3 text-2xl text-brand-charcoal">{data.nextStay.room?.roomNumber ?? 'Villa stay'}</p>
              <p className="mt-2 text-sm text-brand-ink/62">
                {data.nextStay.checkInDate} to {data.nextStay.checkOutDate} · {data.nextStay.status}
              </p>
              <p className="mt-3 text-sm font-semibold text-brand-forest">{formatVnd(data.nextStay.totalPrice)}</p>
              <Link href={`/account/bookings/${data.nextStay.id}`} className="mt-5 inline-flex min-h-11 items-center rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
                View booking
              </Link>
            </div>
          ) : (
            <div className="mt-5 rounded-xl bg-brand-white p-5 text-sm text-brand-ink/60">
              No upcoming paid stay yet.
            </div>
          )}
        </section>

        <section className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)]">
          <h2 className="text-xl font-semibold text-brand-charcoal">Lifetime summary</h2>
          <dl className="mt-5 grid gap-4 text-sm">
            {[
              ['Current stays', data.currentCount],
              ['Completed stays', data.completedCount],
              ['Cancelled/expired', data.cancelledCount],
              ['Total spent', formatVnd(data.totalSpent)],
            ].map(([label, value]) => (
              <div key={String(label)} className="flex items-center justify-between rounded-xl bg-brand-white px-4 py-3">
                <dt className="text-brand-ink/58">{label}</dt>
                <dd className="font-semibold text-brand-charcoal">{value}</dd>
              </div>
            ))}
          </dl>
        </section>
      </div>
    </div>
  );
}
