'use client';

import { useMemo, useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { fetchVillas } from './villasApi';
import { fallbackVillas } from './villaFallbacks';
import { AvailabilityCalendar } from './components/AvailabilityCalendar';
import {
  SpotlightStageView,
  EstateBlueprintView,
  QuickInspectDrawer,
  VillaCompareModal,
  getArchitecturalSpecs,
  getVillaGallery,
  type VibeCategory,
} from './components/SanctuaryVisualizer';
import type { SortOption, Villa, VillaFilters } from './types';
import { useCurrency, useTranslation } from '../../shared/i18n/hooks';
import { motion } from 'framer-motion';
import { CustomSelect } from '../../shared/components/CustomSelect';

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 28 },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      type: 'spring' as const,
      stiffness: 100,
      damping: 16,
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

type ViewMode = 'spotlight' | 'bento' | 'blueprint';

function uniqueValues(values: string[]) {
  return Array.from(new Set(values)).filter(Boolean).sort((a, b) => a.localeCompare(b));
}

function applyFilters(villas: Villa[], filters: VillaFilters, activeVibe: VibeCategory) {
  const query = filters.query.trim().toLowerCase();
  const minPrice = filters.minPrice ? Number(filters.minPrice) : null;
  const maxPrice = filters.maxPrice ? Number(filters.maxPrice) : null;
  const adults = filters.adults ? parseInt(filters.adults, 10) : 0;
  const childrenCount = filters.children ? parseInt(filters.children, 10) : 0;
  const requiredCapacity = adults + childrenCount;

  return villas.filter((villa) => {
    const spec = getArchitecturalSpecs(villa);
    const matchesVibe = activeVibe === 'all' || spec.vibes.includes(activeVibe);
    const amenityNames = villa.amenities.map((amenity) => amenity.name.toLowerCase());
    const matchesQuery =
      query.length === 0 ||
      villa.name.toLowerCase().includes(query) ||
      villa.roomNumber.toLowerCase().includes(query) ||
      villa.description.toLowerCase().includes(query) ||
      villa.roomType.toLowerCase().includes(query) ||
      amenityNames.some((name) => name.includes(query));
    const matchesType = filters.roomType === 'all' || villa.roomType === filters.roomType;
    const matchesAmenity =
      filters.amenity === 'all' || villa.amenities.some((amenity) => amenity.name === filters.amenity);
    const matchesMin = minPrice === null || villa.pricePerNight >= minPrice;
    const matchesMax = maxPrice === null || villa.pricePerNight <= maxPrice;
    const matchesCapacity = requiredCapacity === 0 || villa.capacity >= requiredCapacity;

    return matchesVibe && matchesQuery && matchesType && matchesAmenity && matchesMin && matchesMax && matchesCapacity;
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
    <div className="grid gap-7 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 6 }, (_, index) => (
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

/* -------------------------------------------------------------------------- */
/* AVANT-GARDE EDITORIAL BENTO CARD                                           */
/* -------------------------------------------------------------------------- */

function EditorialVillaCard({
  villa,
  index,
  featured,
  onQuickInspect,
  isCompared,
  onToggleCompare,
}: {
  villa: Villa;
  index: number;
  featured: boolean;
  onQuickInspect: (villa: Villa) => void;
  isCompared: boolean;
  onToggleCompare: (villa: Villa) => void;
}) {
  const { formatCurrency } = useCurrency();
  const { t, lang } = useTranslation();
  const isVi = lang === 'vi';
  const spec = getArchitecturalSpecs(villa);
  const gallery = getVillaGallery(villa);
  const [photoIdx, setPhotoIdx] = useState(0);

  const activePhoto = gallery[photoIdx % gallery.length] || villa.imageUrl;

  return (
    <article
      className={`group relative flex h-full flex-col overflow-hidden rounded-[2.2rem] bg-brand-white shadow-[0_18px_60px_rgba(32,52,43,0.07)] transition-all duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-2 hover:shadow-[0_32px_90px_rgba(32,52,43,0.15)] ${
        featured ? 'lg:flex-row' : ''
      }`}
    >
      {/* Image Canvas with Interactive Mini-Gallery Switcher */}
      <div
        className={`relative overflow-hidden ${
          featured ? 'aspect-[16/11] lg:aspect-auto lg:w-[56%]' : 'aspect-[16/11]'
        }`}
      >
        <Link href={`/villas/${villa.slug || villa.id}`} className="block h-full w-full">
          <img
            src={activePhoto}
            alt={`${villa.name} at Nhu Villas`}
            className="h-full w-full object-cover transition-transform duration-[950ms] ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.05]"
            loading="lazy"
            decoding="async"
          />
        </Link>

        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/65 via-transparent to-black/20" />

        {/* Architectural Number Watermark + Zone Badge */}
        <div className="absolute left-5 top-5 flex flex-wrap items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-full border border-white/30 bg-black/45 font-serif text-sm italic text-white backdrop-blur-md">
            0{index + 1}
          </span>
          <span className="rounded-full bg-brand-white/95 px-3.5 py-1 text-[0.65rem] font-bold uppercase tracking-[0.18em] text-brand-forest shadow-sm backdrop-blur-md">
            {isVi ? spec.zoneVi : spec.zoneEn}
          </span>
          {villa.promotions &&
            villa.promotions.slice(0, 1).map((promo) => (
              <span
                key={promo.id}
                className="rounded-full bg-brand-coral px-3 py-1 text-[0.65rem] font-bold uppercase tracking-[0.16em] text-white shadow-sm"
              >
                🏷️ {promo.title}
              </span>
            ))}
        </div>

        {/* Compare Toggle Top-Right */}
        <button
          type="button"
          onClick={() => onToggleCompare(villa)}
          className={`absolute right-4 top-4 z-10 rounded-full border px-3 py-1 text-[0.62rem] font-semibold uppercase tracking-wider backdrop-blur-md transition ${
            isCompared
              ? 'border-amber-300 bg-amber-400 text-brand-forest-deep shadow-lg'
              : 'border-white/30 bg-black/40 text-white hover:bg-white hover:text-brand-forest-deep'
          }`}
        >
          {isCompared ? (isVi ? '✓ Đang so sánh' : '✓ Comparing') : (isVi ? '+ So sánh' : '+ Compare')}
        </button>

        {/* Bottom Interactive Image Scrubber Dots + Architectural Footprint */}
        <div className="absolute inset-x-5 bottom-4 z-10 flex items-center justify-between">
          <div className="flex items-center gap-2 rounded-full bg-black/45 px-3 py-1 text-[0.68rem] font-medium text-white backdrop-blur-md">
            <span>📐 {spec.areaSqm}</span>
            <span className="opacity-40">|</span>
            <span>⛰️ {spec.elevation}</span>
          </div>

          <div className="flex items-center gap-1.5 rounded-full bg-black/45 px-2.5 py-1.5 backdrop-blur-md">
            {gallery.slice(0, 5).map((_, idx) => (
              <button
                key={idx}
                type="button"
                aria-label={`View photo ${idx + 1}`}
                onMouseEnter={() => setPhotoIdx(idx)}
                onClick={(e) => {
                  e.preventDefault();
                  setPhotoIdx(idx);
                }}
                className={`h-2 rounded-full transition-all ${
                  idx === photoIdx % gallery.length ? 'w-5 bg-white' : 'w-2 bg-white/45 hover:bg-white/80'
                }`}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Content Deck */}
      <div className={`flex flex-1 flex-col justify-between p-6 sm:p-8 ${featured ? 'lg:w-[44%]' : ''}`}>
        <div>
          <div className="flex items-center justify-between gap-2">
            <p className="text-[0.68rem] font-bold uppercase tracking-[0.22em] text-brand-sage">
              {villa.roomNumber} · {villa.roomType}
            </p>
            <div className="flex items-center gap-1.5">
              <StarRating rating={Math.round(villa.rating)} size="sm" />
              <span className="text-xs font-medium text-brand-ink/60">
                {villa.rating.toFixed(1)} ({villa.reviewCount})
              </span>
            </div>
          </div>

          <Link href={`/villas/${villa.slug || villa.id}`}>
            <h2
              className={`mt-2 font-serif leading-tight text-brand-charcoal transition-colors group-hover:text-brand-forest ${
                featured ? 'text-3xl sm:text-4xl' : 'text-2xl sm:text-[1.75rem]'
              }`}
            >
              {villa.name}
            </h2>
          </Link>

          <p className="mt-3 line-clamp-2 text-sm leading-relaxed text-brand-ink/68">
            {villa.description}
          </p>

          {/* Architectural Highlight Pills */}
          <div className="mt-5 flex flex-wrap gap-2">
            <span className="rounded-xl bg-brand-sand px-3 py-1.5 text-xs font-medium text-brand-charcoal">
              🌊 {isVi ? spec.poolSpecVi : spec.poolSpecEn}
            </span>
            <span className="rounded-xl bg-brand-sand px-3 py-1.5 text-xs font-medium text-brand-charcoal">
              ✨ {isVi ? spec.butlerVi : spec.butlerEn}
            </span>
          </div>
        </div>

        {/* Footer Tariff & Dual Actions */}
        <div className="mt-7 border-t border-brand-stone/50 pt-5">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="text-[0.65rem] font-semibold uppercase tracking-[0.18em] text-brand-ink/45">
                {t('villas.from')} · {villa.capacity} {t('villas.guests')} · {villa.bedrooms} {t('villas.bedrooms')}
              </p>
              <p className="mt-1 font-serif text-3xl text-brand-charcoal">
                {formatCurrency(villa.pricePerNight)}
                <span className="ml-1 font-sans text-xs font-normal text-brand-ink/50">
                  / {isVi ? 'đêm' : 'night'}
                </span>
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => onQuickInspect(villa)}
                className="inline-flex h-11 items-center justify-center rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white shadow-sm transition hover:bg-brand-forest-deep"
              >
                ⚡ {isVi ? 'Đặt nhanh' : 'Quick Book'}
              </button>
              <Link
                href={`/villas/${villa.slug || villa.id}`}
                className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-brand-stone text-brand-charcoal transition hover:border-brand-forest hover:bg-brand-forest hover:text-white"
                aria-label={`Explore ${villa.name}`}
              >
                →
              </Link>
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}

/* -------------------------------------------------------------------------- */
/* MAIN VILLAS PAGE — SANCTUARY CURATOR                                       */
/* -------------------------------------------------------------------------- */

export const VillasPage = () => {
  const searchParams = useSearchParams();
  const router = useRouter();
  const resultsRef = useRef<HTMLDivElement>(null);
  const [showCalendar, setShowCalendar] = useState(false);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [viewMode, setViewMode] = useState<ViewMode>('spotlight');
  const [activeVibe, setActiveVibe] = useState<VibeCategory>('all');
  const [quickInspectVilla, setQuickInspectVilla] = useState<Villa | null>(null);
  const [comparedVillas, setComparedVillas] = useState<Villa[]>([]);
  const [showCompareModal, setShowCompareModal] = useState(false);

  const { t, lang } = useTranslation();
  const isVi = lang === 'vi';

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
    staleTime: 60_000,
  });
  const sourceVillas: Villa[] = query.data ?? (query.isError ? fallbackVillas : emptyVillas);

  const roomTypes = useMemo(() => uniqueValues(sourceVillas.map((villa) => villa.roomType)), [sourceVillas]);
  const amenities = useMemo(
    () => uniqueValues(sourceVillas.flatMap((villa) => villa.amenities.map((amenity) => amenity.name))),
    [sourceVillas],
  );
  const filteredVillas = useMemo(
    () => sortVillas(applyFilters(sourceVillas, filters, activeVibe), filters.sort),
    [filters, sourceVillas, activeVibe],
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
    setActiveVibe('all');
    setPage(1);
    router.replace('/villas');
  };

  const toggleCompare = (villa: Villa) => {
    setComparedVillas((prev) => {
      if (prev.some((v) => v.id === villa.id)) {
        return prev.filter((v) => v.id !== villa.id);
      }
      if (prev.length >= 3) {
        return [...prev.slice(1), villa];
      }
      return [...prev, villa];
    });
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

  const vibePills: { id: VibeCategory; icon: string; labelVi: string; labelEn: string }[] = [
    { id: 'all', icon: '✨', labelVi: 'Tất Cả Tuyệt Tác', labelEn: 'All Sanctuaries' },
    { id: 'water', icon: '🌊', labelVi: 'Thủy Đình & Hồ Vô Cực', labelEn: 'Infinity Pool & Water' },
    { id: 'forest', icon: '🌲', labelVi: 'Ẩn Mình Giữa Rừng Thông', labelEn: 'Deep Pine Seclusion' },
    { id: 'family', icon: '🥂', labelVi: 'Dinh Thự Gia Đình & Tiệc Riêng', labelEn: 'Family & Hosted Estates' },
    { id: 'couple', icon: '💑', labelVi: 'Trăng Mật Lãng Mạn', labelEn: 'Intimate Couples Retreat' },
  ];

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-28 pt-32 sm:px-8 xl:px-12 lg:pt-40">
      <div className="mx-auto w-full max-w-[1680px]">
        {/* Architectural Hero Header */}
        <div className="mb-10 grid gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-end">
          <div>
            <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-brand-forest/20 bg-brand-paper px-4 py-1.5 text-[0.68rem] font-semibold uppercase tracking-[0.22em] text-brand-forest">
              <span>🏛️ {t('villas.collection')}</span>
              <span>·</span>
              <span>Hồ Tuyền Lâm, Đà Lạt</span>
            </div>
            <h1 className="max-w-[14ch] font-serif text-5xl leading-[1.02] text-brand-charcoal sm:text-6xl lg:text-7xl">
              {isVi ? 'Không Gian Nghỉ Dưỡng Độc Bản.' : t('villas.title')}
            </h1>
          </div>

          <div className="flex flex-col items-start gap-5 lg:items-end">
            <p className="max-w-xl text-base leading-relaxed text-brand-ink/68 lg:text-right">
              {isVi
                ? 'Trải nghiệm chọn biệt thự theo cảm xúc kiến trúc: Trình chiếu Sân khấu Điện ảnh, Tạp chí Bất đối xứng hoặc Bản đồ Địa hình Đồi thông & Hồ Tuyền Lâm.'
                : t('villas.subtitle')}
            </p>

            {/* 3 Avant-Garde View Mode Switcher */}
            <div className="inline-flex flex-wrap items-center gap-1.5 rounded-full border border-brand-stone bg-brand-paper p-1.5 shadow-sm">
              <button
                type="button"
                onClick={() => setViewMode('spotlight')}
                className={`flex items-center gap-2 rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition-all ${
                  viewMode === 'spotlight'
                    ? 'bg-brand-forest-deep text-white shadow-md'
                    : 'text-brand-ink/70 hover:text-brand-charcoal'
                }`}
              >
                <span>🎬</span>
                <span>{isVi ? 'Sân Khấu Điện Ảnh' : 'Spotlight Stage'}</span>
              </button>
              <button
                type="button"
                onClick={() => setViewMode('bento')}
                className={`flex items-center gap-2 rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition-all ${
                  viewMode === 'bento'
                    ? 'bg-brand-forest-deep text-white shadow-md'
                    : 'text-brand-ink/70 hover:text-brand-charcoal'
                }`}
              >
                <span>📰</span>
                <span>{isVi ? 'Tạp Chí Bất Đối Xứng' : 'Editorial Bento'}</span>
              </button>
              <button
                type="button"
                onClick={() => setViewMode('blueprint')}
                className={`flex items-center gap-2 rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] transition-all ${
                  viewMode === 'blueprint'
                    ? 'bg-brand-forest-deep text-white shadow-md'
                    : 'text-brand-ink/70 hover:text-brand-charcoal'
                }`}
              >
                <span>🗺️</span>
                <span>{isVi ? 'Bản Đồ Đồi Thông' : 'Estate Masterplan'}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Sensory Mood / Vibe Selector Bar */}
        <div className="mb-6 flex items-center gap-2.5 overflow-x-auto pb-2 no-scrollbar">
          {vibePills.map((vibe) => {
            const active = activeVibe === vibe.id;
            return (
              <button
                key={vibe.id}
                type="button"
                onClick={() => {
                  setActiveVibe(vibe.id);
                  setPage(1);
                }}
                className={`inline-flex shrink-0 items-center gap-2 rounded-full border px-5 py-2.5 text-xs font-semibold tracking-wide transition-all duration-300 ${
                  active
                    ? 'border-brand-forest bg-brand-forest text-white shadow-[0_10px_25px_rgba(25,52,42,0.22)]'
                    : 'border-brand-stone/80 bg-brand-paper/90 text-brand-charcoal hover:border-brand-forest/50 hover:bg-brand-white'
                }`}
              >
                <span>{vibe.icon}</span>
                <span>{isVi ? vibe.labelVi : vibe.labelEn}</span>
              </button>
            );
          })}
        </div>

        {/* Floating Glass Filter Dock */}
        <div className="relative z-30 mb-10 rounded-[2rem] border border-brand-stone/70 bg-brand-paper/90 p-4 shadow-[0_20px_60px_rgba(32,52,43,0.07)] backdrop-blur-xl sm:p-5">
          <div className="grid items-center gap-3 md:grid-cols-2 lg:grid-cols-[1.2fr_1fr_0.8fr_0.9fr_auto]">
            {/* Search Input */}
            <div className="relative">
              <input
                value={filters.query}
                onChange={(event) => updateFilter('query', event.target.value)}
                placeholder={t('villas.searchPlaceholder')}
                className="h-12 w-full rounded-full border border-brand-stone bg-brand-white pl-11 pr-5 text-sm text-brand-ink outline-none transition focus:border-brand-forest"
              />
              <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-brand-ink/40">
                🔍
              </span>
            </div>

            {/* Date Picker Trigger */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowCalendar(!showCalendar)}
                className={`flex h-12 w-full items-center justify-between rounded-full border px-5 text-sm transition ${
                  filters.checkIn && filters.checkOut
                    ? 'border-brand-forest bg-brand-forest/10 font-semibold text-brand-forest'
                    : 'border-brand-stone bg-brand-white text-brand-ink/80 hover:border-brand-forest'
                }`}
              >
                <span className="truncate">
                  {filters.checkIn && filters.checkOut ? formattedDates : t('villas.selectDates')}
                </span>
                <span>📅</span>
              </button>
              {showCalendar && (
                <div className="absolute left-0 top-14 z-50 w-[340px] overflow-hidden rounded-2xl border border-brand-stone bg-brand-white shadow-2xl sm:w-[380px]">
                  <AvailabilityCalendar
                    onDateSelect={handleDateSelect}
                    initialCheckIn={filters.checkIn || undefined}
                    initialCheckOut={filters.checkOut || undefined}
                  />
                </div>
              )}
            </div>

            {/* Guests Filter */}
            <CustomSelect
              value={filters.adults || ''}
              onChange={(val) => updateFilter('adults', val)}
              placeholder={t('villas.adults')}
              options={[
                { label: isVi ? 'Mọi quy mô khách' : t('villas.adults'), value: '' },
                ...[2, 4, 6, 8].map((n) => ({
                  label: `${n}+ ${t('villas.guests')}`,
                  value: String(n),
                })),
              ]}
            />

            {/* Sort Selector */}
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

            {/* Advanced Filter Toggle & Reset */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                className={`inline-flex h-12 items-center gap-2 rounded-full border px-4 text-xs font-semibold uppercase tracking-wider transition ${
                  showAdvancedFilters
                    ? 'border-brand-forest bg-brand-forest text-white'
                    : 'border-brand-stone bg-brand-white text-brand-charcoal hover:border-brand-forest'
                }`}
              >
                <span>⚙️ {isVi ? 'Bộ lọc sâu' : 'Filters'}</span>
              </button>
              {(filters.query ||
                filters.roomType !== 'all' ||
                filters.amenity !== 'all' ||
                filters.minPrice ||
                filters.maxPrice ||
                filters.checkIn ||
                filters.adults ||
                activeVibe !== 'all') && (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="inline-flex h-12 items-center justify-center rounded-full border border-brand-coral/40 bg-brand-coral/10 px-4 text-xs font-semibold uppercase tracking-wider text-brand-coral hover:bg-brand-coral hover:text-white"
                >
                  {t('villas.resetFilters')}
                </button>
              )}
            </div>
          </div>

          {/* Expandable Advanced Filters Drawer */}
          {showAdvancedFilters && (
            <div className="mt-4 grid gap-3 border-t border-brand-stone/60 pt-4 sm:grid-cols-2 lg:grid-cols-5">
              <CustomSelect
                value={filters.roomType}
                onChange={(val) => updateFilter('roomType', val)}
                placeholder={t('villas.allTypes')}
                options={[
                  { label: t('villas.allTypes'), value: 'all' },
                  ...roomTypes.map((rt) => ({ label: rt, value: rt })),
                ]}
              />
              <CustomSelect
                value={filters.amenity}
                onChange={(val) => updateFilter('amenity', val)}
                placeholder={t('villas.allAmenities')}
                options={[
                  { label: t('villas.allAmenities'), value: 'all' },
                  ...amenities.map((a) => ({ label: a, value: a })),
                ]}
              />
              <CustomSelect
                value={filters.bedrooms || ''}
                onChange={(val) => updateFilter('bedrooms', val)}
                placeholder={t('villas.bedrooms')}
                options={[
                  { label: t('villas.bedrooms'), value: '' },
                  ...[1, 2, 3, 4].map((n) => ({ label: `${n}+ ${t('villas.bedrooms')}`, value: String(n) })),
                ]}
              />
              <CustomSelect
                value={filters.minPrice || ''}
                onChange={(val) => updateFilter('minPrice', val)}
                placeholder={t('villas.minPrice')}
                options={[
                  { label: t('villas.minPrice'), value: '' },
                  { label: '$300+', value: '300' },
                  { label: '$500+', value: '500' },
                  { label: '$800+', value: '800' },
                  { label: '$1,000+', value: '1000' },
                ]}
              />
              <CustomSelect
                value={filters.maxPrice || ''}
                onChange={(val) => updateFilter('maxPrice', val)}
                placeholder={t('villas.maxPrice')}
                options={[
                  { label: t('villas.maxPrice'), value: '' },
                  { label: '≤ $600', value: '600' },
                  { label: '≤ $1,200', value: '1200' },
                  { label: '≤ $2,500', value: '2500' },
                ]}
              />
            </div>
          )}
        </div>

        <div ref={resultsRef} className="scroll-mt-32" />

        {/* Loading State */}
        {query.isLoading ? <VillaSkeletonGrid /> : null}

        {/* Empty State */}
        {!query.isLoading && filteredVillas.length === 0 ? (
          <div className="mt-4 rounded-[2rem] bg-brand-paper px-6 py-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
            <h2 className="font-serif text-3xl text-brand-charcoal">{t('villas.noVillasAvailable')}</h2>
            <p className="mx-auto mt-4 max-w-xl text-base leading-8 text-brand-ink/62">
              {isSearchMode ? t('villas.noVillasSearch') : t('villas.noVillasFilter')}
            </p>
            <button
              type="button"
              onClick={resetFilters}
              className="mt-8 inline-flex h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
            >
              {t('villas.showAll')}
            </button>
          </div>
        ) : null}

        {/* 3 AVANT-GARDE SHOWCASE VIEWS */}
        {!query.isLoading && filteredVillas.length > 0 ? (
          <>
            {viewMode === 'spotlight' && (
              <SpotlightStageView
                villas={paginatedVillas}
                onQuickInspect={(villa) => setQuickInspectVilla(villa)}
                comparedIds={comparedVillas.map((v) => v.id)}
                onToggleCompare={toggleCompare}
              />
            )}

            {viewMode === 'blueprint' && (
              <EstateBlueprintView
                villas={paginatedVillas}
                onQuickInspect={(villa) => setQuickInspectVilla(villa)}
              />
            )}

            {viewMode === 'bento' && (
              <motion.div
                variants={containerVariants}
                initial="hidden"
                animate="show"
                className="grid gap-8 md:grid-cols-2 lg:grid-cols-3"
              >
                {paginatedVillas.map((villa, idx) => {
                  // Make the 1st and 4th cards span 2 columns on desktop for an Editorial Asymmetric Bento layout
                  const isFeaturedSpan = idx === 0 || idx === 3;
                  return (
                    <motion.div
                      key={villa.id}
                      variants={itemVariants}
                      className={isFeaturedSpan ? 'md:col-span-2 lg:col-span-2' : 'col-span-1'}
                    >
                      <EditorialVillaCard
                        villa={villa}
                        index={idx}
                        featured={isFeaturedSpan}
                        onQuickInspect={(v) => setQuickInspectVilla(v)}
                        isCompared={comparedVillas.some((c) => c.id === villa.id)}
                        onToggleCompare={toggleCompare}
                      />
                    </motion.div>
                  );
                })}
              </motion.div>
            )}

            {totalPages > 1 && (
              <nav className="mt-12 flex items-center justify-center gap-3" aria-label="Villa pagination">
                {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                  <button
                    key={pageNumber}
                    type="button"
                    onClick={() => setPage(pageNumber)}
                    className={`h-11 w-11 rounded-full border text-sm font-semibold transition ${
                      pageNumber === currentPage
                        ? 'border-brand-forest bg-brand-forest text-brand-white'
                        : 'border-brand-stone bg-brand-paper text-brand-forest hover:border-brand-forest'
                    }`}
                  >
                    {pageNumber}
                  </button>
                ))}
              </nav>
            )}
          </>
        ) : null}
      </div>

      {/* Floating Compare Bar at Bottom */}
      {comparedVillas.length > 0 && (
        <div className="fixed bottom-6 left-1/2 z-40 flex -translate-x-1/2 items-center gap-4 rounded-full border border-white/20 bg-brand-forest-deep/95 px-6 py-3 text-white shadow-2xl backdrop-blur-xl">
          <div className="flex items-center gap-2 text-xs">
            <span className="font-semibold uppercase tracking-wider text-brand-sage-light">
              {isVi ? 'Đối chiếu kiến trúc:' : 'Compare:'}
            </span>
            {comparedVillas.map((v) => (
              <span
                key={v.id}
                className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-xs font-medium"
              >
                {v.roomNumber}
                <button
                  type="button"
                  onClick={() => toggleCompare(v)}
                  className="opacity-70 hover:opacity-100"
                >
                  ✕
                </button>
              </span>
            ))}
          </div>
          <button
            type="button"
            onClick={() => setShowCompareModal(true)}
            className="rounded-full bg-amber-300 px-4 py-1.5 text-xs font-bold uppercase tracking-wider text-brand-forest-deep transition hover:bg-white"
          >
            {isVi ? `So sánh ngay (${comparedVillas.length})` : `Compare Now (${comparedVillas.length})`}
          </button>
        </div>
      )}

      {/* Slide-Over Quick Sanctuary Inspector & Instant Reserve Drawer */}
      <QuickInspectDrawer
        villa={quickInspectVilla}
        onClose={() => setQuickInspectVilla(null)}
      />

      {/* Side-by-Side Comparison Matrix Modal */}
      {showCompareModal && (
        <VillaCompareModal
          villas={comparedVillas}
          onClose={() => setShowCompareModal(false)}
          onQuickInspect={(villa) => setQuickInspectVilla(villa)}
        />
      )}
    </section>
  );
};
