'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';

type Booking = {
  id: number;
  room?: { id: number; roomNumber?: string };
  checkInDate: string;
  checkOutDate: string;
  status: string;
  paidAt?: string;
  approvedAt?: string;
  createdAt?: string;
};

type NotifType = 'booking_confirmed' | 'booking_cancelled' | 'payment_success' | 'upcoming_stay' | 'general';

type Notification = {
  id: string;
  type: NotifType;
  title: string;
  body: string;
  date: string;
  link?: string;
  read: boolean;
};

type BackendNotification = {
  id: number;
  title: string;
  message: string;
  type: string;
  isRead?: boolean;
  link?: string;
  createdAt: string;
};

type NotificationPayload = {
  notifications: BackendNotification[];
  unreadCount: number;
};

function mapNotificationType(type?: string): NotifType {
  switch ((type ?? '').toUpperCase()) {
    case 'BOOKING':
      return 'booking_confirmed';
    case 'PAYMENT':
    case 'REFUND':
      return 'payment_success';
    case 'PROMOTION':
      return 'general';
    default:
      return 'general';
  }
}

function fromBackendNotification(n: BackendNotification): Notification {
  return {
    id: String(n.id),
    type: mapNotificationType(n.type),
    title: n.title,
    body: n.message,
    date: n.createdAt,
    link: n.link,
    read: Boolean(n.isRead),
  };
}

function fmtDate(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleDateString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
    });
  } catch { return dateStr; }
}

const TYPE_CONFIG: Record<NotifType, { icon: React.ReactNode; color: string }> = {
  booking_confirmed: {
    color: 'bg-brand-forest/10 text-brand-forest',
    icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>,
  },
  booking_cancelled: {
    color: 'bg-red-100 text-red-600',
    icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>,
  },
  payment_success: {
    color: 'bg-emerald-100 text-emerald-700',
    icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>,
  },
  upcoming_stay: {
    color: 'bg-amber-100 text-amber-700',
    icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>,
  },
  general: {
    color: 'bg-blue-100 text-blue-600',
    icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>,
  },
};

function synthesizeNotifications(bookings: Booking[]): Notification[] {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const notifications: Notification[] = [];

  for (const b of bookings) {
    const roomLabel = `Villa ${b.room?.roomNumber ?? b.id}`;
    const link = `/account/bookings`;

    // Booking confirmed
    if (['PAID', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED'].includes(b.status) && (b.approvedAt || b.paidAt)) {
      notifications.push({
        id: `confirmed-${b.id}`,
        type: 'booking_confirmed',
        title: 'Booking Confirmed',
        body: `Your booking for ${roomLabel} (check-in ${fmtDate(b.checkInDate)}) has been confirmed.`,
        date: b.approvedAt ?? b.paidAt ?? b.createdAt ?? new Date().toISOString(),
        link,
        read: true,
      });
    }

    // Cancelled
    if (['CANCELLED', 'EXPIRED', 'NO_SHOW'].includes(b.status)) {
      notifications.push({
        id: `cancelled-${b.id}`,
        type: 'booking_cancelled',
        title: 'Booking Cancelled',
        body: `Your booking #${b.id} for ${roomLabel} has been cancelled.`,
        date: b.createdAt ?? new Date().toISOString(),
        link,
        read: true,
      });
    }

    // Payment success
    if (b.paidAt) {
      notifications.push({
        id: `paid-${b.id}`,
        type: 'payment_success',
        title: 'Payment Received',
        body: `Payment confirmed for booking #${b.id} — ${roomLabel}.`,
        date: b.paidAt,
        link,
        read: true,
      });
    }

    // Upcoming stay (within 7 days)
    if (['PAID'].includes(b.status)) {
      const cin = new Date(b.checkInDate); cin.setHours(0, 0, 0, 0);
      const daysUntil = Math.ceil((cin.getTime() - today.getTime()) / 86400000);
      if (daysUntil >= 0 && daysUntil <= 7) {
        notifications.push({
          id: `upcoming-${b.id}`,
          type: 'upcoming_stay',
          title: 'Upcoming Stay',
          body: `Your stay at ${roomLabel} begins in ${daysUntil === 0 ? 'today' : `${daysUntil} day${daysUntil !== 1 ? 's' : ''}`} (${fmtDate(b.checkInDate)}).`,
          date: new Date().toISOString(),
          link,
          read: false,
        });
      }
    }
  }

  return notifications.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}

type FilterKey = 'all' | NotifType;

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'all',               label: 'All' },
  { key: 'upcoming_stay',     label: 'Upcoming' },
  { key: 'booking_confirmed', label: 'Confirmations' },
  { key: 'payment_success',   label: 'Payments' },
  { key: 'booking_cancelled', label: 'Cancellations' },
];

