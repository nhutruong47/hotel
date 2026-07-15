'use client';

import { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dynamic from 'next/dynamic';
import { fetchVillas } from './villasApi';
import { fallbackVillas } from './villaFallbacks';
import { findVillaById, getSimilarVillas } from './villaSelectors';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import type { Villa } from './types';
import { useCurrency, useTranslation } from '../../shared/i18n/hooks';

const InteractiveMap = dynamic(() => import('./components/InteractiveMap'), {
  ssr: false,
});

const fallbackImages = [
  '/images/nhu-hero-villa.jpg',
  '/images/nhu-villa-interior.jpg',
  '/images/nhu-infinity-pool.jpg',
  '/images/nhu-private-dining.jpg',
];

interface Review {
  id: number;
  rating: number;
  comment: string;
  user: { fullName: string };
  createdAt: string;
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

function GalleryLightbox({
  images,
  currentIndex,
  onClose,
  onNext,
  onPrev,
}: {
  images: string[];
  currentIndex: number;
  onClose: () => void;
  onNext: () => void;
  onPrev: () => void;
}) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowRight') onNext();
      if (e.key === 'ArrowLeft') onPrev();
    };
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [onClose, onNext, onPrev]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/95">
      <button
        onClick={onClose}
        className="absolute right-6 top-6 z-10 flex h-12 w-12 items-center justify-center rounded-full bg-white/10 text-2xl text-white transition hover:bg-white/20"
      >
        ✕
      </button>
      <button
        onClick={onPrev}
        className="absolute left-6 top-1/2 z-10 flex h-12 w-12 items-center justify-center rounded-full bg-white/10 text-2xl text-white transition hover:bg-white/20"
      >
        ‹
      </button>
      <button
        onClick={onNext}
        className="absolute right-6 top-1/2 z-10 flex h-12 w-12 items-center justify-center rounded-full bg-white/10 text-2xl text-white transition hover:bg-white/20"
      >
        ›
      </button>
      <div className="relative max-h-[85vh] max-w-[90vw]">
        <img
          src={images[currentIndex]}
          alt={`Gallery ${currentIndex + 1}`}
          className="max-h-[85vh] max-w-[90vw] rounded-lg object-contain"
        />
        <p className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-black/50 px-4 py-2 text-sm text-white">
          {currentIndex + 1} / {images.length}
        </p>
      </div>
    </div>
  );
}

function StarRating({ rating, size = 'md' }: { rating: number; size?: 'sm' | 'md' }) {
  const sizeClass = size === 'sm' ? 'text-sm' : 'text-lg';
  return (
    <div className={`flex gap-0.5 ${sizeClass}`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={star <= rating ? 'text-amber-400' : 'text-gray-300'}>
          ★
        </span>
      ))}
    </div>
  );
}

function BookingCard({ villa }: { villa: Villa }) {
  const { formatCurrency } = useCurrency();
  const { t } = useTranslation();

  return (
    <aside className="rounded-[2rem] bg-brand-paper p-2 shadow-[0_24px_90px_rgba(32,52,43,0.12)] lg:sticky lg:top-28">
      <div className="rounded-[1.55rem] bg-brand-white p-6 sm:p-7">
        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-sage">{t('villas.from')}</p>
        <p className="mt-2 font-serif text-4xl text-brand-charcoal">{formatCurrency(villa.pricePerNight)}</p>
        <p className="mt-2 text-sm text-brand-ink/58">{t('villas.perNightDesc')}</p>
        <div className="mt-7 grid grid-cols-2 gap-3 text-sm">
          <div className="rounded-[1rem] bg-brand-sand p-4">
            <p className="font-semibold text-brand-charcoal">{villa.capacity} {t('villas.guests')}</p>
            <p className="mt-1 text-brand-ink/54">{t('villas.capacity')}</p>
          </div>
          <div className="rounded-[1rem] bg-brand-sand p-4">
            <p className="font-semibold text-brand-charcoal">{villa.bedrooms} {t('villas.bedrooms')}</p>
            <p className="mt-1 text-brand-ink/54">{t('villas.privateLayout')}</p>
          </div>
        </div>
        <Link
          href={`/booking/${villa.id}`}
          className="mt-7 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-forest-deep active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
        >
          {t('villas.checkAvailability')}
        </Link>
        <p className="mt-5 text-center text-xs leading-6 text-brand-ink/50">
          {t('villas.noPaymentNeeded')}
        </p>
      </div>
    </aside>
  );
}

