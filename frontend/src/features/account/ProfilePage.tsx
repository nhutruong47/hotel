'use client';

import { useEffect, useMemo, useState } from 'react';
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
  phone?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  nationality?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
  emergencyContactRelation?: string | null;
  createdAt?: string;
};

type PasswordState = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
    reader.onerror = () => reject(reader.error ?? new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

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

function FormField({ label, children, hint }: { label: string; children: React.ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="text-sm font-semibold text-brand-charcoal">{label}</span>
      {children}
      {hint && <p className="mt-1.5 text-xs text-brand-ink/50">{hint}</p>}
    </label>
  );
}

const inputCls = 'mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-charcoal placeholder:text-brand-ink/40 focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest transition-all';
const selectCls = 'mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-charcoal focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest transition-all';

export function ProfilePage() {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.get<Profile>(API_PATHS.profile),
    retry: false,
  });

  const profile = profileQuery.data;

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState('');
  const [nationality, setNationality] = useState('');
  
  const [emergencyContactName, setEmergencyContactName] = useState('');
  const [emergencyContactPhone, setEmergencyContactPhone] = useState('');
  const [emergencyContactRelation, setEmergencyContactRelation] = useState('');

  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);
  const [savedOk, setSavedOk] = useState(false);

  const [pwState, setPwState] = useState<PasswordState>({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwOk, setPwOk] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      setFullName(profile.fullName ?? '');
      setEmail(profile.email ?? '');
      setPhone(profile.phone ?? '');
      setDateOfBirth(profile.dateOfBirth ?? '');
      setGender(profile.gender ?? '');
      setNationality(profile.nationality ?? '');
      setEmergencyContactName(profile.emergencyContactName ?? '');
      setEmergencyContactPhone(profile.emergencyContactPhone ?? '');
      setEmergencyContactRelation(profile.emergencyContactRelation ?? '');
    }
  }, [profile]);

  const avatarUrl = useMemo(() => {
    if (avatarPreview) return avatarPreview;
    if (!profile?.avatarFilename) return null;
    return `/uploads/${profile.avatarFilename}`;
  }, [avatarPreview, profile?.avatarFilename]);

  const displayName = profile?.fullName || profile?.username || '';

  const updateMutation = useMutation<Profile, ApiError, FormData>({
    mutationFn: (fd) => api.put<Profile>(API_PATHS.profile, undefined, { body: fd }),
    onSuccess: (data) => {
      setSavedOk(true);
      setSavedMsg('Profile updated successfully.');
      setAvatarFile(null);
      setAvatarPreview(null);
      queryClient.setQueryData(['profile'], data);
      queryClient.invalidateQueries({ queryKey: ['session'] });
      setTimeout(() => setSavedMsg(null), 4000);
    },
    onError: (err) => {
      setSavedOk(false);
      setSavedMsg(err.message || 'Failed to update profile.');
    },
  });

  const pwMutation = useMutation<{ message: string }, ApiError, PasswordState>({
    mutationFn: (payload) => api.put<{ message: string }>(API_PATHS.profilePassword, payload),
    onSuccess: (data) => {
      setPwError(null);
      setPwOk(data.message || 'Password updated successfully.');
      setPwState({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setTimeout(() => setPwOk(null), 4000);
    },
    onError: (err) => {
      setPwOk(null);
      setPwError(err.message || 'Failed to update password.');
    },
  });

  function handleAvatarChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) { setAvatarFile(null); setAvatarPreview(null); return; }
    setAvatarFile(file);
    readFileAsDataUrl(file).then(setAvatarPreview).catch(() => setAvatarPreview(null));
  }

  function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSavedMsg(null);
    if (!profile) return;
    const fd = new FormData();
    fd.append('fullName', fullName);
    fd.append('email', email);
    fd.append('phone', phone);
    fd.append('dateOfBirth', dateOfBirth);
    fd.append('gender', gender);
    fd.append('nationality', nationality);
    fd.append('emergencyContactName', emergencyContactName);
    fd.append('emergencyContactPhone', emergencyContactPhone);
    fd.append('emergencyContactRelation', emergencyContactRelation);
    if (avatarFile) fd.append('avatar', avatarFile);
    updateMutation.mutate(fd);
  }

  function handlePasswordSave(e: React.FormEvent) {
    e.preventDefault();
    setPwError(null); setPwOk(null);
    if (!pwState.currentPassword || !pwState.newPassword) {
      setPwError('Please fill in all password fields.'); return;
    }
    if (pwState.newPassword.length < 8) {
      setPwError('New password must be at least 8 characters.'); return;
    }
    if (pwState.newPassword !== pwState.confirmPassword) {
      setPwError('Passwords do not match.'); return;
    }
    pwMutation.mutate(pwState);
  }

  if (profileQuery.isLoading || !profile) {
    return (
      <div className="space-y-4">
        {[1, 2].map((i) => (
          <div key={i} className="h-64 animate-pulse rounded-2xl bg-brand-paper" />
        ))}
      </div>
    );
  }

  return (
    <div>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{t('profile.subtitle')}</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">{t('profile.title')}</h1>
      </div>

      <div className="space-y-6">
        {/* ── Personal information ── */}
        <form onSubmit={handleSave}>
          <SectionCard
            title="Personal Information"
            description="Your private details — only visible to you and our team."
          >
            {/* Avatar row */}
            <div className="mb-6 flex items-center gap-5">
              <div className="shrink-0">
                {avatarUrl ? (
                  <img src={avatarUrl} alt="Avatar" className="h-20 w-20 rounded-full object-cover ring-2 ring-brand-forest/25" />
                ) : (
                  <div className="flex h-20 w-20 items-center justify-center rounded-full bg-brand-forest text-3xl font-semibold text-brand-white">
                    {displayName[0]?.toUpperCase() || 'U'}
                  </div>
                )}
              </div>
              <div>
                <p className="text-sm font-semibold text-brand-charcoal">{displayName}</p>
                <p className="text-xs text-brand-ink/50">
                  {profile.role === 'ADMIN' ? 'Administrator' : 'Guest Member'}
                </p>
                <label className="mt-3 inline-flex cursor-pointer items-center gap-2 rounded-full border border-brand-stone px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-brand-ink transition hover:bg-brand-ink/5">
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
                  Change Photo
                  <input type="file" accept="image/*" onChange={handleAvatarChange} className="sr-only" />
                </label>
              </div>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <FormField label={t('profile.name')}>
                <input value={fullName} onChange={(e) => setFullName(e.target.value)} required className={inputCls} placeholder="Your full name" />
              </FormField>
              <FormField label={t('profile.email')} hint={profile.emailVerified === false ? '⚠ Email not verified. A new verification link will be sent if you change it.' : undefined}>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required className={inputCls} placeholder="your@email.com" />
              </FormField>
              <FormField label={t('profile.phone')}>
                <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} className={inputCls} placeholder="+84 90 123 4567" />
              </FormField>
              <FormField label="Date of Birth">
                <input type="date" value={dateOfBirth} onChange={(e) => setDateOfBirth(e.target.value)} className={inputCls} />
              </FormField>
              <FormField label="Gender">
                <select value={gender} onChange={(e) => setGender(e.target.value)} className={selectCls}>
                  <option value="">Prefer not to say</option>
                  <option value="male">Male</option>
                  <option value="female">Female</option>
                  <option value="other">Other</option>
                </select>
              </FormField>
              <FormField label="Nationality">
                <select value={nationality} onChange={(e) => setNationality(e.target.value)} className={selectCls}>
                  <option value="">Select Nationality</option>
                  <option value="Viet Nam">Viet Nam</option>
                  <option value="United States">United States</option>
                  <option value="United Kingdom">United Kingdom</option>
                  <option value="Australia">Australia</option>
                  <option value="Canada">Canada</option>
                  <option value="Japan">Japan</option>
                  <option value="South Korea">South Korea</option>
                  <option value="China">China</option>
                  <option value="Singapore">Singapore</option>
                  <option value="Other">Other</option>
                </select>
              </FormField>
            </div>

            <div className="mt-8 mb-6 border-b border-brand-ink/6 pb-4">
              <h3 className="text-lg font-semibold text-brand-charcoal">Emergency Contact</h3>
              <p className="mt-1 text-sm text-brand-ink/60">Required for luxury stays and special requests.</p>
            </div>
            <div className="grid gap-5 sm:grid-cols-2">
              <FormField label="Contact Name">
                <input value={emergencyContactName} onChange={(e) => setEmergencyContactName(e.target.value)} className={inputCls} placeholder="e.g. Jane Doe" />
              </FormField>
              <FormField label="Contact Phone">
                <input type="tel" value={emergencyContactPhone} onChange={(e) => setEmergencyContactPhone(e.target.value)} className={inputCls} placeholder="+84 90 123 4567" />
              </FormField>
              <FormField label="Relationship">
                <select value={emergencyContactRelation} onChange={(e) => setEmergencyContactRelation(e.target.value)} className={selectCls}>
                  <option value="">Select Relationship</option>
                  <option value="Spouse">Spouse</option>
                  <option value="Parent">Parent</option>
                  <option value="Sibling">Sibling</option>
                  <option value="Friend">Friend</option>
                  <option value="Other">Other</option>
                </select>
              </FormField>
            </div>

            {savedMsg && (
              <p className={`mt-5 rounded-[1rem] px-5 py-3.5 text-sm ${savedOk ? 'bg-brand-forest/10 text-brand-forest' : 'bg-red-50 text-red-700'}`}>
                {savedMsg}
              </p>
            )}

            <div className="mt-6 flex items-center gap-3">
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="rounded-full bg-brand-forest px-7 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
              >
                {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
              </button>
              <p className="text-xs text-brand-ink/40">
                Username: <span className="font-medium text-brand-ink/70">{profile.username}</span>
              </p>
            </div>
          </SectionCard>
        </form>

        {/* ── Security ── */}
        <form onSubmit={handlePasswordSave}>
          <SectionCard
            title="Security"
            description="Keep your account safe with a strong password."
          >
            <div className="grid gap-5 sm:grid-cols-3">
              <FormField label="Current Password">
                <input type="password" value={pwState.currentPassword} onChange={(e) => setPwState({ ...pwState, currentPassword: e.target.value })} autoComplete="current-password" className={inputCls} placeholder="••••••••" />
              </FormField>
              <FormField label="New Password">
                <input type="password" value={pwState.newPassword} onChange={(e) => setPwState({ ...pwState, newPassword: e.target.value })} autoComplete="new-password" className={inputCls} placeholder="Min. 8 characters" />
              </FormField>
              <FormField label="Confirm New Password">
                <input type="password" value={pwState.confirmPassword} onChange={(e) => setPwState({ ...pwState, confirmPassword: e.target.value })} autoComplete="new-password" className={inputCls} placeholder="Re-enter password" />
              </FormField>
            </div>

            {/* Password strength indicator */}
            {pwState.newPassword && (
              <div className="mt-4">
                <div className="flex gap-1">
                  {[
                    pwState.newPassword.length >= 8,
                    /[A-Z]/.test(pwState.newPassword),
                    /[0-9]/.test(pwState.newPassword),
                    /[^A-Za-z0-9]/.test(pwState.newPassword),
                  ].map((passed, i) => (
                    <div key={i} className={`h-1 flex-1 rounded-full transition-all ${passed ? 'bg-brand-forest' : 'bg-brand-stone'}`} />
                  ))}
                </div>
                <p className="mt-1.5 text-xs text-brand-ink/50">
                  {[pwState.newPassword.length >= 8, /[A-Z]/.test(pwState.newPassword), /[0-9]/.test(pwState.newPassword), /[^A-Za-z0-9]/.test(pwState.newPassword)].filter(Boolean).length} / 4 requirements met
                </p>
              </div>
            )}

            {pwError && <p className="mt-4 rounded-[1rem] bg-red-50 px-5 py-3.5 text-sm text-red-700">{pwError}</p>}
            {pwOk   && <p className="mt-4 rounded-[1rem] bg-brand-forest/10 px-5 py-3.5 text-sm text-brand-forest">{pwOk}</p>}

            <button
              type="submit"
              disabled={pwMutation.isPending}
              className="mt-6 rounded-full bg-brand-forest px-7 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
            >
              {pwMutation.isPending ? 'Updating…' : 'Update Password'}
            </button>
          </SectionCard>
        </form>

        {/* ── Account Info (read-only) ── */}
        <SectionCard title="Account Information">
          <dl className="grid gap-4 sm:grid-cols-2">
            {[
              { label: 'Username',     value: profile.username },
              { label: t('profile.role'),         value: profile.role === 'ADMIN' ? 'Administrator' : 'Guest Member' },
              { label: 'Email Status', value: profile.emailVerified ? '✓ Verified' : '⚠ Not Verified' },
              { label: t('profile.memberSince'), value: profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' }) : '—' },
            ].map(({ label, value }) => (
              <div key={label}>
                <dt className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/40">{label}</dt>
                <dd className="mt-1 text-sm text-brand-charcoal">{value}</dd>
              </div>
            ))}
          </dl>
        </SectionCard>
      </div>
    </div>
  );
}
