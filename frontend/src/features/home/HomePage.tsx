'use client';

import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { motion, useScroll, useTransform } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { Hero } from './components/Hero';
import { PromotionMarquee } from './components/PromotionMarquee';
import { Reveal } from '../../shared/components/Reveal';
import { api, API_PATHS } from '../../shared/api/client';
import { fallbackVillas } from '../villas/villaFallbacks';
import type { Villa } from '../villas/types';
import { getArchitecturalSpecs } from '../villas/components/SanctuaryVisualizer';
import { useTranslation, useCurrency } from '../../shared/i18n/hooks';
import { MagneticButton } from '../../shared/components/MagneticButton';
import { ParallaxImage } from '../../shared/components/ParallaxImage';

const villaExteriorImage = '/images/nhu-garden-pool-villa.jpg';
const villaInteriorImage = '/images/nhu-villa-interior.jpg';
const diningImage = '/images/nhu-private-dining.jpg';
const poolImage = '/images/nhu-infinity-pool.jpg';
const locationImage = '/images/nhu-location-landscape.jpg';

const amenities = [
  'Private pool',
  'Open-air living',
  'Breakfast service',
  'High-speed Wi-Fi',
  'Airport transfer',
  'Housekeeping',
  'Garden terrace',
  'Concierge planning',
];

const gallery = [
  { src: villaInteriorImage, alt: 'Villa bedroom with floor to ceiling windows and soft linen' },
  { src: poolImage, alt: 'Private infinity pool looking toward a calm landscape' },
  { src: villaExteriorImage, alt: 'Minimal villa bedroom with natural wood and warm light' },
  { src: diningImage, alt: 'Quiet bedroom detail with soft bedding and natural textures' },
];

// Star Rating Component
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

// Loading Skeleton for Villa Cards
function VillaCardSkeleton({ delay = 0 }: { delay?: number }) {
  return (
    <div
      className="rounded-[2rem] bg-brand-paper/70 p-2 shadow-[0_24px_80px_rgba(32,52,43,0.1)]"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className="overflow-hidden rounded-t-[1.55rem]">
        <div className="aspect-[4/5] animate-pulse bg-brand-stone" />
      </div>
      <div className="p-6 sm:p-7">
        <div className="h-8 w-2/3 animate-pulse rounded-full bg-brand-stone" />
        <div className="mt-5 h-20 animate-pulse rounded-full bg-brand-stone" />
        <div className="mt-7 flex items-center justify-between border-t border-brand-stone/80 pt-5">
          <div className="h-5 w-20 animate-pulse rounded-full bg-brand-stone" />
          <div className="h-5 w-24 animate-pulse rounded-full bg-brand-stone" />
        </div>
      </div>
    </div>
  );
}

