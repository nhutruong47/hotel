'use client';

import { useState, useEffect, useMemo, type ReactNode } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useTranslation } from '../../shared/i18n/hooks';

export function ContentShell({
  eyebrow,
  title,
  copy,
  children,
}: {
  eyebrow: string;
  title: string;
  copy: string;
  children: ReactNode;
}) {
  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1180px]">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{eyebrow}</p>
        <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-7xl">{title}</h1>
        <p className="mt-6 max-w-2xl text-base leading-8 text-brand-ink/64">{copy}</p>
        <div className="mt-12">{children}</div>
      </div>
    </section>
  );
}

// Types
export type PromotionCategory = 'SEASONAL' | 'WEEKEND' | 'HOLIDAY' | 'HONEYMOON' | 'FAMILY' | 'EARLY_BIRD' | 'LAST_MINUTE' | 'LOYALTY' | 'LONG_STAY' | 'SPECIAL';

export interface Promotion {
  id: number;
  title: string;
  subtitle: string;
  description: string;
  termsConditions: string;
  imageUrl: string;
  bannerUrl: string;
  category: PromotionCategory;
  isActive: boolean;
  isFeatured: boolean;
  startDate: string;
  endDate: string;
  countdownEndDate: string;
  discountPercent: number;
  discountAmount: number;
  minimumBookingAmount: number;
  minimumNights: number;
  promoCode: string;
  displayOrder: number;
  rooms?: { id: number; roomNumber: string }[];
}

export interface PromotionPreview {
  valid: boolean;
  code: string;
  discountType: string;
  discountValue: number;
  discount: number;
  finalAmount: number;
  promotion?: Promotion;
  error?: string;
}

// Countdown Timer Component
export function CountdownTimer({ endDate }: { endDate: string }) {
  const [timeLeft, setTimeLeft] = useState({ days: 0, hours: 0, minutes: 0, seconds: 0 });

  useEffect(() => {
    const calculateTimeLeft = () => {
      const difference = new Date(endDate).getTime() - new Date().getTime();
      if (difference <= 0) {
        setTimeLeft({ days: 0, hours: 0, minutes: 0, seconds: 0 });
        return;
      }
      setTimeLeft({
        days: Math.floor(difference / (1000 * 60 * 60 * 24)),
        hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
        minutes: Math.floor((difference / 1000 / 60) % 60),
        seconds: Math.floor((difference / 1000) % 60),
      });
    };

    calculateTimeLeft();
    const timer = setInterval(calculateTimeLeft, 1000);
    return () => clearInterval(timer);
  }, [endDate]);

  return (
    <div className="flex gap-2">
      {timeLeft.days > 0 && (
        <div className="rounded-lg bg-brand-forest-deep/20 px-2 py-1 text-center">
          <span className="block text-lg font-bold text-brand-forest">{timeLeft.days}</span>
          <span className="text-xs uppercase">Days</span>
        </div>
      )}
      <div className="rounded-lg bg-brand-forest-deep/20 px-2 py-1 text-center">
        <span className="block text-lg font-bold text-brand-forest">{String(timeLeft.hours).padStart(2, '0')}</span>
        <span className="text-xs uppercase">Hours</span>
      </div>
      <div className="rounded-lg bg-brand-forest-deep/20 px-2 py-1 text-center">
        <span className="block text-lg font-bold text-brand-forest">{String(timeLeft.minutes).padStart(2, '0')}</span>
        <span className="text-xs uppercase">Min</span>
      </div>
      <div className="rounded-lg bg-brand-forest-deep/20 px-2 py-1 text-center">
        <span className="block text-lg font-bold text-brand-forest">{String(timeLeft.seconds).padStart(2, '0')}</span>
        <span className="text-xs uppercase">Sec</span>
      </div>
    </div>
  );
}

// Category Badge
export const getCategoryLabels = (t: any): Record<PromotionCategory, string> => ({
  SEASONAL: t('content.promo.seasonal') || 'Mùa cao điểm',
  WEEKEND: t('content.promo.weekend') || 'Cuối tuần',
  HOLIDAY: t('content.promo.holiday') || 'Ngày lễ',
  HONEYMOON: t('content.promo.honeymoon') || 'Tuần trăng mật',
  FAMILY: t('content.promo.family') || 'Gia đình',
  EARLY_BIRD: t('content.promo.earlyBird') || 'Đặt sớm',
  LAST_MINUTE: t('content.promo.lastMinute') || 'Last Minute',
  LOYALTY: t('content.promo.loyalty') || 'Khách quen',
  LONG_STAY: t('content.promo.longStay') || 'Dài ngày',
  SPECIAL: t('content.promo.special') || 'Đặc biệt',
});

