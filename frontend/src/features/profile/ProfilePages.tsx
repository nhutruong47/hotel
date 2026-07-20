'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'next/navigation';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useTranslation } from '../../shared/i18n/hooks';

type SessionUser = {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  avatarFilename?: string | null;
  avatarUrl?: string | null;
  emailVerified?: boolean;
};

type Booking = {
  id: number;
  room?: { id: number; roomNumber?: string };
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
  status: string;
  guestName: string;
};

function PageShell({
  eyebrow,
  title,
  children,
}: {
  eyebrow: string;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1280px]">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{eyebrow}</p>
        <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-7xl">{title}</h1>
        <div className="mt-10">{children}</div>
      </div>
    </section>
  );
}

export const ProfilePage = () => {
  const { t } = useTranslation();
  const router = useRouter();
  const sessionQuery = useQuery({
    queryKey: ['session'],
    queryFn: () => api.get<{ user: SessionUser | null }>(API_PATHS.auth.session),
    retry: false,
  });

  useEffect(() => {
    if (sessionQuery.isFetched && !sessionQuery.data?.user) {
      router.push('/login');
    }
  }, [sessionQuery.data, sessionQuery.isFetched, router]);

  const user = sessionQuery.data?.user;

  const logoutMutation = useMutation<{ message: string }, ApiError, void>({
    mutationFn: () => api.post<{ message: string }>(API_PATHS.auth.logout),
    onSuccess: () => router.push('/login'),
  });

  if (!user) {
    return (
      <PageShell eyebrow={t('profile.guestProfile')} title={t('profile.loading')}>
        <div className="rounded-[2rem] bg-brand-paper p-10 text-center text-brand-ink/58">
          {t('profile.redirecting')}
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell eyebrow={t('profile.guestProfile')} title={t('profile.title')}>
      <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-5">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-forest text-2xl text-brand-white">
              {(user.fullName || user.username).charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 className="text-2xl text-brand-charcoal">{user.fullName || user.username}</h2>
              <p className="text-sm text-brand-ink/62">{user.email}</p>
              {user.role === 'ADMIN' ? (
                <span className="mt-2 inline-flex rounded-full bg-brand-forest/15 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest">
                  {t('profile.admin')}
                </span>
              ) : null}
            </div>
          </div>
          <button
            type="button"
            onClick={() => logoutMutation.mutate()}
            className="rounded-full border border-brand-stone px-5 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink hover:bg-brand-white"
          >
            {t('profile.logout')}
          </button>
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        {[
          [t('nav.myBookings'), t('profile.myBookingsDesc'), '/my-bookings'],
          [t('nav.wishlist'), t('profile.wishlistDesc'), '/wishlist'],
          [t('nav.reviews'), t('profile.reviewsDesc'), '/reviews'],
        ].map(([title, copy, href]) => (
          <Link
            key={title}
            href={href}
            className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
          >
            <h2 className="text-3xl text-brand-charcoal">{title}</h2>
            <p className="mt-4 text-sm leading-7 text-brand-ink/62">{copy}</p>
          </Link>
        ))}
      </div>
    </PageShell>
  );
};

function formatCurrency(value: number | string): string {
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return '—';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(num);
}

export const MyBookingsPage = () => {
  const { t } = useTranslation();
  const router = useRouter();
  const query = useQuery({
    queryKey: ['my-bookings'],
    queryFn: () => api.get<Booking[]>(API_PATHS.bookings.list),
    retry: false,
  });

  useEffect(() => {
    if (query.isFetched && query.isError) router.push('/login');
  }, [query.isFetched, query.isError, router]);

  const cached = window.sessionStorage.getItem('nhu.bookingSuccess');

  return (
    <PageShell eyebrow={t('profile.bookingHistory')} title={t('profile.historyTitle')}>
      {query.isLoading ? (
        <div className="rounded-[2rem] bg-brand-paper p-10 text-center text-brand-ink/58">{t('profile.loadingBookings')}</div>
      ) : query.data && query.data.length > 0 ? (
        <ul className="grid gap-5">
          {query.data.map((b) => (
            <li key={b.id} className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">#{b.id}</p>
                  <h2 className="mt-2 text-2xl text-brand-charcoal">
                    {b.room?.id ? (
                      <Link href={`/villas/${b.room.id}`} className="text-brand-forest underline underline-offset-4 decoration-brand-forest/30 hover:decoration-brand-forest transition-all">
                        {`${t('villas.room')} ${b.room.roomNumber}`}
                      </Link>
                    ) : (
                      `${t('villas.room')} ${b.room?.roomNumber ?? '—'}`
                    )}
                  </h2>
                  <p className="mt-2 text-sm text-brand-ink/62">
                    {b.checkInDate} → {b.checkOutDate} · {b.guestName}
                  </p>
                </div>
                <div className="text-left lg:text-right">
                  <p className="text-xl text-brand-charcoal">{formatCurrency(b.totalPrice)}</p>
                  <p className="mt-2 text-sm text-brand-ink/58">{b.status}</p>
                </div>
              </div>
            </li>
          ))}
        </ul>
      ) : cached ? (
        <article className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          {(() => {
            const b = JSON.parse(cached) as {
              reference: string; villaName: string; checkIn: string; checkOut: string; total: number; status: string;
            };
            return (
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">{b.reference}</p>
                  <h2 className="mt-2 text-3xl text-brand-charcoal">{b.villaName}</h2>
                  <p className="mt-3 text-sm text-brand-ink/62">{b.checkIn} {t('common.to')} {b.checkOut}</p>
                </div>
                <div className="text-left lg:text-right">
                  <p className="text-xl text-brand-charcoal">{formatCurrency(b.total)}</p>
                  <p className="mt-2 text-sm text-brand-ink/58">{b.status}</p>
                </div>
              </div>
            );
          })()}
        </article>
      ) : (
        <div className="rounded-[2rem] bg-brand-paper p-10 text-center">
          <h2 className="text-4xl text-brand-charcoal">{t('profile.noBookings')}</h2>
          <p className="mt-4 text-brand-ink/62">{t('profile.noBookingsDesc')}</p>
          <Link href="/villas" className="mt-8 inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            {t('nav.villas')}
          </Link>
        </div>
      )}
    </PageShell>
  );
};

type WishlistItem = {
  id: number;
  room: {
    id: number;
    roomNumber: string;
    pricePerNight: number;
    imageUrl?: string;
    description?: string;
    amenities?: unknown[];
  };
};

export const WishlistPage = () => {
  const { t } = useTranslation();
  const router = useRouter();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ['wishlist'],
    queryFn: () => api.get<WishlistItem[]>(API_PATHS.wishlist),
    retry: false,
  });

  useEffect(() => {
    if (query.isFetched && query.isError) router.push('/login');
  }, [query.isFetched, query.isError, router]);

  const toggle = useMutation<{ isAdded: boolean }, ApiError, number>({
    mutationFn: (roomId) => api.post<{ isAdded: boolean }>(API_PATHS.wishlistToggle, { roomId }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wishlist'] }),
  });

  return (
    <PageShell eyebrow={t('nav.wishlist')} title={t('profile.wishlistTitle')}>
      {query.isLoading ? (
        <div className="rounded-[2rem] bg-brand-paper p-10 text-center text-brand-ink/58">{t('profile.loadingWishlist')}</div>
      ) : !query.data || query.data.length === 0 ? (
        <div className="rounded-[2rem] bg-brand-paper p-10 text-center">
          <h2 className="text-4xl text-brand-charcoal">{t('profile.noWishlist')}</h2>
          <p className="mt-4 text-brand-ink/62">{t('profile.noWishlistDesc')}</p>
          <Link href="/villas" className="mt-8 inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            {t('nav.villas')}
          </Link>
        </div>
      ) : (
        <ul className="grid gap-6 md:grid-cols-3">
          {query.data.map((item) => (
            <li key={item.id} className="rounded-[2rem] bg-brand-paper p-2 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
              <Link href={`/villas/${item.room.id}`}>
                <img
                  src={item.room.imageUrl || '/images/placeholder.svg'}
                  alt={item.room.roomNumber}
                  className="aspect-[4/3] w-full rounded-[1.55rem] object-cover"
                />
              </Link>
              <div className="p-5">
                <h2 className="text-2xl text-brand-charcoal">{`${t('villas.room')} item.room.roomNumber`}</h2>
                <p className="mt-2 text-sm text-brand-ink/62">{formatCurrency(item.room.pricePerNight)} {t('villas.perNight')}</p>
                <button
                  type="button"
                  onClick={() => toggle.mutate(item.room.id)}
                  className="mt-4 rounded-full border border-brand-stone px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink hover:bg-brand-white"
                >
                  {t('common.remove')}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </PageShell>
  );
};

type ReviewItem = {
  id: number;
  rating: number;
  comment?: string;
  booking?: { id: number };
};

export const ReviewsPage = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const searchParams = useSearchParams();
  const [bookingId, setBookingId] = useState<string>(searchParams.get('bookingId') || '');

  useEffect(() => {
    const id = searchParams.get('bookingId');
    if (id) {
      setBookingId(id);
    }
  }, [searchParams]);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const bookingsQuery = useQuery({
    queryKey: ['my-bookings'],
    queryFn: () => api.get<Booking[]>(API_PATHS.bookings.list),
    retry: false,
  });

  const mineQuery = useQuery({
    queryKey: ['my-reviews'],
    queryFn: () => api.get<ReviewItem[]>(API_PATHS.reviews.mine),
    retry: false,
  });

  const submit = useMutation<{ message: string }, ApiError, { bookingId: number; rating: number; comment: string }>({
    mutationFn: (body) => api.post<{ message: string }>(API_PATHS.reviews.submitForBooking(body.bookingId), body),
    onSuccess: (data) => {
      setSuccessMsg(data.message);
      setComment('');
      setBookingId('');
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] });
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const eligible = (bookingsQuery.data ?? []).filter((b) => 
    ['PAID', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED'].includes(b.status)
  );

  return (
    <PageShell eyebrow={t('nav.reviews')} title={t('profile.reviewsTitle')}>
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <h2 className="text-3xl text-brand-charcoal">{t('profile.writeReview')}</h2>
          {errorMsg ? <p className="mt-4 rounded-full bg-brand-coral/10 px-4 py-3 text-sm">{errorMsg}</p> : null}
          {successMsg ? <p className="mt-4 rounded-full bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest">{successMsg}</p> : null}
          <form
            className="mt-6 grid gap-5"
            onSubmit={(e) => {
              e.preventDefault();
              setErrorMsg(null);
              setSuccessMsg(null);
              if (!bookingId) {
                setErrorMsg(t('errors.somethingWentWrong'));
                return;
              }
              submit.mutate({ bookingId: Number(bookingId), rating, comment });
            }}
          >
            <label>
              <span className="text-sm font-semibold text-brand-charcoal">{t('profile.booking')}</span>
              <select
                value={bookingId}
                onChange={(e) => setBookingId(e.target.value)}
                required
                className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              >
                <option value="">{t('profile.selectBooking')}</option>
                {eligible.map((b) => (
                  <option key={b.id} value={b.id}>
                    #{b.id} · {`${t('villas.room')} ${b.room?.roomNumber || ''}`} · {b.checkInDate}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span className="text-sm font-semibold text-brand-charcoal">{t('profile.rating')}</span>
              <select
                value={rating}
                onChange={(e) => setRating(Number(e.target.value))}
                className="mt-3 h-14 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              >
                <option value={5}>{t('profile.rating5')}</option>
                <option value={4}>{t('profile.rating4')}</option>
                <option value={3}>{t('profile.rating3')}</option>
                <option value={2}>{t('profile.rating2')}</option>
                <option value={1}>{t('profile.rating1')}</option>
              </select>
            </label>
            <label>
              <span className="text-sm font-semibold text-brand-charcoal">{t('profile.note')}</span>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="mt-3 min-h-36 w-full rounded-[1.25rem] border border-brand-stone bg-brand-white p-5"
              />
            </label>
            <button
              type="submit"
              disabled={submit.isPending}
              className="min-h-12 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
            >
              {submit.isPending ? t('common.submitting') : t('profile.submitReview')}
            </button>
          </form>
        </div>

        <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <h2 className="text-3xl text-brand-charcoal">{t('profile.yourReviews')}</h2>
          {mineQuery.isLoading ? (
            <p className="mt-4 text-brand-ink/58">{t('common.loading')}</p>
          ) : !mineQuery.data || mineQuery.data.length === 0 ? (
            <p className="mt-4 text-brand-ink/58">{t('profile.noReviews')}</p>
          ) : (
            <ul className="mt-6 space-y-4">
              {mineQuery.data.map((r) => (
                <li key={r.id} className="rounded-[1.5rem] bg-brand-white p-5">
                  <p className="text-sm font-semibold text-brand-forest">{'★'.repeat(r.rating)}</p>
                  <p className="mt-2 text-sm text-brand-ink/72">{r.comment || t('profile.noComment')}</p>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </PageShell>
  );
};