export function NotificationsPage() {
  const [filter, setFilter] = useState<FilterKey>('all');
  const [readIds, setReadIds] = useState<Set<string>>(new Set());

  const { data: notificationPayload, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => api.get<NotificationPayload>(API_PATHS.notifications.list),
    retry: false,
  });

  const all = useMemo(
    () => (notificationPayload?.notifications ?? []).map(fromBackendNotification),
    [notificationPayload],
  );

  const displayed = useMemo(() => {
    if (filter === 'all') return all;
    return all.filter((n) => n.type === filter);
  }, [all, filter]);

  const unreadCount = useMemo(() => all.filter((n) => !n.read && !readIds.has(n.id)).length, [all, readIds]);

  async function markAllRead() {
    await api.post(API_PATHS.notifications.markAllRead, {});
    setReadIds(new Set(all.map((n) => n.id)));
  }

  return (
    <div>
      <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Account</p>
          <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">Notifications</h1>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllRead}
            className="rounded-full border border-brand-stone px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.12em] text-brand-ink transition hover:bg-brand-ink/5"
          >
            Mark all as read ({unreadCount})
          </button>
        )}
      </div>

      {/* Filter tabs */}
      <div className="mb-5 flex gap-1.5 overflow-x-auto rounded-2xl bg-brand-paper p-1.5 shadow-[0_4px_20px_rgba(32,52,43,0.06)]">
        {FILTERS.map((f) => {
          const count = f.key === 'all' ? all.length : all.filter((n) => n.type === f.key).length;
          const isActive = filter === f.key;
          return (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`flex shrink-0 items-center gap-1.5 rounded-xl px-4 py-2.5 text-sm font-medium transition-all ${
                isActive
                  ? 'bg-brand-forest text-brand-white shadow-sm'
                  : 'text-brand-ink/60 hover:bg-brand-ink/4 hover:text-brand-ink'
              }`}
            >
              {f.label}
              {count > 0 && (
                <span className={`rounded-full px-1.5 text-[0.6rem] font-bold ${isActive ? 'bg-brand-white/20 text-brand-white' : 'bg-brand-ink/8 text-brand-ink/60'}`}>
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* List */}
      {isLoading ? (
        <div className="space-y-3">
          {[1,2,3].map((i) => <div key={i} className="h-20 animate-pulse rounded-2xl bg-brand-paper" />)}
        </div>
      ) : displayed.length === 0 ? (
        <div className="rounded-2xl bg-brand-paper p-12 text-center shadow-[0_4px_24px_rgba(32,52,43,0.06)]">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-sage/10 text-brand-sage">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
            </svg>
          </div>
          <h2 className="mt-5 text-2xl text-brand-charcoal">No notifications yet.</h2>
          <p className="mt-3 text-sm text-brand-ink/58">
            Booking confirmations, payment updates, and reminders will appear here.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {displayed.map((n) => {
            const isRead = n.read || readIds.has(n.id);
            const cfg = TYPE_CONFIG[n.type];
            return (
              <li
                key={n.id}
                className={`rounded-2xl bg-brand-paper p-5 shadow-[0_2px_12px_rgba(32,52,43,0.06)] transition-shadow hover:shadow-[0_4px_24px_rgba(32,52,43,0.10)] ${!isRead ? 'border-l-4 border-brand-forest' : ''}`}
              >
                <div className="flex items-start gap-4">
                  <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${cfg.color}`}>
                    {cfg.icon}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className={`text-sm font-semibold ${isRead ? 'text-brand-ink/70' : 'text-brand-charcoal'}`}>
                        {n.title}
                        {!isRead && <span className="ml-2 inline-block h-2 w-2 rounded-full bg-brand-forest align-middle" />}
                      </p>
                      <time className="text-xs text-brand-ink/40">{fmtDate(n.date)}</time>
                    </div>
                    <p className={`mt-1 text-sm ${isRead ? 'text-brand-ink/50' : 'text-brand-ink/70'}`}>{n.body}</p>
                    {n.link && (
                      <Link
                        href={n.link}
                        onClick={() => setReadIds((prev) => new Set([...prev, n.id]))}
                        className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-brand-forest hover:underline underline-offset-4"
                      >
                        View Booking →
                      </Link>
                    )}
                  </div>
                  {!isRead && (
                    <button
                      onClick={() => setReadIds((prev) => new Set([...prev, n.id]))}
                      className="shrink-0 text-xs text-brand-ink/40 hover:text-brand-ink transition-colors"
                      title="Mark as read"
                    >
                      ✓
                    </button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
