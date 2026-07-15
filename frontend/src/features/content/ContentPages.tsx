'use client';

import { useState, useEffect, useMemo, type ReactNode } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useTranslation } from '../../shared/i18n/hooks';

function ContentShell({
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
type PromotionCategory = 'SEASONAL' | 'WEEKEND' | 'HOLIDAY' | 'HONEYMOON' | 'FAMILY' | 'EARLY_BIRD' | 'LAST_MINUTE' | 'LOYALTY' | 'LONG_STAY' | 'SPECIAL';

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

interface PromotionPreview {
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
function CountdownTimer({ endDate }: { endDate: string }) {
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
const getCategoryLabels = (t: any): Record<PromotionCategory, string> => ({
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

const categoryColors: Record<PromotionCategory, string> = {
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
function PromotionCard({ promo }: { promo: Promotion }) {
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
function FeaturedBanner({ promo }: { promo: Promotion }) {
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
function PromoCodeValidator() {
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
function SeasonalDealsBanner({ promos }: { promos: Promotion[] }) {
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
export const OffersPage = () => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'active' | 'upcoming' | 'expired'>('active');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  const { data: activePromos = [], isLoading: loadingActive } = useQuery({
    queryKey: ['promotions', 'active'],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.all),
    staleTime: 60_000,
  });

  const { data: featuredPromos = [] } = useQuery({
    queryKey: ['promotions', 'featured'],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.featured),
    staleTime: 60_000,
  });

  const { data: upcomingPromos = [], isLoading: loadingUpcoming } = useQuery({
    queryKey: ['promotions', 'upcoming'],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.upcoming),
    staleTime: 60_000,
  });

  const { data: expiredPromos = [], isLoading: loadingExpired } = useQuery({
    queryKey: ['promotions', 'expired'],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.expired),
    staleTime: 60_000,
  });

  const categories = useMemo(() => {
    const cats = new Set<string>();
    activePromos.forEach((p) => cats.add(p.category));
    return Array.from(cats);
  }, [activePromos]);

  const displayedPromos = useMemo(() => {
    let proms =
      activeTab === 'active'
        ? activePromos
        : activeTab === 'upcoming'
        ? upcomingPromos
        : expiredPromos;

    if (selectedCategory !== 'all') {
      proms = proms.filter((p) => p.category === selectedCategory);
    }
    return proms;
  }, [activeTab, activePromos, upcomingPromos, expiredPromos, selectedCategory]);

  const isLoading = activeTab === 'active' ? loadingActive : activeTab === 'upcoming' ? loadingUpcoming : loadingExpired;

  const featuredPromo = featuredPromos[0];

  return (
    <ContentShell
      eyebrow="Offers"
      title="Seasonal stay notes."
      copy="Offers are curated by the stay team and adjusted to booking intent, length, and availability."
    >
      {/* Featured Banner */}
      {featuredPromo && (
        <div className="mb-10">
          <FeaturedBanner promo={featuredPromo} />
        </div>
      )}

      {/* Seasonal Deals Banner */}
      <SeasonalDealsBanner promos={activePromos} />

      {/* Tabs & Category Filter */}
      <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
        <div className="flex gap-2 rounded-full bg-brand-paper p-1">
          {(['active', 'upcoming', 'expired'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                activeTab === tab
                  ? 'bg-brand-forest text-brand-white'
                  : 'text-brand-ink/62 hover:text-brand-charcoal'
              }`}
            >
              {tab === 'active' ? 'Active' : tab === 'upcoming' ? 'Upcoming' : 'Expired'}
            </button>
          ))}
        </div>

        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="h-11 rounded-full border border-brand-stone bg-brand-paper px-4 text-sm focus:border-brand-forest focus:outline-none"
        >
          <option value="all">All Categories</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {getCategoryLabels(t)[cat as PromotionCategory]}
            </option>
          ))}
        </select>
      </div>

      {/* Promotions Grid */}
      {isLoading ? (
        <div className="grid gap-7 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="rounded-[2rem] bg-brand-paper p-2">
              <div className="aspect-[16/9] animate-pulse rounded-[1.55rem] bg-brand-stone" />
              <div className="p-6">
                <div className="h-8 w-3/4 animate-pulse rounded-full bg-brand-stone" />
                <div className="mt-3 h-4 w-full animate-pulse rounded-full bg-brand-stone" />
                <div className="mt-2 h-4 w-5/6 animate-pulse rounded-full bg-brand-stone" />
              </div>
            </div>
          ))}
        </div>
      ) : displayedPromos.length > 0 ? (
        <div className="grid gap-7 md:grid-cols-2 lg:grid-cols-3">
          {displayedPromos.map((promo) => (
            <PromotionCard key={promo.id} promo={promo} />
          ))}
        </div>
      ) : (
        <div className="rounded-[2rem] bg-brand-paper p-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <h3 className="text-3xl text-brand-charcoal">
            {activeTab === 'expired' ? 'No expired promotions' : 'No promotions found'}
          </h3>
          <p className="mt-3 text-brand-ink/62">
            {activeTab === 'upcoming'
              ? 'Check back soon for new offers!'
              : activeTab === 'expired'
              ? 'Past promotions will be shown here'
              : 'Try selecting a different category'}
          </p>
        </div>
      )}

      {/* Promo Code Validator */}
      <div className="mt-16">
        <PromoCodeValidator />
      </div>

      {/* CTA */}
      <div className="mt-10 text-center">
        <Link
          href="/villas"
          className="inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
        >
          Explore villas
        </Link>
      </div>
    </ContentShell>
  );
};

export const AboutPage = () => {
  const timeline = [
    { year: '2018', event: 'Khởi công xây dựng dự án Nhu Villas' },
    { year: '2019', event: 'Khai trương 3 villa đầu tiên' },
    { year: '2020', event: 'Mở rộng thêm 2 villa và khu vực dining' },
    { year: '2021', event: 'Giải thưởng Luxury Boutique Resort' },
    { year: '2022', event: 'Ra mắt dịch vụ private dining' },
    { year: '2023', event: 'Chứng nhận Green Hospitality' },
    { year: '2024', event: 'Mở rộng khu vực spa và wellness' },
  ];

  const facilities = [
    { icon: '🏊', name: 'Private Pool', desc: 'Hồ bơi riêng cho mỗi villa' },
    { icon: '🍽️', name: 'Private Dining', desc: 'Bữa ăn được nấu theo yêu cầu' },
    { icon: '💆', name: 'Spa & Wellness', desc: 'Trải nghiệm thư giãn đích thực' },
    { icon: '🚴', name: 'Bike Tours', desc: 'Khám phá xung quanh bằng xe đạp' },
    { icon: '🍳', name: 'Breakfast Service', desc: 'Bữa sáng tại ban công riêng' },
    { icon: '🚗', name: 'Airport Transfer', desc: 'Đưa đón sân bay riêng' },
    { icon: '🧹', name: 'Housekeeping', desc: 'Dọn phòng hàng ngày' },
    { icon: '🌿', name: 'Garden Walks', desc: 'Đi bộ trong vườn tropical' },
  ];

  const awards = [
    { year: '2021', title: 'Best Boutique Resort', org: 'Travel + Leisure' },
    { year: '2022', title: 'Excellence Award', org: 'Booking.com' },
    { year: '2023', title: 'Green Hospitality', org: 'Sustainable Travel' },
    { year: '2024', title: 'Top 10 Villas', org: 'Condé Nast Traveler' },
  ];

  return (
    <ContentShell
      eyebrow="About"
      title="Private hospitality, shaped slowly."
      copy="Nhu Villas is a direct booking experience for private villa stays built around calm design, clear rules, and quiet service."
    >
      {/* Our Story */}
      <section className="mb-16">
        <h2 className="mb-8 text-4xl text-brand-charcoal">Our Story</h2>
        <div className="grid gap-8 lg:grid-cols-2">
          <div>
            <p className="text-base leading-8 text-brand-ink/74">
              Nhu Villas was born from a simple belief: luxury hospitality should feel unhurried, private, and deeply personal. 
              Founded in 2018, we set out to create a retreat where guests could disconnect from the noise and reconnect 
              with what matters most.
            </p>
            <p className="mt-4 text-base leading-8 text-brand-ink/62">
              Every villa is designed with natural materials, oriented toward light and landscape, and staffed by a team 
              who knows your name before you arrive. Our approach to service is quiet but present — available when needed, 
              invisible when not.
            </p>
          </div>
          <div className="overflow-hidden rounded-[2rem]">
            <img
              src="/images/nhu-villa-interior.jpg"
              alt="Nhu Villas interior"
              className="h-full w-full object-cover"
            />
          </div>
        </div>
      </section>

      {/* Brand Philosophy */}
      <section className="mb-16">
        <h2 className="mb-8 text-4xl text-brand-charcoal">Brand Philosophy</h2>
        <div className="grid gap-6 md:grid-cols-3">
          <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
            <div className="mb-4 text-4xl">🌿</div>
            <h3 className="text-2xl text-brand-charcoal">Privacy First</h3>
            <p className="mt-4 text-sm leading-7 text-brand-ink/62">
              Every product decision should make the guest feel informed, unhurried, and cared for. 
              We never compromise on the space and solitude that our guests deserve.
            </p>
          </div>
          <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
            <div className="mb-4 text-4xl">🏡</div>
            <h3 className="text-2xl text-brand-charcoal">Natural Materials</h3>
            <p className="mt-4 text-sm leading-7 text-brand-ink/62">
              Stone, wood, linen, and water. Our architecture draws from the landscape it sits in, 
              creating spaces that feel rooted rather than imported.
            </p>
          </div>
          <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
            <div className="mb-4 text-4xl">✨</div>
            <h3 className="text-2xl text-brand-charcoal">Hosted Details</h3>
            <p className="mt-4 text-sm leading-7 text-brand-ink/62">
              Service without ceremony. A stocked terrace when you arrive, a quiet breakfast when you wake, 
              a recommendation when you ask. Nothing forced, everything considered.
            </p>
          </div>
        </div>
      </section>

      {/* Facilities */}
      <section className="mb-16">
        <h2 className="mb-8 text-4xl text-brand-charcoal">Included Facilities</h2>
        <div className="grid gap-px overflow-hidden rounded-[2rem] bg-brand-stone sm:grid-cols-2 lg:grid-cols-4">
          {facilities.map((facility) => (
            <div key={facility.name} className="bg-brand-paper p-6">
              <span className="text-3xl">{facility.icon}</span>
              <h3 className="mt-3 text-lg font-semibold text-brand-charcoal">{facility.name}</h3>
              <p className="mt-1 text-sm text-brand-ink/58">{facility.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Timeline */}
      <section className="mb-16">
        <h2 className="mb-8 text-4xl text-brand-charcoal">Our Journey</h2>
        <div className="relative">
          <div className="absolute left-4 top-0 h-full w-px bg-brand-stone lg:left-1/2 lg:-translate-x-px" />
          <div className="space-y-8">
            {timeline.map((item, index) => (
              <div key={item.year} className={`relative flex items-center gap-6 lg:${index % 2 === 0 ? 'justify-end' : 'justify-start'} lg:pr-[calc(50%+2rem)]`}>
                <div className={`relative z-10 flex h-8 w-8 items-center justify-center rounded-full bg-brand-forest text-sm font-bold text-brand-white lg:absolute lg:left-1/2 lg:-translate-x-1/2`}>
                  {item.year.slice(-2)}
                </div>
                <div className="flex-1 rounded-[1.5rem] bg-brand-paper p-5 shadow-[0_12px_40px_rgba(32,52,43,0.06)] lg:flex-none lg:text-right">
                  <span className="text-xs font-semibold text-brand-sage">{item.year}</span>
                  <p className="mt-1 text-brand-charcoal">{item.event}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Awards */}
      <section className="mb-16">
        <h2 className="mb-8 text-4xl text-brand-charcoal">Recognition</h2>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {awards.map((award) => (
            <div key={`${award.year}-${award.title}`} className="rounded-[1.5rem] bg-brand-sand p-6 text-center">
              <div className="text-4xl">🏆</div>
              <p className="mt-3 font-serif text-2xl text-brand-charcoal">{award.title}</p>
              <p className="mt-1 text-xs text-brand-ink/58">{award.org}</p>
              <p className="mt-2 text-sm font-semibold text-brand-sage">{award.year}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Sustainability */}
      <section className="mb-16 rounded-[2rem] bg-brand-forest-deep p-8 text-brand-white">
        <h2 className="text-3xl text-brand-white">Sustainability Commitment</h2>
        <p className="mt-4 max-w-2xl text-brand-paper/74">
          We are committed to minimizing our environmental footprint while maximizing the quality of our guests' experience. 
          From solar-powered amenities to locally-sourced ingredients, every decision considers its impact on the world around us.
        </p>
        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl bg-brand-white/10 p-4">
            <span className="text-2xl">☀️</span>
            <p className="mt-2 font-semibold">100% Renewable Energy</p>
          </div>
          <div className="rounded-xl bg-brand-white/10 p-4">
            <span className="text-2xl">🌱</span>
            <p className="mt-2 font-semibold">Zero Single-Use Plastics</p>
          </div>
          <div className="rounded-xl bg-brand-white/10 p-4">
            <span className="text-2xl">♻️</span>
            <p className="mt-2 font-semibold">Local Sourcing</p>
          </div>
        </div>
      </section>

      {/* Location */}
      <section className="rounded-[2rem] bg-brand-paper p-8 shadow-[0_22px_70px_rgba(32,52,43,0.08)]">
        <h2 className="text-3xl text-brand-charcoal">Location</h2>
        <div className="mt-6 grid gap-8 lg:grid-cols-2">
          <div>
            <p className="text-base leading-8 text-brand-ink/74">
              Nhu Villas is nestled in a tranquil landscape, offering easy access to local attractions while 
              maintaining the privacy and serenity that define our guests' experience. Exact directions 
              are shared after booking confirmation.
            </p>
            <dl className="mt-6 grid gap-4 sm:grid-cols-3">
              <div className="rounded-xl bg-brand-sand p-4">
                <dt className="text-2xl font-serif text-brand-forest">35 min</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/52">Airport</dd>
              </div>
              <div className="rounded-xl bg-brand-sand p-4">
                <dt className="text-2xl font-serif text-brand-forest">12 min</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/52">Local Dining</dd>
              </div>
              <div className="rounded-xl bg-brand-sand p-4">
                <dt className="text-2xl font-serif text-brand-forest">8 min</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/52">Garden Walks</dd>
              </div>
            </dl>
          </div>
          <div className="overflow-hidden rounded-[1.5rem]">
            <img
              src="/images/nhu-location-landscape.jpg"
              alt="Location"
              className="h-full w-full object-cover"
            />
          </div>
        </div>
      </section>
    </ContentShell>
  );
};

// FAQ Types
type FaqCategory = 'BOOKING' | 'PAYMENT' | 'REFUND' | 'CANCELLATION' | 'POLICIES' | 'SERVICES' | 'FACILITIES' | 'TRANSPORT' | 'GENERAL';

interface FaqItem {
  id: number;
  question: string;
  answer: string;
  category: FaqCategory;
  displayOrder: number;
  helpfulCount: number;
  notHelpfulCount: number;
}

// Enhanced FAQ Page
export const FAQPage = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [openFaqId, setOpenFaqId] = useState<number | null>(null);

  const { data: faqs = [], isLoading } = useQuery({
    queryKey: ['faqs'],
    queryFn: () => api.get<FaqItem[]>(API_PATHS.faqs.all),
    staleTime: 60_000,
  });

  const { data: groupedFaqs = {} } = useQuery({
    queryKey: ['faqs', 'grouped'],
    queryFn: () => api.get<Record<string, FaqItem[]>>(API_PATHS.faqs.grouped),
    staleTime: 60_000,
  });

  const categoryLabels: Record<string, string> = {
    BOOKING: 'Đặt phòng',
    PAYMENT: 'Thanh toán',
    REFUND: 'Hoàn tiền',
    CANCELLATION: 'Hủy phòng',
    POLICIES: 'Chính sách',
    SERVICES: 'Dịch vụ',
    FACILITIES: 'Tiện ích',
    TRANSPORT: 'Di chuyển',
    GENERAL: 'Chung',
  };

  const filteredFaqs = useMemo(() => {
    let result = searchQuery
      ? faqs.filter(
          (f) =>
            f.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
            f.answer.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : selectedCategory === 'all'
      ? faqs
      : faqs.filter((f) => f.category === selectedCategory);
    return result;
  }, [faqs, searchQuery, selectedCategory]);

  const markHelpful = async (id: number, helpful: boolean) => {
    try {
      await api.post(`${API_PATHS.faqs.helpful(id)}?helpful=${helpful}`);
    } catch {
      // silently fail
    }
  };

  return (
    <ContentShell
      eyebrow="FAQ"
      title="Answers before arrival."
      copy="The public FAQ keeps booking confidence high and reduces avoidable support friction."
    >
      {/* Search */}
      <div className="mb-8">
        <div className="relative">
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search questions..."
            className="h-14 w-full rounded-full border border-brand-stone bg-brand-paper px-6 pl-14 text-sm focus:border-brand-forest focus:outline-none"
          />
          <span className="absolute left-5 top-1/2 -translate-y-1/2 text-brand-ink/40">🔍</span>
        </div>
      </div>

      {/* Category Pills */}
      <div className="mb-8 flex flex-wrap gap-2">
        <button
          onClick={() => setSelectedCategory('all')}
          className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
            selectedCategory === 'all'
              ? 'bg-brand-forest text-brand-white'
              : 'bg-brand-paper text-brand-ink/62 hover:bg-brand-forest/10'
          }`}
        >
          All
        </button>
        {Object.entries(categoryLabels).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setSelectedCategory(key)}
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
              selectedCategory === key
                ? 'bg-brand-forest text-brand-white'
                : 'bg-brand-paper text-brand-ink/62 hover:bg-brand-forest/10'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* FAQ List */}
      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="rounded-[1.5rem] bg-brand-paper p-6">
              <div className="h-6 w-3/4 animate-pulse rounded-full bg-brand-stone" />
              <div className="mt-3 h-4 w-full animate-pulse rounded-full bg-brand-stone" />
            </div>
          ))}
        </div>
      ) : filteredFaqs.length > 0 ? (
        <div className="space-y-4">
          {filteredFaqs.map((faq) => (
            <details
              key={faq.id}
              open={openFaqId === faq.id}
              onToggle={() => setOpenFaqId(openFaqId === faq.id ? null : faq.id)}
              className="rounded-[1.5rem] bg-brand-paper"
            >
              <summary className="cursor-pointer p-6 text-xl text-brand-charcoal marker:content-['']">
                <span className="mr-2 inline-block text-brand-sage">+</span>
                {faq.question}
              </summary>
              <div className="px-6 pb-6">
                <p className="pl-6 text-sm leading-7 text-brand-ink/62">{faq.answer}</p>
                <div className="mt-4 flex items-center gap-4 pl-6">
                  <span className="text-xs text-brand-ink/50">Was this helpful?</span>
                  <button
                    onClick={() => markHelpful(faq.id, true)}
                    className="flex items-center gap-1 rounded-full bg-brand-sage/20 px-3 py-1 text-xs text-brand-forest hover:bg-brand-sage/30"
                  >
                    👍 {faq.helpfulCount || 0}
                  </button>
                  <button
                    onClick={() => markHelpful(faq.id, false)}
                    className="flex items-center gap-1 rounded-full bg-brand-coral/10 px-3 py-1 text-xs text-brand-coral hover:bg-brand-coral/20"
                  >
                    👎 {faq.notHelpfulCount || 0}
                  </button>
                </div>
              </div>
            </details>
          ))}
        </div>
      ) : (
        <div className="rounded-[2rem] bg-brand-paper p-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <h3 className="text-3xl text-brand-charcoal">No results found</h3>
          <p className="mt-3 text-brand-ink/62">
            Try different keywords or{' '}
            <Link href="/contact" className="text-brand-forest underline">
              contact us
            </Link>{' '}
            for help.
          </p>
        </div>
      )}

      {/* Contact CTA */}
      <div className="mt-12 rounded-[2rem] bg-brand-forest-deep p-8 text-center text-brand-white">
        <h3 className="text-2xl text-brand-white">Still have questions?</h3>
        <p className="mt-2 text-brand-paper/72">Our team is here to help you.</p>
        <Link
          href="/contact"
          className="mt-6 inline-flex min-h-12 items-center justify-center rounded-full bg-brand-white px-8 py-4 text-sm font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition hover:bg-brand-paper"
        >
          Contact Us
        </Link>
      </div>
    </ContentShell>
  );
};

// Enhanced Contact Page
export const ContactPage = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [type, setType] = useState('GENERAL');
  const [success, setSuccess] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccess(null);

    if (name.trim().length < 2 || !email.includes('@') || message.trim().length < 5) {
      setErrorMsg('Please fill in all required fields correctly.');
      return;
    }

    try {
      const result = await api.post<{ success: boolean; message: string; ticketId: string }>(
        API_PATHS.contact,
        { name, email, phone, subject, message, type }
      );
      setSuccess(`${result.message} (${result.ticketId})`);
      setName('');
      setEmail('');
      setPhone('');
      setSubject('');
      setMessage('');
    } catch (err) {
      setErrorMsg('Không thể gửi tin nhắn. Vui lòng thử lại.');
    }
  };

  const contactTypes = [
    { value: 'GENERAL', label: 'Chung' },
    { value: 'RESERVATION', label: 'Đặt phòng' },
    { value: 'SUPPORT', label: 'Hỗ trợ' },
    { value: 'FEEDBACK', label: 'Phản hồi' },
    { value: 'PARTNERSHIP', label: 'Hợp tác' },
    { value: 'EMERGENCY', label: 'Khẩn cấp' },
  ];

  return (
    <ContentShell
      eyebrow="Contact"
      title="Speak with the stay team."
      copy="For exact address guidance, arrival timing, special occasions, or long-stay requests, contact the reservations team."
    >
      <div className="grid gap-8 lg:grid-cols-[0.8fr_1fr]">
        {/* Contact Info */}
        <div className="space-y-6">
          <div className="rounded-[2rem] bg-brand-paper p-7">
            <h2 className="text-3xl text-brand-charcoal">Reservations</h2>
            <p className="mt-5 text-brand-ink/64">reservations@nhuvillas.com</p>
            <p className="mt-2 text-brand-ink/64">Viet Nam</p>
          </div>

          <div className="rounded-[2rem] bg-brand-paper p-7">
            <h3 className="text-xl text-brand-charcoal">Business Hours</h3>
            <dl className="mt-4 space-y-2 text-sm text-brand-ink/64">
              <div className="flex justify-between">
                <dt>Monday - Friday</dt>
                <dd>08:00 - 20:00</dd>
              </div>
              <div className="flex justify-between">
                <dt>Saturday - Sunday</dt>
                <dd>09:00 - 18:00</dd>
              </div>
              <div className="flex justify-between border-t border-brand-stone/30 pt-2">
                <dt className="font-semibold text-brand-coral">Emergency</dt>
                <dd className="font-semibold text-brand-coral">24/7</dd>
              </div>
            </dl>
          </div>

          <div className="rounded-[2rem] bg-brand-paper p-7">
            <h3 className="text-xl text-brand-charcoal">Quick Contact</h3>
            <div className="mt-4 space-y-3">
              <a href="tel:+84123456789" className="flex items-center gap-3 text-brand-ink/64 hover:text-brand-forest">
                <span>📞</span> +84 123 456 789
              </a>
              <a href="mailto:reservations@nhuvillas.com" className="flex items-center gap-3 text-brand-ink/64 hover:text-brand-forest">
                <span>✉️</span> reservations@nhuvillas.com
              </a>
            </div>
          </div>

          <div className="rounded-[2rem] bg-brand-sand p-7">
            <h3 className="text-xl text-brand-charcoal">Need immediate help?</h3>
            <p className="mt-2 text-sm text-brand-ink/62">
              Check our{' '}
              <Link href="/faq" className="text-brand-forest underline">
                FAQ
              </Link>{' '}
              or use our AI chat assistant for instant support.
            </p>
          </div>
        </div>

        {/* Contact Form */}
        <form onSubmit={submit} className="space-y-5 rounded-[2rem] bg-brand-paper p-7">
          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-sm font-semibold text-brand-charcoal">
                Name <span className="text-brand-coral">*</span>
              </span>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
                className="h-14 rounded-full border border-brand-stone px-5"
              />
            </label>
            <label className="grid gap-2">
              <span className="text-sm font-semibold text-brand-charcoal">
                Email <span className="text-brand-coral">*</span>
              </span>
              <input
                required
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                className="h-14 rounded-full border border-brand-stone px-5"
              />
            </label>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-sm font-semibold text-brand-charcoal">Phone</span>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Phone number"
                className="h-14 rounded-full border border-brand-stone px-5"
              />
            </label>
            <label className="grid gap-2">
              <span className="text-sm font-semibold text-brand-charcoal">Type</span>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="h-14 rounded-full border border-brand-stone px-5"
              >
                {contactTypes.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="grid gap-2">
            <span className="text-sm font-semibold text-brand-charcoal">
              Subject <span className="text-brand-coral">*</span>
            </span>
            <input
              required
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="Subject"
              className="h-14 rounded-full border border-brand-stone px-5"
            />
          </label>

          <label className="grid gap-2">
            <span className="text-sm font-semibold text-brand-charcoal">
              Message <span className="text-brand-coral">*</span>
            </span>
            <textarea
              required
              minLength={5}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="How can we help?"
              className="min-h-36 rounded-[1.25rem] border border-brand-stone p-5"
            />
          </label>

          {errorMsg && (
            <p className="rounded-full bg-brand-coral/10 px-4 py-3 text-sm text-brand-ink">{errorMsg}</p>
          )}
          {success && (
            <p className="rounded-full bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest">{success}</p>
          )}

          <button
            type="submit"
            className="min-h-12 w-full rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
          >
            Send message
          </button>
        </form>
      </div>
    </ContentShell>
  );
};

