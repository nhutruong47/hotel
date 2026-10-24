'use client';

import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useSession, type SessionUser } from '../../shared/auth/SessionProvider';
import { useTranslation } from '../../shared/i18n/hooks';

type AuthResponse = SessionUser;
type RegisterResponse = { message: string; user: SessionUser };

/* ============================================================
   Avant-Garde Luxury Sanctuary Auth Shell
   Split-Screen Architectural Stage + Digital Keycard Access
   ============================================================ */

function AuthShell({
  eyebrow,
  headline,
  lead,
  perks,
  quickAccessSlot,
  children,
}: {
  eyebrow: string;
  headline: string;
  lead: string;
  perks?: string[];
  quickAccessSlot?: ReactNode;
  children: ReactNode;
}) {
  const { t, lang } = useTranslation();
  const isVi = lang === 'vi';

  return (
    <section className="relative min-h-[100dvh] overflow-hidden bg-brand-forest-deep text-brand-white">
      {/* Ambient architectural background */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-0">
        <img
          src="/images/nhu-hero-villa-4k.jpg"
          alt=""
          className="h-full w-full object-cover opacity-20 mix-blend-luminosity"
        />
        <div className="absolute inset-0 bg-gradient-to-br from-brand-forest-deep via-brand-forest-deep/95 to-black/90" />
        <div className="absolute -left-24 top-1/4 h-[440px] w-[440px] rounded-full bg-brand-sage/20 blur-[150px]" />
        <div className="absolute bottom-0 right-0 h-[360px] w-[360px] rounded-full bg-amber-300/10 blur-[140px]" />
      </div>

      <div className="relative mx-auto grid min-h-[100dvh] max-w-[1340px] gap-10 px-5 pb-16 pt-28 sm:px-8 lg:grid-cols-[1.08fr_0.92fr] lg:items-center lg:gap-14 lg:px-12 lg:pt-32">
        {/* LEFT STAGE: Architectural Dossier & Digital Keycards */}
        <div className="flex flex-col justify-between gap-8">
          <div>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <Link
                href="/"
                className="inline-flex items-center gap-3 rounded-full border border-white/15 bg-white/5 px-4 py-2 text-xs font-semibold uppercase tracking-[0.2em] text-brand-paper transition hover:bg-white/15"
              >
                &larr; {t('auth.backToHome')}
              </Link>
              <span className="rounded-full border border-amber-300/30 bg-amber-300/10 px-3.5 py-1 font-mono text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-amber-300">
                11.9038° N, 108.4368° E · 1,520M
              </span>
            </div>

            <p className="mt-8 text-xs font-semibold uppercase tracking-[0.26em] text-amber-300">
              ✦ {eyebrow}
            </p>
            <h1 className="mt-3 max-w-[14ch] font-serif text-4xl font-light leading-[1.05] tracking-tight text-white sm:text-6xl">
              {headline}
            </h1>
            <p className="mt-5 max-w-xl text-sm leading-7 text-brand-paper/75 sm:text-base">
              {lead}
            </p>
          </div>

          {/* 1-Click Digital Sanctuary Keycard Slot */}
          {quickAccessSlot}

          {perks && perks.length ? (
            <ul className="grid gap-3.5 sm:grid-cols-2">
              {perks.map((perk) => (
                <li
                  key={perk}
                  className="flex items-start gap-3 rounded-2xl border border-white/12 bg-white/6 p-4 backdrop-blur-md"
                >
                  <span
                    aria-hidden="true"
                    className="mt-0.5 inline-flex h-5 w-5 flex-none items-center justify-center rounded-full bg-amber-300 text-brand-forest-deep"
                  >
                    <svg viewBox="0 0 12 12" className="h-3 w-3" fill="none" stroke="currentColor" strokeWidth="2.2">
                      <path d="M2.5 6.2l2.4 2.4 4.6-4.6" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </span>
                  <span className="text-xs leading-relaxed text-brand-paper/85 sm:text-sm">{perk}</span>
                </li>
              ))}
            </ul>
          ) : null}

          <div className="hidden border-t border-white/12 pt-5 text-xs leading-6 text-brand-paper/60 lg:flex lg:items-center lg:justify-between">
            <span>
              🛎️ {isVi ? 'Tổng đài Quản gia 24/7:' : t('auth.conciergeLine')}{' '}
              <span className="font-semibold text-amber-300">concierge@nhuvillas.com</span>
            </span>
            <span>🔒 {t('auth.encrypted')}</span>
          </div>
        </div>

        {/* RIGHT STAGE: Interactive Sanctuary Access Portal */}
        <div className="relative">
          <div className="rounded-[2.4rem] border border-white/15 bg-brand-paper/95 p-2.5 text-brand-ink shadow-[0_32px_100px_rgba(0,0,0,0.45)] backdrop-blur-xl">
            <div className="rounded-[1.9rem] bg-brand-white p-6 sm:p-8 lg:p-10">{children}</div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ============================================================
   Form sub-components
   ============================================================ */

function TabSwitch({ active }: { active: 'login' | 'register' }) {
  const params = useSearchParams();
  const redirectTo = params.get('redirect');
  const query = redirectTo ? `?redirect=${encodeURIComponent(redirectTo)}` : '';

  const { t } = useTranslation();
  return (
    <div className="mb-7 flex w-full rounded-full border border-brand-stone bg-brand-paper p-1 text-xs font-semibold uppercase tracking-[0.15em]">
      <Link
        href={`/login${query}`}
        className={`flex-1 rounded-full py-2.5 text-center transition ${
          active === 'login' ? 'bg-brand-forest text-brand-white shadow-sm' : 'text-brand-ink/60 hover:text-brand-forest'
        }`}
      >
        {t('auth.login')}
      </Link>
      <Link
        href={`/register${query}`}
        className={`flex-1 rounded-full py-2.5 text-center transition ${
          active === 'register' ? 'bg-brand-forest text-brand-white shadow-sm' : 'text-brand-ink/60 hover:text-brand-forest'
        }`}
      >
        {t('auth.register')}
      </Link>
    </div>
  );
}

function FieldError({ children }: { children: ReactNode }) {
  return (
    <p className="mt-1.5 text-xs font-medium text-brand-coral" role="alert">
      {children}
    </p>
  );
}

function FieldLabel({ htmlFor: _htmlFor, children }: { htmlFor?: string; children: ReactNode }) {
  return (
    <span className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/70">
      {children}
    </span>
  );
}

function getPasswordScore(password: string): 0 | 1 | 2 | 3 | 4 {
  if (!password) return 0;
  let score = 0;
  if (password.length >= 8) score += 1;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;
  return score as 0 | 1 | 2 | 3 | 4;
}

function PasswordStrength({ score }: { score: ReturnType<typeof getPasswordScore> }) {
  const { t } = useTranslation();
  const labels = [t('auth.passStr0'), t('auth.passStr1'), t('auth.passStr2'), t('auth.passStr3'), t('auth.passStr4')] as const;
  const colors = ['bg-brand-stone', 'bg-brand-coral', 'bg-brand-sage-light', 'bg-brand-sage', 'bg-brand-forest'] as const;
  return (
    <div className="mt-2 flex items-center gap-2">
      <div className="flex flex-1 gap-1">
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className={`h-1.5 flex-1 rounded-full transition ${i < score ? colors[score] : 'bg-brand-stone/70'}`}
          />
        ))}
      </div>
      <span className="w-16 text-right text-[10px] font-semibold uppercase tracking-[0.12em] text-brand-ink/54">
        {labels[score]}
      </span>
    </div>
  );
}

