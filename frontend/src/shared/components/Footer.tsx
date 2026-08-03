'use client';

import Link from 'next/link';
import { useTranslation } from '../i18n/hooks';
import { Reveal } from './Reveal';

export function Footer() {
  const { t } = useTranslation();
  return (
    <footer className="bg-brand-charcoal px-5 py-14 text-brand-paper sm:px-8 lg:px-12">
      <Reveal className="mx-auto grid max-w-[1440px] gap-10 md:grid-cols-[1.1fr_0.9fr_0.9fr]">
        <div>
          <Link href="/" className="text-3xl font-serif text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">
            Nhu Villas
          </Link>
          <p className="mt-5 max-w-sm text-sm leading-7 text-brand-paper/62">
            {t('footer.desc')}
          </p>
        </div>
        <nav aria-label="Footer navigation" className="grid grid-cols-2 gap-3 text-sm text-brand-paper/74">
          <Link href="/villas" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.villas')}</Link>
          <Link href="/#experiences" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.experiences')}</Link>
          <Link href="/#gallery" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.gallery')}</Link>
          <Link href="/#reviews" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.reviews')}</Link>
          <Link href="/offers" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.offers')}</Link>
          <Link href="/about" className="hover:text-brand-white focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper">{t('nav.about')}</Link>
        </nav>
        <div className="text-sm leading-7 text-brand-paper/68">
          <p>reservations@nhuvillas.com</p>
          <p>+84 123 456 789</p>
          <p>Viet Nam</p>
          <p className="mt-6 text-xs uppercase tracking-[0.18em] text-brand-paper/46">
            {t('footer.tag')}
          </p>
        </div>
      </Reveal>
    </footer>
  );
}