// Dynamic Villa Card Component
function VillaCard({ 
  villa, 
  delay = 0, 
  featured = false,
  x = 0,
  y = 36,
  scale = 1,
  duration = 1.1
}: { 
  villa: Villa; 
  delay?: number; 
  featured?: boolean;
  x?: number;
  y?: number;
  scale?: number;
  duration?: number;
}) {
  const { formatCurrency } = useCurrency();
  const { t } = useTranslation();
  return (
    <Reveal delay={delay} y={y} x={x} scale={scale} duration={duration} className="h-full">
      <Link
        href={`/villas/${villa.id}`}
        className={`group flex h-full flex-col rounded-[2rem] bg-brand-paper/70 p-2 shadow-[0_24px_80px_rgba(32,52,43,0.1)] transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-2 hover:shadow-[0_34px_100px_rgba(32,52,43,0.16)] ${
          featured ? 'lg:mt-16' : ''
        }`}
      >
        <div className="relative shrink-0 overflow-hidden rounded-t-[1.55rem]">
          <img
            src={villa.imageUrl}
            alt={villa.description || villa.name}
            className="aspect-[4/5] w-full object-cover transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.035]"
            loading="lazy"
            decoding="async"
          />
          {villa.promotions && villa.promotions.map((promo) => (
            <div key={promo.id} className="absolute left-4 top-4 rounded-full bg-brand-forest px-3 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-brand-white shadow-sm">
              🏷️ {promo.title}
            </div>
          ))}
        </div>
        <div className="flex flex-1 flex-col p-6 sm:p-7">
          <div className="flex items-start justify-between gap-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">
                {t(`villas.data.${villa.id}.roomType` as any) === `villas.data.${villa.id}.roomType` ? villa.roomType : t(`villas.data.${villa.id}.roomType` as any)}
              </p>
              <h3 className="mt-2 font-serif text-3xl leading-tight text-brand-charcoal">
                {t(`villas.data.${villa.id}.name` as any) === `villas.data.${villa.id}.name` ? villa.name : t(`villas.data.${villa.id}.name` as any)}
              </h3>
            </div>
            <span className="shrink-0 rounded-full border border-brand-stone px-3 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-brand-forest">
              {villa.capacity} {t('villas.guests')}
            </span>
          </div>
          <p className="mt-5 min-h-20 text-sm leading-7 text-brand-ink/64">
            {t(`villas.data.${villa.id}.description` as any) === `villas.data.${villa.id}.description` ? villa.description : t(`villas.data.${villa.id}.description` as any)}
          </p>
          <div className="mt-auto flex items-center justify-between border-t border-brand-stone/80 pt-5">
            <div className="flex items-center gap-2">
              <StarRating rating={Math.round(villa.rating)} size="sm" />
              <span className="text-xs text-brand-ink/50">({villa.reviewCount})</span>
            </div>
            <span className="font-semibold text-brand-charcoal">
              {t('villas.fromPrice').replace('{price}', formatCurrency(villa.pricePerNight))}
            </span>
          </div>
        </div>
      </Link>
    </Reveal>
  );
}

function StorySection() {
  const { t } = useTranslation();
  return (
    <section id="story" className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12 lg:py-36">
      <div className="mx-auto grid max-w-[1440px] gap-16 lg:grid-cols-[0.9fr_1.1fr] lg:items-center lg:gap-24 xl:gap-32">
        <Reveal className="max-w-xl">
          <p className="mb-6 text-[10px] font-semibold uppercase tracking-[0.2em] text-brand-ink/40">
            {t('home.storyEyebrow')}
          </p>
          <h2 className="mb-12 font-serif text-4xl leading-[1.1] text-brand-charcoal sm:text-5xl lg:text-[4rem] xl:text-[4.5rem]">
            {t('home.storyTitle')}
          </h2>
          <div className="flex flex-col gap-6 border-l border-brand-stone/60 pl-6 lg:pl-8">
            <p className="text-base leading-[1.8] text-brand-ink/70">
              {t('home.storyP1')}
            </p>
            <p className="text-base leading-[1.8] text-brand-ink/60">
              {t('home.storyP2')}
            </p>
          </div>
        </Reveal>
        
        <Reveal delay={0.2} className="relative aspect-[4/5] w-full overflow-hidden rounded-bl-[8rem] rounded-tr-[8rem] rounded-br-[2rem] rounded-tl-[2rem] lg:aspect-square">
          <img
            src={villaExteriorImage}
            alt="Quiet resort architecture surrounded by natural garden light"
            className="h-full w-full object-cover transition-transform duration-1000 hover:scale-[1.03]"
            loading="lazy"
            decoding="async"
          />
        </Reveal>
      </div>
    </section>
  );
}

