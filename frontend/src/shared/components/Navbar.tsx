'use client';

import { useEffect, useId, useState, useRef } from 'react';
import Link from 'next/link';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { api, API_PATHS } from '../api/client';
import { useSession, type SessionUser } from '../auth/SessionProvider';
import { BookingSearchModal } from '../../features/booking/components/BookingSearchModal';
import { useTranslation } from '../i18n/hooks';

const navItems = [
    { key: 'villas', href: '/villas' },
    { key: 'offers', href: '/offers' },
    { key: 'blogs', href: '/blogs' },
    { key: 'about', href: '/about' },
    { key: 'faq', href: '/faq' },
    { key: 'contact', href: '/contact' },
] as const;

function navLabel(key: (typeof navItems)[number]['key'], t: (key: any) => string) {
    if (key === 'blogs') return 'Journal';
    return t(`nav.${key}` as any);
}

export const Navbar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [isScrolled, setIsScrolled] = useState(false);
    const [isNavigating, setIsNavigating] = useState(false);
    const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);
    const menuId = useId();
    const router = useRouter();
    const pathname = usePathname();
    const searchParams = useSearchParams();
    const { user, signOut } = useSession();
    const { t } = useTranslation();

    const currentSearch = searchParams.toString();
    const currentUrl = pathname + (currentSearch ? `?${currentSearch}` : '');
    const redirectQuery = pathname !== '/login' && pathname !== '/register' 
        ? `?redirect=${encodeURIComponent(currentUrl)}` 
        : '';

    const handleLogout = async () => {
        try {
            await api.post(API_PATHS.auth.logout);
        } catch {
            /* server already invalidates session via cookie */
        }
        await signOut();
        router.push('/');
    };

    const isAdmin = (user as SessionUser | null)?.role === 'ADMIN';
    const displayName = user?.fullName || user?.username;

    // Instantly reset scroll state on route change before paint
    const prevPathname = useRef(pathname);
    if (pathname !== prevPathname.current) {
        prevPathname.current = pathname;
        if (isScrolled) {
            setIsScrolled(false);
        }
    }

    // Throttled scroll handler
    useEffect(() => {
        if (typeof window === 'undefined') return;
        let raf = 0;
        const onScroll = () => {
            if (raf) return;
            raf = requestAnimationFrame(() => {
                raf = 0;
                setIsScrolled((window.scrollY || 0) > 24);
            });
        };
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll(); // Initial check
        return () => {
            cancelAnimationFrame(raf);
            window.removeEventListener('scroll', onScroll);
        };
    }, []);

    useEffect(() => {
        if (!isOpen) return;
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') setIsOpen(false);
        };
        document.addEventListener('keydown', onKeyDown);
        document.body.style.overflow = 'hidden';
        return () => {
            document.removeEventListener('keydown', onKeyDown);
            document.body.style.overflow = '';
        };
    }, [isOpen]);

    const closeMenu = () => setIsOpen(false);

    const [mounted, setMounted] = useState(false);
    useEffect(() => {
        setMounted(true);
    }, []);

    return (
        <header className="fixed inset-x-0 top-0 z-40 px-4 pt-4 sm:px-6 lg:px-8 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]">
            <div
                className={`mx-auto flex max-w-[1180px] items-center justify-between rounded-full px-3 pl-5 backdrop-blur-xl md:px-4 md:pl-7 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                    isScrolled
                        ? `h-[56px] shadow-[0_20px_50px_rgba(8,17,14,0.12)] ${
                            pathname === '/' ? 'border border-brand-white/28 bg-brand-forest-deep/90 supports-[not(backdrop-filter:blur(1px))]:bg-brand-forest-deep' : 'border border-brand-stone bg-brand-white/95 supports-[not(backdrop-filter:blur(1px))]:bg-brand-white'
                        }`
                        : `h-[68px] shadow-[0_18px_60px_rgba(8,17,14,0.08)] ${
                            pathname === '/' ? 'border border-brand-white/22 bg-brand-forest-deep/62 supports-[not(backdrop-filter:blur(1px))]:bg-brand-forest-deep' : 'border border-brand-stone/60 bg-brand-paper/80 supports-[not(backdrop-filter:blur(1px))]:bg-brand-paper'
                        }`
                }`}
            >
                <Link
                    href="/"
                    className="flex min-w-0 items-center gap-3 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
                    onClick={closeMenu}
                >
                    <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full border font-serif text-2xl italic leading-none transition-colors duration-500 ${pathname === '/' ? 'border-brand-white/32 bg-brand-white/10 text-brand-white' : 'border-brand-forest/20 bg-brand-forest/5 text-brand-forest-deep'}`}>
                        N
                    </span>
                    <span className="min-w-0">
                        <span className={`block font-serif text-xl leading-none tracking-wide transition-colors duration-500 ${pathname === '/' ? 'text-brand-white' : 'text-brand-forest-deep'}`}>{t('common.brandName')}</span>
                        <span className={`mt-1 hidden text-[0.58rem] font-semibold uppercase tracking-[0.22em] transition-colors duration-500 sm:block ${pathname === '/' ? 'text-brand-paper/62' : 'text-brand-ink/50'}`}>
                            {t('common.brandSubtitle')}
                        </span>
                    </span>
                </Link>

                <nav aria-label="Primary navigation" className="hidden items-center gap-7 lg:flex">
                    {navItems.map((item) => {
                        const isActive = pathname === item.href;
                        const isHome = pathname === '/';
                        return (
                            <Link
                                key={item.key}
                                href={item.href}
                                className={`nav-link relative text-sm font-medium transition duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper ${
                                    isActive
                                        ? (isHome ? 'text-brand-white' : 'text-brand-forest-deep')
                                        : (isHome ? 'text-brand-paper/78 hover:text-brand-white' : 'text-brand-ink/70 hover:text-brand-forest-deep')
                                }`}
                                aria-current={isActive ? 'page' : undefined}
                            >
                                {navLabel(item.key, t)}
                            </Link>
                        );
                    })}
                    {isAdmin ? (
                        <Link
                            href="/admin"
                            className={`nav-link text-sm font-medium transition duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                pathname === '/admin' ? 'text-brand-forest-deep' : 'text-brand-ink/70 hover:text-brand-forest-deep'
                            }`}
                        >
                            {t('nav.admin')}
                        </Link>
                    ) : null}
                </nav>

                <div className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => setIsSearchModalOpen(true)}
                        className={`hidden min-h-11 items-center justify-center rounded-full px-5 text-[0.7rem] font-semibold uppercase tracking-[0.14em] transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-[1px] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper sm:inline-flex ${
                            pathname === '/' 
                                ? 'bg-brand-paper text-brand-forest-deep hover:bg-brand-white hover:shadow-[0_18px_44px_rgba(8,17,14,0.32)]'
                                : 'bg-brand-forest text-brand-white hover:bg-brand-forest-deep hover:shadow-[0_18px_44px_rgba(32,52,43,0.22)]'
                        }`}
                    >
                        {t('nav.checkAvailability')}
                    </button>
                    {!mounted ? (
                        <div className="hidden min-h-11 w-20 md:inline-flex"></div>
                    ) : user ? (
                        <div className="relative group hidden md:inline-block">
                            <button className={`min-h-11 flex items-center justify-center gap-2 rounded-full border pl-2 pr-4 text-[0.7rem] font-semibold uppercase tracking-[0.14em] transition duration-500 ${
                                pathname === '/'
                                    ? 'border-brand-white/24 text-brand-paper hover:bg-brand-white/12'
                                    : 'border-brand-stone text-brand-charcoal hover:bg-brand-stone/40'
                            }`}>
                                <span className={`flex h-7 w-7 items-center justify-center rounded-full text-xs ${
                                    pathname === '/' ? 'bg-brand-white/20 text-brand-white' : 'bg-brand-forest/10 text-brand-forest'
                                }`}>
                                    {displayName?.[0]?.toUpperCase() || 'U'}
                                </span>
                                <span>{displayName}</span>
                                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="transition-transform group-hover:rotate-180">
                                    <path d="m6 9 6 6 6-6"/>
                                </svg>
                            </button>
                            <div className="absolute right-0 top-full mt-2 w-56 rounded-2xl bg-brand-paper p-2 shadow-[0_18px_60px_rgba(8,17,14,0.18)] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 transform translate-y-2 group-hover:translate-y-0 border border-brand-ink/5">
                                <div className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-brand-forest-deep/60 border-b border-brand-ink/5 mb-1 truncate">
                                    {displayName}
                                </div>
                                <Link href="/account/bookings" className="block w-full text-left px-4 py-3 text-sm text-brand-charcoal hover:bg-brand-sage/10 rounded-xl transition-colors">
                                    {t('nav.myBookings')}
                                </Link>
                                <Link href="/account/profile" className="block w-full text-left px-4 py-3 text-sm text-brand-charcoal hover:bg-brand-sage/10 rounded-xl transition-colors">
                                    {t('nav.profile')}
                                </Link>
                                <Link href="/account/settings" className="block w-full text-left px-4 py-3 text-sm text-brand-charcoal hover:bg-brand-sage/10 rounded-xl transition-colors">
                                    {t('nav.settings')}
                                </Link>
                                <button onClick={handleLogout} className="block w-full text-left px-4 py-3 text-sm text-red-600 hover:bg-red-50 rounded-xl transition-colors">
                                    {t('nav.logout')}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <Link
                            href={`/login${redirectQuery}`}
                            className={`hidden min-h-11 items-center justify-center rounded-full border px-4 text-[0.7rem] font-semibold uppercase tracking-[0.14em] transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper md:inline-flex ${
                                pathname === '/'
                                    ? 'border-brand-white/24 text-brand-paper hover:bg-brand-white/12'
                                    : 'border-brand-stone text-brand-charcoal hover:bg-brand-stone/40'
                            }`}
                        >
                            {t('nav.login')}
                        </Link>
                    )}
                    <button
                        type="button"
                        className={`relative flex h-11 w-11 items-center justify-center rounded-full border transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper lg:hidden ${
                            pathname === '/'
                                ? 'border-brand-white/24 bg-brand-white/10 hover:bg-brand-white/16'
                                : 'border-brand-stone bg-brand-stone/40 hover:bg-brand-stone/60'
                        }`}
                        aria-label={isOpen ? 'Close navigation menu' : 'Open navigation menu'}
                        aria-expanded={isOpen}
                        aria-controls={menuId}
                        onClick={() => setIsOpen((current) => !current)}
                    >
                        <span
                            className={`absolute h-px w-5 transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                pathname === '/' ? 'bg-brand-paper' : 'bg-brand-charcoal'
                            } ${
                                isOpen ? 'translate-y-0 rotate-45' : '-translate-y-1.5'
                            }`}
                        />
                        <span
                            className={`absolute h-px w-5 transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                pathname === '/' ? 'bg-brand-paper' : 'bg-brand-charcoal'
                            } ${
                                isOpen ? 'translate-y-0 -rotate-45' : 'translate-y-1.5'
                            }`}
                        />
                    </button>
                </div>
            </div>

            <div
                id={menuId}
                className={`fixed inset-0 z-30 bg-brand-forest-deep/96 px-5 pt-28 text-brand-paper transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] lg:hidden ${
                    isOpen ? 'pointer-events-auto opacity-100' : 'pointer-events-none opacity-0'
                }`}
                aria-hidden={!isOpen}
            >
                <nav aria-label="Mobile navigation" className="mx-auto flex max-w-md flex-col gap-5">
                    {navItems.map((item, index) => {
                        const isActive = pathname === item.href;
                        return (
                                <Link
                                    key={item.key}
                                    href={item.href}
                                    onClick={closeMenu}
                                    className={`nav-link-mobile border-b border-brand-white/12 pb-5 font-serif text-4xl text-brand-white transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper ${
                                        isOpen ? 'translate-y-0 opacity-100' : 'translate-y-7 opacity-0'
                                    } ${isActive ? 'text-brand-paper' : ''}`}
                                    style={{ transitionDelay: isOpen ? `${90 + index * 45}ms` : '0ms' }}
                                    aria-current={isActive ? 'page' : undefined}
                                >
                                {navLabel(item.key, t)}
                                </Link>
                        );
                    })}
                    {isAdmin ? (
                        <Link
                            href="/admin"
                            onClick={closeMenu}
                            className="border-b border-brand-white/12 pb-5 font-serif text-4xl text-brand-white"
                        >
                            {t('nav.admin')}
                        </Link>
                    ) : null}
                    <button
                        type="button"
                        onClick={() => { closeMenu(); setIsSearchModalOpen(true); }}
                        className="mt-4 inline-flex min-h-12 items-center justify-center rounded-full bg-brand-paper px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-white active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
                    >
                        {t('nav.checkAvailability')}
                    </button>
                    {user ? (
                        <>
                            <Link
                                href="/account"
                                onClick={closeMenu}
                                className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper/35 px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-paper"
                            >
                                {t('nav.account')} ({displayName})
                            </Link>
                            <button
                                type="button"
                                onClick={() => { closeMenu(); handleLogout(); }}
                                className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper/35 px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-paper"
                            >
                                {t('nav.logout')}
                            </button>
                        </>
                    ) : (
                        <Link
                            href={`/login${redirectQuery}`}
                            onClick={closeMenu}
                            className="inline-flex min-h-12 items-center justify-center rounded-full border border-brand-paper/35 px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-paper"
                        >
                            {t('nav.login')}
                        </Link>
                    )}
                </nav>
            </div>

            <BookingSearchModal 
                isOpen={isSearchModalOpen} 
                onClose={() => setIsSearchModalOpen(false)} 
            />
        </header>
    );
};