function AmenityBadge({ name }: { name: string }) {
  const icons: Record<string, string> = {
    'Private pool': '🏊',
    'Infinity pool': '🌊',
    'Air conditioning': '❄️',
    'Wi-Fi': '📶',
    'Kitchen': '🍳',
    'Garden': '🌿',
    'Terrace': '🪑',
    'Spa': '💆',
    'Gym': '💪',
    'Parking': '🚗',
    'Breakfast': '🍳',
    'Room service': '🛎️',
    'Airport transfer': '✈️',
    'Concierge': '👤',
  };

  return (
    <div className="flex items-center gap-3 rounded-xl bg-brand-white p-4">
      <span className="text-2xl">{icons[name] || '✨'}</span>
      <span className="text-sm font-medium text-brand-charcoal">{name}</span>
    </div>
  );
}

function DetailSkeleton() {
  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1440px]">
        <div className="h-12 w-48 animate-pulse rounded-full bg-brand-stone" />
        <div className="mt-8 h-[56vh] animate-pulse rounded-[2rem] bg-brand-stone" />
      </div>
    </section>
  );
}

export const VillaDetailPage = () => {
  const { villaId } = useParams() as { villaId: string };
  const router = useRouter();
  const searchParams = useSearchParams();
  const reviewBookingId = searchParams.get('reviewBookingId');
  const queryClient = useQueryClient();
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const { formatCurrency } = useCurrency();
  const { t } = useTranslation();

  const submitReview = useMutation<{ message: string }, ApiError, { bookingId: number; rating: number; comment: string }>({
    mutationFn: (body) => api.post<{ message: string }>(API_PATHS.reviews.submitForBooking(body.bookingId), body),
    onSuccess: (data) => {
      setSuccessMsg(data.message);
      setComment('');
      queryClient.invalidateQueries({ queryKey: ['reviews', villaId] });
      // Remove query param to close the form
      router.replace(`/villas/${villaId}#reviews`);
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const query = useQuery({ queryKey: ['villas'], queryFn: () => fetchVillas(), staleTime: 60_000 });
  const villas = (query.data ?? (query.isError ? fallbackVillas : undefined)) as Villa[] | undefined;
  const villa = findVillaById(villas, villaId);

  // Fetch real reviews
  const { data: reviewsData } = useQuery({
    queryKey: ['reviews', villaId],
    queryFn: () => api.get<{ reviews: Review[]; avgRating: number; reviewCount: number; categoryAverages?: { cleanliness: number; service: number; location: number; value: number } }>(
      API_PATHS.reviews.room(villaId)
    ),
    enabled: !!villaId && !isNaN(Number(villaId)),
    staleTime: 30_000,
  });

  if (query.isLoading && !villa) {
    return <DetailSkeleton />;
  }

  if (!villa) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-3xl rounded-[2rem] bg-brand-paper p-8 text-center shadow-[0_24px_90px_rgba(32,52,43,0.1)]">
          <h1 className="text-5xl text-brand-charcoal">{t('villas.notFound')}</h1>
          <p className="mt-5 text-brand-ink/62">{t('villas.notFoundDesc')}</p>
          <Link
            href="/villas"
            className="mt-8 inline-flex min-h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
          >
            {t('villas.returnToVillas')}
          </Link>
        </div>
      </section>
    );
  }

  const galleryImages = [villa.imageUrl, ...fallbackImages].slice(0, 5);
  const similarVillas = getSimilarVillas(villa, villas);
  const reviews = reviewsData?.reviews || [];
  const avgRating = reviewsData?.avgRating ?? villa.rating;
  const reviewCount = reviewsData?.reviewCount ?? villa.reviewCount;

  return (
    <article className="bg-brand-sand">
      {/* Header */}
      <section className="px-5 pb-16 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-[1440px]">
          <Link
            href="/villas"
            className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest hover:text-brand-charcoal focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
          >
            {t('villas.backToCollection')}
          </Link>
          <div className="mt-8 grid gap-10 lg:grid-cols-[1fr_0.42fr] lg:items-end">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{villa.roomType}</p>
              <h1 className="mt-5 max-w-[12ch] text-5xl leading-[1.02] text-brand-charcoal sm:text-6xl lg:text-8xl">
                {villa.name}
              </h1>
              {villa.promotions && villa.promotions.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {villa.promotions.map((promo) => (
                    <span
                      key={promo.id}
                      className="rounded-full bg-brand-forest/20 px-3 py-1 text-xs font-semibold text-brand-forest shadow-sm"
                    >
                      🏷️ {promo.title} ({promo.discountPercent ? `${promo.discountPercent}% OFF` : `$${promo.discountAmount} OFF`})
                    </span>
                  ))}
                </div>
              )}
              <div className="mt-4 flex items-center gap-4">
                <StarRating rating={Math.round(avgRating)} />
                <span className="text-sm text-brand-ink/62">
                  {avgRating.toFixed(1)} ({reviewCount} {t('villas.reviews')})
                </span>
              </div>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-brand-ink/66">{villa.description}</p>
            </div>
            <BookingCard villa={villa} />
          </div>
        </div>
      </section>

      {/* Gallery */}
      <section className="px-5 pb-24 sm:px-8 lg:px-12">
        <div className="mx-auto grid max-w-[1440px] gap-5 md:grid-cols-4 md:grid-rows-[260px_260px] lg:grid-rows-[360px_360px]">
          {galleryImages.map((image, index) => (
            <button
              key={`${image}-${index}`}
              onClick={() => setLightboxIndex(index)}
              className="group relative h-full min-h-[260px] w-full overflow-hidden rounded-[1.5rem] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
            >
              <img
                src={image}
                alt={`${villa.name} gallery view ${index + 1}`}
                className={`h-full w-full rounded-[1.5rem] object-cover transition duration-500 group-hover:scale-[1.02] ${
                  index === 0 ? 'md:col-span-2 md:row-span-2' : ''
                }`}
                loading={index === 0 ? 'eager' : 'lazy'}
                decoding="async"
              />
              {index === 4 && (
                <div className="absolute inset-0 flex items-center justify-center rounded-[1.5rem] bg-black/40">
                  <span className="text-2xl font-semibold text-white">+{galleryImages.length - 5} {t('common.more')}</span>
                </div>
              )}
            </button>
          ))}
        </div>
      </section>

      {/* Lightbox */}
      {lightboxIndex !== null && (
        <GalleryLightbox
          images={galleryImages}
          currentIndex={lightboxIndex}
          onClose={() => setLightboxIndex(null)}
          onNext={() => setLightboxIndex((lightboxIndex + 1) % galleryImages.length)}
          onPrev={() => setLightboxIndex(lightboxIndex === 0 ? galleryImages.length - 1 : lightboxIndex - 1)}
        />
      )}

      {/* Details Section */}
      <section className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12">
        <div className="mx-auto grid max-w-[1440px] gap-14 lg:grid-cols-[0.9fr_1.1fr]">
          <div>
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('villas.details')}</p>
            <h2 className="max-w-[12ch] text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
              {t('villas.detailsTitle')}
            </h2>
            <p className="mt-6 max-w-xl text-base leading-8 text-brand-ink/64">
              {t('villas.detailsSubtitle')}
            </p>
            <div className="mt-8">
              <h3 className="mb-4 text-lg font-semibold text-brand-charcoal">{t('villas.guests')}</h3>
              <div className="flex gap-8">
                <div>
                  <p className="text-3xl font-bold text-brand-forest">{villa.capacity}</p>
                  <p className="text-sm text-brand-ink/54">{t('villas.guests')}</p>
                </div>
                <div>
                  <p className="text-3xl font-bold text-brand-forest">{villa.bedrooms}</p>
                  <p className="text-sm text-brand-ink/54">{t('villas.bedrooms')}</p>
                </div>
                <div>
                  <p className="text-3xl font-bold text-brand-forest">{villa.capacity * 2}</p>
                  <p className="text-sm text-brand-ink/54">{t('villas.beds')}</p>
                </div>
              </div>
            </div>
          </div>
          <div>
            <h3 className="mb-5 text-lg font-semibold text-brand-charcoal">{t('villas.amenitiesLabel')}</h3>
            <div className="grid gap-3 sm:grid-cols-2">
              {villa.amenities.map((amenity) => (
                <AmenityBadge key={amenity.name} name={amenity.name} />
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Reviews Section */}
      <section className="bg-brand-sand px-5 py-24 sm:px-8 lg:px-12" id="reviews">
        <div className="mx-auto max-w-[1440px]">
          <div className="mb-10 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('villas.reviews')}</p>
              <h2 className="text-4xl text-brand-charcoal sm:text-5xl">{t('villas.guestNotes')}</h2>
              <div className="mt-3 flex items-center gap-3">
                <StarRating rating={Math.round(avgRating)} />
                <span className="font-semibold text-brand-charcoal">{avgRating.toFixed(1)}</span>
                <span className="text-brand-ink/62">· {reviewCount} {t('villas.stays')}</span>
              </div>
            </div>
            
            {reviewsData?.categoryAverages && (
              <div className="flex flex-wrap gap-4 text-sm mt-4 lg:mt-0">
                <div className="bg-brand-white px-4 py-2 rounded-full border border-brand-stone">
                  <span className="text-brand-ink/70">{t('villas.cleanliness')}</span> <span className="font-semibold ml-1">{reviewsData.categoryAverages.cleanliness.toFixed(1)}</span>
                </div>
                <div className="bg-brand-white px-4 py-2 rounded-full border border-brand-stone">
                  <span className="text-brand-ink/70">{t('villas.service')}</span> <span className="font-semibold ml-1">{reviewsData.categoryAverages.service.toFixed(1)}</span>
                </div>
                <div className="bg-brand-white px-4 py-2 rounded-full border border-brand-stone">
                  <span className="text-brand-ink/70">{t('villas.locationRating')}</span> <span className="font-semibold ml-1">{reviewsData.categoryAverages.location.toFixed(1)}</span>
                </div>
                <div className="bg-brand-white px-4 py-2 rounded-full border border-brand-stone">
                  <span className="text-brand-ink/70">{t('villas.value')}</span> <span className="font-semibold ml-1">{reviewsData.categoryAverages.value.toFixed(1)}</span>
                </div>
              </div>
            )}
          </div>


          {reviewBookingId && (
            <div className="mb-10 rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
              <h3 className="text-3xl text-brand-charcoal">{t('profile.writeReview')}</h3>
              {errorMsg ? <p className="mt-4 rounded-full bg-brand-coral/10 px-4 py-3 text-sm text-red-600">{errorMsg}</p> : null}
              {successMsg ? <p className="mt-4 rounded-full bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest">{successMsg}</p> : null}
              {!successMsg && (
                <form
                  className="mt-6 grid gap-5"
                  onSubmit={(e) => {
                    e.preventDefault();
                    setErrorMsg(null);
                    setSuccessMsg(null);
                    submitReview.mutate({ bookingId: Number(reviewBookingId), rating, comment });
                  }}
                >
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
                  <div className="flex gap-4">
                    <button
                      type="submit"
                      disabled={submitReview.isPending}
                      className="min-h-12 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
                    >
                      {submitReview.isPending ? t('common.submitting') : t('profile.submitReview')}
                    </button>
                    <button
                      type="button"
                      onClick={() => router.replace(`/villas/${villaId}#reviews`)}
                      className="min-h-12 rounded-full border border-brand-stone px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink transition hover:bg-brand-ink/5"
                    >
                      {t('bookings.cancel')}
                    </button>
                  </div>
                </form>
              )}
            </div>
          )}
          {reviews.length > 0 ? (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {reviews.slice(0, 6).map((review) => (
                <blockquote
                  key={review.id}
                  className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_18px_60px_rgba(32,52,43,0.08)]"
                >
                  <StarRating rating={review.rating} size="sm" />
                  <p className="mt-4 text-lg leading-7 text-brand-charcoal">"{review.comment}"</p>
                  <footer className="mt-6 flex items-center justify-between">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">
                        {review.user.fullName}
                      </p>
                      <p className="mt-1 text-xs text-brand-ink/50">{formatDate(review.createdAt)}</p>
                    </div>
                  </footer>
                </blockquote>
              ))}
            </div>
          ) : (
            <div className="rounded-[2rem] bg-brand-paper p-12 text-center shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
              <p className="text-brand-ink/62">{t('villas.noReviews')}</p>
            </div>
          )}

          {reviews.length > 6 && (
            <div className="mt-8 text-center">
              <Link
                href={`/reviews?villa=${villa.id}`}
                className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-brand-white"
              >
                {t('villas.viewAllReviews')} ({reviewCount})
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* Policies Section */}
      <section className="grid bg-brand-forest-deep text-brand-paper lg:grid-cols-2">
        <div className="px-5 py-24 sm:px-8 lg:px-12">
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">{t('villas.policies')}</p>
          <h2 className="max-w-[12ch] text-4xl leading-[1.08] text-brand-white sm:text-5xl">
            {t('villas.rulesTitle')}
          </h2>
          <dl className="mt-10 space-y-6 text-sm leading-7 text-brand-paper/72">
            <div>
              <dt className="font-semibold uppercase tracking-[0.14em] text-brand-white">{t('villas.checkIn')}</dt>
              <dd className="mt-2">{t('villas.checkInRule')}</dd>
            </div>
            <div>
              <dt className="font-semibold uppercase tracking-[0.14em] text-brand-white">{t('villas.checkOut')}</dt>
              <dd className="mt-2">{t('villas.checkOutRule')}</dd>
            </div>
            <div>
              <dt className="font-semibold uppercase tracking-[0.14em] text-brand-white">{t('villas.cancellation')}</dt>
              <dd className="mt-2">{t('villas.cancellationRule')}</dd>
            </div>
            <div>
              <dt className="font-semibold uppercase tracking-[0.14em] text-brand-white">{t('villas.noSmoking')}</dt>
              <dd className="mt-2">{t('villas.noSmokingRule')}</dd>
            </div>
            <div>
              <dt className="font-semibold uppercase tracking-[0.14em] text-brand-white">{t('villas.noParties')}</dt>
              <dd className="mt-2">{t('villas.noPartiesRule')}</dd>
            </div>
          </dl>
        </div>
        <div className="min-h-[520px] bg-brand-charcoal p-10 flex flex-col justify-center">
          <h3 className="mb-5 text-xl font-semibold text-brand-white">{t('villas.location')}</h3>
          <InteractiveMap latitude={villa.latitude} longitude={villa.longitude} name={villa.name} />
          
          <div className="mt-6 grid grid-cols-2 gap-4 text-sm text-brand-paper/80">
            {villa.nearbyAirport && (
              <div>
                <strong className="text-brand-white block">{t('villas.airport')}</strong>
                {villa.nearbyAirport}
              </div>
            )}
            {villa.nearbyAttractions && (
              <div>
                <strong className="text-brand-white block">{t('villas.attractions')}</strong>
                {villa.nearbyAttractions}
              </div>
            )}
          </div>
        </div>
      </section>

      {/* Similar Villas */}
      {similarVillas.length > 0 && (
        <section className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12">
          <div className="mx-auto max-w-[1440px]">
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('villas.similar')}</p>
            <div className="grid gap-6 md:grid-cols-3">
              {similarVillas.map((item) => (
                <Link
                  key={item.id}
                  href={`/villas/${item.id}`}
                  className="group rounded-[2rem] bg-brand-sand p-2 shadow-[0_18px_60px_rgba(32,52,43,0.08)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
                >
                  <img
                    src={item.imageUrl}
                    alt={item.name}
                    className="aspect-[4/3] w-full rounded-[1.55rem] object-cover transition duration-700 group-hover:scale-[1.02]"
                    loading="lazy"
                    decoding="async"
                  />
                  <div className="p-5">
                    <div className="flex items-center gap-2">
                      <StarRating rating={Math.round(item.rating)} size="sm" />
                      <span className="text-xs text-brand-ink/50">({item.reviewCount})</span>
                    </div>
                    <h3 className="mt-2 text-2xl text-brand-charcoal">{item.name}</h3>
                    <p className="mt-2 text-sm text-brand-ink/58">{formatCurrency(item.pricePerNight)} {t('villas.perNight')}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}
    </article>
  );
};
