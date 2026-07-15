'use client';

import { Component, type ErrorInfo, type ReactNode } from 'react';
import { useTranslation } from '../i18n/hooks';

interface Props {
    fallback?: ReactNode;
    children: ReactNode;
}

interface State {
    error: Error | null;
}

/**
 * Catches uncaught render errors anywhere in the tree and surfaces a
 * minimal, branded recovery screen instead of a blank page. Logs the
 * original stack to the console so engineers can still debug.
 */
export class ErrorBoundary extends Component<Props, State> {
    state: State = { error: null };

    static getDerivedStateFromError(error: Error): State {
        return { error };
    }

    componentDidCatch(error: Error, info: ErrorInfo): void {
        // eslint-disable-next-line no-console
        console.error('Unhandled UI error:', error, info.componentStack);
    }

    private handleReload = () => {
        this.setState({ error: null });
        if (typeof window !== 'undefined') {
            window.location.reload();
        }
    };

    render() {
        if (this.state.error) {
            if (this.props.fallback) {
                return this.props.fallback;
            }
            return <DefaultErrorFallback onReload={this.handleReload} />;
        }
        return this.props.children;
    }
}

function DefaultErrorFallback({ onReload }: { onReload: () => void }) {
    return (
        <section className="flex min-h-screen items-center justify-center bg-brand-sand p-8 text-center">
            <div className="max-w-md rounded-[1.75rem] bg-brand-paper p-10 shadow-[0_30px_90px_rgba(32,52,43,0.12)]">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
                    Something went wrong
                </p>
                <h1 className="mt-4 text-3xl font-light text-brand-charcoal">
                    We couldn't load this page.
                </h1>
                <p 
                  className="mt-4 text-sm leading-7 text-brand-ink/64" 
                  dangerouslySetInnerHTML={{ __html: 'Refresh to try again. If the problem keeps happening, contact <span class="text-brand-forest">reservations@nhuvillas.com</span>.' }} 
                />
                <button
                    type="button"
                    onClick={onReload}
                    className="mt-6 inline-flex h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
                >
                    Refresh
                </button>
            </div>
        </section>
    );
}