function VillaCollectionSection() {
  const { data: villas, isLoading } = useQuery({
    queryKey: ['villas-home'],
    queryFn: async () => {
      try {
        const { fetchVillas } = await import('../villas/villasApi');
        const res = await fetchVillas();
        return res.slice(0, 6);
      } catch {
        return fallbackVillas.slice(0, 6);
      }
    },
    staleTime: 60_000,
  });

  const displayVillas = villas && villas.length > 0 ? villas.slice(0, 5) : fallbackVillas.slice(0, 5);
  const [expandedIdx, setExpandedIdx] = useState(0);
  const { t, lang } = useTranslation();
  const { formatCurrency } = useCurrency();
  const isVi = lang === 'vi';

  return (
    <section id="villas" className="bg-brand-sand py-24 lg:py-36 overflow-hidden">
      <div className="mx-auto max-w-[1440px] px-5 sm:px-8 lg:px-12">
        <Reveal className="mb-14 flex flex-col justify-between gap-6 lg:flex-row lg:items-end">
          <div className="max-w-3xl">
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
              {t('home.featured')}
            </p>
            <h2 className="font-serif text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
              {t('home.featuredSubtitle')}
            </h2>
          </div>
          <p className="max-w-md text-base leading-8 text-brand-ink/66">
            {t('home.featuredDesc')}
          </p>
        </Reveal>
      </div>

      {/* Desktop Avant-Garde Interactive Expanding Accordion Stage */}
      <div className="mx-auto hidden max-w-[1480px] px-5 lg:block sm:px-8 lg:px-12">
        {isLoading ? (
          <div className="grid gap-7 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <VillaCardSkeleton key={i} delay={i * 100} />
            ))}
          </div>
        ) : (
          <div className="flex h-[580px] w-full gap-4">
            {displayVillas.map((villa, idx) => {
              const isExpanded = idx === expandedIdx;
              const spec = getArchitecturalSpecs(villa);
              return (
                <div
                  key={villa.id}
                  onMouseEnter={() => setExpandedIdx(idx)}
                  onClick={() => setExpandedIdx(idx)}
                  className={`group relative h-full cursor-pointer overflow-hidden rounded-[2.2rem] transition-all duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                    isExpanded ? 'flex-[3.4] shadow-[0_32px_90px_rgba(12,28,22,0.28)]' : 'flex-[0.85] opacity-85 hover:opacity-100'
                  }`}
                >
                  <img
                    src={villa.imageUrl}
                    alt={villa.name}
                    className={`h-full w-full object-cover transition-transform duration-1000 ${
                      isExpanded ? 'scale-100' : 'scale-110'
                    }`}
                  />
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[#0b1914]/95 via-[#0b1914]/30 to-black/25" />

                  {/* Top Index Pill */}
                  <div className="absolute left-6 right-6 top-6 flex items-center justify-between text-white">
                    <span className="flex h-10 w-10 items-center justify-center rounded-full border border-white/30 bg-black/40 font-serif text-base italic backdrop-blur-md">
                      0{idx + 1}
                    </span>
                    {isExpanded && (
                      <span className="rounded-full border border-white/20 bg-black/45 px-4 py-1.5 text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-brand-sage-light backdrop-blur-md">
                        {isVi ? spec.zoneVi : spec.zoneEn} · {spec.areaSqm}
                      </span>
                    )}
                  </div>

                  {/* Collapsed Vertical Title */}
                  {!isExpanded && (
                    <div className="absolute inset-x-0 bottom-10 flex flex-col items-center gap-3 text-white">
                      <span className="text-[0.65rem] font-bold uppercase tracking-[0.22em] text-brand-sage-light">
                        {villa.roomNumber}
                      </span>
                      <p className="writing-mode-vertical font-serif text-xl tracking-wide text-white/90">
                        {villa.name}
                      </p>
                    </div>
                  )}

                  {/* Expanded Full Architectural Showcase */}
                  {isExpanded && (
                    <div className="absolute inset-x-8 bottom-8 flex flex-col justify-end text-white">
                      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-brand-sage-light">
                        {villa.roomNumber} · {villa.roomType} · {villa.capacity} {t('villas.guests')}
                      </p>
                      <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
                        <h3 className="font-serif text-4xl leading-tight text-white xl:text-5xl">
                          {villa.name}
                        </h3>
                        <p className="font-serif text-3xl text-brand-paper">
                          {formatCurrency(villa.pricePerNight)}
                          <span className="ml-1 font-sans text-xs text-white/60">/{isVi ? 'đêm' : 'night'}</span>
                        </p>
                      </div>
                      <p className="mt-3 max-w-xl line-clamp-2 text-sm leading-relaxed text-brand-paper/80">
                        {villa.description}
                      </p>
                      <div className="mt-5 flex flex-wrap items-center justify-between gap-4 border-t border-white/15 pt-4">
                        <div className="flex flex-wrap gap-2 text-xs text-brand-paper/90">
                          <span className="rounded-full bg-white/10 px-3 py-1 backdrop-blur-md">
                            🌊 {isVi ? spec.poolSpecVi : spec.poolSpecEn}
                          </span>
                          <span className="rounded-full bg-white/10 px-3 py-1 backdrop-blur-md">
                            ⛰️ {spec.elevation}
                          </span>
                        </div>
                        <div className="flex items-center gap-2.5">
                          <Link
                            href={`/booking/${villa.id}`}
                            className="rounded-full bg-brand-paper px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.15em] text-brand-forest-deep transition hover:bg-white"
                          >
                            {isVi ? 'Đặt Phòng Ngay' : 'Reserve Now'}
                          </Link>
                          <Link
                            href={`/villas/${villa.slug || villa.id}`}
                            className="rounded-full border border-white/35 bg-white/10 px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.15em] text-white backdrop-blur-md transition hover:bg-white/20"
                          >
                            {isVi ? 'Chi tiết →' : 'Explore →'}
                          </Link>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Mobile & Tablet Grid */}
      <div className="mx-auto max-w-[1440px] px-5 lg:hidden sm:px-8">
        <div className="grid gap-7 sm:grid-cols-2">
          {displayVillas.slice(0, 4).map((villa, index) => (
            <VillaCard key={villa.id} villa={villa} delay={index * 0.1} />
          ))}
        </div>
      </div>

      <div className="mx-auto max-w-[1440px] px-5 sm:px-8 lg:px-12">
        <Reveal delay={0.2} className="mt-12 text-center">
          <MagneticButton>
            <Link
              href="/villas"
              className="inline-flex min-h-12 w-fit items-center justify-center rounded-full border border-brand-forest bg-brand-forest px-8 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-forest-deep focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
            >
              {t('home.viewAll')}
            </Link>
          </MagneticButton>
        </Reveal>
      </div>
    </section>
  );
}

const experiences = [
  {
    title: 'Morning rituals',
    copy: 'Tea, water, and a quiet breakfast terrace before the day begins.',
    image: villaInteriorImage,
    alt: 'Calm spa room prepared for a morning wellness ritual',
  },
  {
    title: 'Private dining',
    copy: 'Seasonal menus served where the evening light is at its softest.',
    image: diningImage,
    alt: 'Refined private dining table with warm hospitality lighting',
  },
  {
    title: 'Garden evenings',
    copy: 'Poolside stillness, soft towels, and enough space to disappear for a while.',
    image: poolImage,
    alt: 'Luxury resort pool framed by tropical garden at golden hour',
  },
];

function ExperienceSection() {
  const { t } = useTranslation();
  return (
    <section id="experiences" className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12 lg:py-36">
      <div className="mx-auto max-w-[1440px]">
        <div className="grid gap-10 lg:grid-cols-[0.8fr_1.2fr] lg:gap-20">
          <Reveal className="lg:sticky lg:top-32 lg:h-fit">
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
              {t('home.expEyebrow')}
            </p>
            <h2 className="max-w-[11ch] font-serif text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
              {t('home.expTitle')}
            </h2>
          </Reveal>
          <div className="space-y-7">
            {experiences.map((experience, index) => (
              <Reveal
                key={experience.title}
                delay={index * 0.1}
                y={40}
                className="grid overflow-hidden rounded-[2rem] bg-brand-sand p-2 shadow-[0_22px_70px_rgba(32,52,43,0.09)] md:grid-cols-[0.9fr_1.1fr]"
              >
                <img
                  src={experience.image}
                  alt={experience.alt}
                  className="h-full min-h-[280px] w-full rounded-[1.55rem] object-cover"
                  loading="lazy"
                  decoding="async"
                />
                <div className="flex flex-col justify-end p-7 sm:p-9">
                  <h3 className="font-serif text-3xl text-brand-charcoal sm:text-4xl">
                    {index === 0 ? t('home.expMorningTitle') : index === 1 ? t('home.expDiningTitle') : t('home.expGardenTitle')}
                  </h3>
                  <p className="mt-5 max-w-xl text-base leading-8 text-brand-ink/65">
                    {index === 0 ? t('home.expMorningCopy') : index === 1 ? t('home.expDiningCopy') : t('home.expGardenCopy')}
                  </p>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function AmenitiesSection() {
  const { t } = useTranslation();
  return (
    <section id="amenities" className="bg-brand-forest-deep px-5 py-24 text-brand-paper sm:px-8 lg:px-12 lg:py-32">
      <div className="mx-auto grid max-w-[1440px] gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-start">
        <Reveal>
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">
            {t('home.amenitiesEyebrow')}
          </p>
          <h2 className="max-w-[12ch] font-serif text-4xl leading-[1.08] text-brand-white sm:text-5xl lg:text-7xl">
            {t('home.amenitiesTitle')}
          </h2>
          <p className="mt-6 max-w-xl text-base leading-8 text-brand-paper/72">
            {t('home.amenitiesDesc')}
          </p>
        </Reveal>
        <div className="grid gap-px overflow-hidden rounded-[2rem] bg-brand-white/12 p-px sm:grid-cols-2">
          {amenities.map((amenity, index) => (
            <Reveal
              key={amenity}
              delay={index * 0.05}
              y={28}
              duration={0.9}
              className="min-h-28 bg-brand-forest px-6 py-7"
            >
              <p className="text-lg text-brand-white">
                {t(`villas.amenities.${amenity}` as any) === `villas.amenities.${amenity}` ? amenity : t(`villas.amenities.${amenity}` as any)}
              </p>
              <p className="mt-3 text-sm leading-6 text-brand-paper/58">
                {t('home.amenitiesPrepared')}
              </p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

function ReviewsSection() {
  const { t } = useTranslation();
  return (
    <section id="reviews" className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12 lg:py-32">
      <Reveal className="mx-auto max-w-[1120px] text-center">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
          {t('home.reviewsEyebrow')}
        </p>
        <blockquote className="font-serif text-3xl leading-[1.18] text-brand-charcoal sm:text-5xl lg:text-6xl">
          {t('home.reviewsQuote')}
        </blockquote>
        <p className="mt-8 text-sm font-semibold uppercase tracking-[0.18em] text-brand-forest">
          {t('home.reviewsAuthor')}
        </p>
      </Reveal>
    </section>
  );
}

function GallerySection() {
  const { t } = useTranslation();
  return (
    <section id="gallery" className="bg-brand-sand px-5 py-24 sm:px-8 lg:px-12 lg:py-36">
      <div className="mx-auto max-w-[1440px]">
        <div className="mb-12 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <Reveal>
            <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
              {t('home.galleryEyebrow')}
            </p>
            <h2 className="max-w-[12ch] font-serif text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
              {t('home.galleryTitle')}
            </h2>
          </Reveal>
          <Reveal delay={0.15}>
            <MagneticButton>
              <Link
                href="/gallery"
                className="inline-flex min-h-12 w-fit items-center justify-center rounded-full border border-brand-forest px-6 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-forest hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
              >
                {t('home.galleryBtn')}
              </Link>
            </MagneticButton>
          </Reveal>
        </div>
        <div className="grid gap-5 md:grid-cols-4 md:grid-rows-[260px_260px] lg:grid-rows-[340px_340px]">
          {gallery.map((item, index) => (
            <Reveal
              key={item.src}
              delay={index * 0.08}
              y={32}
              className={`overflow-hidden rounded-[1.5rem] ${
                index === 1 ? 'md:col-span-2 md:row-span-2' : ''
              }`}
            >
              <ParallaxImage src={item.src} alt={item.alt} />
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

function LocationSection() {
  const { t } = useTranslation();
  return (
    <section id="location" className="bg-brand-paper px-5 py-24 sm:px-8 lg:px-12 lg:py-32">
      <div className="mx-auto grid max-w-[1440px] gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
        <Reveal>
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
            {t('home.locEyebrow')}
          </p>
          <h2 className="max-w-[12ch] font-serif text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
            {t('home.locTitle')}
          </h2>
          <p className="mt-6 max-w-xl text-base leading-8 text-brand-ink/66">
            {t('home.locDesc')}
          </p>
          <dl className="mt-10 grid gap-5 sm:grid-cols-3">
            {[
              [t('home.locDist1'), t('home.locDist1Label')],
              [t('home.locDist2'), t('home.locDist2Label')],
              [t('home.locDist3'), t('home.locDist3Label')],
            ].map(([value, label]) => (
              <div key={label} className="border-t border-brand-stone pt-5">
                <dt className="text-3xl font-serif text-brand-charcoal">{value}</dt>
                <dd className="mt-2 text-sm uppercase tracking-[0.14em] text-brand-ink/52">{label}</dd>
              </div>
            ))}
          </dl>
        </Reveal>
        <Reveal delay={0.15} y={32} className="rounded-[2rem] bg-brand-sand p-2 shadow-[0_24px_90px_rgba(32,52,43,0.11)]">
          <div className="relative min-h-[420px] overflow-hidden rounded-[1.55rem] bg-brand-forest-deep">
            <img
              src={locationImage}
              alt="Green landscape and quiet road leading toward a private resort area"
              className="absolute inset-0 h-full w-full object-cover opacity-72"
              loading="lazy"
              decoding="async"
            />
            <div className="absolute inset-0 bg-[linear-gradient(120deg,rgba(25,52,42,0.86),rgba(25,52,42,0.28))]" />
            <div className="absolute left-8 top-8 rounded-full bg-brand-paper px-4 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest">
              Nhu Villas
            </div>
            <div className="absolute bottom-8 left-8 right-8 max-w-sm rounded-[1.25rem] border border-brand-white/18 bg-brand-forest-deep/72 p-6 text-brand-paper backdrop-blur-md">
              <p className="text-2xl font-serif text-brand-white">{t('home.locAddrNote')}</p>
              <p className="mt-4 text-sm leading-7 text-brand-paper/68">
                {t('home.locAddrDesc')}
              </p>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function FinalCtaSection() {
  const { t } = useTranslation();
  return (
    <section className="relative isolate overflow-hidden bg-brand-forest-deep px-5 py-24 text-brand-white sm:px-8 lg:px-12 lg:py-36">
      <img
        src={poolImage}
        alt="Golden sunset over a quiet water horizon"
        className="absolute inset-0 -z-10 h-full w-full object-cover opacity-[0.42]"
        loading="lazy"
        decoding="async"
      />
      <div className="absolute inset-0 -z-10 bg-brand-forest-deep/72" />
      <Reveal className="mx-auto max-w-4xl text-center">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">
          {t('home.ctaEyebrow')}
        </p>
        <h2 className="font-serif text-4xl leading-[1.08] !text-white sm:text-5xl lg:text-7xl">
          {t('home.ctaTitle')}
        </h2>
        <p className="mx-auto mt-6 max-w-2xl text-base leading-8 text-brand-paper/76">
          {t('home.ctaDesc')}
        </p>
        <MagneticButton className="mt-9">
          <Link
            href="/villas"
            className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper bg-brand-paper px-8 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-white hover:shadow-[0_22px_60px_rgba(8,17,14,0.38)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
          >
            {t('home.ctaBtn')}
          </Link>
        </MagneticButton>
      </Reveal>
    </section>
  );
}



import { FeaturedResidences } from './components/FeaturedResidences';

export const HomePage = () => {
  return (
    <>
      <Hero />
      <PromotionMarquee />
      <StorySection />
      <FeaturedResidences />
      <VillaCollectionSection />
      <ExperienceSection />
      <AmenitiesSection />
      <ReviewsSection />
      <GallerySection />
      <LocationSection />
      <FinalCtaSection />
    </>
  );
};