const experienceList = [
  {
    title: 'Morning rituals',
    image: '/images/nhu-villa-interior.jpg',
    copy: 'Tea, water, and a quiet breakfast terrace before the day begins.',
  },
  {
    title: 'Private dining',
    image: '/images/nhu-private-dining.jpg',
    copy: 'Seasonal menus served where the evening light is at its softest.',
  },
  {
    title: 'Garden evenings',
    image: '/images/nhu-infinity-pool.jpg',
    copy: 'Poolside stillness, soft towels, and enough space to disappear for a while.',
  },
  {
    title: 'Forest walks',
    image: '/images/nhu-location-landscape.jpg',
    copy: 'Guided slow walks through the surrounding pine and tropical gardens.',
  },
];

export const ExperiencesPage = () => (
  <ContentShell
    eyebrow="Experiences"
    title="Days with a slower pulse."
    copy="Curated rituals, private dining, and quiet hours shaped by forest and water."
  >
    <div className="grid gap-7 md:grid-cols-2">
      {experienceList.map((item) => (
        <article key={item.title} className="overflow-hidden rounded-[2rem] bg-brand-paper shadow-[0_22px_70px_rgba(32,52,43,0.09)]">
          <img
            src={item.image}
            alt={item.title}
            className="h-64 w-full object-cover"
            loading="lazy"
            decoding="async"
          />
          <div className="p-7">
            <h2 className="text-3xl text-brand-charcoal">{item.title}</h2>
            <p className="mt-4 text-base leading-8 text-brand-ink/64">{item.copy}</p>
          </div>
        </article>
      ))}
    </div>
  </ContentShell>
);

