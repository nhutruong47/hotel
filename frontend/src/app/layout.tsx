import { Suspense } from 'react';
import type { Metadata } from 'next';
import { Cormorant_Garamond, Plus_Jakarta_Sans } from 'next/font/google';
import '../index.css';
import { Providers } from './providers';
import { Navbar } from '../shared/components/Navbar';
import { Footer } from '../shared/components/Footer';
import { AiChatWidget } from '../features/ai/AiChatWidget';

const cormorant = Cormorant_Garamond({
  subsets: ['latin', 'vietnamese'],
  weight: ['400', '500', '600', '700'],
  style: ['normal', 'italic'],
  variable: '--font-serif-next',
});

const sansFont = Plus_Jakarta_Sans({
  subsets: ['latin', 'vietnamese'],
  variable: '--font-sans-next',
});

export const metadata: Metadata = {
  title: 'Nhu Villas',
  description: 'A place to breathe.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body suppressHydrationWarning className={`bg-brand-paper min-h-screen text-brand-ink selection:bg-brand-sage selection:text-brand-white overflow-x-hidden ${cormorant.variable} ${sansFont.variable}`}>
        <a href="#main-content" className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:px-6 focus:py-3 focus:bg-brand-white focus:text-brand-ink focus:text-sm focus:rounded focus:shadow-lg">
          Skip to main content
        </a>
        <Providers>
          <Suspense fallback={null}>
            <Navbar />
          </Suspense>
          <main id="main-content">
            {children}
          </main>
          <Footer />
          <AiChatWidget />
        </Providers>
      </body>
    </html>
  );
}
