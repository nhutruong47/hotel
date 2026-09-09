'use client';

import Link from 'next/link';
import { useTranslation } from '../../../shared/i18n/hooks';

export function PromotionMarquee() {
  const { lang } = useTranslation();
  const isVietnamese = lang === 'vi';
  const label = isVietnamese
    ? 'Khám phá các ưu đãi của Nhu Villas'
    : 'Explore Nhu Villas offers';
  const messages = isVietnamese
    ? [
        'Ưu đãi nghỉ dưỡng theo mùa',
        'Đặt trực tiếp tại Nhu Villas',
        'Khám phá kỳ nghỉ riêng tư',
      ]
    : [
        'Seasonal villa offers',
        'Book direct with Nhu Villas',
        'Discover your private escape',
      ];

  return (
    <aside
      aria-label={label}
      className="force-light-theme overflow-hidden border-y border-brand-paper/15 bg-brand-forest-deep text-brand-paper"
    >
      <Link
        href="/offers"
        aria-label={label}
        className="group block py-4 focus-visible:outline-2 focus-visible:outline-offset-[-3px] focus-visible:outline-brand-gold sm:py-[1.125rem]"
      >
        <div className="promotion-marquee-track" aria-hidden="true">
          {[0, 1].map((copy) => (
            <div className="promotion-marquee-group" key={copy}>
              {messages.map((message) => (
                <span className="flex shrink-0 items-center gap-8" key={`${copy}-${message}`}>
                  <span className="whitespace-nowrap text-[0.68rem] font-semibold uppercase tracking-[0.24em] sm:text-xs">
                    {message}
                  </span>
                  <span className="text-brand-gold" aria-hidden="true">
                    ◆
                  </span>
                </span>
              ))}
            </div>
          ))}
        </div>
      </Link>
    </aside>
  );
}
