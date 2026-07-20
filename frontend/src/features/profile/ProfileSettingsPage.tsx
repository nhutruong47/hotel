'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useTranslation } from '../../shared/i18n/hooks';

type Profile = {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  avatarFilename?: string | null;
  avatarUrl?: string | null;
  emailVerified?: boolean;
};

type PasswordFormState = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

const EMPTY_PROFILE: Partial<Profile> = {
  fullName: '',
  email: '',
  avatarFilename: null,
};

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
    reader.onerror = () => reject(reader.error ?? new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

export const ProfileSettingsPage = () => {
  const { t } = useTranslation();
  const router = useRouter();
  const queryClient = useQueryClient();

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.get<Profile>(API_PATHS.profile),
    retry: false,
  });

  useEffect(() => {
    if (profileQuery.isFetched && profileQuery.isError) {
      router.push('/login');
    }
  }, [profileQuery.isFetched, profileQuery.isError, router]);

  const profile = profileQuery.data;
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const [passwordState, setPasswordState] = useState<PasswordFormState>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSaved, setPasswordSaved] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      setFullName(profile.fullName ?? '');
      setEmail(profile.email ?? '');
    }
  }, [profile]);

  const avatarUrl = useMemo(() => {
    if (avatarPreview) return avatarPreview;
    if (!profile?.avatarFilename) return null;
    return `/uploads/${profile.avatarFilename}`;
  }, [avatarPreview, profile?.avatarFilename]);

  const updateProfile = useMutation<Profile, ApiError, FormData>({
    mutationFn: async (formData) => api.put<Profile>(API_PATHS.profile, undefined, { body: formData }),
    onSuccess: (data) => {
      setSavedMessage(t('settings.profileUpdated'));
      setAvatarFile(null);
      setAvatarPreview(null);
      queryClient.setQueryData(['profile'], data);
    },
    onError: (err) => setSavedMessage(err.message),
  });

  const changePassword = useMutation<{ message: string }, ApiError, PasswordFormState>({
    mutationFn: async (payload) => api.put<{ message: string }>(API_PATHS.profilePassword, payload),
    onSuccess: (data) => {
      setPasswordError(null);
      setPasswordSaved(data.message || t('settings.passwordUpdated'));
      setPasswordState({ currentPassword: '', newPassword: '', confirmPassword: '' });
    },
    onError: (err) => {
      setPasswordSaved(null);
      setPasswordError(err.message || t('settings.passwordUpdateFailed'));
    },
  });

  function handleAvatarChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      setAvatarFile(null);
      setAvatarPreview(null);
      return;
    }
    setAvatarFile(file);
    readFileAsDataUrl(file)
      .then(setAvatarPreview)
      .catch(() => setAvatarPreview(null));
  }

  function handleProfileSave(e: React.FormEvent) {
    e.preventDefault();
    setSavedMessage(null);
    if (!profile) return;
    if (!fullName.trim() || !email.trim()) {
      setSavedMessage(t('validation.nameEmailReq'));
      return;
    }
    const formData = new FormData();
    formData.append('fullName', fullName);
    formData.append('email', email);
    if (avatarFile) {
      formData.append('avatar', avatarFile);
    }
    updateProfile.mutate(formData);
  }

  function handlePasswordSave(e: React.FormEvent) {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSaved(null);
    const { currentPassword, newPassword, confirmPassword } = passwordState;
    if (!currentPassword || !newPassword) {
      setPasswordError(t('validation.passwordReq'));
      return;
    }
    if (newPassword.length < 8) {
      setPasswordError(t('validation.passwordMin'));
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError(t('validation.passwordMismatch'));
      return;
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(newPassword)) {
      setPasswordError(t('validation.passwordAlphaNum'));
      return;
    }
    changePassword.mutate({ currentPassword, newPassword, confirmPassword });
  }

  if (profileQuery.isLoading || !profile) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-3xl rounded-[2rem] bg-brand-paper p-10 text-center text-brand-ink/58">
          {t('common.loading')}
        </div>
      </section>
    );
  }

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[980px]">
        <Link href="/profile" className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest">
          &larr; {t('common.backToProfile')}
        </Link>
        <h1 className="mt-6 max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-6xl">
          {t('settings.accountSettings')}
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-brand-ink/64">
          {t('settings.accountSettingsDesc')}
        </p>

        <div className="mt-10 grid gap-8 lg:grid-cols-2">
          <form
            className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_20px_70px_rgba(32,52,43,0.08)] sm:p-8"
            onSubmit={handleProfileSave}
          >
            <h2 className="text-2xl text-brand-charcoal">{t('settings.profileInfo')}</h2>

            <div className="mt-6 flex items-center gap-5">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt="Avatar preview"
                  className="h-20 w-20 rounded-full object-cover ring-2 ring-brand-forest/30"
                />
              ) : (
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-brand-forest text-3xl text-brand-white">
                  {(profile.fullName || profile.username).charAt(0).toUpperCase()}
                </div>
              )}
              <label className="flex-1">
                <span className="text-sm font-semibold text-brand-charcoal">{t('settings.avatarOpt')}</span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="mt-2 block w-full text-xs file:mr-4 file:rounded-full file:border-0 file:bg-brand-forest file:px-4 file:py-2 file:text-[11px] file:font-semibold file:uppercase file:tracking-[0.14em] file:text-brand-white"
                />
                {avatarFile ? (
                  <p className="mt-2 text-xs text-brand-ink/58">
                    {t('settings.avatarPreview')} {avatarFile.name} ({Math.round(avatarFile.size / 1024)} KB)
                  </p>
                ) : null}
              </label>
            </div>

            <label className="mt-6 block">
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.fullName')}</span>
              <input
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              />
            </label>
            <label className="mt-4 block">
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.email')}</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              />
              {profile.emailVerified === false ? (
                <p className="mt-2 text-xs text-brand-coral">{t('settings.emailUnverified')}</p>
              ) : null}
            </label>

            {savedMessage ? (
              <p className="mt-4 rounded-[1rem] bg-brand-forest/10 px-4 py-3 text-sm text-brand-forest">
                {savedMessage}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={updateProfile.isPending}
              className="mt-6 min-h-12 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition disabled:opacity-50"
            >
              {updateProfile.isPending ? t('common.submitting') : t('common.saveChanges')}
            </button>
          </form>

          <form
            className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_20px_70px_rgba(32,52,43,0.08)] sm:p-8"
            onSubmit={handlePasswordSave}
          >
            <h2 className="text-2xl text-brand-charcoal">{t('settings.changePassword')}</h2>
            <p className="mt-3 text-sm text-brand-ink/62">
              {t('settings.passwordReq')}
            </p>

            <label className="mt-6 block">
              <span className="text-sm font-semibold text-brand-charcoal">{t('settings.currentPassword')}</span>
              <input
                type="password"
                value={passwordState.currentPassword}
                onChange={(e) => setPasswordState({ ...passwordState, currentPassword: e.target.value })}
                autoComplete="current-password"
                className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              />
            </label>
            <label className="mt-4 block">
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.newPassword')}</span>
              <input
                type="password"
                value={passwordState.newPassword}
                onChange={(e) => setPasswordState({ ...passwordState, newPassword: e.target.value })}
                autoComplete="new-password"
                className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              />
            </label>
            <label className="mt-4 block">
              <span className="text-sm font-semibold text-brand-charcoal">{t('forms.confirmPassword')}</span>
              <input
                type="password"
                value={passwordState.confirmPassword}
                onChange={(e) => setPasswordState({ ...passwordState, confirmPassword: e.target.value })}
                autoComplete="new-password"
                className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5"
              />
              <p className="mt-2 text-xs text-brand-ink/58">
                {passwordState.confirmPassword && passwordState.confirmPassword === passwordState.newPassword
                  ? t('validation.passwordMatchStatus')
                  : t('validation.passwordMismatch')}
              </p>
            </label>

            {passwordError ? (
              <p className="mt-4 rounded-[1rem] bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral">
                {passwordError}
              </p>
            ) : null}
            {passwordSaved ? (
              <p className="mt-4 rounded-[1rem] bg-brand-forest/10 px-4 py-3 text-sm text-brand-forest">
                {passwordSaved}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={changePassword.isPending}
              className="mt-6 min-h-12 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition disabled:opacity-50"
            >
              {changePassword.isPending ? t('common.submitting') : t('settings.updatePassword')}
            </button>
          </form>
        </div>
      </div>
    </section>
  );
}