const galleryList = [
  { src: '/images/nhu-villa-interior.jpg', alt: 'Villa bedroom with floor to ceiling windows and soft linen' },
  { src: '/images/nhu-infinity-pool.jpg', alt: 'Private infinity pool looking toward a calm landscape' },
  { src: '/images/nhu-garden-pool-villa.jpg', alt: 'Private pool villa exterior at golden hour' },
  { src: '/images/nhu-private-dining.jpg', alt: 'Refined private dining table with warm hospitality lighting' },
  { src: '/images/nhu-location-landscape.jpg', alt: 'Surrounding landscape with quiet road through pine trees' },
  { src: '/images/nhu-hero-villa-4k.jpg', alt: 'Hero view of villa in forest setting' },
];

export const GalleryPage = () => (
  <ContentShell
    eyebrow="Gallery"
    title="Light, water, texture."
    copy="A small collection of moments captured across the property."
  >
    <div className="grid gap-4 md:grid-cols-3">
      {galleryList.map((item, index) => (
        <img
          key={`${item.src}-${index}`}
          src={item.src}
          alt={item.alt}
          className="aspect-[4/3] w-full rounded-[1.5rem] object-cover"
          loading="lazy"
          decoding="async"
        />
      ))}
    </div>
  </ContentShell>
);

export const DiningPage = () => (
  <ContentShell
    eyebrow="Dining"
    title="Seasonal menus, served with care."
    copy="From quiet in-villa breakfasts to hosted private dinners, every meal is composed around the day."
  >
    <div className="grid gap-7 lg:grid-cols-[1fr_1.1fr] lg:items-center">
      <img
        src="/images/nhu-private-dining.jpg"
        alt="Private dining table set for an evening meal"
        className="h-full w-full rounded-[2rem] object-cover"
        loading="lazy"
        decoding="async"
      />
      <div className="grid gap-5">
        {[
          ['In-villa breakfast', 'Slow breakfasts prepared at your terrace at the hour that suits you.'],
          ['Hosted dinners', 'Seasonal multi-course menus paired with curated regional producers.'],
          ['Garden tea', 'Afternoon tea service on the pool deck or under the shaded terrace.'],
        ].map(([title, copy]) => (
          <div key={title} className="rounded-[1.5rem] bg-brand-paper p-6 shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
            <h2 className="text-2xl text-brand-charcoal">{title}</h2>
            <p className="mt-3 text-sm leading-7 text-brand-ink/64">{copy}</p>
          </div>
        ))}
      </div>
    </div>
  </ContentShell>
);
