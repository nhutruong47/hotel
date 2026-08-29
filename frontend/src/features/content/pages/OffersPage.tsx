"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

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
        activePromos.forEach((p: Promotion) => cats.add(p.category));
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
          proms = proms.filter((p: Promotion) => p.category === selectedCategory);
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

