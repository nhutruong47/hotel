"use client";
import { useState, useMemo } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS } from "../../../shared/api/client";
import { useCurrency, useTranslation } from "../../../shared/i18n/hooks";
import {
  ContentShell,
  Promotion,
  getCategoryLabels,
  PromotionCard,
  FeaturedBanner,
  PromoCodeValidator,
  SeasonalDealsBanner,
} from "../shared";

const VIP_TICKETS = [
  {
    code: "WELCOME10",
    badge: "10% OFF",
    titleVi: "Đặc Quyền Chào Mừng Thành Viên",
    titleEn: "First Sanctuary Welcome Privilege",
    descVi: "Áp dụng cho mọi hạng biệt thự, không giới hạn số đêm lưu trú.",
    descEn: "Valid across all villa categories with no minimum night restriction.",
    tier: "MEMBER PASS",
  },
  {
    code: "NHUVILLAS20",
    badge: "20% OFF",
    titleVi: "Thẻ Độc Quyền Nhu Villas Signature",
    titleEn: "Nhu Villas Signature Pass",
    descVi: "Giảm ngay 20% trên giá phòng gốc kèm đặc quyền nhận phòng sớm.",
    descEn: "Instant 20% off nightly rates plus complimentary early check-in.",
    tier: "BLACK TIER",
  },
  {
    code: "VIP100",
    badge: "-$100",
    titleVi: "Tín Dụng Nghỉ Dưỡng Trực Tiếp $100",
    titleEn: "$100 Direct Sanctuary Credit",
    descVi: "Khấu trừ trực tiếp $100 (2.500.000 ₫) vào tổng hóa đơn đặt phòng.",
    descEn: "Deducts $100 directly from your reservation total at checkout.",
    tier: "GOLD CREDIT",
  },
  {
    code: "HONEYMOON20",
    badge: "20% + WINE",
    titleVi: "Gói Trăng Mật & Kỷ Niệm Lãng Mạn",
    titleEn: "Honeymoon & Anniversary Curation",
    descVi: "Ưu đãi 20% từ 2 đêm kèm rượu vang đỏ và bàn tiệc nến bên hồ bơi.",
    descEn: "20% off 2+ nights with complimentary vintage wine and poolside setup.",
    tier: "ROMANCE PASS",
  },
];

