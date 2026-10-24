'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter, useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';
import { fetchVillas, fetchBookedDates, fetchVillaById } from '../villas/villasApi';
import { fallbackVillas } from '../villas/villaFallbacks';
import { findVillaById } from '../villas/villaSelectors';
import { AvailabilityCalendar } from '../villas/components/AvailabilityCalendar';
import { getArchitecturalSpecs } from '../villas/components/SanctuaryVisualizer';
import {
  calculateNights,
  getTodayDate,
  getTomorrowDate,
  type BookingDraft,
} from './bookingUtils';
import { useCurrency, useTranslation } from '../../shared/i18n/hooks';

const QUICK_VOUCHERS = [
  { code: 'WELCOME10', labelVi: 'Giảm 10% mọi kỳ nghỉ', labelEn: '10% OFF any stay' },
  { code: 'NHUVILLAS20', labelVi: 'Ưu đãi VIP 20%', labelEn: '20% OFF VIP Privilege' },
  { code: 'VIP100', labelVi: 'Giảm trực tiếp $100', labelEn: '$100 Instant Credit' },
  { code: 'HONEYMOON20', labelVi: 'Gói Trăng Mật -20%', labelEn: 'Honeymoon -20% (2+ nights)' },
];

export const BookingPage = () => {
  const { t, lang } = useTranslation();
  const { formatCurrency } = useCurrency();
  const isVi = lang === 'vi';
  const { villaId } = useParams() as { villaId: string };
  const router = useRouter();

  const villasQuery = useQuery({
    queryKey: ['villas'],
    queryFn: () => fetchVillas(),
    staleTime: 60_000,
  });

  const villaQuery = useQuery({
    queryKey: ['villa', villaId],
    queryFn: () => fetchVillaById(villaId!),
    enabled: Boolean(villaId),
    retry: false,
  });

  const availabilityQuery = useQuery({
    queryKey: ['booked-dates', villaId],
    queryFn: () => fetchBookedDates(villaId!),
    enabled: Boolean(villaId),
    retry: false,
  });

  const villas = villasQuery.data ?? (villasQuery.isError ? fallbackVillas : undefined);
  const villaFromList = findVillaById(villas, villaId);
  const villa = villaFromList ?? villaQuery.data ?? findVillaById(fallbackVillas, villaId) ?? null;

  const [checkIn, setCheckIn] = useState(getTodayDate());
  const [checkOut, setCheckOut] = useState(getTomorrowDate());
  const [showVisualCalendar, setShowVisualCalendar] = useState(false);
  const [guests, setGuests] = useState(2);
  const [voucherCode, setVoucherCode] = useState('');
  const [appliedVoucher, setAppliedVoucher] = useState<{
    code: string;
    amount: number;
    percent: boolean;
  } | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  // Bespoke Butler Personalization
  const [scent, setScent] = useState('Thông Đỏ Đà Lạt');
  const [breakfastStyle, setBreakfastStyle] = useState('Khay Nổi Hồ Bơi');
  const [welcomeDrink, setWelcomeDrink] = useState('Trà Ô Long Cầu Đất');
  const [airportTransfer, setAirportTransfer] = useState(false);

  const bookedDates = availabilityQuery.data ?? [];
  const nights = calculateNights(checkIn, checkOut);

  const pricingQuery = useQuery({
    queryKey: ['pricing', villa?.id, checkIn, checkOut, guests],
    queryFn: async () => {
      if (!villa || nights <= 0) return null;
      try {
        return await api.get<{
          subtotal: number;
          serviceFee: number;
          taxAmount: number;
          total: number;
        }>(`${API_PATHS.bookings.list}/pricing-preview?roomId=${villa.id}&checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}`);
      } catch {
        const subtotal = nights * villa.pricePerNight;
        const serviceFee = subtotal > 0 ? Math.round(subtotal * 0.08) : 0;
        const taxAmount = subtotal > 0 ? Math.round(subtotal * 0.1) : 0;
        return { subtotal, serviceFee, taxAmount, total: subtotal + serviceFee + taxAmount };
      }
    },
    enabled: Boolean(villa && nights > 0),
    staleTime: 60_000,
  });

  const basePricing = pricingQuery.data ?? {
    subtotal: villa ? nights * villa.pricePerNight : 0,
    serviceFee: villa && nights > 0 ? Math.round(nights * villa.pricePerNight * 0.08) : 0,
    taxAmount: villa && nights > 0 ? Math.round(nights * villa.pricePerNight * 0.1) : 0,
    total: villa
      ? nights * villa.pricePerNight +
        Math.round(nights * villa.pricePerNight * 0.08) +
        Math.round(nights * villa.pricePerNight * 0.1)
      : 0,
  };

  const discount = useMemo(() => {
    if (!appliedVoucher) return 0;
    if (appliedVoucher.percent) {
      return Math.round((basePricing.subtotal * appliedVoucher.amount) / 100);
    }
    return appliedVoucher.amount;
  }, [appliedVoucher, basePricing.subtotal]);

  const finalTotal = Math.max(0, basePricing.total - discount);
  const exceedsCapacity = villa ? guests > villa.capacity : false;
  const invalidDates = nights <= 0;
  const validationMessage = useMemo(() => {
    if (!villa) return t('validation.bookingVillaReq');
    if (invalidDates) return t('validation.bookingDatesInvalid');
    if (exceedsCapacity) return `${t('validation.bookingCapacity')} ${villa.capacity} ${t('booking.guests')}`;
    return '';
  }, [exceedsCapacity, invalidDates, villa, t]);

  const canContinue = Boolean(villa) && !invalidDates && !exceedsCapacity;

  const applyCode = async (rawCode: string) => {
    setVoucherError(null);
    setAppliedVoucher(null);
    const code = rawCode.trim().toUpperCase();
    setVoucherCode(code);
    if (!code) return;
    try {
      const res = await api.get<{ valid: boolean; message?: string; amount?: string; percent?: boolean }>(
        `${API_PATHS.bookings.validateVoucher}?code=${encodeURIComponent(code)}&roomId=${villa?.id || ''}&checkIn=${checkIn}&checkOut=${checkOut}`
      );
      if (res?.valid) {
        setAppliedVoucher({
          code,
          amount: Number(res.amount ?? 0),
          percent: Boolean(res.percent),
        });
      } else {
        setVoucherError(res?.message || t('validation.voucherInvalid'));
      }
    } catch {
      setVoucherError(t('errors.somethingWentWrong'));
    }
  };

  const continueToCheckout = () => {
    setTouched(true);
    if (!villa || !canContinue) return;
    const draft: BookingDraft = {
      villaId: villa.id,
      villaName: villa.name,
      checkIn,
      checkOut,
      guests,
      nights,
      nightlyRate: villa.pricePerNight,
      subtotal: basePricing.subtotal,
      discount,
      serviceFee: basePricing.serviceFee,
      taxAmount: basePricing.taxAmount,
      total: finalTotal,
      voucherCode: appliedVoucher ? appliedVoucher.code : undefined,
    };
    const bespokePrefs = `Hương phòng: ${scent} | Bữa sáng: ${breakfastStyle} | Thức uống đón tiếp: ${welcomeDrink}${airportTransfer ? ' | Có yêu cầu xe đón sân bay Liên Khương' : ''}`;
    window.sessionStorage.setItem('nhu.bookingDraft', JSON.stringify(draft));
    window.sessionStorage.setItem('nhu.bespokeNotes', bespokePrefs);
    router.push('/checkout');
  };

  if ((villasQuery.isLoading || villaQuery.isLoading) && !villa) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-[1080px] rounded-[2rem] bg-brand-paper p-8">
          <div className="h-12 w-64 animate-pulse rounded-full bg-brand-stone" />
          <div className="mt-8 h-72 animate-pulse rounded-[1.5rem] bg-brand-stone" />
        </div>
      </section>
    );
  }

  if (!villa) {
    return (
      <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
        <div className="mx-auto max-w-3xl rounded-[2rem] bg-brand-paper p-8 text-center">
          <h1 className="text-5xl text-brand-charcoal">{t('booking.needsVillaTitle')}</h1>
          <Link href="/villas" className="mt-8 inline-flex rounded-full bg-brand-forest px-6 py-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            {t('booking.chooseVilla')}
          </Link>
        </div>
      </section>
    );
  }

  const spec = getArchitecturalSpecs(villa);

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1380px]">
        <Link href={`/villas/${villa.slug || villa.id}`} className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest">
          &larr; {t('booking.backToVilla')}
        </Link>

        <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_0.52fr]">
          {/* LEFT: Bespoke Stay Configurator */}
          <div className="space-y-8">
            <div className="rounded-[2.2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.08)] sm:p-9">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
                  01 · {t('booking.bookingExp')}
                </p>
                <button
                  type="button"
                  onClick={() => setShowVisualCalendar(!showVisualCalendar)}
                  className="rounded-full border border-brand-forest/30 bg-brand-white px-4 py-1.5 text-xs font-semibold text-brand-forest transition hover:bg-brand-forest hover:text-white"
                >
                  📅 {showVisualCalendar ? (isVi ? 'Ẩn Lịch Trực Quan' : 'Hide Visual Calendar') : (isVi ? 'Mở Lịch Trống Trực Quan' : 'Open Visual Calendar')}
                </button>
              </div>

              <h1 className="mt-4 max-w-[14ch] font-serif text-4xl leading-[1.06] text-brand-charcoal sm:text-6xl">
                {t('booking.chooseDatesTitle')}
              </h1>
              <p className="mt-4 max-w-2xl text-base leading-relaxed text-brand-ink/64">
                {t('booking.chooseDatesLead')}
              </p>

              {showVisualCalendar && (
                <div className="mt-6 overflow-hidden rounded-2xl border border-brand-stone bg-brand-white p-2 shadow-inner">
                  <AvailabilityCalendar
                    bookedDates={bookedDates}
                    initialCheckIn={checkIn}
                    initialCheckOut={checkOut}
                    onDateSelect={(range) => {
                      if (range.checkIn) setCheckIn(range.checkIn);
                      if (range.checkOut) setCheckOut(range.checkOut);
                    }}
                  />
                </div>
              )}

              <div className="mt-8 grid gap-5 sm:grid-cols-3">
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">{t('booking.checkIn')}</span>
                  <input
                    type="date"
                    value={checkIn}
                    min={getTodayDate()}
                    onChange={(event) => setCheckIn(event.target.value)}
                    className="mt-2 h-13 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none focus:border-brand-forest"
                  />
                </label>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">{t('booking.checkOut')}</span>
                  <input
                    type="date"
                    value={checkOut}
                    min={checkIn || getTomorrowDate()}
                    onChange={(event) => setCheckOut(event.target.value)}
                    className="mt-2 h-13 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-ink outline-none focus:border-brand-forest"
                  />
                </label>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
                    {t('booking.guests')} ({isVi ? `Tối đa ${villa.capacity}` : `Max ${villa.capacity}`})
                  </span>
                  <input
                    type="number"
                    value={guests}
                    min={1}
                    max={villa.capacity}
                    onChange={(event) => setGuests(Number(event.target.value))}
                    className="mt-2 h-13 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-ink outline-none focus:border-brand-forest"
                  />
                </label>
              </div>

              {/* 1-Click VIP Voucher Selector */}
              <div className="mt-8 rounded-[1.5rem] border border-brand-stone/60 bg-brand-white p-5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-forest">
                    🎁 {isVi ? 'Kho Mã Ưu Đãi Độc Quyền (Chạm để áp dụng ngay)' : 'VIP Privilege Codes (Tap to apply)'}
                  </span>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {QUICK_VOUCHERS.map((v) => {
                    const isActive = appliedVoucher?.code === v.code;
                    return (
                      <button
                        key={v.code}
                        type="button"
                        onClick={() => applyCode(v.code)}
                        className={`flex items-center gap-2 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition ${
                          isActive
                            ? 'border-brand-forest bg-brand-forest text-white shadow-sm'
                            : 'border-brand-stone bg-brand-sand/60 text-brand-charcoal hover:border-brand-forest'
                        }`}
                      >
                        <span className="font-mono font-bold">{v.code}</span>
                        <span className="opacity-75">· {isVi ? v.labelVi : v.labelEn}</span>
                      </button>
                    );
                  })}
                </div>

                <div className="mt-4 flex gap-2">
                  <input
                    value={voucherCode}
                    onChange={(event) => {
                      setVoucherCode(event.target.value);
                      setAppliedVoucher(null);
                    }}
                    placeholder={t('forms.placeholder.voucher')}
                    className="h-12 w-full rounded-full border border-brand-stone bg-brand-paper px-5 text-sm text-brand-ink outline-none focus:border-brand-forest"
                  />
                  <button
                    type="button"
                    onClick={() => applyCode(voucherCode)}
                    className="shrink-0 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
                  >
                    {t('booking.applyVoucher')}
                  </button>
                </div>

                {voucherError && <p className="mt-2 text-xs text-brand-coral">{voucherError}</p>}
                {appliedVoucher && (
                  <p className="mt-2 text-xs font-semibold text-brand-forest">
                    ✓ {t('booking.appliedVoucher')} {appliedVoucher.code}:{' '}
                    {appliedVoucher.percent
                      ? `${appliedVoucher.amount}% ${t('booking.off')}`
                      : `${formatCurrency(appliedVoucher.amount)} ${t('booking.off')}`}
                  </p>
                )}
              </div>

              {touched && validationMessage && (
                <div className="mt-6 rounded-[1.25rem] border border-brand-coral/30 bg-brand-coral/10 p-4 text-sm text-brand-ink">
                  {validationMessage}
                </div>
              )}
            </div>

            {/* STEP 2: Bespoke Butler & Sensory Personalization */}
            <div className="rounded-[2.2rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.08)] sm:p-9">
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">
                02 · {isVi ? 'Cá Nhân Hóa Trải Nghiệm Quản Gia (Miễn Phí Đi Kèm)' : 'Complimentary Bespoke Butler Curation'}
              </p>
              <h2 className="mt-3 font-serif text-3xl text-brand-charcoal sm:text-4xl">
                {isVi ? 'Chuẩn Bị Biệt Thự Theo Gu Của Quý Khách.' : 'Tailor Your Sanctuary Before Arrival.'}
              </h2>

              <div className="mt-6 grid gap-5 sm:grid-cols-2">
                <div className="rounded-2xl bg-brand-white p-4">
                  <p className="text-xs font-semibold uppercase tracking-wider text-brand-sage">
                    🌲 {isVi ? 'Hương Tinh Dầu Biệt Thự' : 'In-Villa Botanical Scent'}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {[
                      { id: 'Thông Đỏ Đà Lạt', labelVi: 'Thông Đỏ Đà Lạt', labelEn: 'Highland Red Pine' },
                      { id: 'Gỗ Tuyết Tùng Ấm', labelVi: 'Gỗ Tuyết Tùng Ấm', labelEn: 'Warm Cedarwood' },
                      { id: 'Oải Hương Cầu Đất', labelVi: 'Oải Hương Cầu Đất', labelEn: 'Cau Dat Lavender' },
                    ].map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => setScent(item.id)}
                        className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                          scent === item.id
                            ? 'bg-brand-forest text-white'
                            : 'bg-brand-sand text-brand-charcoal hover:bg-brand-stone/60'
                        }`}
                      >
                        {isVi ? item.labelVi : item.labelEn}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-2xl bg-brand-white p-4">
                  <p className="text-xs font-semibold uppercase tracking-wider text-brand-sage">
                    🥐 {isVi ? 'Phong Cách Phục Vụ Bữa Sáng' : 'Morning Breakfast Ritual'}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {[
                      { id: 'Khay Nổi Hồ Bơi', labelVi: 'Khay Nổi Trên Hồ Bơi', labelEn: 'Floating Pool Tray' },
                      { id: 'Bàn Tiệc Sân Hiên', labelVi: 'Bàn Tiệc Sân Hiên Thông', labelEn: 'Pine Terrace Table' },
                      { id: 'Tại Phòng Ngủ', labelVi: 'Phục Vụ Tại Phòng Ngủ', labelEn: 'In-Suite Breakfast' },
                    ].map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => setBreakfastStyle(item.id)}
                        className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                          breakfastStyle === item.id
                            ? 'bg-brand-forest text-white'
                            : 'bg-brand-sand text-brand-charcoal hover:bg-brand-stone/60'
                        }`}
                      >
                        {isVi ? item.labelVi : item.labelEn}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-2xl bg-brand-white p-4">
                  <p className="text-xs font-semibold uppercase tracking-wider text-brand-sage">
                    🍵 {isVi ? 'Thức Uống Chào Mừng Khi Nhận Phòng' : 'Welcome Arrival Libation'}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {[
                      { id: 'Trà Ô Long Cầu Đất', labelVi: 'Trà Ô Long & Mứt Đà Lạt', labelEn: 'Hot Oolong & Artisanal Preserves' },
                      { id: 'Rượu Vang Đỏ & Phô Mai', labelVi: 'Rượu Vang Đỏ & Phô Mai', labelEn: 'Fine Red Wine & Cheese' },
                      { id: 'Trà Hoa Cúc Mật Ong', labelVi: 'Trà Hoa Cúc & Mật Ong Rừng', labelEn: 'Wild Honey & Chamomile' },
                    ].map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => setWelcomeDrink(item.id)}
                        className={`rounded-full px-3.5 py-1.5 text-xs font-medium transition ${
                          welcomeDrink === item.id
                            ? 'bg-brand-forest text-white'
                            : 'bg-brand-sand text-brand-charcoal hover:bg-brand-stone/60'
                        }`}
                      >
                        {isVi ? item.labelVi : item.labelEn}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="flex items-center justify-between rounded-2xl bg-brand-white p-4">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-wider text-brand-sage">
                      ✈️ {isVi ? 'Xe Riêng Đón Sân Bay Liên Khương' : 'Private Airport Chauffeur'}
                    </p>
                    <p className="mt-1 text-xs text-brand-ink/65">
                      {isVi ? 'Quản gia liên hệ xác nhận giờ hạ cánh' : 'Our concierge coordinates your flight arrival'}
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    checked={airportTransfer}
                    onChange={(e) => setAirportTransfer(e.target.checked)}
                    className="h-5 w-5 accent-brand-forest"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT: Sticky Architectural Dossier & Pricing Breakdown */}
          <aside className="h-fit rounded-[2.2rem] bg-brand-paper p-2 shadow-[0_24px_90px_rgba(32,52,43,0.12)] lg:sticky lg:top-28">
            <div className="rounded-[1.75rem] bg-brand-white p-6 sm:p-7">
              <div className="relative aspect-[16/10] overflow-hidden rounded-[1.25rem]">
                <img
                  src={villa.imageUrl}
                  alt={villa.name}
                  className="h-full w-full object-cover"
                />
                <div className="absolute left-3 top-3 rounded-full bg-black/55 px-3 py-1 text-[0.65rem] font-semibold uppercase tracking-wider text-white backdrop-blur-md">
                  {villa.roomNumber} · {spec.areaSqm}
                </div>
              </div>

              <p className="mt-5 text-[0.68rem] font-bold uppercase tracking-[0.2em] text-brand-sage">
                {isVi ? spec.zoneVi : spec.zoneEn}
              </p>
              <h2 className="mt-1 font-serif text-3xl text-brand-charcoal">{villa.name}</h2>

              <dl className="mt-6 space-y-3.5 border-t border-brand-stone/60 pt-5 text-sm text-brand-ink/70">
                <div className="flex justify-between gap-4">
                  <dt>
                    {formatCurrency(villa.pricePerNight)} × {nights || 0} {t('booking.nights')}
                  </dt>
                  <dd className="font-medium text-brand-charcoal">{formatCurrency(basePricing.subtotal)}</dd>
                </div>
                {discount > 0 && (
                  <div className="flex justify-between gap-4 text-brand-forest">
                    <dt>
                      {t('booking.voucher')} ({appliedVoucher?.code})
                    </dt>
                    <dd className="font-semibold">-{formatCurrency(discount)}</dd>
                  </div>
                )}
                <div className="flex justify-between gap-4">
                  <dt>{t('booking.serviceFee')} (8%)</dt>
                  <dd>{formatCurrency(basePricing.serviceFee)}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt>{t('booking.taxes')} (10%)</dt>
                  <dd>{formatCurrency(basePricing.taxAmount)}</dd>
                </div>
                <div className="flex justify-between gap-4 border-t border-brand-stone pt-4 text-base font-bold text-brand-charcoal">
                  <dt>{t('booking.total')}</dt>
                  <dd className="font-serif text-2xl text-brand-forest">{formatCurrency(finalTotal)}</dd>
                </div>
              </dl>

              <button
                type="button"
                onClick={continueToCheckout}
                className="mt-7 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
              >
                {t('booking.continueToCheckout')} &rarr;
              </button>
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
};
