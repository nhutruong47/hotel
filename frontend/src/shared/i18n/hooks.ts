'use client';

import { useSettings } from '../settings/SettingsContext';
import { translations } from './translations';

type Translations = typeof translations.en;
type NestedKeyOf<ObjectType extends object> = {
  [Key in keyof ObjectType & (string | number)]: ObjectType[Key] extends object
    ? `${Key}.${NestedKeyOf<ObjectType[Key]>}`
    : `${Key}`;
}[keyof ObjectType & (string | number)];

export function useTranslation() {
  const { settings } = useSettings();
  const lang = settings.lang;
  const t = (key: NestedKeyOf<Translations>): string => {
    const keys = key.split('.');
    let current: any = translations[lang];
    for (const k of keys) {
      if (current[k] === undefined) {
        // Fallback to English
        let fallback: any = translations.en;
        for (const fk of keys) {
          if (fallback[fk] === undefined) return key;
          fallback = fallback[fk];
        }
        return fallback;
      }
      current = current[k];
    }
    return current;
  };
  return { t, lang };
}

export function useCurrency() {
  const { settings } = useSettings();

  const formatCurrency = (value: number | string | undefined): string => {
    if (value == null) return '—';
    const num = typeof value === 'string' ? Number(value) : value;
    if (!Number.isFinite(num)) return '—';

    if (settings.currency === 'USD') {
      const usdValue = num / 25000;
      return new Intl.NumberFormat(settings.lang === 'vi' ? 'vi-VN' : 'en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
      }).format(usdValue);
    }

    return new Intl.NumberFormat(settings.lang === 'vi' ? 'vi-VN' : 'en-US', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(num);
  };

  return { formatCurrency, currency: settings.currency };
}