export const OffersPage = () => {
  const { t, lang } = useTranslation();
  const { formatCurrency } = useCurrency();
  const isVi = lang === "vi";

  const [activeTab, setActiveTab] = useState<"active" | "upcoming" | "expired">("active");
  const [selectedCategory, setSelectedCategory] = useState<string>("all");
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Interactive Savings Simulator State
  const [simRate, setSimRate] = useState<number>(720);
  const [simNights, setSimNights] = useState<number>(3);
  const [simDiscountPct, setSimDiscountPct] = useState<number>(20);

  const { data: activePromos = [], isLoading: loadingActive } = useQuery({
    queryKey: ["promotions", "active"],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.all),
    staleTime: 60_000,
  });

  const { data: featuredPromos = [] } = useQuery({
    queryKey: ["promotions", "featured"],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.featured),
    staleTime: 60_000,
  });

  const { data: upcomingPromos = [], isLoading: loadingUpcoming } = useQuery({
    queryKey: ["promotions", "upcoming"],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.upcoming),
    staleTime: 60_000,
  });

  const { data: expiredPromos = [], isLoading: loadingExpired } = useQuery({
    queryKey: ["promotions", "expired"],
    queryFn: () => api.get<Promotion[]>(API_PATHS.promotions.expired),
    staleTime: 60_000,
  });

  const categories = useMemo(() => {
    const cats = new Set<string>();
    activePromos.forEach((p: Promotion) => cats.add(p.category));
    return Array.from(cats);
  }, [activePromos]);

  const displayedPromos = useMemo(() => {
    let proms =
      activeTab === "active"
        ? activePromos
        : activeTab === "upcoming"
        ? upcomingPromos
        : expiredPromos;

    if (selectedCategory !== "all") {
      proms = proms.filter((p: Promotion) => p.category === selectedCategory);
    }
    return proms;
  }, [activeTab, activePromos, upcomingPromos, expiredPromos, selectedCategory]);

  const isLoading =
    activeTab === "active"
      ? loadingActive
      : activeTab === "upcoming"
      ? loadingUpcoming
      : loadingExpired;

  const featuredPromo = featuredPromos[0];

  const simOriginal = simRate * simNights;
  const simSaved = Math.round((simOriginal * simDiscountPct) / 100);
  const simFinal = simOriginal - simSaved;

  const handleCopy = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2500);
  };

  return (
    <ContentShell
      eyebrow={isVi ? "Đặc Quyền & Ưu Đãi" : "Curated Privileges"}
      title={isVi ? "Đặc Quyền Cho Kỳ Nghỉ Chậm." : "Seasonal stay notes."}
      copy={
        isVi
          ? "Những gói ưu đãi và thẻ thành viên được thiết kế riêng theo mùa, độ dài kỳ nghỉ và cảm xúc lưu trú tại Hồ Tuyền Lâm."
          : "Offers are curated by the stay team and adjusted to booking intent, length, and availability."
      }
    >
      {/* 1. VIP Perforated Boarding-Pass Voucher Vault */}
      <div className="mb-14 rounded-[2.2rem] bg-brand-forest-deep p-6 text-brand-white shadow-[0_28px_90px_rgba(12,28,22,0.24)] sm:p-10">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">
              🎟️ {isVi ? "Kho Thẻ Mời & Voucher Tức Thì" : "Instant Privilege Pass Vault"}
            </p>
            <h2 className="mt-2 font-serif text-3xl text-white sm:text-4xl">
              {isVi ? "Chạm Để Sao Chép Mã & Sử Dụng Ngay Khi Đặt Phòng" : "Tap Any Pass to Copy & Redeem at Checkout"}
            </h2>
          </div>
          <Link
            href="/villas"
            className="rounded-full bg-brand-paper px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.15em] text-brand-forest-deep transition hover:bg-white"
          >
            {isVi ? "Chọn Biệt Thự Ngay →" : "Choose Sanctuary →"}
          </Link>
        </div>

        <div className="grid gap-5 md:grid-cols-2">
          {VIP_TICKETS.map((ticket) => {
            const isCopied = copiedCode === ticket.code;
            return (
              <div
                key={ticket.code}
                className="group relative flex flex-col justify-between overflow-hidden rounded-[1.5rem] border border-white/15 bg-white/8 p-6 backdrop-blur-md transition hover:border-amber-300/60 hover:bg-white/12 sm:flex-row sm:items-center"
              >
                <div className="pr-4">
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-amber-300/20 px-3 py-0.5 text-[0.62rem] font-bold uppercase tracking-[0.2em] text-amber-300">
                      {ticket.tier}
                    </span>
                    <span className="rounded-full bg-white/15 px-2.5 py-0.5 text-xs font-bold text-white">
                      {ticket.badge}
                    </span>
                  </div>
                  <h3 className="mt-3 font-serif text-2xl text-white">
                    {isVi ? ticket.titleVi : ticket.titleEn}
                  </h3>
                  <p className="mt-1.5 text-xs leading-relaxed text-brand-paper/75">
                    {isVi ? ticket.descVi : ticket.descEn}
                  </p>
                </div>

                <div className="mt-5 flex shrink-0 flex-col items-start gap-2 border-t border-dashed border-white/20 pt-4 sm:mt-0 sm:items-end sm:border-l sm:border-t-0 sm:pl-5 sm:pt-0">
                  <span className="font-mono text-lg font-bold tracking-widest text-amber-300">
                    {ticket.code}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleCopy(ticket.code)}
                    className={`rounded-full px-4 py-2 text-[0.68rem] font-bold uppercase tracking-wider transition ${
                      isCopied
                        ? "bg-emerald-400 text-brand-forest-deep"
                        : "bg-white text-brand-forest-deep hover:bg-amber-300"
                    }`}
                  >
                    {isCopied
                      ? isVi
                        ? "✓ Đã sao chép"
                        : "✓ Copied"
                      : isVi
                      ? "Sao chép mã"
                      : "Copy Code"}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 2. Interactive Stay Savings Simulator */}
      <div className="mb-14 grid items-center gap-8 rounded-[2.2rem] bg-brand-paper p-6 shadow-[0_20px_70px_rgba(32,52,43,0.08)] lg:grid-cols-[1.1fr_0.9fr] sm:p-9">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
            🧮 {isVi ? "Trình Mô Phỏng Ưu Đãi" : "Interactive Savings Calculator"}
          </p>
          <h2 className="mt-2 font-serif text-3xl text-brand-charcoal sm:text-4xl">
            {isVi ? "Ước Tính Mức Tiết Kiệm Cho Kỳ Nghỉ Của Bạn" : "Calculate Your Direct Booking Privilege"}
          </h2>

          <div className="mt-6 space-y-5">
            <div>
              <div className="flex justify-between text-xs font-semibold text-brand-charcoal">
                <span>{isVi ? "1. Chọn Hạng Biệt Thự" : "1. Select Villa Tier"}</span>
                <span className="text-brand-forest">{formatCurrency(simRate)} / {isVi ? "đêm" : "night"}</span>
              </div>
              <div className="mt-2.5 grid grid-cols-4 gap-2">
                {[
                  { name: "Pavilion", rate: 520 },
                  { name: "Pool Villa", rate: 450 },
                  { name: "Courtyard", rate: 720 },
                  { name: "Residence", rate: 1150 },
                ].map((tier) => (
                  <button
                    key={tier.name}
                    type="button"
                    onClick={() => setSimRate(tier.rate)}
                    className={`rounded-xl border py-2.5 text-xs font-semibold transition ${
                      simRate === tier.rate
                        ? "border-brand-forest bg-brand-forest text-white"
                        : "border-brand-stone bg-brand-white text-brand-charcoal hover:border-brand-forest"
                    }`}
                  >
                    {tier.name}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="flex justify-between text-xs font-semibold text-brand-charcoal">
                <span>{isVi ? "2. Số Đêm Lưu Trú" : "2. Length of Stay"}</span>
                <span className="text-brand-forest">{simNights} {isVi ? "đêm" : "nights"}</span>
              </div>
              <input
                type="range"
                min={1}
                max={10}
                value={simNights}
                onChange={(e) => setSimNights(Number(e.target.value))}
                className="mt-2 w-full accent-brand-forest"
              />
            </div>

            <div>
              <div className="flex justify-between text-xs font-semibold text-brand-charcoal">
                <span>{isVi ? "3. Gói Đặc Quyền Áp Dụng" : "3. Privilege Tier"}</span>
                <span className="text-brand-forest">-{simDiscountPct}%</span>
              </div>
              <div className="mt-2 grid grid-cols-3 gap-2">
                {[
                  { label: "WELCOME10 (-10%)", pct: 10 },
                  { label: "SUMMER15 (-15%)", pct: 15 },
                  { label: "HONEYMOON20 (-20%)", pct: 20 },
                ].map((opt) => (
                  <button
                    key={opt.pct}
                    type="button"
                    onClick={() => setSimDiscountPct(opt.pct)}
                    className={`rounded-xl border py-2 text-xs font-semibold transition ${
                      simDiscountPct === opt.pct
                        ? "border-brand-forest bg-brand-forest text-white"
                        : "border-brand-stone bg-brand-white text-brand-charcoal hover:border-brand-forest"
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="rounded-[1.75rem] bg-brand-white p-7 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
            {isVi ? "Kết Quả Ước Tính" : "Estimated Privilege Summary"}
          </p>
          <dl className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between text-brand-ink/65">
              <dt>{isVi ? "Giá niêm yết gốc" : "Standard Tariff"}</dt>
              <dd className="line-through">{formatCurrency(simOriginal)}</dd>
            </div>
            <div className="flex justify-between font-semibold text-emerald-700">
              <dt>{isVi ? "Số tiền tiết kiệm được" : "Direct Privilege Savings"}</dt>
              <dd>-{formatCurrency(simSaved)}</dd>
            </div>
            <div className="flex justify-between border-t border-brand-stone pt-4 text-base font-bold text-brand-charcoal">
              <dt>{isVi ? "Chi phí sau ưu đãi" : "Net Sanctuary Tariff"}</dt>
              <dd className="font-serif text-3xl text-brand-forest">{formatCurrency(simFinal)}</dd>
            </div>
          </dl>
          <Link
            href="/villas"
            className="mt-6 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
          >
            {isVi ? "Chọn Biệt Thự Với Mức Giá Này →" : "Lock In This Privilege →"}
          </Link>
        </div>
      </div>

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
          {(["active", "upcoming", "expired"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                activeTab === tab
                  ? "bg-brand-forest text-brand-white"
                  : "text-brand-ink/62 hover:text-brand-charcoal"
              }`}
            >
              {tab === "active"
                ? isVi
                  ? "Đang áp dụng"
                  : "Active"
                : tab === "upcoming"
                ? isVi
                  ? "Sắp diễn ra"
                  : "Upcoming"
                : isVi
                ? "Đã kết thúc"
                : "Expired"}
            </button>
          ))}
        </div>

        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="h-11 rounded-full border border-brand-stone bg-brand-paper px-4 text-sm focus:border-brand-forest focus:outline-none"
        >
          <option value="all">{isVi ? "Tất cả danh mục" : "All Categories"}</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {getCategoryLabels(t)[cat as keyof ReturnType<typeof getCategoryLabels>]}
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
          {displayedPromos.map((promo: Promotion) => (
            <PromotionCard key={promo.id} promo={promo} />
          ))}
        </div>
      ) : (
        <div className="rounded-[2rem] bg-brand-paper p-16 text-center shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
          <h3 className="text-3xl text-brand-charcoal">
            {activeTab === "expired" ? "Chưa có chương trình hết hạn" : "Chưa có ưu đãi trong mục này"}
          </h3>
        </div>
      )}

      {/* Promo Code Validator */}
      <div className="mt-16">
        <PromoCodeValidator />
      </div>
    </ContentShell>
  );
};
