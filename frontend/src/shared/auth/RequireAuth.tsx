'use client';

import { useEffect } from 'react';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { useSession } from './SessionProvider';
import type { ReactNode } from 'react';

/**
 * Client-side guard. Routes wrapped in this element require an active
 * session - otherwise we bounce the user back to /login while preserving
 * the original URL via the `redirect` query parameter so they land back
 * here after authenticating.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
    const { user, loading } = useSession();
    const router = useRouter();
    const pathname = usePathname();
    const searchParams = useSearchParams();

    useEffect(() => {
        if (!loading && !user) {
            const currentSearch = searchParams.toString();
            const currentUrl = pathname + (currentSearch ? `?${currentSearch}` : '');
            const redirect = encodeURIComponent(currentUrl);
            router.replace(`/login?redirect=${redirect}&reason=login_required`);
        }
    }, [loading, user, pathname, searchParams, router]);

    if (loading || !user) {
        return (
            <div
                role="status"
                aria-label="Loading session"
                className="flex min-h-[60vh] items-center justify-center bg-brand-sand"
            >
                <div className="h-10 w-10 animate-pulse rounded-full bg-brand-stone/60" />
            </div>
        );
    }

    return <>{children}</>;
}