function PasswordInput({
  id,
  value,
  onChange,
  placeholder,
  autoComplete,
  required,
  minLength,
}: {
  id: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
  minLength?: number;
}) {
  const [show, setShow] = useState(false);
  return (
    <div className="relative">
      <input
        id={id}
        type={show ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        required={required}
        minLength={minLength}
        autoComplete={autoComplete}
        placeholder={placeholder}
        className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 pl-5 pr-12 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
      />
      <button
        type="button"
        tabIndex={-1}
        onClick={() => setShow((s) => !s)}
        className="absolute right-4 top-1/2 -translate-y-1/2 text-brand-ink/40 transition hover:text-brand-forest"
        aria-label={show ? 'Hide password' : 'Show password'}
      >
        {show ? (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
        )}
      </button>
    </div>
  );
}

/* ============================================================
   Login Page with 1-Click Digital Keycards
   ============================================================ */

export const LoginPage = () => {
  const { t, lang } = useTranslation();
  const isVi = lang === 'vi';
  const router = useRouter();
  const { setUser, refresh } = useSession();
  const params = useSearchParams();
  const redirectTo = params.get('redirect') ?? '';
  const reason = params.get('reason') ?? '';

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [infoMsg, setInfoMsg] = useState<string | null>(
    reason === 'login_required'
      ? isVi
        ? 'Vui lòng đăng nhập hoặc chọn Thẻ Thành Viên Mẫu bên dưới để truy cập không gian riêng tư.'
        : 'Please sign in or select a Digital Keycard below to access your private sanctuary portal.'
      : reason === 'verify_email'
        ? isVi
          ? 'Tài khoản đã được khởi tạo! Quý khách có thể đăng nhập ngay bên dưới.'
          : 'Your account has been created! You may now sign in below.'
        : null
  );
  const [staySignedIn, setStaySignedIn] = useState(true);

  const mutation = useMutation<AuthResponse, ApiError, { user: string; pass: string }>({
    mutationFn: ({ user, pass }) =>
      api.post<AuthResponse>(API_PATHS.auth.login, { username: user.trim(), password: pass }),
    onSuccess: async (data) => {
      // Synchronously update session context so RequireAuth never bounces back to /login
      setUser(data);
      refresh().catch(() => undefined);

      const role = data?.role;
      const safeRedirect =
        redirectTo && redirectTo.startsWith('/') && !redirectTo.startsWith('//')
          ? redirectTo
          : null;

      if (safeRedirect) {
        router.push(safeRedirect);
      } else {
        router.push(['STAFF', 'MANAGER', 'ADMIN'].includes(role ?? '') ? '/admin' : '/account');
      }
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const handleInstantKeycard = (user: string, pass: string, autoSubmit = true) => {
    setErrorMsg(null);
    setInfoMsg(null);
    setIdentifier(user);
    setPassword(pass);
    if (autoSubmit && pass) {
      mutation.mutate({ user, pass });
    }
  };

  const canSubmit = identifier.trim().length >= 3 && password.length >= 1;

  const quickKeycardDeck = (
    <div className="rounded-[1.9rem] border border-amber-300/30 bg-white/8 p-5 backdrop-blur-md">
      <div className="flex items-center justify-between gap-2">
        <p className="text-[0.68rem] font-bold uppercase tracking-[0.22em] text-amber-300">
          🗝️ {isVi ? 'Chìa Khóa Trải Nghiệm Nhanh 1-Chạm (Digital Keycards)' : '1-Click Digital Sanctuary Keycards'}
        </p>
        <span className="rounded-full bg-emerald-400/20 px-2.5 py-0.5 text-[0.6rem] font-bold uppercase tracking-wider text-emerald-300">
          INSTANT ACCESS
        </span>
      </div>
      <p className="mt-1.5 text-xs text-brand-paper/75">
        {isVi
          ? 'Chạm vào thẻ thành viên dưới đây để tự động đăng nhập ngay mà không cần gõ phím:'
          : 'Tap any keycard below to auto-fill or sign in immediately:'}
      </p>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <button
          type="button"
          onClick={() => handleInstantKeycard('minh.anh.guest', 'GuestReview@2026!', true)}
          className="group flex flex-col justify-between rounded-2xl border border-white/15 bg-black/35 p-4 text-left transition hover:border-amber-300 hover:bg-black/55"
        >
          <div className="flex items-center justify-between">
            <span className="rounded-full bg-amber-300/20 px-2.5 py-0.5 text-[0.6rem] font-bold uppercase tracking-widest text-amber-300">
              VIP GUEST KEYCARD
            </span>
            <span className="text-xs text-amber-300 transition group-hover:translate-x-1">&rarr;</span>
          </div>
          <p className="mt-2.5 font-serif text-lg text-white">Nguyễn Minh Anh</p>
          <p className="text-[0.7rem] text-brand-paper/65">minh.anh.guest · {isVi ? 'Vào thẳng trang Thành viên' : 'Instant Guest Portal'}</p>
        </button>

        <button
          type="button"
          onClick={() => handleInstantKeycard('alex.chen.guest', 'GuestReview@2026!', true)}
          className="group flex flex-col justify-between rounded-2xl border border-white/15 bg-black/35 p-4 text-left transition hover:border-amber-300 hover:bg-black/55"
        >
          <div className="flex items-center justify-between">
            <span className="rounded-full bg-emerald-400/20 px-2.5 py-0.5 text-[0.6rem] font-bold uppercase tracking-widest text-emerald-300">
              GLOBAL MEMBER
            </span>
            <span className="text-xs text-emerald-300 transition group-hover:translate-x-1">&rarr;</span>
          </div>
          <p className="mt-2.5 font-serif text-lg text-white">Alexander Chen</p>
          <p className="text-[0.7rem] text-brand-paper/65">alex.chen.guest · {isVi ? 'Đăng nhập 1-Click' : '1-Click Sign In'}</p>
        </button>
      </div>
    </div>
  );

  return (
    <AuthShell
      eyebrow={t('auth.loginEyebrow')}
      headline={t('auth.loginTitle')}
      lead={t('auth.loginLead')}
      quickAccessSlot={quickKeycardDeck}
      perks={[
        t('auth.loginPerk1'),
        t('auth.loginPerk2'),
        t('auth.loginPerk3'),
        t('auth.loginPerk4'),
      ]}
    >
      <TabSwitch active="login" />
      <div className="mb-6">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
          {isVi ? 'Cổng Định Danh Cá Nhân' : 'Private Sanctuary Access'}
        </p>
        <h2 className="mt-1 font-serif text-3xl text-brand-charcoal">
          {isVi ? 'Chào Mừng Quý Khách Trở Lại' : 'Welcome Back to Nhu Villas'}
        </h2>
      </div>

      <form
        className="grid gap-5"
        noValidate
        onSubmit={(e: FormEvent<HTMLFormElement>) => {
          e.preventDefault();
          setErrorMsg(null);
          setInfoMsg(null);
          mutation.mutate({ user: identifier, pass: password });
        }}
      >
        <label className="grid gap-2">
          <FieldLabel htmlFor="login-identifier">{t('forms.usernameOrEmail')}</FieldLabel>
          <input
            id="login-identifier"
            value={identifier}
            onChange={(e) => {
              setIdentifier(e.target.value);
              setErrorMsg(null);
            }}
            required
            autoComplete="username"
            placeholder={isVi ? 'Nhập username (vd: admin, minh.anh.guest) hoặc email' : t('forms.placeholder.email')}
            className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
          />
        </label>

        <label className="grid gap-2">
          <div className="flex items-center justify-between">
            <FieldLabel htmlFor="login-password">{t('forms.password')}</FieldLabel>
            <Link
              href="/forgot-password"
              className="text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest hover:text-brand-forest-deep"
            >
              {t('auth.forgot')}
            </Link>
          </div>
          <PasswordInput
            id="login-password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setErrorMsg(null);
            }}
            required
            autoComplete="current-password"
            placeholder={t('forms.placeholder.password')}
          />
        </label>

        {/* Quick Fill Pills inside the form card */}
        <div className="flex flex-wrap items-center gap-2 rounded-2xl bg-brand-sand/70 px-3.5 py-2.5 text-xs">
          <span className="font-semibold text-brand-ink/60">
            {isVi ? 'Chọn nhanh:' : 'Quick fill:'}
          </span>
          <button
            type="button"
            onClick={() => handleInstantKeycard('minh.anh.guest', 'GuestReview@2026!', true)}
            className="rounded-full bg-brand-forest px-3 py-1 font-semibold text-white transition hover:bg-brand-forest-deep"
          >
            👑 {isVi ? 'Khách VIP (1-Click)' : 'VIP Guest (1-Click)'}
          </button>
          <button
            type="button"
            onClick={() => {
              setIdentifier('admin');
              setErrorMsg(null);
              const pwInput = document.getElementById('login-password');
              pwInput?.focus();
            }}
            className="rounded-full border border-brand-forest/40 bg-brand-white px-3 py-1 font-semibold text-brand-forest transition hover:bg-brand-forest hover:text-white"
          >
            🛡️ Admin (admin)
          </button>
        </div>

        <label className="flex cursor-pointer select-none items-center gap-3 text-sm text-brand-ink/66">
          <input
            type="checkbox"
            checked={staySignedIn}
            onChange={(e) => setStaySignedIn(e.target.checked)}
            className="h-4 w-4 rounded border-brand-stone text-brand-forest focus:ring-brand-sage/40"
          />
          {t('auth.staySignedIn')}
        </label>

        {infoMsg ? (
          <p className="rounded-2xl border border-brand-sage/30 bg-brand-sage/15 px-4 py-3 text-xs leading-relaxed text-brand-forest" role="status">
            {infoMsg}
          </p>
        ) : null}

        {errorMsg ? (
          <p className="rounded-2xl border border-brand-coral/30 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral" role="alert">
            {errorMsg}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={!canSubmit || mutation.isPending}
          className="mt-1 inline-flex min-h-13 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.16em] text-brand-white transition hover:bg-brand-forest-deep focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest active:translate-y-[1px] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {mutation.isPending ? (
            <span className="inline-flex items-center gap-2">
              <span aria-hidden="true" className="h-2 w-2 animate-pulse rounded-full bg-brand-white" />
              {t('auth.signingIn')}
            </span>
          ) : (
            `${t('auth.login')} →`
          )}
        </button>

        <p className="text-center text-sm text-brand-ink/60">
          {t('auth.noProfile')}{' '}
          <Link
            href={`/register${redirectTo ? `?redirect=${encodeURIComponent(redirectTo)}` : ''}`}
            className="font-semibold text-brand-forest hover:text-brand-forest-deep"
          >
            {t('auth.createOne')}
          </Link>
        </p>
      </form>
    </AuthShell>
  );
};

/* ============================================================
   Register Page
   ============================================================ */

export const RegisterPage = () => {
  const { t, lang } = useTranslation();
  const isVi = lang === 'vi';
  const router = useRouter();
  const params = useSearchParams();
  const redirectTo = params.get('redirect') ?? '';

  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [acceptTerms, setAcceptTerms] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const passwordScore = useMemo(() => getPasswordScore(password), [password]);
  const passwordsMatch = !confirmPassword || password === confirmPassword;
  const usernameValid = /^[a-zA-Z0-9_]{3,20}$/.test(username);
  const emailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const mutation = useMutation<RegisterResponse, ApiError, void>({
    mutationFn: async () => {
      return api.post<RegisterResponse>(API_PATHS.auth.register, {
        username: username.trim(),
        email: email.trim(),
        password,
        fullName: fullName.trim(),
      });
    },
    onSuccess: (data) => {
      setSuccessMsg(data.message);
      router.replace('/login?reason=verify_email');
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const canSubmit =
    usernameValid &&
    fullName.trim().length >= 2 &&
    emailValid &&
    password.length >= 8 &&
    password === confirmPassword &&
    acceptTerms;

  return (
    <AuthShell
      eyebrow={t('auth.registerEyebrow')}
      headline={t('auth.registerTitle')}
      lead={t('auth.registerLead')}
      perks={[
        t('auth.registerPerk1'),
        t('auth.registerPerk2'),
        t('auth.registerPerk3'),
        t('auth.registerPerk4'),
      ]}
    >
      <TabSwitch active="register" />
      <div className="mb-6">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
          {isVi ? 'Khởi Tạo Thẻ Thành Viên' : 'Sanctuary Membership'}
        </p>
        <h2 className="mt-1 font-serif text-3xl text-brand-charcoal">
          {isVi ? 'Trở Thành Thành Viên Nhu Villas' : 'Create Your Guest Profile'}
        </h2>
      </div>

      <form
        className="grid gap-4"
        noValidate
        onSubmit={(e: FormEvent<HTMLFormElement>) => {
          e.preventDefault();
          setErrorMsg(null);
          setSuccessMsg(null);
          if (password !== confirmPassword) {
            setErrorMsg(t('validation.passwordMatch'));
            return;
          }
          if (!acceptTerms) {
            setErrorMsg(t('validation.acceptTerms'));
            return;
          }
          mutation.mutate();
        }}
      >
        <div className="grid items-start gap-4 sm:grid-cols-2">
          <label className="grid gap-1.5">
            <FieldLabel htmlFor="register-username">{t('forms.username')}</FieldLabel>
            <input
              id="register-username"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                setErrorMsg(null);
              }}
              required
              minLength={3}
              maxLength={20}
              autoComplete="username"
              placeholder={t('forms.placeholder.username')}
              className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
            />
            {username.length > 0 && !usernameValid ? (
              <FieldError>{t('validation.usernameReq')}</FieldError>
            ) : null}
          </label>

          <label className="grid gap-1.5">
            <FieldLabel htmlFor="register-fullname">{t('forms.fullName')}</FieldLabel>
            <input
              id="register-fullname"
              value={fullName}
              onChange={(e) => {
                setFullName(e.target.value);
                setErrorMsg(null);
              }}
              required
              minLength={2}
              autoComplete="name"
              placeholder={t('forms.placeholder.fullName')}
              className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
            />
          </label>
        </div>

        <label className="grid gap-1.5">
          <FieldLabel htmlFor="register-email">{t('forms.email')}</FieldLabel>
          <input
            id="register-email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              setErrorMsg(null);
            }}
            required
            autoComplete="email"
            placeholder={t('forms.placeholder.email')}
            className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
          />
          {email.length > 0 && !emailValid ? (
            <FieldError>{t('validation.emailReq')}</FieldError>
          ) : null}
        </label>

        <label className="grid gap-1.5">
          <FieldLabel htmlFor="register-password">{t('forms.password')}</FieldLabel>
          <PasswordInput
            id="register-password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              setErrorMsg(null);
            }}
            required
            minLength={8}
            autoComplete="new-password"
            placeholder={isVi ? 'Tối thiểu 8 ký tự' : 'Minimum 8 characters'}
          />
          <PasswordStrength score={passwordScore} />
        </label>

        <label className="grid gap-1.5">
          <FieldLabel htmlFor="register-confirm">{t('forms.confirmPassword')}</FieldLabel>
          <PasswordInput
            id="register-confirm"
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              setErrorMsg(null);
            }}
            required
            minLength={8}
            autoComplete="new-password"
          />
          {confirmPassword && !passwordsMatch ? (
            <FieldError>{t('validation.passwordMatch')}</FieldError>
          ) : null}
        </label>

        <label className="flex cursor-pointer select-none items-start gap-3 text-xs leading-5 text-brand-ink/66">
          <input
            type="checkbox"
            checked={acceptTerms}
            onChange={(e) => setAcceptTerms(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-brand-stone text-brand-forest focus:ring-brand-sage/40"
          />
          <span>{t('auth.termsText').replace(/<[^>]+>/g, '')}</span>
        </label>

        {errorMsg ? (
          <p className="rounded-2xl border border-brand-coral/30 bg-brand-coral/10 px-4 py-3 text-sm text-brand-coral" role="alert">
            {errorMsg}
          </p>
        ) : null}
        {successMsg ? (
          <p className="rounded-2xl border border-brand-sage/30 bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest" role="status">
            {successMsg}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={!canSubmit || mutation.isPending}
          className="mt-1 inline-flex min-h-13 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.16em] text-brand-white transition hover:bg-brand-forest-deep focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest active:translate-y-[1px] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {mutation.isPending ? (
            <span className="inline-flex items-center gap-2">
              <span aria-hidden="true" className="h-2 w-2 animate-pulse rounded-full bg-brand-white" />
              {t('auth.creatingProfile')}
            </span>
          ) : (
            `${t('auth.createProfile')} →`
          )}
        </button>

        <p className="text-center text-sm text-brand-ink/58">
          {t('auth.alreadyGuest')}{' '}
          <Link
            href={`/login${redirectTo ? `?redirect=${encodeURIComponent(redirectTo)}` : ''}`}
            className="font-semibold text-brand-forest hover:text-brand-forest-deep"
          >
            {t('auth.signInInstead')}
          </Link>
        </p>
      </form>
    </AuthShell>
  );
};

