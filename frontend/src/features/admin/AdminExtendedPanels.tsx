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
