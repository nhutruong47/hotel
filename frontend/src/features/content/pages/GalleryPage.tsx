"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

import { galleryList } from "../data";
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

