'use client';

import { useEffect, useId, useState } from 'react';
import Link from 'next/link';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { api, API_PATHS } from '../api/client';
import { useSession, type SessionUser } from '../auth/SessionProvider';
import { BookingSearchModal } from '../../features/booking/components/BookingSearchModal';
import { useTranslation } from '../i18n/hooks';

const navItems = [
    { key: 'villas', href: '/villas' },
    { key: 'offers', href: '/offers' },
    { key: 'about', href: '/about' },
    { key: 'faq', href: '/faq' },
    { key: 'contact', href: '/contact' },
] as const;

export const Navbar = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [isScrolled, setIsScrolled] = useState(false);
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
        onScroll();
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
        <header className="fixed left-0 right-0 top-0 z-40 px-4 pt-4 text-brand-white sm:px-6 lg:px-8">
            <div
                className={`navbar-pill mx-auto flex h-[68px] max-w-[1180px] items-center justify-between rounded-full border border-brand-white/22 bg-brand-forest-deep/62 px-3 pl-5 shadow-[0_18px_60px_rgba(8,17,14,0.18)] backdrop-blur-xl supports-[not(backdrop-filter:blur(1px))]:bg-brand-forest-deep md:px-4 md:pl-7 ${
                    isScrolled ? 'is-scrolled' : ''
                }`}
            >
                <Link
                    href="/"
                    className="flex min-w-0 items-center gap-3 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper"
                    onClick={closeMenu}
                >
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-brand-white/32 bg-brand-white/10 font-serif text-2xl italic leading-none">
                        N
                    </span>
                    <span className="min-w-0">
                        <span className="block font-serif text-xl leading-none tracking-wide text-brand-white">{t('common.brandName')}</span>
                        <span className="mt-1 hidden text-[0.58rem] font-semibold uppercase tracking-[0.22em] text-brand-paper/62 sm:block">
                            {t('common.brandSubtitle')}
                        </span>
                    </span>
                </Link>

                <nav aria-label="Primary navigation" className="hidden items-center gap-7 lg:flex">
                    {navItems.map((item) => {
                        const isActive = pathname === item.href;
                        return (
                            <Link
                                key={item.key}
                                href={item.href}
                                className={`nav-link text-sm font-medium transition duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                    isActive
                                        ? 'text-brand-white'
                                        : 'text-brand-paper/78 hover:text-brand-white'
                                } focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper`}
                                aria-current={isActive ? 'page' : undefined}
                            >
                                {t(`nav.${item.key}` as any)}
                            </Link>
                        );
                    })}
                    {isAdmin ? (
                        <Link
                            href="/admin"
                            className={`nav-link text-sm font-medium transition duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                pathname === '/admin' ? 'text-brand-white' : 'text-brand-paper/78 hover:text-brand-white'
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
                        className="hidden min-h-11 items-center justify-center rounded-full bg-brand-paper px-5 text-[0.7rem] font-semibold uppercase tracking-[0.14em] text-brand-forest-deep transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:-translate-y-[1px] hover:bg-brand-white hover:shadow-[0_18px_44px_rgba(8,17,14,0.32)] active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper sm:inline-flex"
                    >
                        {t('nav.checkAvailability')}
                    </button>
                    {!mounted ? (
                        <div className="hidden min-h-11 w-20 md:inline-flex"></div>
                    ) : user ? (
                        <div className="relative group hidden md:inline-block">
                            <button className="min-h-11 flex items-center justify-center gap-2 rounded-full border border-brand-white/24 pl-2 pr-4 text-[0.7rem] font-semibold uppercase tracking-[0.14em] text-brand-paper transition duration-500 hover:bg-brand-white/12">
                                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-white/20 text-xs text-brand-white">
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
                            className="hidden min-h-11 items-center justify-center rounded-full border border-brand-white/24 px-4 text-[0.7rem] font-semibold uppercase tracking-[0.14em] text-brand-paper transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-white/12 active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper md:inline-flex"
                        >
                            {t('nav.login')}
                        </Link>
                    )}
                    <button
                        type="button"
                        className="relative flex h-11 w-11 items-center justify-center rounded-full border border-brand-white/24 bg-brand-white/10 transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] hover:bg-brand-white/16 active:scale-[0.98] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-paper lg:hidden"
                        aria-label={isOpen ? 'Close navigation menu' : 'Open navigation menu'}
                        aria-expanded={isOpen}
                        aria-controls={menuId}
                        onClick={() => setIsOpen((current) => !current)}
                    >
                        <span
                            className={`absolute h-px w-5 bg-brand-paper transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
                                isOpen ? 'translate-y-0 rotate-45' : '-translate-y-1.5'
                            }`}
                        />
                        <span
                            className={`absolute h-px w-5 bg-brand-paper transition duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
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
                                    {t(`nav.${item.key}` as any)}
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
