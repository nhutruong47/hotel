"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

export const AboutPage = () => {
      const timeline = [
        { year: '2018', event: 'Khá»Ÿi cÃ´ng xÃ¢y dá»±ng dá»± Ã¡n Nhu Villas' },
        { year: '2019', event: 'Khai trÆ°Æ¡ng 3 villa Ä‘áº§u tiÃªn' },
        { year: '2020', event: 'Má»Ÿ rá»™ng thÃªm 2 villa vÃ  khu vá»±c dining' },
        { year: '2021', event: 'Giáº£i thÆ°á»Ÿng Luxury Boutique Resort' },
        { year: '2022', event: 'Ra máº¯t dá»‹ch vá»¥ private dining' },
        { year: '2023', event: 'Chá»©ng nháº­n Green Hospitality' },
        { year: '2024', event: 'Má»Ÿ rá»™ng khu vá»±c spa vÃ  wellness' },
      ];

      const facilities = [
        { icon: 'ðŸŠ', name: 'Private Pool', desc: 'Há»“ bÆ¡i riÃªng cho má»—i villa' },
        { icon: 'ðŸ½ï¸', name: 'Private Dining', desc: 'Bá»¯a Äƒn Ä‘Æ°á»£c náº¥u theo yÃªu cáº§u' },
        { icon: 'ðŸ’†', name: 'Spa & Wellness', desc: 'Tráº£i nghiá»‡m thÆ° giÃ£n Ä‘Ã­ch thá»±c' },
        { icon: 'ðŸš´', name: 'Bike Tours', desc: 'KhÃ¡m phÃ¡ xung quanh báº±ng xe Ä‘áº¡p' },
        { icon: 'ðŸ³', name: 'Breakfast Service', desc: 'Bá»¯a sÃ¡ng táº¡i ban cÃ´ng riÃªng' },
        { icon: 'ðŸš—', name: 'Airport Transfer', desc: 'ÄÆ°a Ä‘Ã³n sÃ¢n bay riÃªng' },
        { icon: 'ðŸ§¹', name: 'Housekeeping', desc: 'Dá»n phÃ²ng hÃ ng ngÃ y' },
        { icon: 'ðŸŒ¿', name: 'Garden Walks', desc: 'Äi bá»™ trong vÆ°á»n tropical' },
      ];

      const awards = [
        { year: '2021', title: 'Best Boutique Resort', org: 'Travel + Leisure' },
        { year: '2022', title: 'Excellence Award', org: 'Booking.com' },
        { year: '2023', title: 'Green Hospitality', org: 'Sustainable Travel' },
        { year: '2024', title: 'Top 10 Villas', org: 'CondÃ© Nast Traveler' },
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
                  who knows your name before you arrive. Our approach to service is quiet but present â€” available when needed, 
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
                <div className="mb-4 text-4xl">ðŸŒ¿</div>
                <h3 className="text-2xl text-brand-charcoal">Privacy First</h3>
                <p className="mt-4 text-sm leading-7 text-brand-ink/62">
                  Every product decision should make the guest feel informed, unhurried, and cared for. 
                  We never compromise on the space and solitude that our guests deserve.
                </p>
              </div>
              <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
                <div className="mb-4 text-4xl">ðŸ¡</div>
                <h3 className="text-2xl text-brand-charcoal">Natural Materials</h3>
                <p className="mt-4 text-sm leading-7 text-brand-ink/62">
                  Stone, wood, linen, and water. Our architecture draws from the landscape it sits in, 
                  creating spaces that feel rooted rather than imported.
                </p>
              </div>
              <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
                <div className="mb-4 text-4xl">âœ¨</div>
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
                  <div className="text-4xl">ðŸ†</div>
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
                <span className="text-2xl">â˜€ï¸</span>
                <p className="mt-2 font-semibold">100% Renewable Energy</p>
              </div>
              <div className="rounded-xl bg-brand-white/10 p-4">
                <span className="text-2xl">ðŸŒ±</span>
                <p className="mt-2 font-semibold">Zero Single-Use Plastics</p>
              </div>
              <div className="rounded-xl bg-brand-white/10 p-4">
                <span className="text-2xl">â™»ï¸</span>
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

