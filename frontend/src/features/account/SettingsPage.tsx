'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useSettings, Theme, Currency, Lang } from '../../shared/settings/SettingsContext';
import { useTranslation } from '../../shared/i18n/hooks';
import { api, API_PATHS } from '../../shared/api/client';

function SectionCard({ title, description, children }: { title: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)] sm:p-8">
      <div className="mb-6 border-b border-brand-ink/6 pb-5">
        <h2 className="text-xl font-semibold text-brand-charcoal">{title}</h2>
        {description && <p className="mt-1 text-sm text-brand-ink/60">{description}</p>}
      </div>
      {children}
    </div>
  );
}

function ToggleGroup<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string; icon?: React.ReactNode }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="flex gap-2 flex-wrap">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={`flex items-center gap-2 rounded-full px-5 py-2.5 text-sm font-medium transition-all ${
            value === opt.value
              ? 'bg-brand-forest text-brand-white shadow-sm'
              : 'border border-brand-stone text-brand-ink/70 hover:bg-brand-ink/4 hover:text-brand-ink'
          }`}
        >
          {opt.icon}
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function Toggle({ label, description, checked, onChange }: { label: string; description?: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <div className="flex items-center justify-between gap-4 py-4 first:pt-0 last:pb-0">
      <div>
        <p className="text-sm font-semibold text-brand-charcoal">{label}</p>
        {description && <p className="mt-0.5 text-xs text-brand-ink/55">{description}</p>}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${checked ? 'bg-brand-forest' : 'bg-brand-stone'}`}
      >
        <span className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-brand-white shadow transition-transform ${checked ? 'translate-x-5' : 'translate-x-0'}`} />
      </button>
    </div>
  );
}

export function SettingsPage() {
  const { settings, updateSetting } = useSettings();
  const { t } = useTranslation();
  const [saved, setSaved] = useState(false);

  const preferencesQuery = useQuery({
    queryKey: ['profile-preferences'],
    queryFn: () => api.get<Partial<typeof settings> & { language?: Lang }>(API_PATHS.profilePreferences),
    retry: false,
  });

  const savePreferences = useMutation({
    mutationFn: (payload: typeof settings) => api.put(API_PATHS.profilePreferences, {
      theme: payload.theme,
      language: payload.lang,
      currency: payload.currency,
      emailBooking: payload.emailBooking,
      emailReminders: payload.emailReminders,
      emailMarketing: payload.emailMarketing,
      smsBooking: payload.smsBooking,
    }),
  });

  useEffect(() => {
    const pref = preferencesQuery.data;
    if (!pref) return;
    if (pref.theme && pref.theme !== settings.theme) updateSetting('theme', pref.theme as Theme);
    const lang = (pref.language ?? pref.lang) as Lang | undefined;
    if (lang && lang !== settings.lang) updateSetting('lang', lang);
    if (pref.currency && pref.currency !== settings.currency) updateSetting('currency', pref.currency as Currency);
    (['emailBooking', 'emailReminders', 'emailMarketing', 'smsBooking'] as const).forEach((key) => {
      if (typeof pref[key] === 'boolean' && pref[key] !== settings[key]) {
        updateSetting(key, pref[key]);
      }
    });
  }, [preferencesQuery.data]);

  function update<K extends keyof typeof settings>(key: K, value: typeof settings[K]) {
    const next = { ...settings, [key]: value };
    updateSetting(key, value);
    savePreferences.mutate(next);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  }

  return (
    <div>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('settings.subtitle')}</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">{t('settings.title')}</h1>
      </div>

      {saved && (
        <div className="mb-4 flex items-center gap-2 rounded-2xl bg-brand-forest/10 px-5 py-3.5 text-sm text-brand-forest">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
          {t('settings.saved')}
        </div>
      )}

      <div className="space-y-6">
        {/* ── Appearance ── */}
        <SectionCard title={t('settings.appearance')} description={t('settings.appearanceDesc')}>
          <ToggleGroup<Theme>
            value={settings.theme}
            onChange={(v) => update('theme', v)}
            options={[
              { value: 'light',  label: t('settings.light'),  icon: <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg> },
              { value: 'dark',   label: t('settings.dark'),   icon: <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg> },
              { value: 'system', label: t('settings.system'), icon: <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg> },
            ]}
          />
        </SectionCard>

        {/* ── Language & Currency ── */}
        <SectionCard title={t('settings.language') + ' & ' + t('settings.currency')} description={t('settings.langCurrDesc')}>
          <div className="grid gap-6 sm:grid-cols-2">
            <div>
              <p className="mb-3 text-sm font-semibold text-brand-charcoal">{t('settings.language')}</p>
              <ToggleGroup<Lang>
                value={settings.lang}
                onChange={(v) => update('lang', v)}
                options={[
                  { value: 'vi', label: 'Tiếng Việt' },
                  { value: 'en', label: 'English' },
                ]}
              />
            </div>
            <div>
              <p className="mb-3 text-sm font-semibold text-brand-charcoal">{t('settings.currency')}</p>
              <ToggleGroup<Currency>
                value={settings.currency}
                onChange={(v) => update('currency', v)}
                options={[
                  { value: 'VND', label: 'VND ₫' },
                  { value: 'USD', label: 'USD $' },
                ]}
              />
            </div>
          </div>
        </SectionCard>

        {/* ── Notifications ── */}
        <SectionCard title={t('settings.notifications')} description={t('settings.notificationsDesc')}>
          <div className="divide-y divide-brand-ink/6">
            <Toggle
              label={t('settings.bookingConfirm')}
              description={t('settings.bookingConfirmDesc')}
              checked={settings.emailBooking}
              onChange={(v) => update('emailBooking', v)}
            />
            <Toggle
              label={t('settings.upcomingStay')}
              description={t('settings.upcomingStayDesc')}
              checked={settings.emailReminders}
              onChange={(v) => update('emailReminders', v)}
            />
            <Toggle
              label={t('settings.smsAlerts')}
              description={t('settings.smsAlertsDesc')}
              checked={settings.smsBooking}
              onChange={(v) => update('smsBooking', v)}
            />
            <Toggle
              label={t('settings.marketing')}
              description={t('settings.marketingDesc')}
              checked={settings.emailMarketing}
              onChange={(v) => update('emailMarketing', v)}
            />
          </div>
        </SectionCard>


      </div>
    </div>
  );
}
