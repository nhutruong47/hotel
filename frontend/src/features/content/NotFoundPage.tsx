'use client';

import Link from 'next/link';

export const NotFoundPage = () => (
  <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
    <div className="mx-auto max-w-[1180px]">
      <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Not found</p>
      <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-7xl">This path is quiet.</h1>
      <p className="mt-6 max-w-2xl text-base leading-8 text-brand-ink/64">
        The page may have moved during the React migration. Return to the villa collection or the homepage to
        continue.
      </p>
      <div className="mt-12">
        <div className="rounded-[2rem] bg-brand-paper p-8 text-center">
          <Link
            href="/"
            className="inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
          >
            Return home
          </Link>
        </div>
      </div>
    </div>
  </section>
);