'use client';

import { useMemo, useRef } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { AmbientLayers } from './AmbientLayers';
import { useMouseParallax } from '../hooks/useMouseParallax';
import { useTranslation } from '../../../shared/i18n/hooks';

const heroImage = '/images/nhu-hero-villa-4k.jpg';

export function Hero() {
  const { t } = useTranslation();
  // Refs for the three parallax layers. Mountains slowest, foreground fastest.
  const mountainsRef = useRef<HTMLDivElement>(null);
  const villaRef = useRef<HTMLDivElement>(null);
  const foregroundRef = useRef<HTMLDivElement>(null);

  const refs = useMemo(
    () => [mountainsRef, villaRef, foregroundRef],
    [],
  );
  const speeds = useMemo(() => [0.3, 0.6, 1.0], []);
  // Cap displacement to 6 px so the camera motion stays subliminal.
  useMouseParallax(refs, speeds, 6);

  return (
    <section
      aria-labelledby="home-hero-title"
      className="relative isolate flex min-h-[100dvh] w-full items-center justify-start overflow-hidden bg-brand-forest-deep text-brand-white"
    >
      {/* Background media stack */}
      <div className="pointer-events-none absolute inset-0 -z-30">
        <div
          ref={mountainsRef}
          className="hero-media-motion absolute inset-0 will-change-transform"
        >
          <Image
            src={heroImage}
            alt=""
            aria-hidden="true"
            fill
            priority
            sizes="100vw"
            className="object-cover object-center"
          />
        </div>

        {/* Lake reflection — flipped, blurred, masked to the lower 40% */}
        <div className="absolute inset-x-0 bottom-0 h-[44%] overflow-hidden opacity-[0.18] [mask-image:linear-gradient(to_top,black_30%,transparent_100%)] [-webkit-mask-image:linear-gradient(to_top,black_30%,transparent_100%)]">
          <div
            ref={villaRef}
            className="hero-reflection relative h-full w-full will-change-transform"
            style={{ transform: 'scaleY(-1)' }}
          >
            <Image
              src={heroImage}
              alt=""
              aria-hidden="true"
              fill
              sizes="100vw"
              className="object-cover object-center blur-md"
            />
          </div>
        </div>

        {/* Foreground vegetation drift */}
        <div
          ref={foregroundRef}
          className="absolute inset-0 will-change-transform"
        >
          <div className="absolute inset-x-0 bottom-0 h-[35%] bg-[radial-gradient(60%_100%_at_20%_100%,rgba(15,40,28,0.62)_0%,rgba(15,40,28,0)_70%),radial-gradient(50%_100%_at_85%_100%,rgba(25,52,42,0.50)_0%,rgba(25,52,42,0)_70%)] mix-blend-multiply" />
        </div>

        {/* Single readability overlay per spec */}
        <div className="absolute inset-0 bg-[linear-gradient(rgba(8,20,16,0.55),rgba(8,20,16,0.45))]" />
      </div>

      {/* Ambient cinematic layers (mist, dust, sunrays, breeze, glints) */}
      <AmbientLayers />

      {/* Foreground vignette so the text always reads */}
      <div className="pointer-events-none absolute inset-0 z-[1] bg-[radial-gradient(120%_70%_at_15%_50%,rgba(8,17,14,0.55)_0%,rgba(8,17,14,0.18)_45%,rgba(8,17,14,0)_75%)]" />

      {/* Content */}
      <div className="relative z-10 mx-auto flex w-full max-w-[1400px] flex-col items-start justify-center px-6 py-28 sm:px-8 lg:px-14 lg:py-36">
        <div className="hero-copy-motion max-w-[34rem]">
          <p className="mb-7 w-fit rounded-full border border-brand-white/25 bg-brand-white/10 px-5 py-2 text-[0.68rem] font-medium uppercase tracking-[0.22em] text-brand-paper backdrop-blur-md">
            {t('home.heroLocation')}
          </p>
          <h1
            id="home-hero-title"
            className="max-w-[14ch] font-serif text-[clamp(3.25rem,7.4vw,5.75rem)] font-medium leading-[1.02] tracking-[-0.01em] text-brand-white"
          >
            {t('home.heroTitle')}
          </h1>
          <p className="mt-8 max-w-[32rem] text-base leading-8 text-brand-paper/88 sm:text-lg">
            {t('home.heroSubtitle')}
          </p>
          <div className="mt-12 flex flex-col gap-3 sm:flex-row sm:items-center">
            <Link
              href="/villas"
              className="group relative inline-flex min-h-12 min-w-[14rem] items-center justify-center rounded-full border border-brand-paper bg-brand-paper px-8 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest-deep shadow-[0_18px_44px_rgba(8,17,14,0.32)] transition duration-[450ms] ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-[1px] hover:bg-brand-white hover:shadow-[0_22px_60px_rgba(8,17,14,0.38)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
            >
              {t('home.heroCheckAvail')}
            </Link>
            <Link
              href="/villas"
              className="inline-flex min-h-12 min-w-[12rem] items-center justify-center rounded-full border border-brand-paper/45 bg-brand-white/10 px-8 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-brand-paper backdrop-blur-md transition duration-[450ms] ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-[1px] hover:border-brand-paper hover:bg-brand-white/16 hover:shadow-[0_18px_48px_rgba(8,17,14,0.34)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
            >
              {t('home.heroExplore')}
            </Link>
          </div>
        </div>
      </div>

      {/* Scroll hint */}
      <div
        className="pointer-events-none absolute bottom-7 left-1/2 z-10 hidden -translate-x-1/2 flex-col items-center gap-2 text-brand-paper/70 sm:flex"
        aria-hidden="true"
      >
        <span className="text-[0.62rem] font-semibold uppercase tracking-[0.28em]">
          Scroll
        </span>
        <span className="h-10 w-px bg-gradient-to-b from-brand-paper/0 via-brand-paper/55 to-brand-paper/0" />
      </div>
    </section>
  );
}

export default Hero;
