'use client';

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, API_PATHS } from '../api/client';

export interface SessionUser {
    id: number;
    username: string;
    email?: string | null;
    fullName?: string | null;
    role: 'USER' | 'ADMIN';
    avatarFilename?: string | null;
    emailVerified?: boolean;
}

export interface SessionState {
    user: SessionUser | null;
    loading: boolean;
    refresh: () => Promise<void>;
    signOut: () => Promise<void>;
}

const Ctx = createContext<SessionState | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<SessionUser | null>(null);
    const [loading, setLoading] = useState(true);

    const refresh = async () => {
        try {
            const data = await api.get<{ user: SessionUser | null }>(API_PATHS.auth.session);
            setUser(data?.user ?? null);
        } catch {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    const signOut = async () => {
        try {
            await api.post(API_PATHS.auth.logout);
        } catch {
            /* ignore */
        }
        try {
            window.sessionStorage.removeItem('nhu.session');
        } catch {
            /* ignore */
        }
        setUser(null);
    };

    useEffect(() => {
        refresh();
    }, []);

    const value = useMemo<SessionState>(() => ({ user, loading, refresh, signOut }), [user, loading]);
    return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSession(): SessionState {
    const ctx = useContext(Ctx);
    if (!ctx) {
        throw new Error('useSession must be used inside a SessionProvider');
    }
    return ctx;
}
