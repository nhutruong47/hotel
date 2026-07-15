'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';

export type Theme = 'light' | 'dark' | 'system';
export type Currency = 'VND' | 'USD';
export type Lang = 'vi' | 'en';

export type Settings = {
  theme: Theme;
  currency: Currency;
  lang: Lang;
  emailBooking: boolean;
  emailReminders: boolean;
  emailMarketing: boolean;
  smsBooking: boolean;
};

const STORAGE_KEY = 'nhu.account.settings';

const defaultSettings: Settings = {
  theme: 'system',
  currency: 'VND',
  lang: 'vi',
  emailBooking: true,
  emailReminders: true,
  emailMarketing: false,
  smsBooking: false,
};

function loadSettings(): Settings {
  if (typeof window === 'undefined') return defaultSettings;
  try {
    return { ...defaultSettings, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') };
  } catch {
    return defaultSettings;
  }
}

type SettingsContextType = {
  settings: Settings;
  updateSetting: <K extends keyof Settings>(key: K, value: Settings[K]) => void;
};

const SettingsContext = createContext<SettingsContextType | null>(null);

export function SettingsProvider({ children }: { children: React.ReactNode }) {
  const [settings, setSettings] = useState<Settings>(defaultSettings);
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    setIsMounted(true);
    const s = loadSettings();
    setSettings(s);
    applyTheme(s.theme);
    applyLang(s.lang);
  }, []);

  function applyTheme(theme: Theme) {
    const isDark = theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  function applyLang(lang: Lang) {
    document.documentElement.lang = lang;
  }

  const updateSetting = <K extends keyof Settings>(key: K, value: Settings[K]) => {
    setSettings((prev) => {
      const next = { ...prev, [key]: value };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));

      if (key === 'theme') {
        applyTheme(value as Theme);
      } else if (key === 'lang') {
        applyLang(value as Lang);
      }

      return next;
    });
  };

  return (
    <SettingsContext.Provider value={{ settings, updateSetting }}>
      {!isMounted ? (
        <div style={{ opacity: 0 }}>{children}</div>
      ) : (
        children
      )}
    </SettingsContext.Provider>
  );
}

export function useSettings() {
  const context = useContext(SettingsContext);
  if (!context) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
}
