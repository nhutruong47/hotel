'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useMutation, useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';

type SessionUser = {
  id: number;
  username: string;
  email: string;
  fullName?: string;
  role: string;
  avatarFilename?: string | null;
  avatarUrl?: string | null;
  createdAt?: string;
};

const navItems = [
  {
    href: '/account',
    label: 'Overview',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>
      </svg>
    ),
  },
  {
    href: '/account/bookings',
    label: 'My Bookings',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/>
      </svg>
    ),
  },
  {
    href: '/account/profile',
    label: 'Profile',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
      </svg>
    ),
  },
  {
    href: '/account/settings',
    label: 'Settings',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/>
      </svg>
    ),
  },
  {
    href: '/account/wishlist',
    label: 'Wishlist',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
      </svg>
    ),
  },
  {
    href: '/account/notifications',
    label: 'Notifications',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
      </svg>
    ),
  },
  {
    href: '/account/reviews',
    label: 'Reviews',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
      </svg>
    ),
  },
  {
    href: '/account/support',
    label: 'Support',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
    ),
  },
];

export function AccountShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const sessionQuery = useQuery({
    queryKey: ['session'],
    queryFn: () => api.get<{ user?: SessionUser }>(API_PATHS.auth.session),
    retry: false,
    staleTime: 30_000,
  });

  const logoutMutation = useMutation({
    mutationFn: () => api.post(API_PATHS.auth.logout),
    onSuccess: () => router.push('/'),
  });

  const user = sessionQuery.data?.user;
  const displayName = user?.fullName || user?.username || '';
  const initial = displayName[0]?.toUpperCase() || 'U';

  const memberSince = user?.createdAt
    ? new Date(user.createdAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
    : null;

  return (
    <div className="min-h-screen bg-brand-sand">
      {/* Mobile top nav */}
      <div className="fixed left-0 right-0 top-0 z-30 border-b border-brand-ink/8 bg-brand-paper/95 backdrop-blur-md pt-[72px] lg:hidden">
        <div className="flex overflow-x-auto scrollbar-none px-4 py-1 gap-1">
          {navItems.map((item) => {
            const isActive = item.href === '/account' ? pathname === '/account' : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex shrink-0 items-center gap-1.5 rounded-full px-4 py-2.5 text-xs font-semibold uppercase tracking-[0.1em] transition-all ${
                  isActive
                    ? 'bg-brand-forest text-brand-white'
                    : 'text-brand-ink/60 hover:bg-brand-ink/5 hover:text-brand-ink'
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </div>
      </div>

      <div className="mx-auto max-w-[1280px] px-4 pb-24 pt-[148px] sm:px-6 lg:flex lg:gap-10 lg:px-8 lg:pt-32">
        {/* Sidebar (desktop) */}
        <aside className="hidden lg:block lg:w-[280px] lg:shrink-0">
          <div className="sticky top-28">
            {/* User card */}
            <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_8px_40px_rgba(32,52,43,0.08)]">
              <div className="flex items-center gap-4">
                {user?.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt={displayName}
                    className="h-14 w-14 rounded-full object-cover ring-2 ring-brand-forest/20"
                  />
                ) : (
                  <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-brand-forest text-xl font-semibold text-brand-white">
                    {initial}
                  </div>
                )}
                <div className="min-w-0">
                  <p className="truncate font-semibold text-brand-charcoal">{displayName}</p>
                  <p className="mt-0.5 truncate text-xs text-brand-ink/60">{user?.email}</p>
                  {memberSince && (
                    <p className="mt-1 text-[0.65rem] font-semibold uppercase tracking-[0.12em] text-brand-sage">
                      Member since {memberSince}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Nav */}
            <nav className="mt-4 rounded-2xl bg-brand-paper p-2 shadow-[0_8px_40px_rgba(32,52,43,0.08)]">
              {navItems.map((item) => {
                const isActive = item.href === '/account' ? pathname === '/account' : pathname.startsWith(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-brand-forest/8 text-brand-forest'
                        : 'text-brand-ink/70 hover:bg-brand-ink/4 hover:text-brand-ink'
                    }`}
                  >
                    <span className={isActive ? 'text-brand-forest' : 'text-brand-ink/40'}>
                      {item.icon}
                    </span>
                    {item.label}
                  </Link>
                );
              })}

              <div className="my-2 border-t border-brand-ink/6" />

              <button
                onClick={() => logoutMutation.mutate()}
                disabled={logoutMutation.isPending}
                className="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-red-600/80 transition-all hover:bg-red-50 hover:text-red-600"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/>
                </svg>
                {logoutMutation.isPending ? 'Signing out...' : 'Sign Out'}
              </button>
            </nav>
          </div>
        </aside>

        {/* Main content */}
        <main className="min-w-0 flex-1">
          {children}
        </main>
      </div>
    </div>
  );
}
