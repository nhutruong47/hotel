'use client';

import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';

type Booking = {
  id: number;
  guestName?: string;
  guestEmail?: string;
  checkInDate?: string;
  checkOutDate?: string;
  totalPrice?: number | string;
  refundAmount?: number | string;
  refundPercentage?: number;
  status?: string;
  room?: { id: number; roomNumber?: string };
};

type Promotion = {
  id?: number;
  title: string;
  subtitle?: string;
  description: string;
  category: string;
  startDate: string;
  endDate: string;
  discountPercent?: number | string | null;
  discountAmount?: number | string | null;
  minimumBookingAmount?: number | string;
  minimumNights?: number;
  maximumUses?: number | null;
  promoCode?: string;
  imageUrl?: string;
  isActive?: boolean;
  isFeatured?: boolean;
  displayOrder?: number;
};

type Review = {
  id: number;
  rating: number;
  comment?: string;
  adminReply?: string;
  isHidden?: boolean;
  reportCount?: number;
  createdAt?: string;
  user?: { username?: string; email?: string };
  room?: { roomNumber?: string };
};

type Contact = {
  id: number;
  name: string;
  email: string;
  phone?: string;
  subject: string;
  message: string;
  type?: string;
  priority?: string;
  status: string;
  adminNotes?: string;
  createdAt?: string;
};

type AuditLog = {
  id: number;
  action?: string;
  entityType?: string;
  entityId?: number;
  username?: string;
  details?: string;
  createdAt?: string;
  ipAddress?: string;
};

type RoomType = { id: number; name: string; description?: string };
type Amenity = { id: number; name: string; iconCode?: string };
type Payment = {
  id: number;
  amount?: number | string;
  status?: string;
  method?: string;
  transactionRef?: string;
  refundAmount?: number | string;
  booking?: { id: number; guestName?: string };
};

const fieldCls = 'mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm text-brand-charcoal outline-none focus:border-brand-forest';
const areaCls = 'mt-1 w-full rounded-[1rem] border border-brand-stone bg-brand-white p-3 text-sm text-brand-charcoal outline-none focus:border-brand-forest';

function formatVnd(value: number | string | undefined): string {
  if (value == null || value === '') return '-';
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return String(value);
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(num);
}

function today(offsetDays = 0) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

function downloadBlob(blob: Blob, filename = 'download') {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="rounded-[1rem] bg-brand-white p-4 text-sm text-brand-ink/58">{children}</p>;
}

