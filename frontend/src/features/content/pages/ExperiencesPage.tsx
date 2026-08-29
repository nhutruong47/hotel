"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

import { experienceList } from "../data";
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