/* ============================================================
   Forgot password / reset password
   ============================================================ */

export const ForgotPasswordPage = () => {
  const { t } = useTranslation();
  const params = useSearchParams();
  const tokenFromUrl = params.get('token') ?? '';
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [token, setToken] = useState(tokenFromUrl);
  const [step, setStep] = useState<'email' | 'reset'>(tokenFromUrl ? 'reset' : 'email');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const newPasswordScore = useMemo(() => getPasswordScore(newPassword), [newPassword]);

  const lookupMutation = useMutation<{ message: string }, ApiError, void>({
    mutationFn: () => api.post<{ message: string }>(API_PATHS.auth.forgotPassword, { email: email.trim() }),
    onSuccess: (data) => {
      setStep('reset');
      setSuccessMsg(data.message);
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const resetMutation = useMutation<{ message: string }, ApiError, void>({
    mutationFn: () =>
      api.post<{ message: string }>(API_PATHS.auth.resetPassword, {
        token,
        newPassword,
        confirmPassword: confirm,
      }),
    onSuccess: (data) => {
      setSuccessMsg(data.message);
    },
    onError: (err) => setErrorMsg(err.message || t('errors.somethingWentWrong')),
  });

  const resetCanSubmit =
    token.trim().length > 0 && newPasswordScore >= 2 && newPassword === confirm;

  return (
    <AuthShell
      eyebrow={t('auth.recoveryEyebrow')}
      headline={t('auth.recoveryTitle')}
      lead={t('auth.recoveryLead')}
    >
      {step === 'email' ? (
        <form
          className="grid gap-5"
          noValidate
          onSubmit={(e: FormEvent<HTMLFormElement>) => {
            e.preventDefault();
            setErrorMsg(null);
            lookupMutation.mutate();
          }}
        >
          <label className="grid gap-2">
            <FieldLabel htmlFor="forgot-email">{t('forms.accountEmail')}</FieldLabel>
            <input
              id="forgot-email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setErrorMsg(null);
              }}
              required
              autoComplete="email"
              placeholder={t('forms.placeholder.email')}
              className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
            />
          </label>
          {errorMsg ? (
            <p className="rounded-2xl border border-brand-coral/30 bg-brand-coral/10 px-4 py-3 text-sm text-brand-ink" role="alert">
              {errorMsg}
            </p>
          ) : null}
          <button
            type="submit"
            disabled={!email.trim() || lookupMutation.isPending}
            className="inline-flex min-h-13 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep active:translate-y-[1px] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {lookupMutation.isPending ? t('auth.sending') : t('auth.sendResetLink')}
          </button>
          <p className="text-sm leading-7 text-brand-ink/58">{t('auth.recoveryNote')}</p>
          <p className="text-sm text-brand-ink/58">
            <Link href="/login" className="font-semibold text-brand-forest hover:text-brand-forest-deep">
              &larr; {t('auth.backToLogin')}
            </Link>
          </p>
        </form>
      ) : (
        <form
          className="grid gap-5"
          noValidate
          onSubmit={(e: FormEvent<HTMLFormElement>) => {
            e.preventDefault();
            setErrorMsg(null);
            if (!token.trim()) {
              setErrorMsg(t('validation.resetTokenReq'));
              return;
            }
            if (newPassword !== confirm) {
              setErrorMsg(t('validation.passwordMatch'));
              return;
            }
            resetMutation.mutate();
          }}
        >
          {successMsg ? (
            <p className="rounded-2xl border border-brand-sage/30 bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest" role="status">
              {successMsg}
            </p>
          ) : null}
          <label className="grid gap-2">
            <FieldLabel htmlFor="reset-token">{t('forms.resetToken')}</FieldLabel>
            <input
              id="reset-token"
              value={token}
              onChange={(e) => {
                setToken(e.target.value);
                setErrorMsg(null);
              }}
              required
              autoComplete="one-time-code"
              placeholder={t('forms.placeholder.token')}
              className="h-13 w-full rounded-2xl border border-brand-stone bg-brand-paper/60 px-5 text-brand-ink outline-none transition placeholder:text-brand-ink/40 focus:border-brand-forest focus:bg-brand-white focus:ring-2 focus:ring-brand-sage/30"
            />
          </label>
          <label className="grid gap-2">
            <FieldLabel htmlFor="reset-password">{t('forms.newPassword')}</FieldLabel>
            <PasswordInput
              id="reset-password"
              value={newPassword}
              onChange={(e) => {
                setNewPassword(e.target.value);
                setErrorMsg(null);
              }}
              required
              minLength={8}
              autoComplete="new-password"
              placeholder={t('forms.placeholder.min6')}
            />
            <PasswordStrength score={newPasswordScore} />
          </label>
          <label className="grid gap-2">
            <FieldLabel htmlFor="reset-confirm">{t('forms.confirmPassword')}</FieldLabel>
            <PasswordInput
              id="reset-confirm"
              value={confirm}
              onChange={(e) => {
                setConfirm(e.target.value);
                setErrorMsg(null);
              }}
              required
              minLength={8}
              autoComplete="new-password"
            />
            {confirm && newPassword !== confirm ? (
              <FieldError>{t('validation.passwordMatch')}</FieldError>
            ) : null}
          </label>
          {errorMsg ? (
            <p className="rounded-2xl border border-brand-coral/30 bg-brand-coral/10 px-4 py-3 text-sm text-brand-ink" role="alert">
              {errorMsg}
            </p>
          ) : null}
          <button
            type="submit"
            disabled={!resetCanSubmit || resetMutation.isPending}
            className="inline-flex min-h-13 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep active:translate-y-[1px] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {resetMutation.isPending ? t('auth.resetting') : t('auth.resetPassword')}
          </button>
          <p className="text-sm text-brand-ink/58">
            <Link href="/login" className="font-semibold text-brand-forest hover:text-brand-forest-deep">
              &larr; {t('auth.backToLogin')}
            </Link>
          </p>
        </form>
      )}
    </AuthShell>
  );
};

/* ============================================================
   Verify email landing
   ============================================================ */

export const VerifyEmailPage = () => {
  const { t } = useTranslation();
  const params = useSearchParams();
  const token = params.get('token') ?? '';
  const [status, setStatus] = useState<'verifying' | 'ok' | 'error'>('verifying');
  const [message, setMessage] = useState<string>(t('auth.verifying'));

  useEffect(() => {
    let cancelled = false;
    if (!token) {
      setStatus('error');
      setMessage(t('auth.noToken'));
      return () => undefined;
    }
    api
      .get<{ message: string }>(API_PATHS.auth.verify(token))
      .then((res) => {
        if (cancelled) return;
        setStatus('ok');
        setMessage(res?.message || t('auth.verifySuccess'));
      })
      .catch((err: ApiError) => {
        if (cancelled) return;
        setStatus('error');
        setMessage(err.message || t('auth.verifyFailed'));
      });
    return () => {
      cancelled = true;
    };
  }, [t, token]);

  return (
    <AuthShell
      eyebrow={t('auth.verifyEyebrow')}
      headline={t('auth.verifyTitle')}
      lead={t('auth.verifyLead')}
    >
      <div
        className={`rounded-2xl border px-5 py-6 text-sm leading-7 ${
          status === 'ok'
            ? 'border-brand-sage/40 bg-brand-sage/15 text-brand-forest'
            : status === 'error'
            ? 'border-brand-coral/30 bg-brand-coral/10 text-brand-ink'
            : 'border-brand-stone bg-brand-paper/60 text-brand-ink/66'
        }`}
        role="status"
      >
        {status === 'verifying' ? (
          <span className="inline-flex items-center gap-3">
            <span aria-hidden="true" className="h-2 w-2 animate-pulse rounded-full bg-brand-forest" />
            {message}
          </span>
        ) : (
          message
        )}
      </div>
      <Link
        href="/login"
        className="mt-6 inline-flex min-h-12 items-center justify-center rounded-full border border-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-brand-white"
      >
        {t('auth.continueToSignIn')}
      </Link>
    </AuthShell>
  );
};
