'use client';

import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';

type WishlistItem = {
  id: number;
  room: {
    id: number;
    roomNumber: string;
    pricePerNight: number;
    imageUrl?: string;
    description?: string;
    roomType?: { displayName?: string };
    capacity?: number;
    avgRating?: number;
  };
};

function fmt(value: number | string | undefined): string {
  if (value == null) return '—';
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0,
  }).format(num);
}

export function WishlistPage() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['wishlist'],
    queryFn: () => api.get<WishlistItem[]>(API_PATHS.wishlist),
    retry: false,
  });

  const removeMutation = useMutation<{ isAdded: boolean; changed: boolean }, ApiError, number>({
    mutationFn: (roomId) => api.delete<{ isAdded: boolean; changed: boolean }>(API_PATHS.wishlistItem(roomId)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['wishlist'] }),
  });

  return (
    <div>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Account</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">Wishlist</h1>
      </div>

      {isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-72 animate-pulse rounded-2xl bg-brand-paper" />
          ))}
        </div>
      ) : !data || data.length === 0 ? (
        <div className="rounded-2xl bg-brand-paper p-12 text-center shadow-[0_4px_24px_rgba(32,52,43,0.06)]">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-sage/10 text-brand-sage">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
          </div>
          <h2 className="mt-5 text-2xl text-brand-charcoal">No saved villas yet.</h2>
          <p className="mt-3 text-sm text-brand-ink/58">
            Tap the ♡ icon on any villa to save it here for later.
          </p>
          <Link
            href="/villas"
            className="mt-7 inline-flex rounded-full bg-brand-forest px-6 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
          >
            Browse Villas
          </Link>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((item) => (
            <article key={item.id} className="group overflow-hidden rounded-2xl bg-brand-paper shadow-[0_4px_24px_rgba(32,52,43,0.07)] transition-shadow hover:shadow-[0_8px_40px_rgba(32,52,43,0.12)]">
              {/* Thumbnail */}
              <div className="relative overflow-hidden">
                <Link href={`/villas/${item.room.id}`}>
                  <img
                    src={item.room.imageUrl || '/images/placeholder.svg'}
                    alt={item.room.roomNumber}
                    className="aspect-[4/3] w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                </Link>
                {/* Remove button */}
                <button
                  onClick={() => removeMutation.mutate(item.room.id)}
                  disabled={removeMutation.isPending}
                  title="Remove from wishlist"
                  className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-brand-paper/90 text-red-500 shadow-sm backdrop-blur-sm transition hover:bg-brand-paper hover:text-red-600 disabled:opacity-50"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                </button>
                {/* Room type badge */}
                {item.room.roomType?.displayName && (
                  <span className="absolute bottom-3 left-3 rounded-full bg-brand-forest-deep/80 px-3 py-1 text-[0.6rem] font-semibold uppercase tracking-wider text-brand-white backdrop-blur-sm">
                    {item.room.roomType.displayName}
                  </span>
                )}
              </div>

              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold text-brand-charcoal">Villa {item.room.roomNumber}</h3>
                    {item.room.capacity && (
                      <p className="mt-1 text-xs text-brand-ink/55">Up to {item.room.capacity} guests</p>
                    )}
                  </div>
                  {item.room.avgRating != null && Number(item.room.avgRating) > 0 && (
                    <div className="flex shrink-0 items-center gap-1 rounded-full bg-brand-forest/8 px-2.5 py-1">
                      <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="currentColor" className="text-amber-500"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                      <span className="text-xs font-semibold text-brand-forest">
                        {Number(item.room.avgRating).toFixed(1)}
                      </span>
                    </div>
                  )}
                </div>

                <p className="mt-3 text-sm text-brand-ink/70">
                  <span className="font-semibold text-brand-charcoal">{fmt(item.room.pricePerNight)}</span>
                  <span className="text-brand-ink/45"> / night</span>
                </p>

                {item.room.description && (
                  <p className="mt-2 line-clamp-2 text-xs text-brand-ink/55">{item.room.description}</p>
                )}

                <div className="mt-4 flex gap-2">
                  <Link
                    href={`/booking/${item.room.id}`}
                    className="flex-1 rounded-full bg-brand-forest py-2.5 text-center text-xs font-semibold uppercase tracking-[0.12em] text-brand-white transition hover:bg-brand-forest-deep"
                  >
                    Book Now
                  </Link>
                  <Link
                    href={`/villas/${item.room.id}`}
                    className="flex-1 rounded-full border border-brand-stone py-2.5 text-center text-xs font-semibold uppercase tracking-[0.12em] text-brand-ink transition hover:bg-brand-ink/5"
                  >
                    View Details
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
