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
import { motion } from 'framer-motion';
import { CustomSelect } from '../../shared/components/CustomSelect';

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      type: "spring" as any,
      stiffness: 100,
      damping: 15,
    },
  },
};

const pageSize = 12;
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
    <div className="grid gap-7 sm:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: 8 }, (_, index) => (
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
    <article className="group relative flex h-full flex-col overflow-hidden rounded-[1.75rem] bg-brand-white shadow-[0_12px_40px_rgba(32,52,43,0.06)] transition-all duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-2 hover:shadow-[0_24px_80px_rgba(32,52,43,0.12)]">
      <Link href={`/villas/${villa.id}`} className="flex h-full flex-col focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest">
        <div className="relative aspect-[16/12] overflow-hidden">
          <img
            src={villa.imageUrl}
            alt={`${villa.name} at Nhu Villas`}
            className="h-full w-full object-cover transition-transform duration-[850ms] ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.05]"
            loading="lazy"
            decoding="async"
          />
          {/* Subtle gradient overlay for premium cinematic feel */}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-brand-charcoal/50 via-transparent to-brand-charcoal/10 opacity-60 mix-blend-multiply transition-opacity duration-700 group-hover:opacity-40" />
          
          <div className="absolute left-4 top-4 flex flex-col gap-2">
            <div className="rounded-full bg-brand-white/95 px-3 py-1 text-[0.65rem] font-bold uppercase tracking-[0.18em] text-brand-forest shadow-sm backdrop-blur-md">
              {villa.isAvailable ? t('villas.available') : t('villas.limited')}
            </div>
            {villa.promotions && villa.promotions.map((promo) => (
              <div key={promo.id} className="rounded-full bg-brand-coral/95 px-3 py-1 text-[0.65rem] font-bold uppercase tracking-[0.18em] text-brand-white shadow-sm backdrop-blur-md">
                🏷️ {promo.title}
              </div>
            ))}
          </div>

          {/* Hover Explore Button */}
          <div className="absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:opacity-100">
             <div className="flex h-14 w-14 translate-y-4 items-center justify-center rounded-full bg-brand-white/95 text-brand-forest shadow-2xl backdrop-blur-md transition-transform duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:translate-y-0">
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
             </div>
          </div>
        </div>

        <div className="flex flex-1 flex-col p-6 sm:p-7">
          <div className="flex flex-col items-start gap-2">
            <p className="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-brand-sage">
              {t(`villas.data.${villa.id}.roomType` as any) === `villas.data.${villa.id}.roomType` ? villa.roomType : t(`villas.data.${villa.id}.roomType` as any)}
            </p>
            <h2 className="font-serif text-2xl leading-tight text-brand-charcoal sm:text-[1.65rem]">
              {t(`villas.data.${villa.id}.name` as any) === `villas.data.${villa.id}.name` ? villa.name : t(`villas.data.${villa.id}.name` as any)}
            </h2>
            <div className="flex items-center gap-2">
              <StarRating rating={Math.round(villa.rating)} size="sm" />
              <span className="text-[0.7rem] text-brand-ink/50">({villa.reviewCount})</span>
            </div>
            <div className="mt-3 rounded-full bg-brand-forest px-4 py-1.5 text-xs font-semibold tracking-wider text-brand-white shadow-md">
              {t('villas.fromPrice').replace('{price}', formatCurrency(villa.pricePerNight))}
            </div>
          </div>
          
          <p className="mt-5 line-clamp-2 text-[0.9rem] leading-relaxed text-brand-ink/64">
            {t(`villas.data.${villa.id}.description` as any) === `villas.data.${villa.id}.description` ? villa.description : t(`villas.data.${villa.id}.description` as any)}
          </p>
          
          <div className="mt-auto pt-7">
            <div className="flex items-center gap-4 border-t border-brand-stone/40 pt-5 text-[0.8rem] font-medium text-brand-ink/70">
              <span className="flex items-center gap-1.5">
                 <svg className="h-4 w-4 opacity-70" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                 </svg>
                 {villa.capacity} {t('villas.guests')}
              </span>
              <span className="h-[3px] w-[3px] rounded-full bg-brand-stone" />
              <span className="flex items-center gap-1.5">
                 <svg className="h-4 w-4 opacity-70" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3.75h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008z" />
                 </svg>
                 {villa.bedrooms} {t('villas.bedrooms')}
              </span>
            </div>
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
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 xl:px-12 lg:pt-40">
      <div className="mx-auto w-full max-w-[1920px]">
        {/* Header */}
        <div className="mb-12 grid gap-8 lg:grid-cols-[0.9fr_1.1fr] lg:items-end">
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

        <div className="flex flex-col items-start gap-10 lg:flex-row lg:gap-14">
          {/* LEFT SIDEBAR: Filters */}
          <aside className="w-full shrink-0 lg:sticky lg:top-32 lg:w-[340px]">
            <div className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.06)]">
              <div className="mb-6 flex items-center justify-between">
                <h2 className="font-serif text-2xl text-brand-charcoal">Filters</h2>
                <button
                  type="button"
                  onClick={resetFilters}
                  className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest hover:text-brand-forest-deep"
                >
                  {t('villas.resetFilters')}
                </button>
              </div>

              <div className="flex flex-col gap-5">
                <label className="block">
                  <span className="sr-only">Search villas</span>
                  <input
                    value={filters.query}
                    onChange={(event) => updateFilter('query', event.target.value)}
                    placeholder={t('villas.searchPlaceholder')}
                    className="h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                  />
                </label>

                <CustomSelect
                  value={filters.roomType}
                  onChange={(val) => updateFilter('roomType', val)}
                  placeholder={t('villas.allTypes')}
                  options={[
                    { label: t('villas.allTypes'), value: 'all' },
                    ...roomTypes.map((roomType) => ({ label: roomType, value: roomType }))
                  ]}
                />

                <CustomSelect
                  value={filters.amenity}
                  onChange={(val) => updateFilter('amenity', val)}
                  placeholder={t('villas.allAmenities')}
                  options={[
                    { label: t('villas.allAmenities'), value: 'all' },
                    ...amenities.map((amenity) => ({ label: amenity, value: amenity }))
                  ]}
                />

                <div className="flex items-center gap-3">
                  <input
                    value={filters.minPrice}
                    onChange={(event) => updateFilter('minPrice', event.target.value)}
                    inputMode="numeric"
                    placeholder={t('villas.minPrice')}
                    className="h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                  />
                  <span className="text-brand-stone/60">-</span>
                  <input
                    value={filters.maxPrice}
                    onChange={(event) => updateFilter('maxPrice', event.target.value)}
                    inputMode="numeric"
                    placeholder={t('villas.maxPrice')}
                    className="h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none transition focus:border-brand-forest focus:ring-2 focus:ring-brand-sage/30"
                  />
                </div>

                <div className="mt-2 border-t border-brand-stone/50 pt-5">
                  <button
                    onClick={() => setShowCalendar(!showCalendar)}
                    className={`flex h-12 w-full items-center justify-between rounded-full border px-5 text-sm transition ${
                      filters.checkIn && filters.checkOut
                        ? 'border-brand-forest bg-brand-forest/10 text-brand-forest'
                        : 'border-brand-stone bg-brand-white hover:border-brand-forest'
                    }`}
                  >
                    <span>{filters.checkIn && filters.checkOut ? formattedDates : t('villas.selectDates')}</span>
                    <span>📅</span>
                  </button>
                  {showCalendar && (
                    <div className="mt-4 overflow-hidden rounded-2xl bg-brand-white shadow-lg">
                      <AvailabilityCalendar
                        onDateSelect={handleDateSelect}
                        initialCheckIn={filters.checkIn || undefined}
                        initialCheckOut={filters.checkOut || undefined}
                      />
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <CustomSelect
                    value={filters.adults || ''}
                    onChange={(val) => updateFilter('adults', val)}
                    placeholder={t('villas.adults')}
                    options={[
                      { label: t('villas.adults'), value: '' },
                      ...[1, 2, 3, 4, 5, 6].map((n) => ({ label: `${n} ${t('villas.adults')}`, value: String(n) }))
                    ]}
                  />
                  <CustomSelect
                    value={filters.children || ''}
                    onChange={(val) => updateFilter('children', val)}
                    placeholder={t('villas.children')}
                    options={[
                      { label: t('villas.children'), value: '' },
                      ...[0, 1, 2, 3, 4].map((n) => ({ label: `${n} ${t('villas.children')}`, value: String(n) }))
                    ]}
                  />
                </div>

                <CustomSelect
                  value={filters.bedrooms || ''}
                  onChange={(val) => updateFilter('bedrooms', val)}
                  placeholder={t('villas.bedrooms')}
                  options={[
                    { label: t('villas.bedrooms'), value: '' },
                    ...[1, 2, 3, 4, 5, 6].map((n) => ({ label: `${n}+ ${t('villas.bedrooms')}`, value: String(n) }))
                  ]}
                />
              </div>
            </div>
          </aside>

          {/* RIGHT CONTENT: Main Grid */}
          <main className="min-w-0 flex-1">
            <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
              <p className="text-sm font-medium text-brand-ink/70">
                {t('villas.showing')} <span className="font-bold text-brand-forest">{filteredVillas.length}</span>
              </p>
              <div className="flex items-center gap-3">
                <span className="text-sm text-brand-ink/60">Sort by</span>
                <div className="w-48">
                  <CustomSelect
                    value={filters.sort}
                    onChange={(val) => updateFilter('sort', val as SortOption)}
                    options={[
                      { label: t('villas.sort.recommended'), value: 'recommended' },
                      { label: t('villas.sort.priceAsc'), value: 'price-asc' },
                      { label: t('villas.sort.priceDesc'), value: 'price-desc' },
                      { label: t('villas.sort.capacityDesc'), value: 'capacity-desc' },
                    ]}
                  />
                </div>
              </div>
            </div>

            <div ref={resultsRef} className="scroll-mt-32" />

            {isSearchMode && !query.isLoading && (
              <div className="mb-8 rounded-2xl bg-brand-forest/5 p-4 text-brand-forest">
                <h2 className="text-xl font-serif">{t('villas.searchResults')}</h2>
                <div className="mt-2 flex flex-wrap items-center gap-3 text-sm opacity-80">
                  <span>{filteredVillas.length} {t('villas.villasAvailable')}</span>
                  {formattedDates && (
                    <>
                      <span className="h-1 w-1 rounded-full bg-brand-forest/40" />
                      <span>{formattedDates}</span>
                    </>
                  )}
                </div>
              </div>
            )}

            {query.isLoading ? (
              <VillaSkeletonGrid />
            ) : null}

            {!query.isLoading && filteredVillas.length === 0 ? (
              <div className="mt-4 rounded-[2rem] bg-brand-paper px-6 py-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
                <h2 className="font-serif text-3xl text-brand-charcoal">
                  {isSearchMode ? t('villas.noVillasAvailable') : t('villas.noVillasAvailable')}
                </h2>
                <p className="mx-auto mt-4 max-w-xl text-base leading-8 text-brand-ink/62">
                  {isSearchMode ? t('villas.noVillasSearch') : t('villas.noVillasFilter')}
                </p>
                <button
                  type="button"
                  onClick={resetFilters}
                  className="mt-8 inline-flex h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
                >
                  {isSearchMode ? t('villas.clearSearch') : t('villas.showAll')}
                </button>
              </div>
            ) : null}

            {!query.isLoading && filteredVillas.length > 0 ? (
              <>
                <motion.div
                  variants={containerVariants}
                  initial="hidden"
                  animate="show"
                  className="grid gap-7 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4"
                >
                  {paginatedVillas.map((villa) => (
                    <motion.div key={villa.id} variants={itemVariants} className="h-full">
                      <VillaCard villa={villa} />
                    </motion.div>
                  ))}
                </motion.div>

                <nav className="mt-12 flex items-center justify-center gap-3" aria-label="Villa pagination">
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
          </main>
        </div>
      </div>
    </section>
  );
};
