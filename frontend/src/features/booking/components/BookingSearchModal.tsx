'use client';

import { useState, useEffect, useRef, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useRouter } from 'next/navigation';
import Input from '../../../shared/components/Input';
import { useTranslation } from '../../../shared/i18n/hooks';

interface BookingSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function BookingSearchModal({ isOpen, onClose }: BookingSearchModalProps) {
  const { t } = useTranslation();
  const router = useRouter();
  const modalRef = useRef<HTMLDivElement>(null);

  // Form State
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [adults, setAdults] = useState('2');
  const [children, setChildren] = useState('0');
  const [promoCode, setPromoCode] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Today's date string (YYYY-MM-DD) for min attribute
  const today = useMemo(() => {
    const d = new Date();
    // Offset for local timezone
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().split('T')[0];
  }, []);

  // Handle Close with Escape key
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    
    // Prevent background scrolling
    const originalStyle = window.getComputedStyle(document.body).overflow;
    document.body.style.overflow = 'hidden';

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = originalStyle;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  // Click outside to close
  useEffect(() => {
    if (!isOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (modalRef.current && !modalRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    // Delay adding listener so the open click doesn't trigger close
    const timer = setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
    }, 10);
    return () => {
      clearTimeout(timer);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, onClose]);

  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  if (!isOpen || !mounted) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    // Validation
    if (!checkIn || !checkOut) {
      setErrorMsg(t('validation.bookingDatesReq'));
      return;
    }

    if (new Date(checkOut) <= new Date(checkIn)) {
      setErrorMsg(t('validation.bookingDatesInvalid'));
      return;
    }

    const adultsNum = parseInt(adults, 10);
    const childrenNum = parseInt(children, 10);

    if (isNaN(adultsNum) || adultsNum < 1) {
      setErrorMsg(t('validation.bookingAdultsReq'));
      return;
    }

    if (isNaN(childrenNum) || childrenNum < 0) {
      setErrorMsg(t('validation.bookingChildrenInvalid'));
      return;
    }

    // Build URL search params
    const sp = new URLSearchParams();
    sp.append('checkIn', checkIn);
    sp.append('checkOut', checkOut);
    sp.append('adults', adultsNum.toString());
    sp.append('children', childrenNum.toString());
    if (promoCode.trim()) {
      sp.append('promoCode', promoCode.trim());
    }

    onClose();
    router.push(`/villas?${sp.toString()}`);
  };

  const modalContent = (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-brand-forest-deep/60 p-4 backdrop-blur-sm transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] animate-in fade-in zoom-in-95">
      <div 
        ref={modalRef}
        className="relative w-full max-w-3xl overflow-hidden rounded-[2rem] border border-brand-stone/50 bg-brand-paper shadow-[0_40px_100px_rgba(8,17,14,0.2)]"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute right-6 top-6 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-brand-stone/40 text-brand-ink/60 transition hover:bg-brand-stone hover:text-brand-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-forest"
          aria-label="Close modal"
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>

        <div className="bg-brand-white p-8 sm:p-12">
          <div className="mb-8">
            <p className="mb-3 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
              {t('booking.searchEyebrow')}
            </p>
            <h2 className="text-4xl leading-tight text-brand-charcoal sm:text-5xl">
              {t('booking.searchTitle')}
            </h2>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="grid gap-6 md:grid-cols-2">
              <Input
                label={t('booking.checkIn')}
                id="checkIn"
                type="date"
                min={today}
                value={checkIn}
                onChange={(e) => setCheckIn(e.target.value)}
                required
              />
              <Input
                label={t('booking.checkOut')}
                id="checkOut"
                type="date"
                min={checkIn || today}
                value={checkOut}
                onChange={(e) => setCheckOut(e.target.value)}
                required
              />
              <Input
                label={t('booking.adults')}
                id="adults"
                type="number"
                min="1"
                value={adults}
                onChange={(e) => setAdults(e.target.value)}
                required
              />
              <Input
                label={t('booking.children')}
                id="children"
                type="number"
                min="0"
                value={children}
                onChange={(e) => setChildren(e.target.value)}
              />
              <div className="md:col-span-2">
                <Input
                  label={t('booking.promoCode')}
                  id="promoCode"
                  type="text"
                  placeholder={t('booking.promoPlaceholder')}
                  value={promoCode}
                  onChange={(e) => setPromoCode(e.target.value)}
                />
              </div>
            </div>

            {errorMsg ? (
              <p className="mt-6 rounded-2xl border border-brand-coral/30 bg-brand-coral/10 px-4 py-3 text-sm text-brand-ink" role="alert">
                {errorMsg}
              </p>
            ) : null}

            <div className="mt-10 flex justify-end">
              <button
                type="submit"
                className="inline-flex min-h-14 w-full items-center justify-center rounded-full bg-brand-forest px-8 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-forest active:translate-y-[1px] md:w-auto"
              >
                {t('booking.searchBtn')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
}

export default BookingSearchModal;