export function AnalyticsPanel() {
  const [startDate, setStartDate] = useState(today(-30));
  const [endDate, setEndDate] = useState(today());
  const [roomId, setRoomId] = useState('');
  const range = `startDate=${startDate}&endDate=${endDate}`;

  const reports = useQuery({
    queryKey: ['admin', 'reports'],
    queryFn: () => api.get<any>(API_PATHS.admin.reports),
    retry: false,
  });
  const revenue = useQuery({
    queryKey: ['admin', 'revenue', startDate, endDate],
    queryFn: () => api.get<{ series: any[] }>(`${API_PATHS.admin.revenue}?${range}`),
    retry: false,
  });
  const occupancy = useQuery({
    queryKey: ['admin', 'occupancy', startDate, endDate],
    queryFn: () => api.get<{ series: any[]; totalRooms: number }>(`${API_PATHS.admin.occupancy}?${range}`),
    retry: false,
  });
  const todayCheckin = useQuery({
    queryKey: ['admin', 'today-checkin'],
    queryFn: () => api.get<{ bookings: Booking[] }>(API_PATHS.admin.todayCheckin),
    retry: false,
  });
  const todayCheckout = useQuery({
    queryKey: ['admin', 'today-checkout'],
    queryFn: () => api.get<{ bookings: Booking[] }>(API_PATHS.admin.todayCheckout),
    retry: false,
  });
  const refunds = useQuery({
    queryKey: ['admin', 'refunds'],
    queryFn: () => api.get<{ bookings: Booking[] }>(API_PATHS.admin.refunds),
    retry: false,
  });
  const payments = useQuery({
    queryKey: ['admin', 'payments'],
    queryFn: () => api.get<{ payments: Payment[] }>(API_PATHS.payments.all),
    retry: false,
  });
  const completePayment = useMutation({
    mutationFn: (id: number) => api.post(API_PATHS.payments.complete(id), {}),
    onSuccess: () => payments.refetch(),
  });
  const failPayment = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => api.post(API_PATHS.payments.fail(id), { reason }),
    onSuccess: () => payments.refetch(),
  });
  const refundPayment = useMutation({
    mutationFn: ({ id, amount, reason, idempotencyKey }: { id: number; amount: number; reason?: string; idempotencyKey: string }) =>
      api.post(API_PATHS.payments.refund(id), { amount, reason }, {
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
    onSuccess: () => {
      payments.refetch();
      refunds.refetch();
    },
  });

  const exportReport = useMutation({
    mutationFn: async ({ path, form }: { path: string; form: FormData }) => {
      const file = await api.download(path, { method: 'POST', body: form });
      downloadBlob(file.blob, file.filename);
    },
  });

  function exportByRange(path: string) {
    const form = new FormData();
    form.append('startDate', startDate);
    form.append('endDate', endDate);
    exportReport.mutate({ path, form });
  }

  function exportByRoom(path: string) {
    if (!roomId.trim()) return;
    const form = new FormData();
    form.append('roomId', roomId.trim());
    exportReport.mutate({ path, form });
  }

  const revenueTotal = useMemo(() => {
    return (revenue.data?.series ?? []).reduce((sum, point) => sum + Number(point.revenue ?? point.total ?? point.amount ?? 0), 0);
  }, [revenue.data?.series]);

  return (
    <div className="mt-10 space-y-6">
      <div className="grid gap-4 md:grid-cols-4">
        {[
          ['Revenue range', formatVnd(revenueTotal)],
          ['Total reviews', reports.data?.totalReviews ?? '-'],
          ['Total users', reports.data?.totalUsers ?? '-'],
          ['Rooms tracked', occupancy.data?.totalRooms ?? '-'],
        ].map(([label, value]) => (
          <div key={label} className="rounded-[1.5rem] bg-brand-paper p-5 shadow-[0_12px_40px_rgba(32,52,43,0.07)]">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/50">{label}</p>
            <p className="mt-3 text-2xl text-brand-charcoal">{value}</p>
          </div>
        ))}
      </div>

      <section className="rounded-[1.5rem] bg-brand-paper p-6">
        <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
          <label className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/60">
            Start
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className={fieldCls} />
          </label>
          <label className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/60">
            End
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className={fieldCls} />
          </label>
          <div className="flex flex-wrap items-end gap-2">
            <button type="button" onClick={() => exportByRange(API_PATHS.admin.reportExcel)} className="min-h-11 rounded-full bg-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
              Excel
            </button>
            <button type="button" onClick={() => exportByRange(API_PATHS.admin.reportPdf)} className="min-h-11 rounded-full border border-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest">
              PDF
            </button>
          </div>
        </div>
        {exportReport.error instanceof ApiError ? <p className="mt-3 text-sm text-brand-coral">{exportReport.error.message}</p> : null}
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        <ListCard title="Today check-in" bookings={todayCheckin.data?.bookings} />
        <ListCard title="Today checkout" bookings={todayCheckout.data?.bookings} />
        <div className="rounded-[1.5rem] bg-brand-paper p-6">
          <h2 className="text-xl text-brand-charcoal">Room report</h2>
          <input value={roomId} onChange={(e) => setRoomId(e.target.value)} placeholder="Room ID" className={`${fieldCls} mt-4`} />
          <div className="mt-3 flex gap-2">
            <button type="button" onClick={() => exportByRoom(API_PATHS.admin.reportExcelByRoom)} className="min-h-10 flex-1 rounded-full bg-brand-forest px-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
              Excel
            </button>
            <button type="button" onClick={() => exportByRoom(API_PATHS.admin.reportPdfByRoom)} className="min-h-10 flex-1 rounded-full border border-brand-forest px-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest">
              PDF
            </button>
          </div>
        </div>
      </section>

      <section className="rounded-[1.5rem] bg-brand-paper p-6">
        <h2 className="text-xl text-brand-charcoal">Refunds</h2>
        {(refunds.data?.bookings ?? []).length === 0 ? <Empty>No refunded bookings.</Empty> : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="text-xs uppercase tracking-[0.14em] text-brand-ink/50">
                <tr><th className="p-3">Booking</th><th className="p-3">Guest</th><th className="p-3">Status</th><th className="p-3">Refund</th><th className="p-3">Room</th></tr>
              </thead>
              <tbody>
                {(refunds.data?.bookings ?? []).map((b) => (
                  <tr key={b.id} className="border-t border-brand-stone/40">
                    <td className="p-3">#{b.id}</td>
                    <td className="p-3">{b.guestName || b.guestEmail || '-'}</td>
                    <td className="p-3">{b.status}</td>
                    <td className="p-3">{formatVnd(b.refundAmount)} {b.refundPercentage ? `(${b.refundPercentage}%)` : ''}</td>
                    <td className="p-3">{b.room?.roomNumber ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="rounded-[1.5rem] bg-brand-paper p-6">
        <h2 className="text-xl text-brand-charcoal">Payments</h2>
        {(payments.data?.payments ?? []).length === 0 ? <Empty>No payments.</Empty> : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[860px] text-left text-sm">
              <thead className="text-xs uppercase tracking-[0.14em] text-brand-ink/50">
                <tr><th className="p-3">Payment</th><th className="p-3">Booking</th><th className="p-3">Method</th><th className="p-3">Amount</th><th className="p-3">Status</th><th className="p-3">Actions</th></tr>
              </thead>
              <tbody>
                {(payments.data?.payments ?? []).map((p) => (
                  <tr key={p.id} className="border-t border-brand-stone/40 align-top">
                    <td className="p-3">#{p.id}<div className="text-xs text-brand-ink/45">{p.transactionRef ?? ''}</div></td>
                    <td className="p-3">#{p.booking?.id ?? '-'}</td>
                    <td className="p-3">{p.method ?? '-'}</td>
                    <td className="p-3">{formatVnd(p.amount)}</td>
                    <td className="p-3">{p.status ?? '-'}</td>
                    <td className="p-3">
                      <div className="flex flex-wrap gap-2">
                        <button type="button" onClick={() => completePayment.mutate(p.id)} className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">Complete</button>
                        <button type="button" onClick={() => failPayment.mutate({ id: p.id, reason: 'Marked failed by admin' })} className="rounded-full border border-brand-stone px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-ink/70">Fail</button>
                        <button type="button" onClick={() => refundPayment.mutate({ id: p.id, amount: Number(p.amount ?? 0), reason: 'Refunded by admin', idempotencyKey: `admin-full-refund-${p.id}` })} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">Refund</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function ListCard({ title, bookings }: { title: string; bookings?: Booking[] }) {
  return (
    <div className="rounded-[1.5rem] bg-brand-paper p-6">
      <h2 className="text-xl text-brand-charcoal">{title}</h2>
      {(bookings ?? []).length === 0 ? <Empty>No bookings.</Empty> : (
        <ul className="mt-4 space-y-2">
          {(bookings ?? []).map((b) => (
            <li key={b.id} className="rounded-[1rem] bg-brand-white p-3 text-sm">
              <p className="font-semibold text-brand-charcoal">#{b.id} {b.guestName ?? ''}</p>
              <p className="mt-1 text-brand-ink/60">{b.checkInDate} to {b.checkOutDate} · {b.room?.roomNumber ?? '-'}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function PromotionsPanel() {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Promotion | null>(null);
  const promotions = useQuery({
    queryKey: ['admin', 'promotions'],
    queryFn: () => api.get<{ promotions: Promotion[] }>(API_PATHS.admin.promotions),
    retry: false,
  });
  const del = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.admin.deletePromotion(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] }),
  });

  return (
    <div className="mt-10 grid gap-6 xl:grid-cols-[1fr_420px]">
      <section className="rounded-[1.5rem] bg-brand-paper p-6">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl text-brand-charcoal">Promotions</h2>
          <button type="button" onClick={() => setEditing(emptyPromotion())} className="min-h-10 rounded-full bg-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">
            New
          </button>
        </div>
        {(promotions.data?.promotions ?? []).length === 0 ? <Empty>No promotions.</Empty> : (
          <div className="mt-4 space-y-3">
            {(promotions.data?.promotions ?? []).map((p) => (
              <article key={p.id} className="rounded-[1rem] bg-brand-white p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-brand-charcoal">{p.title}</p>
                    <p className="mt-1 text-sm text-brand-ink/60">{p.category} · {p.startDate} to {p.endDate}</p>
                    <p className="mt-1 text-sm text-brand-ink/60">
                      {p.discountPercent ? `${p.discountPercent}%` : formatVnd(p.discountAmount ?? undefined)}
                      {p.promoCode ? ` · ${p.promoCode}` : ''}
                      {p.isFeatured ? ' · featured' : ''}
                      {p.isActive === false ? ' · inactive' : ''}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button type="button" onClick={() => setEditing(p)} className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">Edit</button>
                    <button type="button" onClick={() => p.id && del.mutate(p.id)} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">Delete</button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      {editing ? (
        <PromotionForm
          value={editing}
          onCancel={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
          }}
        />
      ) : (
        <aside className="rounded-[1.5rem] bg-brand-paper p-6 text-sm text-brand-ink/60">
          Select a promotion to edit or create a new one.
        </aside>
      )}
    </div>
  );
}

function emptyPromotion(): Promotion {
  return {
    title: '',
    description: '',
    category: 'SEASONAL',
    startDate: today(),
    endDate: today(30),
    discountPercent: 10,
    minimumBookingAmount: 0,
    minimumNights: 1,
    isActive: true,
    isFeatured: false,
    displayOrder: 0,
  };
}

function PromotionForm({ value, onCancel, onSaved }: { value: Promotion; onCancel: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<Promotion>(value);
  const [error, setError] = useState<string | null>(null);
  const save = useMutation({
    mutationFn: () => api.post(API_PATHS.admin.savePromotion, {
      ...form,
      discountPercent: form.discountPercent === '' ? null : form.discountPercent,
      discountAmount: form.discountAmount === '' ? null : form.discountAmount,
      maximumUses: form.maximumUses || null,
    }),
    onSuccess: onSaved,
    onError: (err: ApiError) => setError(err.message),
  });
  const set = <K extends keyof Promotion>(key: K, next: Promotion[K]) => setForm((prev) => ({ ...prev, [key]: next }));

  return (
    <form onSubmit={(e) => { e.preventDefault(); setError(null); save.mutate(); }} className="rounded-[1.5rem] bg-brand-paper p-6">
      <h2 className="text-xl text-brand-charcoal">{form.id ? 'Edit promotion' : 'New promotion'}</h2>
      <div className="mt-4 grid gap-3">
        <input value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="Title" className={fieldCls} required />
        <input value={form.subtitle ?? ''} onChange={(e) => set('subtitle', e.target.value)} placeholder="Subtitle" className={fieldCls} />
        <textarea value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="Description" className={areaCls} rows={4} required />
        <select value={form.category} onChange={(e) => set('category', e.target.value)} className={fieldCls}>
          {['SEASONAL', 'WEEKEND', 'HOLIDAY', 'HONEYMOON', 'FAMILY', 'EARLY_BIRD', 'LAST_MINUTE', 'LOYALTY', 'LONG_STAY', 'SPECIAL'].map((c) => <option key={c}>{c}</option>)}
        </select>
        <div className="grid gap-3 sm:grid-cols-2">
          <input type="date" value={form.startDate} onChange={(e) => set('startDate', e.target.value)} className={fieldCls} required />
          <input type="date" value={form.endDate} onChange={(e) => set('endDate', e.target.value)} className={fieldCls} required />
          <input type="number" value={form.discountPercent ?? ''} onChange={(e) => set('discountPercent', e.target.value)} placeholder="Discount %" className={fieldCls} />
          <input type="number" value={form.discountAmount ?? ''} onChange={(e) => set('discountAmount', e.target.value)} placeholder="Discount amount" className={fieldCls} />
          <input type="number" value={form.minimumBookingAmount ?? 0} onChange={(e) => set('minimumBookingAmount', e.target.value)} placeholder="Minimum booking" className={fieldCls} />
          <input type="number" value={form.minimumNights ?? 1} onChange={(e) => set('minimumNights', Number(e.target.value))} placeholder="Minimum nights" className={fieldCls} />
          <input value={form.promoCode ?? ''} onChange={(e) => set('promoCode', e.target.value.toUpperCase())} placeholder="Promo code" className={fieldCls} />
          <input value={form.imageUrl ?? ''} onChange={(e) => set('imageUrl', e.target.value)} placeholder="Image URL" className={fieldCls} />
        </div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={Boolean(form.isActive)} onChange={(e) => set('isActive', e.target.checked)} /> Active</label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={Boolean(form.isFeatured)} onChange={(e) => set('isFeatured', e.target.checked)} /> Featured</label>
        {error ? <p className="text-sm text-brand-coral">{error}</p> : null}
        <div className="flex gap-2">
          <button disabled={save.isPending} className="min-h-11 flex-1 rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50">
            {save.isPending ? 'Saving...' : 'Save'}
          </button>
          <button type="button" onClick={onCancel} className="min-h-11 rounded-full border border-brand-stone px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/70">Cancel</button>
        </div>
      </div>
    </form>
  );
}

export function ReviewsModerationPanel() {
  const queryClient = useQueryClient();
  const [replyById, setReplyById] = useState<Record<number, string>>({});
  const reviews = useQuery({
    queryKey: ['admin', 'reviews'],
    queryFn: () => api.get<{ reviews: Review[] }>(API_PATHS.admin.reviews),
    retry: false,
  });
  const reply = useMutation({
    mutationFn: ({ id, text }: { id: number; text: string }) => api.post(API_PATHS.admin.replyReview(id), { reply: text }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'reviews'] }),
  });
  const hide = useMutation({
    mutationFn: (id: number) => api.post(API_PATHS.admin.hideReview(id), {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'reviews'] }),
  });
  const del = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.admin.deleteReview(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'reviews'] }),
  });
  return (
    <div className="mt-10 space-y-3">
      {(reviews.data?.reviews ?? []).length === 0 ? <Empty>No reviews.</Empty> : (reviews.data?.reviews ?? []).map((r) => (
        <article key={r.id} className="rounded-[1.5rem] bg-brand-paper p-5">
          <div className="flex flex-wrap justify-between gap-3">
            <div>
              <p className="font-semibold text-brand-charcoal">Review #{r.id} · {r.rating}/5 · {r.room?.roomNumber ?? '-'}</p>
              <p className="mt-1 text-sm text-brand-ink/60">{r.user?.username ?? r.user?.email ?? 'Guest'} · {r.createdAt ?? ''}</p>
              <p className="mt-3 text-sm text-brand-ink/75">{r.comment || '-'}</p>
              {r.adminReply ? <p className="mt-3 rounded-[1rem] bg-brand-white p-3 text-sm text-brand-forest">Reply: {r.adminReply}</p> : null}
            </div>
            <div className="flex h-fit flex-wrap gap-2">
              <button type="button" onClick={() => hide.mutate(r.id)} className="rounded-full border border-brand-stone px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-ink/70">{r.isHidden ? 'Hidden' : 'Hide'}</button>
              <button type="button" onClick={() => del.mutate(r.id)} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">Delete</button>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <input value={replyById[r.id] ?? ''} onChange={(e) => setReplyById((prev) => ({ ...prev, [r.id]: e.target.value }))} placeholder="Management reply" className={fieldCls} />
            <button type="button" disabled={!replyById[r.id]?.trim()} onClick={() => reply.mutate({ id: r.id, text: replyById[r.id] })} className="min-h-11 rounded-full bg-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50">Reply</button>
          </div>
        </article>
      ))}
    </div>
  );
}

export function ContactsPanel() {
  const queryClient = useQueryClient();
  const [notesById, setNotesById] = useState<Record<number, string>>({});
  const contacts = useQuery({
    queryKey: ['admin', 'contacts'],
    queryFn: () => api.get<{ contacts: Contact[] }>(API_PATHS.admin.contacts),
    retry: false,
  });
  const update = useMutation({
    mutationFn: ({ id, status, notes }: { id: number; status: string; notes?: string }) => api.put(API_PATHS.admin.updateContactStatus(id), { status, notes }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'contacts'] }),
  });
  return (
    <div className="mt-10 space-y-3">
      {(contacts.data?.contacts ?? []).length === 0 ? <Empty>No contact messages.</Empty> : (contacts.data?.contacts ?? []).map((c) => (
        <article key={c.id} className="rounded-[1.5rem] bg-brand-paper p-5">
          <div className="flex flex-wrap justify-between gap-4">
            <div className="max-w-3xl">
              <p className="font-semibold text-brand-charcoal">#{c.id} {c.subject}</p>
              <p className="mt-1 text-sm text-brand-ink/60">{c.name} · {c.email} {c.phone ? `· ${c.phone}` : ''}</p>
              <p className="mt-2 text-sm text-brand-ink/70">{c.message}</p>
              <p className="mt-2 text-xs uppercase tracking-[0.14em] text-brand-ink/45">{c.type} · {c.priority} · {c.createdAt}</p>
            </div>
            <select defaultValue={c.status} onChange={(e) => update.mutate({ id: c.id, status: e.target.value, notes: notesById[c.id] })} className="h-11 rounded-full border border-brand-stone bg-brand-white px-4 text-sm">
              {['PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
          <div className="mt-3 flex gap-2">
            <input value={notesById[c.id] ?? c.adminNotes ?? ''} onChange={(e) => setNotesById((prev) => ({ ...prev, [c.id]: e.target.value }))} placeholder="Internal note" className={fieldCls} />
            <button type="button" onClick={() => update.mutate({ id: c.id, status: c.status, notes: notesById[c.id] ?? c.adminNotes })} className="min-h-11 rounded-full bg-brand-forest px-4 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white">Save note</button>
          </div>
        </article>
      ))}
    </div>
  );
}

export function AuditLogsPanel() {
  const [page, setPage] = useState(0);
  const logs = useQuery({
    queryKey: ['admin', 'audit-logs', page],
    queryFn: () => api.get<{ logs: AuditLog[]; totalPages: number; totalElements: number }>(`${API_PATHS.admin.auditLogs}?page=${page}&size=25`),
    retry: false,
  });
  return (
    <div className="mt-10 rounded-[1.5rem] bg-brand-paper p-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl text-brand-charcoal">Audit logs</h2>
        <p className="text-sm text-brand-ink/55">{logs.data?.totalElements ?? 0} entries</p>
      </div>
      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead className="text-xs uppercase tracking-[0.14em] text-brand-ink/50">
            <tr><th className="p-3">Time</th><th className="p-3">User</th><th className="p-3">Action</th><th className="p-3">Entity</th><th className="p-3">Details</th><th className="p-3">IP</th></tr>
          </thead>
          <tbody>
            {(logs.data?.logs ?? []).map((log) => (
              <tr key={log.id} className="border-t border-brand-stone/40">
                <td className="p-3">{log.createdAt ?? '-'}</td>
                <td className="p-3">{log.username ?? '-'}</td>
                <td className="p-3">{log.action ?? '-'}</td>
                <td className="p-3">{log.entityType ?? '-'} {log.entityId ?? ''}</td>
                <td className="p-3">{log.details ?? '-'}</td>
                <td className="p-3">{log.ipAddress ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-4 flex gap-2">
        <button type="button" disabled={page <= 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="min-h-10 rounded-full border border-brand-stone px-4 text-xs font-semibold uppercase tracking-[0.14em] disabled:opacity-50">Prev</button>
        <button type="button" disabled={page + 1 >= (logs.data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)} className="min-h-10 rounded-full border border-brand-stone px-4 text-xs font-semibold uppercase tracking-[0.14em] disabled:opacity-50">Next</button>
      </div>
    </div>
  );
}

export function CatalogPanel() {
  const queryClient = useQueryClient();
  const rooms = useQuery({
    queryKey: ['admin', 'rooms'],
    queryFn: () => api.get<{ roomTypes: RoomType[]; amenities: Amenity[] }>(API_PATHS.admin.rooms),
    retry: false,
  });
  const [roomType, setRoomType] = useState<Partial<RoomType>>({});
  const [amenity, setAmenity] = useState<Partial<Amenity>>({});
  const saveRoomType = useMutation({
    mutationFn: () => api.post(API_PATHS.admin.saveRoomType, roomType),
    onSuccess: () => { setRoomType({}); queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] }); },
  });
  const saveAmenity = useMutation({
    mutationFn: () => api.post(API_PATHS.admin.saveAmenity, amenity),
    onSuccess: () => { setAmenity({}); queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] }); },
  });
  const deleteRoomType = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.admin.deleteRoomType(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] }),
  });
  const deleteAmenity = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.admin.deleteAmenity(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] }),
  });

  return (
    <div className="mt-10 grid gap-6 lg:grid-cols-2">
      <CatalogBox
        title="Room types"
        items={rooms.data?.roomTypes ?? []}
        name={roomType.name ?? ''}
        description={roomType.description ?? ''}
        onName={(name) => setRoomType((prev) => ({ ...prev, name }))}
        onDescription={(description) => setRoomType((prev) => ({ ...prev, description }))}
        onEdit={(item) => setRoomType(item)}
        onDelete={(id) => deleteRoomType.mutate(id)}
        onSave={() => saveRoomType.mutate()}
      />
      <CatalogBox
        title="Amenities"
        items={rooms.data?.amenities ?? []}
        name={amenity.name ?? ''}
        description={amenity.iconCode ?? ''}
        descriptionLabel="Icon code"
        onName={(name) => setAmenity((prev) => ({ ...prev, name }))}
        onDescription={(iconCode) => setAmenity((prev) => ({ ...prev, iconCode }))}
        onEdit={(item) => setAmenity(item)}
        onDelete={(id) => deleteAmenity.mutate(id)}
        onSave={() => saveAmenity.mutate()}
      />
    </div>
  );
}

function CatalogBox({
  title, items, name, description, descriptionLabel = 'Description',
  onName, onDescription, onEdit, onDelete, onSave,
}: {
  title: string;
  items: Array<{ id: number; name: string; description?: string; iconCode?: string }>;
  name: string;
  description: string;
  descriptionLabel?: string;
  onName: (value: string) => void;
  onDescription: (value: string) => void;
  onEdit: (item: any) => void;
  onDelete: (id: number) => void;
  onSave: () => void;
}) {
  return (
    <section className="rounded-[1.5rem] bg-brand-paper p-6">
      <h2 className="text-xl text-brand-charcoal">{title}</h2>
      <div className="mt-4 grid gap-3">
        <input value={name} onChange={(e) => onName(e.target.value)} placeholder="Name" className={fieldCls} />
        <input value={description} onChange={(e) => onDescription(e.target.value)} placeholder={descriptionLabel} className={fieldCls} />
        <button type="button" disabled={!name.trim()} onClick={onSave} className="min-h-11 rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50">Save</button>
      </div>
      <div className="mt-5 space-y-2">
        {items.map((item) => (
          <div key={item.id} className="flex items-center justify-between gap-3 rounded-[1rem] bg-brand-white p-3 text-sm">
            <div>
              <p className="font-semibold text-brand-charcoal">{item.name}</p>
              <p className="text-brand-ink/55">{item.description ?? item.iconCode ?? '-'}</p>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={() => onEdit(item)} className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">Edit</button>
              <button type="button" onClick={() => onDelete(item.id)} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">Delete</button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

export type Inspection = {
  id: number;
  roomId: number;
  roomNumber: string;
  roomTypeName?: string;
  inspectorId?: number;
  inspectorName?: string;
  inspectionType: string;
  droneModel?: string;
  flightAltitudeMeters?: number;
  flightDurationMinutes?: number;
  batteryCycles?: number;
  status: string;
  severityLevel: string;
  checklistResults?: string;
  mediaUrls?: string;
  notes?: string;
  actionRequired?: string;
  scheduledDate: string;
  completedDate?: string;
  createdAt: string;
};

export type VillaMaintenance = {
  id: number;
  room: { id: number; roomNumber: string };
  startDate: string;
  endDate: string;
  reason?: string;
  status: string;
  createdBy?: { id: number; username: string; fullName?: string };
  createdAt: string;
};

const DEFAULT_CHECKLIST = [
  { id: 'roof_integrity', label: 'Roof Integrity & Tile Alignment (Drone Aerial)', passed: true },
  { id: 'facade_windows', label: 'Exterior Façade & Window Glazing', passed: true },
  { id: 'pool_filtration', label: 'Private Pool Filtration, Pump & Water Clarity', passed: true },
  { id: 'hvac_electrical', label: 'HVAC Air Conditioning & Smart Controls', passed: true },
  { id: 'interior_cleanliness', label: 'Luxury Interior Cleanliness & Linen Setup', passed: true },
  { id: 'grounds_landscape', label: 'Surrounding Grounds, Pathway & Garden Lighting', passed: true },
];

export function DroneInspectionPanel() {
  const queryClient = useQueryClient();
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [selectedInspection, setSelectedInspection] = useState<Inspection | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const stats = useQuery({
    queryKey: ['admin', 'inspections', 'stats'],
    queryFn: () => api.get<{
      totalInspections: number;
      droneMissions: number;
      passedInspections: number;
      flaggedIssues: number;
      scheduledCount: number;
      inProgressCount: number;
    }>(API_PATHS.inspections.stats),
    retry: false,
  });

  const list = useQuery({
    queryKey: ['admin', 'inspections', 'list'],
    queryFn: () => api.get<Inspection[]>(API_PATHS.inspections.all),
    retry: false,
  });

  const roomsQuery = useQuery({
    queryKey: ['admin', 'rooms', 'quick'],
    queryFn: () => api.get<{ rooms: any[] }>(API_PATHS.admin.rooms),
    retry: false,
  });

  const deleteInspection = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.inspections.delete(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
    },
  });

  const filtered = useMemo(() => {
    const items = list.data ?? [];
    if (filterStatus === 'ALL') return items;
    return items.filter((i) => i.status === filterStatus);
  }, [list.data, filterStatus]);

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div className="rounded-[1.5rem] bg-brand-paper p-5">
          <p className="text-xs uppercase tracking-[0.14em] text-brand-ink/55">Total Missions</p>
          <p className="mt-2 font-serif text-3xl font-light text-brand-charcoal">{stats.data?.totalInspections ?? 0}</p>
        </div>
        <div className="rounded-[1.5rem] bg-brand-paper p-5">
          <p className="text-xs uppercase tracking-[0.14em] text-brand-ink/55">🚁 Drone Aerial Flights</p>
          <p className="mt-2 font-serif text-3xl font-light text-brand-forest">{stats.data?.droneMissions ?? 0}</p>
        </div>
        <div className="rounded-[1.5rem] bg-brand-paper p-5">
          <p className="text-xs uppercase tracking-[0.14em] text-brand-ink/55">✅ Passed Inspections</p>
          <p className="mt-2 font-serif text-3xl font-light text-emerald-600">{stats.data?.passedInspections ?? 0}</p>
        </div>
        <div className="rounded-[1.5rem] bg-brand-paper p-5">
          <p className="text-xs uppercase tracking-[0.14em] text-brand-ink/55">⚠️ Flagged Issues</p>
          <p className="mt-2 font-serif text-3xl font-light text-brand-coral">{stats.data?.flaggedIssues ?? 0}</p>
        </div>
      </div>

      {/* Header & Filter Controls */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap gap-2">
          {['ALL', 'SCHEDULED', 'IN_PROGRESS', 'PASSED', 'FLAGGED_ISSUES', 'REPAIRED'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setFilterStatus(st)}
              className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] transition-all ${
                filterStatus === st
                  ? 'bg-brand-forest text-brand-white'
                  : 'border border-brand-stone bg-brand-white text-brand-ink/72 hover:border-brand-forest'
              }`}
            >
              {st.replace('_', ' ')}
            </button>
          ))}
        </div>
        <button
          type="button"
          onClick={() => setIsCreating(true)}
          className="rounded-full bg-brand-forest px-6 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white hover:opacity-90"
        >
          + Schedule Drone / Villa Inspection
        </button>
      </div>

      {/* Inspection List Table */}
      <div className="overflow-hidden rounded-[1.5rem] border border-brand-stone bg-brand-white">
        <table className="w-full text-left text-sm text-brand-charcoal">
          <thead className="bg-brand-paper text-xs uppercase tracking-[0.14em] text-brand-ink/65">
            <tr>
              <th className="px-5 py-3.5">ID / Date</th>
              <th className="px-5 py-3.5">Villa</th>
              <th className="px-5 py-3.5">Mission Type</th>
              <th className="px-5 py-3.5">Equipment / Drone</th>
              <th className="px-5 py-3.5">Status</th>
              <th className="px-5 py-3.5">Severity</th>
              <th className="px-5 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-stone/60">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-sm text-brand-ink/55">
                  No inspection records found.
                </td>
              </tr>
            ) : (
              filtered.map((item) => (
                <tr key={item.id} className="hover:bg-brand-paper/50">
                  <td className="px-5 py-4">
                    <p className="font-semibold">#{item.id}</p>
                    <p className="text-xs text-brand-ink/55">{item.scheduledDate?.slice(0, 10) ?? '-'}</p>
                  </td>
                  <td className="px-5 py-4">
                    <p className="font-semibold text-brand-forest">{item.roomNumber}</p>
                    <p className="text-xs text-brand-ink/55">{item.roomTypeName ?? 'Villa'}</p>
                  </td>
                  <td className="px-5 py-4">
                    <span className="rounded-full bg-brand-paper px-3 py-1 text-xs font-medium text-brand-charcoal">
                      {item.inspectionType}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <p className="text-xs font-medium">{item.droneModel || 'Manual Ground Inspection'}</p>
                    {item.flightAltitudeMeters ? (
                      <p className="text-[11px] text-brand-ink/55">
                        {item.flightAltitudeMeters}m alt • {item.flightDurationMinutes ?? 0}m flight
                      </p>
                    ) : null}
                  </td>
                  <td className="px-5 py-4">
                    <span
                      className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.1em] ${
                        item.status === 'PASSED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : item.status === 'FLAGGED_ISSUES'
                          ? 'bg-rose-100 text-rose-800'
                          : item.status === 'IN_PROGRESS'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-slate-100 text-slate-800'
                      }`}
                    >
                      {item.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <span
                      className={`text-xs font-semibold ${
                        item.severityLevel === 'CRITICAL'
                          ? 'text-rose-600'
                          : item.severityLevel === 'HIGH'
                          ? 'text-amber-600'
                          : 'text-brand-ink/65'
                      }`}
                    >
                      {item.severityLevel}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setSelectedInspection(item)}
                        className="rounded-full border border-brand-forest px-3 py-1 text-xs font-semibold text-brand-forest hover:bg-brand-forest hover:text-brand-white"
                      >
                        Inspect / Report
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm('Delete inspection record #' + item.id + '?')) {
                            deleteInspection.mutate(item.id);
                          }
                        }}
                        className="rounded-full border border-brand-coral/40 px-3 py-1 text-xs font-semibold text-brand-coral hover:bg-brand-coral hover:text-brand-white"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create / Schedule Modal */}
      {isCreating ? (
        <CreateInspectionModal
          rooms={roomsQuery.data?.rooms ?? []}
          onClose={() => setIsCreating(false)}
          onSaved={() => {
            setIsCreating(false);
            queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
          }}
        />
      ) : null}

      {/* Edit / Execute Inspection Report Modal */}
      {selectedInspection ? (
        <ExecuteInspectionModal
          inspection={selectedInspection}
          onClose={() => setSelectedInspection(null)}
          onSaved={() => {
            setSelectedInspection(null);
            queryClient.invalidateQueries({ queryKey: ['admin', 'inspections'] });
          }}
        />
      ) : null}
    </div>
  );
}

