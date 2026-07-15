'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Hero } from './components/Hero';
import { Reveal } from '../../shared/components/Reveal';
import { api, API_PATHS } from '../../shared/api/client';
import { fallbackVillas } from '../villas/villaFallbacks';
import type { Villa } from '../villas/types';
import { useTranslation, useCurrency } from '../../shared/i18n/hooks';

// Removed formatPrice in favor of useCurrency

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
function VillaCard({ villa, delay = 0, featured = false }: { villa: Villa; delay?: number; featured?: boolean }) {
  const { formatCurrency } = useCurrency();
  const { t } = useTranslation();
  return (
    <Reveal delay={delay} y={36}>
      <Link
        href={`/villas/${villa.id}`}
        className={`group block rounded-[2rem] bg-brand-paper/70 p-2 shadow-[0_24px_80px_rgba(32,52,43,0.1)] transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-2 hover:shadow-[0_34px_100px_rgba(32,52,43,0.16)] ${
          featured ? 'lg:mt-16' : ''
        }`}
      >
        <div className="relative overflow-hidden rounded-t-[1.55rem]">
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
        <div className="p-6 sm:p-7">
          <div className="flex items-start justify-between gap-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-sage">
                {t(`villas.data.${villa.id}.roomType` as any) === `villas.data.${villa.id}.roomType` ? villa.roomType : t(`villas.data.${villa.id}.roomType` as any)}
              </p>
              <h3 className="mt-2 text-3xl leading-tight text-brand-charcoal">
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
          <div className="mt-7 flex items-center justify-between border-t border-brand-stone/80 pt-5">
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
      <div className="mx-auto grid max-w-[1440px] gap-12 lg:grid-cols-[0.95fr_1.05fr] lg:items-center lg:gap-24">
        <Reveal>
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
            {t('home.storyEyebrow')}
          </p>
          <h2 className="max-w-[13ch] text-4xl leading-[1.06] text-brand-charcoal sm:text-5xl lg:text-7xl">
            {t('home.storyTitle')}
          </h2>
        </Reveal>
        <div className="grid gap-6 md:grid-cols-[0.72fr_1fr] md:items-end">
          <Reveal delay={0.1} className="md:pb-16">
            <p className="text-lg leading-8 text-brand-ink/74">
              {t('home.storyP1')}
            </p>
            <p className="mt-6 text-base leading-8 text-brand-ink/62">
              {t('home.storyP2')}
            </p>
          </Reveal>
          <Reveal delay={0.2} className="overflow-hidden rounded-[2rem] bg-brand-stone p-2 shadow-[0_26px_80px_rgba(32,52,43,0.13)]">
            <img
              src={villaExteriorImage}
              alt="Quiet resort architecture surrounded by natural garden light"
              className="h-full min-h-[420px] w-full rounded-[1.55rem] object-cover transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02]"
              loading="lazy"
              decoding="async"
            />
          </Reveal>
        </div>
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
        return res.slice(0, 3);
      } catch {
        return fallbackVillas.slice(0, 3);
      }
    },
    staleTime: 60_000,
  });

  const displayVillas = villas && villas.length > 0 ? villas : fallbackVillas.slice(0, 3);
  const { t } = useTranslation();

  return (
    <section id="villas" className="bg-brand-sand px-5 py-24 sm:px-8 lg:px-12 lg:py-36">
      <div className="mx-auto max-w-[1440px]">
        <Reveal className="mb-14 max-w-3xl">
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
            {t('home.featured')}
          </p>
          <h2 className="text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
            {t('home.featuredSubtitle')}
          </h2>
          <p className="mt-6 max-w-2xl text-base leading-8 text-brand-ink/66 sm:text-lg">
            {t('home.featuredDesc')}
          </p>
        </Reveal>

        {isLoading ? (
          <div className="grid gap-7 lg:grid-cols-3 lg:gap-8">
            {[0, 1, 2].map((i) => (
              <VillaCardSkeleton key={i} delay={i * 100} />
            ))}
          </div>
        ) : (
          <div className="grid gap-7 lg:grid-cols-3 lg:gap-8">
            {displayVillas.map((villa, index) => (
              <VillaCard key={villa.id} villa={villa} delay={index * 0.12} featured={index === 1} />
            ))}
          </div>
        )}

        <Reveal delay={0.3} className="mt-12 text-center">
          <Link
            href="/villas"
            className="inline-flex min-h-12 w-fit items-center justify-center rounded-full border border-brand-forest px-8 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-forest hover:text-brand-white active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
          >
            {t('home.viewAll')}
          </Link>
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
            <h2 className="max-w-[11ch] text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
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
                  <h3 className="text-3xl text-brand-charcoal sm:text-4xl">
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
          <h2 className="max-w-[12ch] text-4xl leading-[1.08] text-brand-white sm:text-5xl lg:text-7xl">
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
        <blockquote className="text-3xl leading-[1.18] text-brand-charcoal sm:text-5xl lg:text-6xl">
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
            <h2 className="max-w-[12ch] text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
              {t('home.galleryTitle')}
            </h2>
          </Reveal>
          <Reveal delay={0.15}>
            <Link
              href="/gallery"
              className="inline-flex min-h-12 w-fit items-center justify-center rounded-full border border-brand-forest px-6 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-forest hover:text-brand-white active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest"
            >
              {t('home.galleryBtn')}
            </Link>
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
              <img
                src={item.src}
                alt={item.alt}
                className="h-full min-h-[260px] w-full rounded-[1.5rem] object-cover transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.02]"
                loading="lazy"
                decoding="async"
              />
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
          <h2 className="max-w-[12ch] text-4xl leading-[1.08] text-brand-charcoal sm:text-5xl lg:text-7xl">
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
        <h2 className="text-4xl leading-[1.08] sm:text-5xl lg:text-7xl">
          {t('home.ctaTitle')}
        </h2>
        <p className="mx-auto mt-6 max-w-2xl text-base leading-8 text-brand-paper/76">
          {t('home.ctaDesc')}
        </p>
        <Link
          href="/villas"
          className="mt-9 inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper bg-brand-paper px-8 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-[1px] hover:bg-brand-white hover:shadow-[0_22px_60px_rgba(8,17,14,0.38)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
        >
          {t('home.ctaBtn')}
        </Link>
      </Reveal>
    </section>
  );
}



export const HomePage = () => {
  return (
    <>
      <Hero />
      <StorySection />
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
