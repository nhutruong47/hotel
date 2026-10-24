'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import type { Villa } from '../types';
import { useCurrency, useTranslation } from '../../../shared/i18n/hooks';
import { calculateNights, getTodayDate, getTomorrowDate, type BookingDraft } from '../../booking/bookingUtils';

export type VibeCategory = 'all' | 'water' | 'forest' | 'family' | 'couple';

export interface ArchitecturalSpec {
  areaSqm: string;
  elevation: string;
  poolSpecVi: string;
  poolSpecEn: string;
  viewVi: string;
  viewEn: string;
  butlerVi: string;
  butlerEn: string;
  vibes: VibeCategory[];
  mapX: number;
  mapY: number;
  zoneVi: string;
  zoneEn: string;
}

const DEFAULT_FALLBACK_GALLERY = [
  '/images/nhu-garden-pool-villa.jpg',
  '/images/nhu-villa-interior.jpg',
  '/images/nhu-infinity-pool.jpg',
  '/images/nhu-private-dining.jpg',
  '/images/nhu-location-landscape.jpg',
];

export function getArchitecturalSpecs(villa: Villa): ArchitecturalSpec {
  const code = (villa.roomNumber || '').toUpperCase();
  if (code === 'GV-01') {
    return {
      areaSqm: '185 m²',
      elevation: '1,510m',
      poolSpecVi: 'Hồ bơi đá tự nhiên sưởi ấm 29°C',
      poolSpecEn: '29°C Heated Natural Stone Pool',
      viewVi: 'Thung lũng thông & Vườn thiền riêng',
      viewEn: 'Pine Valley & Private Zen Garden',
      butlerVi: 'Bữa sáng nổi tại hồ & Quản gia 24/7',
      butlerEn: 'Floating Breakfast & 24/7 Butler',
      vibes: ['water', 'couple'],
      mapX: 34,
      mapY: 58,
      zoneVi: 'Khu Vườn Thiền Ven Suối',
      zoneEn: 'Streamside Zen Garden',
    };
  }
  if (code === 'TF-02') {
    return {
      areaSqm: '280 m²',
      elevation: '1,525m',
      poolSpecVi: 'Hồ bơi gia đình & Sân hiên tắm nắng',
      poolSpecEn: 'Family Plunge Pool & Sun Deck',
      viewVi: 'Đồi thông cổ thụ & Sân vườn rộng',
      viewEn: 'Ancient Pine Grove & Courtyard',
      butlerVi: 'Tiệc BBQ sân vườn & Khu vui chơi trẻ em',
      butlerEn: 'Private Garden BBQ & Family Setup',
      vibes: ['family', 'forest'],
      mapX: 52,
      mapY: 46,
      zoneVi: 'Sườn Đồi Thông Trung Tâm',
      zoneEn: 'Central Pine Terrace',
    };
  }
  if (code === 'HR-03') {
    return {
      areaSqm: '420 m²',
      elevation: '1,560m',
      poolSpecVi: 'Hồ bơi vô cực tràn bờ sưởi ấm 30°C',
      poolSpecEn: '30°C Cliffside Heated Infinity Pool',
      viewVi: 'Hoàng hôn toàn cảnh Hồ Tuyền Lâm',
      viewEn: 'Panoramic Tuyen Lam Lake Sunset',
      butlerVi: 'Đầu bếp riêng & Đưa đón sân bay miễn phí',
      butlerEn: 'Private Chef Table & Airport Transfer',
      vibes: ['water', 'family', 'forest'],
      mapX: 68,
      mapY: 28,
      zoneVi: 'Đỉnh Đồi Vô Cực (Ridge Crest)',
      zoneEn: 'Sunset Ridge Crest',
    };
  }
  if (code === 'SE-04') {
    return {
      areaSqm: '680 m²',
      elevation: '1,585m',
      poolSpecVi: 'Hồ bơi vô cực 22m + Phòng Spa & Sauna riêng',
      poolSpecEn: '22m Infinity Pool + Private Spa Pavilion',
      viewVi: 'Tầm nhìn 360° Hồ Tuyền Lâm & Đỉnh Pinhatt',
      viewEn: '360° Lake & Pinhatt Peak Panorama',
      butlerVi: 'Quản gia riêng, Hầm rượu vang & Limousine khứ hồi',
      butlerEn: 'Dedicated Butler, Wine Cellar & Limousine',
      vibes: ['water', 'family', 'forest'],
      mapX: 78,
      mapY: 18,
      zoneVi: 'Bán Đảo Độc Bản (Private Peninsula)',
      zoneEn: 'Signature Peninsula',
    };
  }
  if (code === 'WC-05' || code === 'WC-04') {
    return {
      areaSqm: '210 m²',
      elevation: '1,495m',
      poolSpecVi: 'Hồ nước phản chiếu & Bể sục khoáng nóng',
      poolSpecEn: 'Reflective Water Court & Heated Jacuzzi',
      viewVi: 'Mặt hồ tĩnh lặng & Kiến trúc đá ấm',
      viewEn: 'Mirror Water Courtyard & Warm Stone',
      butlerVi: 'Trà chiều thảo mộc & Liệu trình Spa tại phòng',
      butlerEn: 'Herbal High Tea & In-Villa Spa Ritual',
      vibes: ['water', 'couple'],
      mapX: 24,
      mapY: 72,
      zoneVi: 'Vịnh Nước Tĩnh Lặng (Water Sanctuary)',
      zoneEn: 'Water Courtyard Sanctuary',
    };
  }
  return {
    areaSqm: '155 m²',
    elevation: '1,540m',
    poolSpecVi: 'Bồn tắm gỗ Tuyết Tùng ngâm thảo mộc nóng',
    poolSpecEn: 'Heated Cedarwood Onsen & Forest Deck',
    viewVi: 'Ẩn mình 100% giữa rừng thông nguyên sinh',
    viewEn: 'Deep Old-Growth Pine Forest Seclusion',
    butlerVi: 'Thực đơn Organic Farm-to-Table & Thiền chuông',
    butlerEn: 'Organic Farm-to-Table & Sound Healing',
    vibes: ['forest', 'couple'],
    mapX: 42,
    mapY: 26,
    zoneVi: 'Rừng Thông Nguyên Sinh (Deep Forest)',
    zoneEn: 'Deep Pine Sanctuary',
  };
}