export const categoryColors: Record<PromotionCategory, string> = {
  SEASONAL: 'bg-brand-coral/20 text-brand-coral',
  WEEKEND: 'bg-brand-sage/30 text-brand-forest',
  HOLIDAY: 'bg-amber-100 text-amber-700',
  HONEYMOON: 'bg-pink-100 text-pink-700',
  FAMILY: 'bg-blue-100 text-blue-700',
  EARLY_BIRD: 'bg-emerald-100 text-emerald-700',
  LAST_MINUTE: 'bg-orange-100 text-orange-700',
  LOYALTY: 'bg-purple-100 text-purple-700',
  LONG_STAY: 'bg-teal-100 text-teal-700',
  SPECIAL: 'bg-brand-forest/20 text-brand-forest',
};

// Promotion Card
export function PromotionCard({ promo }: { promo: Promotion }) {
  const { t } = useTranslation();
  const [showTerms, setShowTerms] = useState(false);
  const hasCountdown = promo.countdownEndDate && new Date(promo.countdownEndDate) > new Date();
  const discountDisplay = promo.discountPercent
    ? `${promo.discountPercent}%`
    : `$${promo.discountAmount?.toLocaleString()}`;

  return (
    <article className="group overflow-hidden rounded-[2rem] bg-brand-paper shadow-[0_22px_70px_rgba(32,52,43,0.09)]">
      {promo.imageUrl && (
        <div className="relative aspect-[16/9] overflow-hidden">
          <img
            src={promo.imageUrl}
            alt={promo.title}
            className="h-full w-full object-cover transition duration-700 group-hover:scale-[1.02]"
            loading="lazy"
          />
          <div className="absolute left-4 top-4 flex gap-2">
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${categoryColors[promo.category]}`}>
              {getCategoryLabels(t)[promo.category]}
            </span>
            {promo.isFeatured && (
              <span className="rounded-full bg-brand-forest px-3 py-1 text-xs font-semibold text-brand-white">
                Nổi bật
              </span>
            )}
          </div>
          {hasCountdown && (
            <div className="absolute bottom-4 left-4 right-4 rounded-xl bg-brand-forest-deep/90 p-3 backdrop-blur-sm">
              <p className="mb-2 text-center text-xs font-semibold uppercase tracking-wider text-brand-sage-light">
                Kết thúc sau
              </p>
              <CountdownTimer endDate={promo.countdownEndDate} />
            </div>
          )}
        </div>
      )}
      <div className="p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl text-brand-charcoal">{promo.title}</h2>
            <p className="mt-1 text-sm text-brand-ink/64">{promo.subtitle}</p>
          </div>
          <div className="rounded-full bg-brand-sage/20 px-4 py-2 text-center">
            <span className="block text-2xl font-bold text-brand-forest">-{discountDisplay}</span>
          </div>
        </div>

        <p className="mt-4 text-sm leading-7 text-brand-ink/62">{promo.description}</p>

        {promo.minimumNights > 1 && (
          <p className="mt-3 text-xs text-brand-ink/50">
            Áp dụng cho booking từ {promo.minimumNights} đêm trở lên
          </p>
        )}

        <div className="mt-6 flex flex-wrap items-center gap-3">
          {promo.promoCode && (
            <div className="flex items-center gap-2 rounded-full bg-brand-sand px-4 py-2">
              <span className="font-mono text-sm font-bold text-brand-forest">{promo.promoCode}</span>
              <button
                onClick={() => navigator.clipboard.writeText(promo.promoCode)}
                className="text-xs text-brand-ink/50 hover:text-brand-forest"
                title="Copy code"
              >
                Copy
              </button>
            </div>
          )}
          <Link
            href={
              promo.rooms && promo.rooms.length === 1
                ? `/villas/${promo.rooms[0].id}`
                : promo.rooms && promo.rooms.length > 1
                ? `/villas?promotion=${encodeURIComponent(promo.promoCode || promo.title.toLowerCase().replace(/\s+/g, '-'))}`
                : "/villas"
            }
            className="inline-flex min-h-11 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
          >
            Book Now
          </Link>
        </div>

        {promo.termsConditions && (
          <button
            onClick={() => setShowTerms(!showTerms)}
            className="mt-4 text-xs text-brand-ink/50 underline underline-offset-2 hover:text-brand-forest"
          >
            {showTerms ? 'Hide terms' : 'View terms & conditions'}
          </button>
        )}

        {showTerms && promo.termsConditions && (
          <div className="mt-3 rounded-xl bg-brand-sand/50 p-4 text-xs leading-6 text-brand-ink/70">
            <strong>Điều kiện:</strong> {promo.termsConditions}
          </div>
        )}
      </div>
    </article>
  );
}

// Featured Promotion Banner
export function FeaturedBanner({ promo }: { promo: Promotion }) {
  const { t } = useTranslation();
  const hasCountdown = promo.countdownEndDate && new Date(promo.countdownEndDate) > new Date();
  const discountDisplay = promo.discountPercent
    ? `${promo.discountPercent}%`
    : `$${promo.discountAmount?.toLocaleString()}`;

  return (
    <div className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-brand-forest-deep to-brand-forest p-8 text-brand-white lg:p-12">
      <div className="relative z-10">
        <div className="flex flex-wrap items-center gap-3">
          <span className="rounded-full bg-brand-white/20 px-3 py-1 text-xs font-semibold uppercase tracking-wider">
            {getCategoryLabels(t)[promo.category]}
          </span>
          <span className="rounded-full bg-brand-coral px-3 py-1 text-xs font-semibold uppercase tracking-wider text-white">
            Nổi bật
          </span>
        </div>
        <h2 className="mt-6 text-4xl font-serif lg:text-6xl">{promo.title}</h2>
        <p className="mt-4 max-w-xl text-lg text-brand-paper/80">{promo.subtitle}</p>

        <div className="mt-8 flex flex-wrap items-center gap-6">
          <div className="text-center">
            <span className="block text-5xl font-bold lg:text-7xl">-{discountDisplay}</span>
            <span className="text-sm uppercase tracking-wider text-brand-paper/60">Giảm giá</span>
          </div>
          {hasCountdown && (
            <div className="flex-1">
              <p className="mb-2 text-center text-sm uppercase tracking-wider text-brand-sage-light">
                Ưu đãi kết thúc sau
              </p>
              <CountdownTimer endDate={promo.countdownEndDate} />
            </div>
          )}
        </div>

        <div className="mt-8 flex flex-wrap gap-4">
          {promo.promoCode && (
            <div className="flex items-center gap-2 rounded-full bg-brand-white/20 px-4 py-3">
              <span className="font-mono font-bold">{promo.promoCode}</span>
              <button
                onClick={() => navigator.clipboard.writeText(promo.promoCode)}
                className="rounded-full bg-brand-white/20 px-3 py-1 text-xs hover:bg-brand-white/30"
              >
                Copy
              </button>
            </div>
          )}
          <Link
            href={
              promo.rooms && promo.rooms.length === 1
                ? `/villas/${promo.rooms[0].id}`
                : promo.rooms && promo.rooms.length > 1
                ? `/villas?promotion=${encodeURIComponent(promo.promoCode || promo.title.toLowerCase().replace(/\s+/g, '-'))}`
                : "/villas"
            }
            className="inline-flex min-h-12 items-center justify-center rounded-full bg-brand-white px-8 py-4 text-sm font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition hover:bg-brand-paper"
          >
            Khám phá ngay
          </Link>
        </div>
      </div>
      {promo.imageUrl && (
        <div className="absolute -bottom-20 -right-20 h-80 w-80 opacity-20 lg:h-[400px] lg:w-[400px]">
          <img src={promo.imageUrl} alt="" className="h-full w-full rounded-full object-cover" />
        </div>
      )}
    </div>
  );
}

// Promo Code Validator
export function PromoCodeValidator() {
  const [code, setCode] = useState('');
  const [amount, setAmount] = useState('500');
  const [nights, setNights] = useState('3');
  const [preview, setPreview] = useState<PromotionPreview | null>(null);

  const validate = async () => {
    if (!code.trim()) return;
    try {
      const result = await api.get<PromotionPreview>(
        `${API_PATHS.promotions.preview}?code=${encodeURIComponent(code)}&subtotal=${amount}`
      );
      setPreview(result);
    } catch {
      setPreview({ valid: false, code, error: 'Mã không hợp lệ', discountType: '', discountValue: 0, discount: 0, finalAmount: 0 });
    }
  };

  return (
    <div className="rounded-[2rem] bg-brand-paper p-8 shadow-[0_22px_70px_rgba(32,52,43,0.09)]">
      <h3 className="text-2xl text-brand-charcoal">Apply Promo Code</h3>
      <p className="mt-2 text-sm text-brand-ink/64">Enter your promo code to see the discount</p>

      <div className="mt-6 grid gap-4 sm:grid-cols-[1fr_auto_auto]">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          placeholder="Enter promo code"
          className="h-14 rounded-full border border-brand-stone px-6 font-mono text-lg tracking-wider focus:border-brand-forest focus:outline-none"
        />
        <input
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="Amount"
          className="h-14 w-28 rounded-full border border-brand-stone px-4 text-center focus:border-brand-forest focus:outline-none"
        />
        <input
          type="number"
          value={nights}
          onChange={(e) => setNights(e.target.value)}
          placeholder="Nights"
          className="h-14 w-24 rounded-full border border-brand-stone px-4 text-center focus:border-brand-forest focus:outline-none"
        />
      </div>

      <button
        onClick={validate}
        className="mt-4 min-h-12 w-full rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
      >
        Validate Code
      </button>

      {preview && (
        <div className={`mt-6 rounded-xl p-4 ${preview.valid ? 'bg-brand-sage/20' : 'bg-brand-coral/10'}`}>
          {preview.valid ? (
            <div>
              <p className="text-sm font-semibold text-brand-forest">
                Code "{preview.code}" is valid!
              </p>
              <div className="mt-3 flex justify-between text-sm">
                <span>Discount ({preview.discountType === 'percent' ? `${preview.discountValue}%` : 'Fixed'}):</span>
                <span className="font-semibold text-brand-forest">-${preview.discount.toLocaleString()}</span>
              </div>
              <div className="mt-2 flex justify-between border-t border-brand-stone/30 pt-2 text-sm">
                <span>Final Amount:</span>
                <span className="font-bold text-brand-charcoal">${preview.finalAmount.toLocaleString()}</span>
              </div>
            </div>
          ) : (
            <p className="text-sm text-brand-coral">{preview.error}</p>
          )}
        </div>
      )}
    </div>
  );
}

// Seasonal Deals Banner
export function SeasonalDealsBanner({ promos }: { promos: Promotion[] }) {
  if (promos.length === 0) return null;

  const seasonal = promos.filter((p) => p.category === 'SEASONAL');
  const honeymoon = promos.filter((p) => p.category === 'HONEYMOON');
  const family = promos.filter((p) => p.category === 'FAMILY');

  const dealCategories = [
    {
      category: seasonal[0] || promos[0],
      emoji: '☀️',
      title: 'Summer Escape',
      desc: 'Ưu đãi mùa hè',
    },
    {
      category: honeymoon[0] || promos[1],
      emoji: '💕',
      title: 'Honeymoon',
      desc: 'Tuần trăng mật',
    },
    {
      category: family[0] || promos[2],
      emoji: '👨‍👩‍👧‍👦',
      title: 'Family Fun',
      desc: 'Gia đình',
    },
  ].filter((d) => d.category);

  return (
    <div className="mb-12 grid gap-4 md:grid-cols-3">
      {dealCategories.map((deal) => (
        <Link
          key={deal.category.id}
          href={`/offers?category=${deal.category.category}`}
          className="group flex items-center gap-4 rounded-2xl bg-gradient-to-r from-brand-forest-deep to-brand-forest p-5 text-brand-white transition hover:scale-[1.02]"
        >
          <span className="text-4xl">{deal.emoji}</span>
          <div>
            <p className="font-semibold">{deal.title}</p>
            <p className="text-sm text-brand-paper/70">{deal.desc}</p>
            <p className="mt-1 text-xs font-bold text-brand-sage-light">
              -{deal.category.discountPercent || deal.category.discountAmount}%
            </p>
          </div>
        </Link>
      ))}
    </div>
  );
}

// Main Offers Page
// FAQ Types
export type FaqCategory = 'BOOKING' | 'PAYMENT' | 'REFUND' | 'CANCELLATION' | 'POLICIES' | 'SERVICES' | 'FACILITIES' | 'TRANSPORT' | 'GENERAL';

export interface FaqItem {
  id: number;
  question: string;
  answer: string;
  category: FaqCategory;
  displayOrder: number;
  helpfulCount: number;
  notHelpfulCount: number;
}

// Enhanced FAQ Page
// Enhanced Contact Page