function CreateInspectionModal({
  rooms,
  onClose,
  onSaved,
}: {
  rooms: any[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [roomId, setRoomId] = useState<number | ''>(rooms[0]?.id ?? '');
  const [inspectionType, setInspectionType] = useState('DRONE_ROOF_SURVEY');
  const [droneModel, setDroneModel] = useState('DJI Mavic 3 Enterprise Thermal');
  const [flightAltitudeMeters, setFlightAltitudeMeters] = useState('35');
  const [flightDurationMinutes, setFlightDurationMinutes] = useState('15');
  const [scheduledDate, setScheduledDate] = useState(today());
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: () => {
      if (!roomId) throw new Error('Please select a villa');
      return api.post(API_PATHS.inspections.create, {
        roomId: Number(roomId),
        inspectionType,
        droneModel,
        flightAltitudeMeters: Number(flightAltitudeMeters) || undefined,
        flightDurationMinutes: Number(flightDurationMinutes) || undefined,
        scheduledDate: scheduledDate ? `${scheduledDate}T09:00:00` : undefined,
        status: 'SCHEDULED',
        severityLevel: 'NORMAL',
        notes,
      });
    },
    onSuccess: onSaved,
    onError: (err: any) => setError(err.message),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-[2rem] bg-brand-paper p-6 shadow-2xl">
        <h3 className="text-xl font-medium text-brand-charcoal">Schedule Villa / Drone Inspection</h3>
        <p className="mt-1 text-xs text-brand-ink/55">Deploy automated aerial survey or physical staff inspection.</p>

        <form
          className="mt-5 space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            setError(null);
            save.mutate();
          }}
        >
          <label className="block text-xs font-semibold text-brand-charcoal">
            Villa
            <select
              value={roomId}
              onChange={(e) => setRoomId(Number(e.target.value))}
              className={fieldCls}
              required
            >
              <option value="">— Select Villa —</option>
              {rooms.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.roomNumber} ({r.roomType?.name ?? 'Villa'})
                </option>
              ))}
            </select>
          </label>

          <label className="block text-xs font-semibold text-brand-charcoal">
            Mission Type
            <select
              value={inspectionType}
              onChange={(e) => setInspectionType(e.target.value)}
              className={fieldCls}
            >
              <option value="DRONE_ROOF_SURVEY">🚁 Drone Aerial Roof Survey</option>
              <option value="DRONE_FACADE_SURVEY">🚁 Drone Façade & Balcony Survey</option>
              <option value="THERMAL_FACADE">🌡️ Thermal Heat Leakage / HVAC Survey</option>
              <option value="POOL_FACILITY">🏊 Private Pool & Equipment Audit</option>
              <option value="PRE_CHECKIN">✨ Pre-Checkin Luxury Readiness Check</option>
              <option value="POST_CHECKOUT">🔍 Post-Checkout Clearance Check</option>
              <option value="ROUTINE_CHECK">📋 Routine Staff Inspection</option>
              <option value="MAINTENANCE_AUDIT">🛠️ Post-Maintenance Engineering Audit</option>
            </select>
          </label>

          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Drone Model / Equipment
              <input
                value={droneModel}
                onChange={(e) => setDroneModel(e.target.value)}
                className={fieldCls}
                placeholder="e.g. DJI Mavic 3 Enterprise"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Flight Altitude (m)
              <input
                type="number"
                value={flightAltitudeMeters}
                onChange={(e) => setFlightAltitudeMeters(e.target.value)}
                className={fieldCls}
              />
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Est. Duration (mins)
              <input
                type="number"
                value={flightDurationMinutes}
                onChange={(e) => setFlightDurationMinutes(e.target.value)}
                className={fieldCls}
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Scheduled Date
              <input
                type="date"
                value={scheduledDate}
                onChange={(e) => setScheduledDate(e.target.value)}
                className={fieldCls}
                required
              />
            </label>
          </div>

          <label className="block text-xs font-semibold text-brand-charcoal">
            Mission Instructions / Notes
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className={areaCls}
              rows={2}
              placeholder="e.g. Inspect tile joints around chimney and south gutter..."
            />
          </label>

          {error ? <p className="text-xs text-brand-coral">{error}</p> : null}

          <div className="mt-6 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-brand-stone px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/72"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={save.isPending}
              className="rounded-full bg-brand-forest px-6 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50"
            >
              {save.isPending ? 'Scheduling...' : 'Schedule Mission'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function ExecuteInspectionModal({
  inspection,
  onClose,
  onSaved,
}: {
  inspection: Inspection;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [status, setStatus] = useState(inspection.status);
  const [severityLevel, setSeverityLevel] = useState(inspection.severityLevel);
  const [notes, setNotes] = useState(inspection.notes ?? '');
  const [actionRequired, setActionRequired] = useState(inspection.actionRequired ?? '');
  const [mediaUrls, setMediaUrls] = useState(inspection.mediaUrls ?? '');
  const [autoMaintenance, setAutoMaintenance] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Parse or initialize checklist
  const [checklist, setChecklist] = useState<Array<{ id: string; label: string; passed: boolean }>>(() => {
    if (inspection.checklistResults) {
      try {
        return JSON.parse(inspection.checklistResults);
      } catch (ignored) {}
    }
    return DEFAULT_CHECKLIST;
  });

  const toggleChecklistItem = (index: number) => {
    setChecklist((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], passed: !next[index].passed };
      return next;
    });
  };

  const save = useMutation({
    mutationFn: () => {
      return api.put(API_PATHS.inspections.update(inspection.id), {
        roomId: inspection.roomId,
        status,
        severityLevel,
        checklistResults: JSON.stringify(checklist),
        mediaUrls,
        notes,
        actionRequired,
        autoCreateMaintenanceIfCritical: autoMaintenance,
      });
    },
    onSuccess: onSaved,
    onError: (err: any) => setError(err.message),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[2rem] bg-brand-paper p-6 shadow-2xl">
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-xl font-medium text-brand-charcoal">
              Inspection Report: #{inspection.id} — Villa {inspection.roomNumber}
            </h3>
            <p className="mt-1 text-xs text-brand-ink/55">
              Mission: <span className="font-semibold text-brand-forest">{inspection.inspectionType}</span> • Equipment:{' '}
              {inspection.droneModel || 'Manual Ground Inspection'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-brand-stone bg-brand-white p-2 text-xs text-brand-ink/72 hover:bg-brand-stone/30"
          >
            ✕
          </button>
        </div>

        <form
          className="mt-5 space-y-5"
          onSubmit={(e) => {
            e.preventDefault();
            setError(null);
            save.mutate();
          }}
        >
          {/* Status & Severity Selection */}
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Inspection Status
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={fieldCls}>
                <option value="SCHEDULED">Scheduled</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="PASSED">Passed (Ready for Luxury Stay)</option>
                <option value="FLAGGED_ISSUES">Flagged Issues (Action Required)</option>
                <option value="REPAIRED">Repaired & Resolved</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Severity Level
              <select value={severityLevel} onChange={(e) => setSeverityLevel(e.target.value)} className={fieldCls}>
                <option value="NORMAL">Normal (No Defects)</option>
                <option value="LOW">Low (Minor Cosmetic)</option>
                <option value="MEDIUM">Medium (Maintenance Recommended)</option>
                <option value="HIGH">High (Immediate Repair Required)</option>
                <option value="CRITICAL">Critical (Block Villa Booking)</option>
              </select>
            </label>
          </div>

          {/* Interactive Inspection Checklist */}
          <div className="rounded-[1.2rem] border border-brand-stone bg-brand-white p-4">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-charcoal">
              Survey Checklist & Defect Inspection
            </p>
            <div className="mt-3 divide-y divide-brand-stone/50">
              {checklist.map((item, idx) => (
                <div key={item.id} className="flex items-center justify-between py-2.5 text-xs">
                  <span className="font-medium text-brand-charcoal">{item.label}</span>
                  <button
                    type="button"
                    onClick={() => toggleChecklistItem(idx)}
                    className={`rounded-full px-3 py-1 font-semibold uppercase tracking-[0.12em] transition-all ${
                      item.passed
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-rose-100 text-rose-800'
                    }`}
                  >
                    {item.passed ? '✅ PASSED' : '❌ DEFECT FLAGGED'}
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Aerial Drone Footage / Photo URLs */}
          <label className="block text-xs font-semibold text-brand-charcoal">
            Drone Footage & Photo Evidence URLs (Comma or newline separated)
            <textarea
              value={mediaUrls}
              onChange={(e) => setMediaUrls(e.target.value)}
              className={areaCls}
              rows={2}
              placeholder="https://images.nhuvillas.com/inspections/drone-roof-01.jpg, https://videos.nhuvillas.com/drone-4k.mp4"
            />
          </label>

          {/* Action Required & Notes */}
          <label className="block text-xs font-semibold text-brand-charcoal">
            Action Required / Corrective Measures
            <input
              value={actionRequired}
              onChange={(e) => setActionRequired(e.target.value)}
              className={fieldCls}
              placeholder="e.g. Schedule urgent roof technician to replace 2 dislodged tiles."
            />
          </label>

          <label className="block text-xs font-semibold text-brand-charcoal">
            Inspector Notes & Telemetry Observations
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className={areaCls}
              rows={2}
              placeholder="All gutter waterflow tested clear. Pool salinity optimal at 3200 ppm."
            />
          </label>

          {/* Auto-maintenance toggle */}
          <label className="flex items-center gap-2 text-xs font-medium text-brand-charcoal">
            <input
              type="checkbox"
              checked={autoMaintenance}
              onChange={(e) => setAutoMaintenance(e.target.checked)}
              className="rounded"
            />
            Auto-create Villa Maintenance schedule if Severity is HIGH or CRITICAL
          </label>

          {error ? <p className="text-xs text-brand-coral">{error}</p> : null}

          <div className="mt-6 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-brand-stone px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/72"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={save.isPending}
              className="rounded-full bg-brand-forest px-6 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50"
            >
              {save.isPending ? 'Saving Report...' : 'Save Inspection Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function MaintenancesPanel() {
  const queryClient = useQueryClient();
  const [isScheduling, setIsScheduling] = useState(false);

  const list = useQuery({
    queryKey: ['admin', 'maintenances', 'list'],
    queryFn: () => api.get<VillaMaintenance[]>(API_PATHS.maintenances.all),
    retry: false,
  });

  const roomsQuery = useQuery({
    queryKey: ['admin', 'rooms', 'quick'],
    queryFn: () => api.get<{ rooms: any[] }>(API_PATHS.admin.rooms),
    retry: false,
  });

  const cancelMaint = useMutation({
    mutationFn: (id: number) => api.delete(API_PATHS.maintenances.cancel(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'maintenances'] });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl text-brand-charcoal">Villa Maintenances</h2>
          <p className="mt-1 text-xs text-brand-ink/55">Schedule property repairs and prevent guest booking conflicts.</p>
        </div>
        <button
          type="button"
          onClick={() => setIsScheduling(true)}
          className="rounded-full bg-brand-forest px-6 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
        >
          + Schedule Maintenance
        </button>
      </div>

      <div className="overflow-hidden rounded-[1.5rem] border border-brand-stone bg-brand-white">
        <table className="w-full text-left text-sm text-brand-charcoal">
          <thead className="bg-brand-paper text-xs uppercase tracking-[0.14em] text-brand-ink/65">
            <tr>
              <th className="px-5 py-3.5">ID</th>
              <th className="px-5 py-3.5">Villa</th>
              <th className="px-5 py-3.5">Start Date</th>
              <th className="px-5 py-3.5">End Date</th>
              <th className="px-5 py-3.5">Reason</th>
              <th className="px-5 py-3.5">Status</th>
              <th className="px-5 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-stone/60">
            {(list.data ?? []).length === 0 ? (
              <tr>
                <td colSpan={7} className="p-8 text-center text-sm text-brand-ink/55">
                  No active maintenance schedules found.
                </td>
              </tr>
            ) : (
              (list.data ?? []).map((m) => (
                <tr key={m.id} className="hover:bg-brand-paper/50">
                  <td className="px-5 py-4 font-semibold">#{m.id}</td>
                  <td className="px-5 py-4 font-semibold text-brand-forest">{m.room?.roomNumber ?? 'Villa'}</td>
                  <td className="px-5 py-4 text-xs">{m.startDate}</td>
                  <td className="px-5 py-4 text-xs">{m.endDate}</td>
                  <td className="px-5 py-4 text-xs text-brand-ink/75">{m.reason || 'Routine overhaul'}</td>
                  <td className="px-5 py-4">
                    <span className="rounded-full bg-amber-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.1em] text-amber-800">
                      {m.status}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    <button
                      type="button"
                      onClick={() => {
                        if (confirm('Cancel maintenance #' + m.id + '?')) {
                          cancelMaint.mutate(m.id);
                        }
                      }}
                      className="rounded-full border border-brand-coral/40 px-3 py-1 text-xs font-semibold text-brand-coral hover:bg-brand-coral hover:text-brand-white"
                    >
                      Cancel
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {isScheduling ? (
        <ScheduleMaintenanceModal
          rooms={roomsQuery.data?.rooms ?? []}
          onClose={() => setIsScheduling(false)}
          onSaved={() => {
            setIsScheduling(false);
            queryClient.invalidateQueries({ queryKey: ['admin', 'maintenances'] });
          }}
        />
      ) : null}
    </div>
  );
}

function ScheduleMaintenanceModal({
  rooms,
  onClose,
  onSaved,
}: {
  rooms: any[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [roomId, setRoomId] = useState<number | ''>(rooms[0]?.id ?? '');
  const [startDate, setStartDate] = useState(today());
  const [endDate, setEndDate] = useState(today(3));
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: () => {
      if (!roomId) throw new Error('Please select a villa');
      return api.post(API_PATHS.maintenances.schedule, {
        roomId: Number(roomId),
        startDate,
        endDate,
        reason,
      });
    },
    onSuccess: onSaved,
    onError: (err: any) => setError(err.message),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-[2rem] bg-brand-paper p-6 shadow-2xl">
        <h3 className="text-xl font-medium text-brand-charcoal">Schedule Villa Maintenance</h3>
        <form
          className="mt-4 space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            setError(null);
            save.mutate();
          }}
        >
          <label className="block text-xs font-semibold text-brand-charcoal">
            Villa
            <select
              value={roomId}
              onChange={(e) => setRoomId(Number(e.target.value))}
              className={fieldCls}
              required
            >
              <option value="">— Select Villa —</option>
              {rooms.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.roomNumber} ({r.roomType?.name ?? 'Villa'})
                </option>
              ))}
            </select>
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Start Date
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className={fieldCls}
                required
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              End Date
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className={fieldCls}
                required
              />
            </label>
          </div>
          <label className="block text-xs font-semibold text-brand-charcoal">
            Reason / Work Description
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className={areaCls}
              rows={3}
              placeholder="e.g. Annual pool pump overhaul and garden lighting repolishing..."
              required
            />
          </label>
          {error ? <p className="text-xs text-brand-coral">{error}</p> : null}
          <div className="mt-6 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-full border border-brand-stone px-5 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/72"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={save.isPending}
              className="rounded-full bg-brand-forest px-6 py-2.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50"
            >
              {save.isPending ? 'Scheduling...' : 'Confirm Maintenance'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
