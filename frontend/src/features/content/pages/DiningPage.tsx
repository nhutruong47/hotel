"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

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

