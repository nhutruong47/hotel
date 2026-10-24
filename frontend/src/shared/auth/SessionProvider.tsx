'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, API_PATHS } from '../api/client';

export interface SessionUser {
    id: number;
    username: string;
    email?: string | null;
    fullName?: string | null;
    role: 'USER' | 'STAFF' | 'MANAGER' | 'ADMIN';
    avatarFilename?: string | null;
    emailVerified?: boolean;
}

export interface SessionState {
    user: SessionUser | null;
    loading: boolean;
    setUser: (user: SessionUser | null) => void;
    refresh: () => Promise<void>;
    signOut: () => Promise<void>;
}

const Ctx = createContext<SessionState | null>(null);

function readCachedSession(): SessionUser | null {
    if (typeof window === 'undefined') return null;
    try {
        const raw = window.sessionStorage.getItem('nhu.session');
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === 'object' && parsed.username) {
            return parsed as SessionUser;
        }
    } catch {
        /* ignore */
    }
    return null;
}

export function SessionProvider({ children }: { children: ReactNode }) {
    const [user, setUserState] = useState<SessionUser | null>(() => readCachedSession());
    const [loading, setLoading] = useState(true);

    const setUser = useCallback((nextUser: SessionUser | null) => {
        setUserState(nextUser);
        if (typeof window !== 'undefined') {
            try {
                if (nextUser) {
                    window.sessionStorage.setItem('nhu.session', JSON.stringify(nextUser));
                } else {
                    window.sessionStorage.removeItem('nhu.session');
                }
            } catch {
                /* ignore */
            }
        }
    }, []);

    const refresh = useCallback(async () => {
        try {
            const data = await api.get<{ user: SessionUser | null }>(API_PATHS.auth.session);
            if (data?.user) {
                setUser(data.user);
            } else {
                const cached = readCachedSession();
                if (!cached) {
                    setUser(null);
                }
            }
        } catch {
            if (!readCachedSession()) {
                setUser(null);
            }
        } finally {
            setLoading(false);
        }
    }, [setUser]);

    const signOut = useCallback(async () => {
        try {
            await api.post(API_PATHS.auth.logout);
        } catch {
            /* ignore */
        }
        setUser(null);
    }, [setUser]);

    useEffect(() => {
        refresh();
    }, [refresh]);

    const value = useMemo<SessionState>(
        () => ({ user, loading, setUser, refresh, signOut }),
        [user, loading, setUser, refresh, signOut]
    );
    return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSession(): SessionState {
    const ctx = useContext(Ctx);
    if (!ctx) {
        throw new Error('useSession must be used inside a SessionProvider');
    }
    return ctx;
}
