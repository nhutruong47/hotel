'use client';

import { useMemo, useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { fetchVillas, fetchBookedDates } from './villasApi';
import { fallbackVillas } from './villaFallbacks';
import { AvailabilityCalendar } from './components/AvailabilityCalendar';
import type { SortOption, Villa, VillaFilters } from './types';
import { useCurrency, useTranslation } from '../../shared/i18n/hooks';

const pageSize = 4;
const emptyVillas: Villa[] = [];

const initialFilters: VillaFilters = {
  query: '',
  roomType: 'all',
  minPrice: '',
  maxPrice: '',
  amenity: 'all',
  sort: 'recommended',
  bedrooms: '',
  promotion: '',
};

// Removed static formatPrice

function uniqueValues(values: string[]) {
  return Array.from(new Set(values)).filter(Boolean).sort((a, b) => a.localeCompare(b));
}

function applyFilters(villas: Villa[], filters: VillaFilters) {
  const query = filters.query.trim().toLowerCase();
  const minPrice = filters.minPrice ? Number(filters.minPrice) : null;
  const maxPrice = filters.maxPrice ? Number(filters.maxPrice) : null;
  const adults = filters.adults ? parseInt(filters.adults, 10) : 0;
  const childrenCount = filters.children ? parseInt(filters.children, 10) : 0;
  const requiredCapacity = adults + childrenCount;

  return villas.filter((villa) => {
    const amenityNames = villa.amenities.map((amenity) => amenity.name.toLowerCase());
    const matchesQuery =
      query.length === 0 ||
      villa.name.toLowerCase().includes(query) ||
      villa.description.toLowerCase().includes(query) ||
      villa.roomType.toLowerCase().includes(query) ||
      amenityNames.some((name) => name.includes(query));
    const matchesType = filters.roomType === 'all' || villa.roomType === filters.roomType;
    const matchesAmenity =
      filters.amenity === 'all' || villa.amenities.some((amenity) => amenity.name === filters.amenity);
    const matchesMin = minPrice === null || villa.pricePerNight >= minPrice;
    const matchesMax = maxPrice === null || villa.pricePerNight <= maxPrice;
    const matchesCapacity = requiredCapacity === 0 || villa.capacity >= requiredCapacity;

    return matchesQuery && matchesType && matchesAmenity && matchesMin && matchesMax && matchesCapacity;
  });
}

function sortVillas(villas: Villa[], sort: SortOption) {
  const sorted = [...villas];

  if (sort === 'price-asc') {
    sorted.sort((a, b) => a.pricePerNight - b.pricePerNight);
  }

  if (sort === 'price-desc') {
    sorted.sort((a, b) => b.pricePerNight - a.pricePerNight);
  }

  if (sort === 'capacity-desc') {
    sorted.sort((a, b) => b.capacity - a.capacity);
  }

  if (sort === 'recommended') {
    sorted.sort((a, b) => Number(b.isAvailable) - Number(a.isAvailable) || b.rating - a.rating);
  }

  return sorted;
}

function VillaSkeletonGrid() {
  return (
    <div className="grid gap-7 lg:grid-cols-2">
      {Array.from({ length: 4 }, (_, index) => (
        <div key={index} className="rounded-[2rem] bg-brand-paper p-2 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <div className="aspect-[16/11] animate-pulse rounded-[1.55rem] bg-brand-stone" />
          <div className="p-6">
            <div className="h-7 w-2/3 animate-pulse rounded-full bg-brand-stone" />
            <div className="mt-5 h-4 w-full animate-pulse rounded-full bg-brand-stone" />
            <div className="mt-3 h-4 w-4/5 animate-pulse rounded-full bg-brand-stone" />
          </div>
        </div>
      ))}
    </div>
  );
}

function StarRating({ rating, size = 'sm' }: { rating: number; size?: 'sm' | 'md' }) {
  const sizeClass = size === 'sm' ? 'text-sm' : 'text-base';
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

function VillaCard({ villa }: { villa: Villa }) {
  const { formatCurrency } = useCurrency();
  const { t } = useTranslation();
  return (
    <article className="group rounded-[2rem] bg-brand-paper p-2 shadow-[0_22px_80px_rgba(32,52,43,0.1)] transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-1 hover:shadow-[0_32px_100px_rgba(32,52,43,0.15)]">
      <Link href={`/villas/${villa.id}`} className="block rounded-[1.55rem] bg-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest">
        <div className="relative overflow-hidden rounded-t-[1.55rem]">
          <img
            src={villa.imageUrl}
            alt={`${villa.name} at Nhu Villas`}
            className="aspect-[16/11] w-full object-cover transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.035]"
            loading="lazy"
            decoding="async"
          />
          <div className="absolute left-4 top-4 flex flex-col gap-2">
            <div className="rounded-full bg-brand-paper px-3 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-brand-forest shadow-sm">
              {villa.isAvailable ? t('villas.available') : t('villas.limited')}
            </div>
            {villa.promotions && villa.promotions.map((promo) => (
              <div key={promo.id} className="rounded-full bg-brand-forest px-3 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-brand-white shadow-sm">
                🏷️ {promo.title}
              </div>
            ))}
          </div>
        </div>
        <div className="p-6 sm:p-7">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">
                {t(`villas.data.${villa.id}.roomType` as any) === `villas.data.${villa.id}.roomType` ? villa.roomType : t(`villas.data.${villa.id}.roomType` as any)}
              </p>
              <h2 className="mt-2 text-3xl leading-tight text-brand-charcoal">
                {t(`villas.data.${villa.id}.name` as any) === `villas.data.${villa.id}.name` ? villa.name : t(`villas.data.${villa.id}.name` as any)}
              </h2>
              <div className="mt-2 flex items-center gap-2">
                <StarRating rating={Math.round(villa.rating)} size="sm" />
                <span className="text-xs text-brand-ink/50">({villa.reviewCount})</span>
              </div>
            </div>
            <div className="rounded-full border border-brand-stone px-4 py-2 text-sm font-semibold text-brand-forest">
              {t('villas.fromPrice').replace('{price}', formatCurrency(villa.pricePerNight))}
            </div>
          </div>
          <p className="mt-5 min-h-14 text-sm leading-7 text-brand-ink/64">
            {t(`villas.data.${villa.id}.description` as any) === `villas.data.${villa.id}.description` ? villa.description : t(`villas.data.${villa.id}.description` as any)}
          </p>
          <div className="mt-6 flex flex-wrap gap-2">
            {villa.amenities.slice(0, 3).map((amenity) => (
              <span key={amenity.name} className="rounded-full bg-brand-sand px-3 py-1 text-xs text-brand-ink/70">
                {t(`villas.amenities.${amenity.name}` as any) === `villas.amenities.${amenity.name}` ? amenity.name : t(`villas.amenities.${amenity.name}` as any)}
              </span>
            ))}
          </div>
          <div className="mt-7 grid grid-cols-3 gap-3 border-t border-brand-stone/80 pt-5 text-sm text-brand-ink/62">
            <span>{villa.capacity} {t('villas.guests')}</span>
            <span>{villa.bedrooms} {t('villas.bedrooms')}</span>
            <span className="flex items-center gap-1">
              <StarRating rating={Math.round(villa.rating)} size="sm" />
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}

export const VillasPage = () => {
  const searchParams = useSearchParams();
  const router = useRouter();
  const resultsRef = useRef<HTMLDivElement>(null);
  const [showCalendar, setShowCalendar] = useState(false);
  const { t } = useTranslation();

  const [filters, setFilters] = useState<VillaFilters>(() => ({
    ...initialFilters,
    checkIn: searchParams.get('checkIn') || '',
    checkOut: searchParams.get('checkOut') || '',
    adults: searchParams.get('adults') || '',
    children: searchParams.get('children') || '',
    promotion: searchParams.get('promotion') || '',
  }));
  const [page, setPage] = useState(1);
  const apiFilters = useMemo(() => {
    const adults = parseInt(filters.adults || '0', 10);
    const childrenCount = parseInt(filters.children || '0', 10);
    const capacity = adults + childrenCount;
    return {
      checkIn: filters.checkIn,
      checkOut: filters.checkOut,
      capacity: capacity > 0 ? capacity.toString() : undefined,
      bedrooms: filters.bedrooms || undefined,
      minPrice: filters.minPrice || undefined,
      maxPrice: filters.maxPrice || undefined,
      amenities: filters.amenity !== 'all' ? filters.amenity : undefined,
      promotion: filters.promotion || undefined,
    };
  }, [filters]);

  const query = useQuery({ 
    queryKey: ['villas', apiFilters], 
    queryFn: () => fetchVillas(apiFilters), 
    staleTime: 60_000 
  });
  const sourceVillas: Villa[] = query.data ?? (query.isError ? fallbackVillas : emptyVillas);

  const roomTypes = useMemo(() => uniqueValues(sourceVillas.map((villa) => villa.roomType)), [sourceVillas]);
  const amenities = useMemo(
    () => uniqueValues(sourceVillas.flatMap((villa) => villa.amenities.map((amenity) => amenity.name))),
    [sourceVillas],
  );
  const filteredVillas = useMemo(
    () => sortVillas(applyFilters(sourceVillas, filters), filters.sort),
    [filters, sourceVillas],
  );
  const totalPages = Math.max(1, Math.ceil(filteredVillas.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const paginatedVillas = filteredVillas.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const updateFilter = (field: keyof VillaFilters, value: string) => {
    setFilters((current) => ({ ...current, [field]: value }));
    setPage(1);
  };

  const resetFilters = () => {
    setFilters(initialFilters);
    setPage(1);
    router.replace('/villas');
  };

  const isSearchMode = !!(filters.checkIn && filters.checkOut);

  useEffect(() => {
    if (isSearchMode && resultsRef.current && !query.isLoading) {
      setTimeout(() => {
        resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 100);
    }
  }, [isSearchMode, query.isLoading]);

  const formattedDates = useMemo(() => {
    if (!filters.checkIn || !filters.checkOut) return '';
    const inDate = new Date(filters.checkIn);
    const outDate = new Date(filters.checkOut);
    const opts: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };
    return `${inDate.toLocaleDateString('en-GB', opts)} – ${outDate.toLocaleDateString('en-GB', opts)}`;
  }, [filters.checkIn, filters.checkOut]);

  const handleDateSelect = (range: { checkIn: string; checkOut: string }) => {
    updateFilter('checkIn', range.checkIn);
    updateFilter('checkOut', range.checkOut);
    setShowCalendar(false);
  };

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1440px]">
        <div className="grid gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-end">
          <div>
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
              {t('villas.collection')}
            </p>
            <h1 className="max-w-[12ch] text-5xl leading-[1.02] text-brand-charcoal sm:text-6xl lg:text-8xl">
              {t('villas.title')}
            </h1>
          </div>
          <p className="max-w-2xl text-base leading-8 text-brand-ink/66 sm:text-lg lg:justify-self-end">
            {t('villas.subtitle')}
          </p>
        </div>

        {/* Search & Filter Bar */}
        <div className="mt-12 rounded-[2rem] bg-brand-paper p-4 shadow-[0_24px_90px_rgba(32,52,43,0.1)] sm:p-5">
          <div className="grid gap-3 lg:grid-cols-[1.4fr_0.8fr_0.8fr_0.7fr_0.7fr_0.9fr]">
            <label className="block">
              <span className="sr-only">Search villas</span>
              <input
                value={filters.query}
                onChange={(event) => updateFilter('query', event.target.value)}
                placeholder={t('villas.searchPlaceholder')}
                className="h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              />
            </label>
            <select
              value={filters.roomType}
              onChange={(event) => updateFilter('roomType', event.target.value)}
              className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              aria-label="Filter by villa type"
            >
              <option value="all">{t('villas.allTypes')}</option>
              {roomTypes.map((roomType) => (
                <option key={roomType} value={roomType}>
                  {roomType}
                </option>
              ))}
            </select>
            <select
              value={filters.amenity}
              onChange={(event) => updateFilter('amenity', event.target.value)}
              className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              aria-label="Filter by amenity"
            >
              <option value="all">{t('villas.allAmenities')}</option>
              {amenities.map((amenity) => (
                <option key={amenity} value={amenity}>
                  {amenity}
                </option>
              ))}
            </select>
            <input
              value={filters.minPrice}
              onChange={(event) => updateFilter('minPrice', event.target.value)}
              inputMode="numeric"
              placeholder={t('villas.minPrice')}
              className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              aria-label="Minimum price"
            />
            <input
              value={filters.maxPrice}
              onChange={(event) => updateFilter('maxPrice', event.target.value)}
              inputMode="numeric"
              placeholder={t('villas.maxPrice')}
              className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              aria-label="Maximum price"
            />
            <select
              value={filters.sort}
              onChange={(event) => updateFilter('sort', event.target.value as SortOption)}
              className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
              aria-label="Sort villas"
            >
              <option value="recommended">{t('villas.sort.recommended')}</option>
              <option value="price-asc">{t('villas.sort.priceAsc')}</option>
              <option value="price-desc">{t('villas.sort.priceDesc')}</option>
              <option value="capacity-desc">{t('villas.sort.capacityDesc')}</option>
            </select>
          </div>

          {/* Date & Guest Filters */}
          <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-brand-stone/50 pt-4">
            <button
              onClick={() => setShowCalendar(!showCalendar)}
              className={`flex h-12 items-center gap-2 rounded-full border px-5 text-sm transition ${
                filters.checkIn && filters.checkOut
                  ? 'border-brand-forest bg-brand-forest/10 text-brand-forest'
                  : 'border-brand-stone hover:border-brand-forest'
              }`}
            >
              <span>📅</span>
              {filters.checkIn && filters.checkOut ? formattedDates : t('villas.selectDates')}
            </button>
            <div className="flex items-center gap-2">
              <select
                value={filters.adults || ''}
                onChange={(e) => updateFilter('adults', e.target.value)}
                className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm focus:border-brand-forest focus:outline-none"
              >
                <option value="">{t('villas.adults')}</option>
                {[1, 2, 3, 4, 5, 6].map((n) => (
                  <option key={n} value={n}>{n} {t('villas.adults')}</option>
                ))}
              </select>
              <select
                value={filters.children || ''}
                onChange={(e) => updateFilter('children', e.target.value)}
                className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm focus:border-brand-forest focus:outline-none"
              >
                <option value="">{t('villas.children')}</option>
                {[0, 1, 2, 3, 4].map((n) => (
                  <option key={n} value={n}>{n} {t('villas.children')}</option>
                ))}
              </select>
              <select
                value={filters.bedrooms || ''}
                onChange={(e) => updateFilter('bedrooms', e.target.value)}
                className="h-12 rounded-full border border-brand-stone bg-brand-white px-4 text-sm focus:border-brand-forest focus:outline-none"
              >
                <option value="">{t('villas.bedrooms')}</option>
                {[1, 2, 3, 4, 5, 6].map((n) => (
                  <option key={n} value={n}>{n}+ {t('villas.bedrooms')}</option>
                ))}
              </select>
            </div>
            <p className="ml-auto text-sm text-brand-ink/62">
              {t('villas.showing')} {filteredVillas.length}
            </p>
            <button
              type="button"
              onClick={resetFilters}
              className="w-fit rounded-full border border-brand-forest px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
            >
              {t('villas.resetFilters')}
            </button>
          </div>

          {/* Calendar Popup */}
          {showCalendar && (
            <div className="mt-4 border-t border-brand-stone/50 pt-4">
              <AvailabilityCalendar
                onDateSelect={handleDateSelect}
                initialCheckIn={filters.checkIn || undefined}
                initialCheckOut={filters.checkOut || undefined}
              />
            </div>
          )}
        </div>

        <div ref={resultsRef} className="scroll-mt-32" />

        {isSearchMode && !query.isLoading ? (
          <div className="mt-12 mb-2 flex flex-col gap-2 border-b border-brand-stone/60 pb-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 className="text-2xl text-brand-charcoal sm:text-3xl">{t('villas.searchResults')}</h2>
              <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-brand-ink/66">
                <span className="font-semibold text-brand-forest">{filteredVillas.length} {t('villas.villasAvailable')}</span>
                <span className="h-1 w-1 rounded-full bg-brand-stone" />
                <span>{formattedDates}</span>
                <span className="h-1 w-1 rounded-full bg-brand-stone" />
                <span>{parseInt(filters.adults || '0') + parseInt(filters.children || '0')} {t('villas.guests')}</span>
              </div>
            </div>
          </div>
        ) : null}

        {query.isLoading ? (
          <div className="mt-10">
            <VillaSkeletonGrid />
          </div>
        ) : null}

        {!query.isLoading && filteredVillas.length === 0 ? (
          <div className="mt-10 rounded-[2rem] bg-brand-paper px-6 py-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
            <h2 className="text-4xl text-brand-charcoal">
              {isSearchMode ? t('villas.noVillasAvailable') : t('villas.noVillasAvailable')}
            </h2>
            <p className="mx-auto mt-4 max-w-xl text-base leading-8 text-brand-ink/62">
              {isSearchMode ? t('villas.noVillasSearch') : t('villas.noVillasFilter')}
            </p>
            <div className="mt-7 flex flex-wrap items-center justify-center gap-4">
              <button
                type="button"
                onClick={resetFilters}
                className="inline-flex min-h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
              >
                {isSearchMode ? t('villas.clearSearch') : t('villas.showAll')}
              </button>
            </div>
          </div>
        ) : null}

        {!query.isLoading && filteredVillas.length > 0 ? (
          <>
            <div className="mt-10 grid gap-7 lg:grid-cols-2">
              {paginatedVillas.map((villa) => (
                <VillaCard key={villa.id} villa={villa} />
              ))}
            </div>
            <nav className="mt-10 flex items-center justify-center gap-3" aria-label="Villa pagination">
              {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                <button
                  key={pageNumber}
                  type="button"
                  onClick={() => setPage(pageNumber)}
                  className={`h-11 w-11 rounded-full border text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest ${
                    pageNumber === currentPage
                      ? 'border-brand-forest bg-brand-forest text-brand-white'
                      : 'border-brand-stone bg-brand-paper text-brand-forest hover:border-brand-forest'
                  }`}
                  aria-current={pageNumber === currentPage ? 'page' : undefined}
                >
                  {pageNumber}
                </button>
              ))}
            </nav>
          </>
        ) : null}
      </div>
    </section>
  );
};