export function getVillaGallery(villa: Villa): string[] {
  if (villa.galleryImages && villa.galleryImages.length > 0) {
    return villa.galleryImages;
  }
  return Array.from(new Set([villa.imageUrl, ...DEFAULT_FALLBACK_GALLERY])).slice(0, 5);
}

/* -------------------------------------------------------------------------- */
/* 1. SPOTLIGHT STAGE VIEW (Sân Khấu Điện Ảnh Tương Tác)                       */
/* -------------------------------------------------------------------------- */

export function SpotlightStageView({
  villas,
  onQuickInspect,
  comparedIds,
  onToggleCompare,
}: {
  villas: Villa[];
  onQuickInspect: (villa: Villa) => void;
  comparedIds: number[];
  onToggleCompare: (villa: Villa) => void;
}) {
  const { formatCurrency } = useCurrency();
  const { lang } = useTranslation();
  const isVi = lang === 'vi';

  const [activeIndex, setActiveIndex] = useState(0);
  const [activePhotoIdx, setActivePhotoIdx] = useState(0);

  const safeIndex = activeIndex < villas.length ? activeIndex : 0;
  const activeVilla = villas[safeIndex] || villas[0];

  if (!activeVilla) return null;

  const spec = getArchitecturalSpecs(activeVilla);
  const gallery = getVillaGallery(activeVilla);
  const currentPhoto = gallery[activePhotoIdx % gallery.length] || activeVilla.imageUrl;

  return (
    <div className="grid gap-8 lg:grid-cols-[0.42fr_0.58fr] xl:gap-12">
      {/* Left Column: Architectural Numbered Index */}
      <div className="flex flex-col justify-between space-y-3">
        <div className="mb-2 flex items-center justify-between border-b border-brand-stone/60 pb-3 text-[0.68rem] font-semibold uppercase tracking-[0.22em] text-brand-ink/50">
          <span>{isVi ? 'Danh mục Tuyệt tác (Chạm để trình chiếu)' : 'Sanctuary Index (Select to project)'}</span>
          <span>0{safeIndex + 1} / 0{villas.length}</span>
        </div>

        <div className="space-y-2.5">
          {villas.map((villa, idx) => {
            const isSelected = idx === safeIndex;
            const vSpec = getArchitecturalSpecs(villa);
            const isCompared = comparedIds.includes(villa.id);

            return (
              <div
                key={villa.id}
                onMouseEnter={() => {
                  setActiveIndex(idx);
                  setActivePhotoIdx(0);
                }}
                onClick={() => {
                  setActiveIndex(idx);
                  setActivePhotoIdx(0);
                }}
                className={`group cursor-pointer rounded-[1.5rem] border p-5 transition-all duration-500 ${
                  isSelected
                    ? 'border-brand-forest bg-brand-forest-deep text-brand-white shadow-[0_24px_60px_rgba(16,36,28,0.24)] scale-[1.01]'
                    : 'border-brand-stone/60 bg-brand-paper/80 text-brand-charcoal hover:border-brand-forest/40 hover:bg-brand-white'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-baseline gap-4">
                    <span
                      className={`font-serif text-2xl italic transition-colors ${
                        isSelected ? 'text-brand-sage-light' : 'text-brand-ink/35 group-hover:text-brand-forest'
                      }`}
                    >
                      0{idx + 1}
                    </span>
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={`text-[0.62rem] font-bold uppercase tracking-[0.2em] ${
                            isSelected ? 'text-brand-sage-light' : 'text-brand-sage'
                          }`}
                        >
                          {villa.roomNumber} · {villa.roomType}
                        </span>
                        {villa.promotions && villa.promotions.length > 0 && (
                          <span className="rounded-full bg-brand-coral px-2.5 py-0.5 text-[0.6rem] font-bold uppercase tracking-wider text-white">
                            -{villa.promotions[0].discountPercent || 15}%
                          </span>
                        )}
                      </div>
                      <h3 className="mt-1 font-serif text-2xl leading-tight sm:text-3xl">
                        {villa.name}
                      </h3>
                    </div>
                  </div>

                  <div className="text-right">
                    <p className={`font-serif text-xl sm:text-2xl ${isSelected ? 'text-brand-white' : 'text-brand-charcoal'}`}>
                      {formatCurrency(villa.pricePerNight)}
                    </p>
                    <p className={`text-[0.65rem] uppercase tracking-wider ${isSelected ? 'text-brand-paper/60' : 'text-brand-ink/45'}`}>
                      / {isVi ? 'đêm' : 'night'}
                    </p>
                  </div>
                </div>

                {/* Architectural Micro-Specs Bar */}
                <div
                  className={`mt-4 flex flex-wrap items-center justify-between gap-3 border-t pt-3 text-xs ${
                    isSelected ? 'border-white/15 text-brand-paper/80' : 'border-brand-stone/50 text-brand-ink/60'
                  }`}
                >
                  <div className="flex flex-wrap items-center gap-3">
                    <span>📐 {vSpec.areaSqm}</span>
                    <span>·</span>
                    <span>🛏️ {villa.bedrooms} {isVi ? 'Phòng ngủ' : 'BR'}</span>
                    <span>·</span>
                    <span>👥 {villa.capacity} {isVi ? 'Khách' : 'Guests'}</span>
                  </div>

                  <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      onClick={() => onToggleCompare(villa)}
                      className={`rounded-full border px-2.5 py-1 text-[0.62rem] font-semibold uppercase tracking-wider transition ${
                        isCompared
                          ? 'border-amber-400 bg-amber-400/20 text-amber-300'
                          : isSelected
                          ? 'border-white/25 text-white/80 hover:bg-white/10'
                          : 'border-brand-stone text-brand-ink/60 hover:border-brand-forest hover:text-brand-forest'
                      }`}
                    >
                      {isCompared ? (isVi ? '✓ Đang so sánh' : '✓ Comparing') : (isVi ? '+ So sánh' : '+ Compare')}
                    </button>
                    <button
                      type="button"
                      onClick={() => onQuickInspect(villa)}
                      className={`rounded-full px-3 py-1 text-[0.62rem] font-semibold uppercase tracking-wider transition ${
                        isSelected
                          ? 'bg-brand-paper text-brand-forest-deep hover:bg-white'
                          : 'bg-brand-forest/10 text-brand-forest hover:bg-brand-forest hover:text-white'
                      }`}
                    >
                      {isVi ? 'Đặt nhanh' : 'Quick Book'}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right Column: Sticky Interactive Cinema Stage */}
      <div className="lg:sticky lg:top-28 h-fit">
        <div className="relative overflow-hidden rounded-[2.25rem] bg-brand-forest-deep text-brand-white shadow-[0_32px_100px_rgba(12,28,22,0.28)]">
          {/* Main Stage Image with Crossfade */}
          <div className="relative aspect-[16/11] w-full overflow-hidden">
            <AnimatePresence mode="wait">
              <motion.img
                key={`${activeVilla.id}-${currentPhoto}`}
                src={currentPhoto}
                alt={activeVilla.name}
                initial={{ opacity: 0.2, scale: 1.06 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0.2, scale: 0.98 }}
                transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
                className="h-full w-full object-cover"
              />
            </AnimatePresence>

            {/* Cinematic Gradient Vignettes */}
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#0b1914] via-[#0b1914]/25 to-black/30" />

            {/* Top Floating Architectural Badges */}
            <div className="absolute inset-x-6 top-6 flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full border border-white/25 bg-black/40 px-4 py-1.5 text-[0.68rem] font-semibold uppercase tracking-[0.2em] text-white backdrop-blur-md">
                  {spec.zoneVi && isVi ? spec.zoneVi : spec.zoneEn}
                </span>
                <span className="rounded-full border border-white/20 bg-brand-forest/80 px-3.5 py-1.5 text-[0.68rem] font-semibold uppercase tracking-[0.16em] text-brand-paper backdrop-blur-md">
                  ⛰️ {spec.elevation}
                </span>
              </div>

              <div className="flex items-center gap-1.5 rounded-full bg-black/45 px-3.5 py-1.5 text-xs font-medium text-amber-300 backdrop-blur-md">
                <span>★ {activeVilla.rating.toFixed(1)}</span>
                <span className="text-white/60">({activeVilla.reviewCount})</span>
              </div>
            </div>

            {/* Interactive Filmstrip Thumbnails inside Stage */}
            <div className="absolute bottom-5 right-6 z-20 flex items-center gap-2 rounded-full border border-white/20 bg-black/45 p-1.5 backdrop-blur-md">
              {gallery.map((img, i) => (
                <button
                  key={`${img}-${i}`}
                  type="button"
                  onClick={() => setActivePhotoIdx(i)}
                  className={`relative h-10 w-14 overflow-hidden rounded-full border-2 transition-all ${
                    i === activePhotoIdx % gallery.length
                      ? 'border-white scale-105 shadow-md'
                      : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          </div>

          {/* Stage Architectural Blueprint & Action Deck */}
          <div className="p-7 sm:p-9">
            <div className="flex flex-wrap items-end justify-between gap-4 border-b border-white/12 pb-6">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-brand-sage-light">
                  {activeVilla.roomNumber} — {activeVilla.roomType}
                </p>
                <h2 className="mt-2 font-serif text-4xl text-white sm:text-5xl">
                  {activeVilla.name}
                </h2>
              </div>
              <div className="text-left sm:text-right">
                <p className="text-xs uppercase tracking-[0.18em] text-white/50">
                  {isVi ? 'Giá niêm yết mỗi đêm' : ' Sanctuary Tariff'}
                </p>
                <p className="mt-1 font-serif text-3xl text-brand-paper sm:text-4xl">
                  {formatCurrency(activeVilla.pricePerNight)}
                </p>
              </div>
            </div>

            <p className="mt-5 text-sm leading-relaxed text-brand-paper/80 sm:text-base">
              {activeVilla.description}
            </p>

            {/* 4-Column Architectural Blueprint Spec Grid */}
            <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-3.5">
                <p className="text-[0.62rem] uppercase tracking-[0.18em] text-brand-sage-light">
                  {isVi ? 'Diện tích' : 'Total Footprint'}
                </p>
                <p className="mt-1 font-serif text-xl text-white">{spec.areaSqm}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-3.5">
                <p className="text-[0.62rem] uppercase tracking-[0.18em] text-brand-sage-light">
                  {isVi ? 'Hồ bơi & Thủy trị' : 'Water Feature'}
                </p>
                <p className="mt-1 text-xs font-medium leading-snug text-white">
                  {isVi ? spec.poolSpecVi : spec.poolSpecEn}
                </p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-3.5">
                <p className="text-[0.62rem] uppercase tracking-[0.18em] text-brand-sage-light">
                  {isVi ? 'Tầm nhìn' : 'Orientation'}
                </p>
                <p className="mt-1 text-xs font-medium leading-snug text-white">
                  {isVi ? spec.viewVi : spec.viewEn}
                </p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-3.5">
                <p className="text-[0.62rem] uppercase tracking-[0.18em] text-brand-sage-light">
                  {isVi ? 'Đặc quyền Quản gia' : 'Private Butler'}
                </p>
                <p className="mt-1 text-xs font-medium leading-snug text-white">
                  {isVi ? spec.butlerVi : spec.butlerEn}
                </p>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => onQuickInspect(activeVilla)}
                className="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-full bg-brand-paper px-6 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest-deep transition hover:bg-white"
              >
                <span>✨ {isVi ? 'Chọn Ngày & Đặt Nhanh' : 'Instant Reserve & Dates'}</span>
              </button>
              <Link
                href={`/villas/${activeVilla.slug || activeVilla.id}`}
                className="inline-flex min-h-12 items-center justify-center rounded-full border border-white/30 px-6 text-xs font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-white/10"
              >
                {isVi ? 'Khám phá Chi tiết →' : 'Full Sanctuary Tour →'}
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* -------------------------------------------------------------------------- */
/* 2. ESTATE BLUEPRINT VIEW (Bản Đồ Phối Cảnh Đồi Thông & Hồ Tuyền Lâm)       */
/* -------------------------------------------------------------------------- */

export function EstateBlueprintView({
  villas,
  onQuickInspect,
}: {
  villas: Villa[];
  onQuickInspect: (villa: Villa) => void;
}) {
  const { formatCurrency } = useCurrency();
  const { lang } = useTranslation();
  const isVi = lang === 'vi';
  const [selectedVilla, setSelectedVilla] = useState<Villa>(villas[0]);

  const active = villas.find((v) => v.id === selectedVilla?.id) || villas[0];
  if (!active) return null;

  const spec = getArchitecturalSpecs(active);
  const gallery = getVillaGallery(active);

  return (
    <div className="grid gap-8 lg:grid-cols-[1.15fr_0.85fr]">
      {/* Interactive Topographic Masterplan Canvas */}
      <div className="relative min-h-[540px] overflow-hidden rounded-[2.25rem] border border-brand-forest/20 bg-[#10231c] p-6 text-white shadow-[0_28px_90px_rgba(12,28,22,0.25)] sm:p-8">
        {/* Background Atmospheric Image + Topographic SVG Contour Lines */}
        <img
          src="/images/nhu-location-landscape.jpg"
          alt="Nhu Villas Estate Masterplan"
          className="pointer-events-none absolute inset-0 h-full w-full object-cover opacity-25 mix-blend-luminosity"
        />
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_30%_75%,rgba(28,88,78,0.45)_0%,rgba(12,28,22,0.92)_75%)]" />

        {/* Architectural Topographic SVG Contours */}
        <svg
          viewBox="0 0 1000 700"
          className="pointer-events-none absolute inset-0 h-full w-full opacity-30"
          fill="none"
        >
          <path
            d="M-50 620 C 180 540, 340 670, 560 580 C 760 490, 880 550, 1050 470"
            stroke="#8fb3a2"
            strokeWidth="2"
            strokeDasharray="6 6"
          />
          <path
            d="M-50 500 C 200 420, 410 520, 640 410 C 810 330, 920 380, 1050 290"
            stroke="#8fb3a2"
            strokeWidth="1.5"
          />
          <path
            d="M120 260 C 340 190, 550 290, 790 170 C 900 110, 980 140, 1050 90"
            stroke="#8fb3a2"
            strokeWidth="1.2"
          />
          <ellipse cx="220" cy="640" rx="340" ry="120" fill="rgba(56, 152, 136, 0.18)" />
        </svg>

        {/* Masterplan Header Legend */}
        <div className="relative z-10 flex flex-wrap items-center justify-between gap-4">
          <div>
            <span className="inline-block rounded-full border border-white/20 bg-white/10 px-3.5 py-1 text-[0.65rem] font-semibold uppercase tracking-[0.22em] text-brand-sage-light backdrop-blur-md">
              {isVi ? 'Bản Đồ Địa Hình Khu Nghỉ Dưỡng' : 'Interactive Topographic Masterplan'}
            </span>
            <h3 className="mt-2 font-serif text-2xl text-white sm:text-3xl">
              {isVi ? 'Chọn Biệt Thự Theo Vị Trí Đồi Thông & Mặt Hồ' : 'Select Your Sanctuary by Elevation & Lakefront'}
            </h3>
          </div>
          <div className="flex items-center gap-4 text-xs text-white/75">
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
              {isVi ? 'Đang trống' : 'Available'}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-full bg-amber-300" />
              {isVi ? 'Đang chọn' : 'Selected'}
            </span>
          </div>
        </div>

        {/* Landmark Labels on Map */}
        <div className="pointer-events-none absolute bottom-8 left-8 z-10 rounded-2xl border border-cyan-300/25 bg-cyan-950/50 px-4 py-2 text-xs text-cyan-200 backdrop-blur-md">
          🌊 {isVi ? 'Mặt Hồ Tuyền Lâm & Bến Du Thuyền Riêng (1,490m)' : 'Tuyen Lam Lakefront & Private Pier (1,490m)'}
        </div>
        <div className="pointer-events-none absolute right-8 top-24 z-10 hidden rounded-2xl border border-emerald-300/25 bg-emerald-950/50 px-4 py-2 text-xs text-emerald-200 backdrop-blur-md sm:block">
          🌲 {isVi ? 'Đỉnh Đồi Thông Cổ Thụ Panorama (1,585m)' : 'Ancient Pine Summit & Panorama Ridge (1,585m)'}
        </div>

        {/* Interactive Villa Pins */}
        <div className="relative z-20 mt-8 h-[390px] w-full">
          {villas.map((villa) => {
            const vSpec = getArchitecturalSpecs(villa);
            const isCurrent = villa.id === active.id;

            return (
              <button
                key={villa.id}
                type="button"
                onClick={() => setSelectedVilla(villa)}
                style={{ left: `${vSpec.mapX}%`, top: `${vSpec.mapY}%` }}
                className={`group absolute -translate-x-1/2 -translate-y-1/2 transition-all duration-500 ${
                  isCurrent ? 'z-30 scale-110' : 'z-20 hover:z-30 hover:scale-105'
                }`}
              >
                <div
                  className={`flex items-center gap-2 rounded-full border px-3.5 py-2 shadow-2xl backdrop-blur-md transition-all ${
                    isCurrent
                      ? 'border-amber-300 bg-amber-300 text-brand-forest-deep ring-4 ring-amber-300/30'
                      : 'border-white/30 bg-brand-forest-deep/90 text-white hover:border-white'
                  }`}
                >
                  <span
                    className={`h-2.5 w-2.5 rounded-full ${
                      isCurrent ? 'bg-brand-forest-deep animate-ping' : 'bg-emerald-400'
                    }`}
                  />
                  <span className="text-xs font-bold tracking-wider">{villa.roomNumber}</span>
                  <span className="text-xs font-serif opacity-90">{formatCurrency(villa.pricePerNight)}</span>
                </div>
                <div
                  className={`mx-auto mt-1 w-fit rounded-full px-2.5 py-0.5 text-[0.62rem] font-medium backdrop-blur-md transition-opacity ${
                    isCurrent
                      ? 'bg-black/75 text-amber-200 opacity-100'
                      : 'bg-black/50 text-white/80 opacity-0 group-hover:opacity-100'
                  }`}
                >
                  {villa.name}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Selected Villa Architectural Inspector Panel */}
      <div className="flex flex-col justify-between rounded-[2.25rem] bg-brand-paper p-6 shadow-[0_24px_80px_rgba(32,52,43,0.1)] sm:p-8">
        <div>
          <div className="relative aspect-[16/10] overflow-hidden rounded-[1.5rem]">
            <img src={active.imageUrl} alt={active.name} className="h-full w-full object-cover" />
            <div className="absolute left-4 top-4 rounded-full bg-brand-forest-deep/90 px-3.5 py-1.5 text-[0.65rem] font-semibold uppercase tracking-[0.18em] text-white backdrop-blur-md">
              📍 {isVi ? spec.zoneVi : spec.zoneEn}
            </div>
            <div className="absolute bottom-4 right-4 rounded-full bg-white/95 px-3.5 py-1 text-xs font-bold text-brand-forest shadow">
              {spec.areaSqm} · {spec.elevation}
            </div>
          </div>

          <div className="mt-6 flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-sage">
                {active.roomNumber} · {active.roomType}
              </p>
              <h3 className="mt-1 font-serif text-3xl text-brand-charcoal">{active.name}</h3>
            </div>
            <div className="text-right">
              <p className="font-serif text-2xl text-brand-forest">{formatCurrency(active.pricePerNight)}</p>
              <p className="text-xs text-brand-ink/50">/ {isVi ? 'đêm' : 'night'}</p>
            </div>
          </div>

          <p className="mt-4 text-sm leading-relaxed text-brand-ink/70">{active.description}</p>

          <dl className="mt-6 grid grid-cols-2 gap-3 text-xs">
            <div className="rounded-xl bg-brand-white p-3.5">
              <dt className="font-semibold uppercase tracking-wider text-brand-sage">
                {isVi ? 'Hồ bơi & Thủy trị' : 'Pool & Water'}
              </dt>
              <dd className="mt-1 font-medium text-brand-charcoal">
                {isVi ? spec.poolSpecVi : spec.poolSpecEn}
              </dd>
            </div>
            <div className="rounded-xl bg-brand-white p-3.5">
              <dt className="font-semibold uppercase tracking-wider text-brand-sage">
                {isVi ? 'Hướng nhìn' : 'Vista'}
              </dt>
              <dd className="mt-1 font-medium text-brand-charcoal">
                {isVi ? spec.viewVi : spec.viewEn}
              </dd>
            </div>
          </dl>

          {/* Mini Gallery Strip */}
          <div className="mt-5 grid grid-cols-4 gap-2">
            {gallery.slice(1, 5).map((img, i) => (
              <img
                key={i}
                src={img}
                alt=""
                className="aspect-[4/3] w-full rounded-xl object-cover"
              />
            ))}
          </div>
        </div>

        <div className="mt-7 flex gap-3">
          <button
            type="button"
            onClick={() => onQuickInspect(active)}
            className="inline-flex min-h-12 flex-1 items-center justify-center rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
          >
            {isVi ? 'Chọn Ngày & Đặt Ngay' : 'Instant Reserve'}
          </button>
          <Link
            href={`/villas/${active.slug || active.id}`}
            className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-white"
          >
            {isVi ? 'Chi tiết' : 'Details'}
          </Link>
        </div>
      </div>
    </div>
  );
}

/* -------------------------------------------------------------------------- */
/* 3. QUICK SANCTUARY INSPECTOR & INSTANT BOOKING DRAWER                      */
/* -------------------------------------------------------------------------- */

export function QuickInspectDrawer({
  villa,
  onClose,
}: {
  villa: Villa | null;
  onClose: () => void;
}) {
  const router = useRouter();
  const { formatCurrency } = useCurrency();
  const { lang } = useTranslation();
  const isVi = lang === 'vi';

  const [checkIn, setCheckIn] = useState(getTodayDate());
  const [checkOut, setCheckOut] = useState(getTomorrowDate());
  const [guests, setGuests] = useState(2);
  const [photoIdx, setPhotoIdx] = useState(0);

  if (!villa) return null;

  const spec = getArchitecturalSpecs(villa);
  const gallery = getVillaGallery(villa);
  const nights = Math.max(1, calculateNights(checkIn, checkOut));
  const subtotal = nights * villa.pricePerNight;
  const serviceFee = Math.round(subtotal * 0.08);
  const taxAmount = Math.round(subtotal * 0.1);
  const total = subtotal + serviceFee + taxAmount;

  const handleInstantCheckout = () => {
    const draft: BookingDraft = {
      villaId: villa.id,
      villaName: villa.name,
      checkIn,
      checkOut,
      guests: Math.min(guests, villa.capacity),
      nights,
      nightlyRate: villa.pricePerNight,
      subtotal,
      discount: 0,
      serviceFee,
      taxAmount,
      total,
    };
    window.sessionStorage.setItem('nhu.bookingDraft', JSON.stringify(draft));
    router.push('/checkout');
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex justify-end bg-black/65 backdrop-blur-sm" onClick={onClose}>
        <motion.div
          initial={{ x: '100%' }}
          animate={{ x: 0 }}
          exit={{ x: '100%' }}
          transition={{ type: 'spring', damping: 28, stiffness: 240 }}
          onClick={(e) => e.stopPropagation()}
          className="flex h-full w-full max-w-2xl flex-col overflow-y-auto bg-brand-paper p-6 text-brand-ink shadow-2xl sm:p-9"
        >
          {/* Top Bar */}
          <div className="flex items-center justify-between border-b border-brand-stone pb-4">
            <div>
              <p className="text-[0.65rem] font-bold uppercase tracking-[0.22em] text-brand-sage">
                {villa.roomNumber} · {isVi ? spec.zoneVi : spec.zoneEn}
              </p>
              <h2 className="mt-1 font-serif text-3xl text-brand-charcoal">{villa.name}</h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="flex h-10 w-10 items-center justify-center rounded-full border border-brand-stone bg-brand-white text-lg text-brand-charcoal hover:bg-brand-forest hover:text-white"
            >
              ✕
            </button>
          </div>

          {/* Interactive Gallery Carousel */}
          <div className="mt-6">
            <div className="relative aspect-[16/10] overflow-hidden rounded-[1.5rem]">
              <img
                src={gallery[photoIdx % gallery.length]}
                alt={villa.name}
                className="h-full w-full object-cover"
              />
              <div className="absolute bottom-3 right-3 rounded-full bg-black/60 px-3 py-1 text-xs text-white backdrop-blur-md">
                {(photoIdx % gallery.length) + 1} / {gallery.length}
              </div>
            </div>
            <div className="mt-3 grid grid-cols-5 gap-2">
              {gallery.map((img, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setPhotoIdx(idx)}
                  className={`aspect-[4/3] overflow-hidden rounded-xl border-2 transition ${
                    idx === photoIdx % gallery.length ? 'border-brand-forest scale-105' : 'border-transparent opacity-60'
                  }`}
                >
                  <img src={img} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          </div>

          {/* Architectural Specs Summary */}
          <div className="mt-6 grid grid-cols-3 gap-3 rounded-2xl bg-brand-white p-4 text-center text-xs shadow-sm">
            <div>
              <p className="text-brand-ink/50">{isVi ? 'Diện tích' : 'Area'}</p>
              <p className="mt-1 font-serif text-lg font-semibold text-brand-charcoal">{spec.areaSqm}</p>
            </div>
            <div className="border-x border-brand-stone/50">
              <p className="text-brand-ink/50">{isVi ? 'Sức chứa' : 'Capacity'}</p>
              <p className="mt-1 font-serif text-lg font-semibold text-brand-charcoal">
                {villa.capacity} {isVi ? 'Khách' : 'Guests'} · {villa.bedrooms} {isVi ? 'PN' : 'BR'}
              </p>
            </div>
            <div>
              <p className="text-brand-ink/50">{isVi ? 'Độ cao' : 'Elevation'}</p>
              <p className="mt-1 font-serif text-lg font-semibold text-brand-charcoal">{spec.elevation}</p>
            </div>
          </div>

          {/* Instant Booking Configurator */}
          <div className="mt-6 rounded-[1.5rem] border border-brand-forest/20 bg-brand-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h3 className="font-serif text-2xl text-brand-charcoal">
                {isVi ? 'Đặt Phòng Nhanh (1-Click Reserve)' : 'Instant Sanctuary Reservation'}
              </h3>
              <span className="font-serif text-2xl text-brand-forest">
                {formatCurrency(villa.pricePerNight)}
                <span className="text-xs font-sans text-brand-ink/50">/{isVi ? 'đêm' : 'nt'}</span>
              </span>
            </div>

            <div className="mt-4 grid gap-4 sm:grid-cols-3">
              <label className="block text-xs">
                <span className="font-semibold text-brand-charcoal">{isVi ? 'Nhận phòng' : 'Check-in'}</span>
                <input
                  type="date"
                  value={checkIn}
                  min={getTodayDate()}
                  onChange={(e) => setCheckIn(e.target.value)}
                  className="mt-1.5 h-11 w-full rounded-full border border-brand-stone bg-brand-paper px-3.5 text-xs outline-none focus:border-brand-forest"
                />
              </label>
              <label className="block text-xs">
                <span className="font-semibold text-brand-charcoal">{isVi ? 'Trả phòng' : 'Check-out'}</span>
                <input
                  type="date"
                  value={checkOut}
                  min={checkIn || getTomorrowDate()}
                  onChange={(e) => setCheckOut(e.target.value)}
                  className="mt-1.5 h-11 w-full rounded-full border border-brand-stone bg-brand-paper px-3.5 text-xs outline-none focus:border-brand-forest"
                />
              </label>
              <label className="block text-xs">
                <span className="font-semibold text-brand-charcoal">{isVi ? 'Số khách' : 'Guests'}</span>
                <select
                  value={guests}
                  onChange={(e) => setGuests(Number(e.target.value))}
                  className="mt-1.5 h-11 w-full rounded-full border border-brand-stone bg-brand-paper px-3.5 text-xs outline-none focus:border-brand-forest"
                >
                  {Array.from({ length: villa.capacity }, (_, i) => i + 1).map((n) => (
                    <option key={n} value={n}>
                      {n} {isVi ? 'Khách' : 'Guests'}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {/* Live Pricing Breakdown */}
            <div className="mt-5 space-y-2 border-t border-brand-stone/60 pt-4 text-xs text-brand-ink/75">
              <div className="flex justify-between">
                <span>
                  {formatCurrency(villa.pricePerNight)} × {nights} {isVi ? 'đêm' : 'nights'}
                </span>
                <span>{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span>{isVi ? 'Phí dịch vụ Quản gia (8%) & Thuế (10%)' : 'Butler Service (8%) & Tax (10%)'}</span>
                <span>{formatCurrency(serviceFee + taxAmount)}</span>
              </div>
              <div className="flex justify-between border-t border-brand-stone/60 pt-2.5 text-sm font-bold text-brand-charcoal">
                <span>{isVi ? 'Tổng cộng tạm tính' : 'Estimated Total'}</span>
                <span className="text-lg text-brand-forest">{formatCurrency(total)}</span>
              </div>
            </div>

            <div className="mt-6 flex flex-col gap-2.5 sm:flex-row">
              <button
                type="button"
                onClick={handleInstantCheckout}
                className="inline-flex min-h-12 flex-1 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
              >
                ⚡ {isVi ? 'Tiến Hành Thanh Toán Ngay' : 'Proceed to Instant Checkout'}
              </button>
              <Link
                href={`/booking/${villa.id}`}
                className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-stone px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-charcoal hover:border-brand-forest"
              >
                {isVi ? 'Áp mã Voucher' : 'Apply Voucher'}
              </Link>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

/* -------------------------------------------------------------------------- */
/* 4. ARCHITECTURAL COMPARISON MODAL (Đối Chiếu Biệt Thự)                      */
/* -------------------------------------------------------------------------- */

export function VillaCompareModal({
  villas,
  onClose,
  onQuickInspect,
}: {
  villas: Villa[];
  onClose: () => void;
  onQuickInspect: (villa: Villa) => void;
}) {
  const { formatCurrency } = useCurrency();
  const { lang } = useTranslation();
  const isVi = lang === 'vi';

  if (villas.length === 0) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm" onClick={onClose}>
      <div
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-[2rem] bg-brand-paper p-6 shadow-2xl sm:p-8"
      >
        <div className="flex items-center justify-between border-b border-brand-stone pb-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
              {isVi ? 'Bảng Đối Chiếu Kiến Trúc & Đặc Quyền' : 'Sanctuary Side-by-Side Matrix'}
            </p>
            <h2 className="mt-1 font-serif text-3xl text-brand-charcoal">
              {isVi ? `So sánh ${villas.length} Biệt thự đã chọn` : `Comparing ${villas.length} Sanctuaries`}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-full border border-brand-stone bg-brand-white text-brand-charcoal hover:bg-brand-forest hover:text-white"
          >
            ✕
          </button>
        </div>

        <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {villas.map((v) => {
            const spec = getArchitecturalSpecs(v);
            return (
              <div key={v.id} className="flex flex-col justify-between rounded-[1.5rem] bg-brand-white p-5 shadow-sm">
                <div>
                  <img src={v.imageUrl} alt={v.name} className="aspect-[16/10] w-full rounded-xl object-cover" />
                  <p className="mt-4 text-[0.65rem] font-bold uppercase tracking-[0.2em] text-brand-sage">
                    {v.roomNumber} · {v.roomType}
                  </p>
                  <h3 className="mt-1 font-serif text-2xl text-brand-charcoal">{v.name}</h3>
                  <p className="mt-2 font-serif text-2xl text-brand-forest">
                    {formatCurrency(v.pricePerNight)} <span className="text-xs font-sans text-brand-ink/50">/{isVi ? 'đêm' : 'nt'}</span>
                  </p>

                  <dl className="mt-5 space-y-3 border-t border-brand-stone/60 pt-4 text-xs">
                    <div>
                      <dt className="text-brand-ink/50">{isVi ? 'Diện tích & Độ cao' : 'Footprint & Elevation'}</dt>
                      <dd className="font-semibold text-brand-charcoal">{spec.areaSqm} · {spec.elevation}</dd>
                    </div>
                    <div>
                      <dt className="text-brand-ink/50">{isVi ? 'Sức chứa & Phòng ngủ' : 'Capacity & Bedrooms'}</dt>
                      <dd className="font-semibold text-brand-charcoal">
                        {v.capacity} {isVi ? 'Khách' : 'Guests'} · {v.bedrooms} {isVi ? 'Phòng ngủ' : 'Bedrooms'}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-brand-ink/50">{isVi ? 'Hồ bơi riêng' : 'Private Pool'}</dt>
                      <dd className="font-semibold text-brand-charcoal">{isVi ? spec.poolSpecVi : spec.poolSpecEn}</dd>
                    </div>
                    <div>
                      <dt className="text-brand-ink/50">{isVi ? 'Tầm nhìn' : 'Vista'}</dt>
                      <dd className="font-semibold text-brand-charcoal">{isVi ? spec.viewVi : spec.viewEn}</dd>
                    </div>
                    <div>
                      <dt className="text-brand-ink/50">{isVi ? 'Đặc quyền Quản gia' : 'Butler Privileges'}</dt>
                      <dd className="font-semibold text-brand-charcoal">{isVi ? spec.butlerVi : spec.butlerEn}</dd>
                    </div>
                  </dl>
                </div>

                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onQuickInspect(v);
                  }}
                  className="mt-6 inline-flex min-h-11 w-full items-center justify-center rounded-full bg-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-white hover:bg-brand-forest-deep"
                >
                  {isVi ? 'Chọn Căn Này' : 'Select This Sanctuary'}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
