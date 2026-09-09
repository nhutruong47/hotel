"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

interface FaqItem { id: number; question: string; answer: string; category: string; helpfulCount?: number; notHelpfulCount?: number; displayOrder: number; isActive: boolean; }
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
        SERVICES: 'Dá»‹ch vá»¥',
        FACILITIES: 'Tiện ích',
        TRANSPORT: 'Di chuyá»ƒn',
        GENERAL: 'Chung',
      };

      const filteredFaqs = useMemo(() => {
        let result = searchQuery
          ? faqs.filter(
              (f: FaqItem) =>
                f.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
                f.answer.toLowerCase().includes(searchQuery.toLowerCase())
            )
          : selectedCategory === 'all'
          ? faqs
          : faqs.filter((f: FaqItem) => f.category === selectedCategory);
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
              {filteredFaqs.map((faq: FaqItem) => (
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

