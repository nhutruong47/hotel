'use client';

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useSession } from '../../shared/auth/SessionProvider';
import {
  AnalyticsPanel,
  AuditLogsPanel,
  CatalogPanel,
  ContactsPanel,
  DroneInspectionPanel,
  MaintenancesPanel,
  PromotionsPanel,
  ReviewsModerationPanel,
} from './AdminExtendedPanels';

type AdminDashboard = {
  totalRooms: number;
  availableRooms: number;
  totalBookings: number;
  todayBookings: number;
  monthlyRevenue: string | number;
  pendingBookings: number;
  totalReviews: number;
  totalUsers: number;
  recentBookings: AdminBooking[];
  mostBookedRooms: any[];
  mostRatingRooms: any[];
};

type AdminBooking = {
  id: number;
  room?: { id: number; roomNumber: string };
  guestName: string;
  guestEmail?: string;
  guestPhone?: string;
  guests?: number;
  notes?: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number | string;
  status: string;
  user?: { username: string; email: string };
};

type AdminRoom = {
  id: number;
  roomNumber: string;
  slug?: string;
  pricePerNight: number | string;
  isAvailable: boolean;
  description?: string;
  imageUrl?: string;
  capacity?: number;
  bedrooms?: number;
  avgRating?: number;
  reviewCount?: number;
  galleryImages?: string[];
  houseRules?: string;
  policies?: string;
  nearbyAttractions?: string;
  checkInTime?: string;
  checkOutTime?: string;
  latitude?: number;
  longitude?: number;
  nearbyRestaurants?: string;
  nearbyCafes?: string;
  nearbyAirport?: string;
  directions?: string;
  minimumStay?: number;
  maximumStay?: number;
  roomType?: { id?: number; name: string; description?: string };
  amenities?: { id: number; name: string; iconCode?: string }[];
};

type AdminRoomType = { id: number; name: string; description?: string };
type AdminAmenity = { id: number; name: string; iconCode?: string; icon?: string };

type MostBookedRoom = { roomId: number; roomNumber: string; totalBookings: number };

type AdminVoucher = {
  id: number;
  code: string;
  amount: number | string;
  quantity: number;
  usedCount?: number;
  percent?: boolean;
  expiryDate?: string;
  active?: boolean;
};

type AdminUser = {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  emailVerified?: boolean;
  disabled?: boolean;
  disabledReason?: string;
  createdAt?: string;
};

type BookingAction = 'complete' | 'cancel' | 'checkin' | 'checkout' | 'no-show';

function formatVnd(value: number | string | undefined): string {
  if (value == null) return '—';
  const num = typeof value === 'string' ? Number(value) : value;
  if (!Number.isFinite(num)) return String(value);
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(num);
}

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'bookings', label: 'Bookings' },
  { id: 'rooms', label: 'Rooms' },
  { id: 'inspections', label: 'Drone & Inspections' },
  { id: 'maintenances', label: 'Maintenances' },
  { id: 'catalog', label: 'Catalog' },
  { id: 'vouchers', label: 'Vouchers' },
  { id: 'promotions', label: 'Promotions' },
  { id: 'reviews', label: 'Reviews' },
  { id: 'contacts', label: 'Contacts' },
  { id: 'analytics', label: 'Analytics' },
  { id: 'audit', label: 'Audit' },
  { id: 'users', label: 'Users' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const STATUS_FILTERS: { value: string; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'PENDING_PAYMENT', label: 'Pending Payment' },
  { value: 'PAID', label: 'Paid' },
  { value: 'CHECKED_IN', label: 'Checked In' },
  { value: 'CHECKED_OUT', label: 'Checked Out' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'NO_SHOW', label: 'No Show' }
];

export const AdminDashboardPage = () => {
  const { user } = useSession();
  const [tab, setTab] = useState<TabId>('overview');
  const queryClient = useQueryClient();
  const visibleTabs = TABS.filter((item) => {
    if (user?.role === 'ADMIN') return true;
    if (user?.role === 'MANAGER') return !['audit', 'users'].includes(item.id);
    return ['overview', 'bookings', 'contacts', 'inspections'].includes(item.id);
  });

  const dashboard = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () => api.get<AdminDashboard>(API_PATHS.admin.dashboard),
    retry: false,
  });

  const bookingsQuery = useQuery({
    queryKey: ['admin', 'bookings'],
    queryFn: () => api.get<{ bookings: AdminBooking[]; statuses: string[] }>(API_PATHS.admin.bookings),
    enabled: tab === 'bookings',
    retry: false,
  });

  const roomsQuery = useQuery({
    queryKey: ['admin', 'rooms'],
    queryFn: () =>
      api.get<{ rooms: AdminRoom[]; roomTypes: AdminRoomType[]; amenities: AdminAmenity[] }>(
        API_PATHS.admin.rooms,
      ),
    enabled: tab === 'rooms',
    retry: false,
  });

  const vouchersQuery = useQuery({
    queryKey: ['admin', 'vouchers'],
    queryFn: () => api.get<{ vouchers: AdminVoucher[] }>(API_PATHS.admin.vouchers),
    enabled: tab === 'vouchers',
    retry: false,
  });

  const usersQuery = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => api.get<AdminUser[]>(API_PATHS.users.list),
    enabled: tab === 'users',
    retry: false,
  });

  const performBookingAction = useMutation<unknown, ApiError, { action: BookingAction; id: number; reason?: string }>({
    mutationFn: ({ action, id, reason }) => {
      const pathByAction: Record<BookingAction, string> = {
        complete: API_PATHS.admin.completeBooking(id),
        cancel: API_PATHS.admin.cancelBooking(id),
        checkin: API_PATHS.admin.checkInBooking(id),
        checkout: API_PATHS.admin.checkOutBooking(id),
        'no-show': API_PATHS.admin.noShowBooking(id),
      };
      return api.post<unknown>(pathByAction[action], reason ? { reason } : {});
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'bookings'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });

  const deleteRoom = useMutation<{ message: string }, ApiError, number>({
    mutationFn: (id) => api.delete<{ message: string }>(API_PATHS.admin.deleteRoom(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] }),
  });

  const updateUserRole = useMutation<{ message: string }, ApiError, { id: number; role: string }>({
    mutationFn: ({ id, role }) => api.put<{ message: string }>(API_PATHS.users.updateRole(id), { role }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });

  const setUserDisabled = useMutation<{ message: string }, ApiError, { id: number; disabled: boolean; reason?: string }>({
    mutationFn: ({ id, disabled, reason }) =>
      api.patch<{ message: string }>(API_PATHS.users.setDisabled(id), { disabled, reason }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });

  const deleteUser = useMutation<{ message: string }, ApiError, number>({
    mutationFn: (id) => api.delete<{ message: string }>(API_PATHS.users.delete(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1440px]">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Admin</p>
        <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-7xl">
          Operational clarity.
        </h1>

        <nav className="mt-10 flex flex-wrap gap-2">
          {visibleTabs.map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => setTab(t.id)}
              className={`min-h-11 rounded-full px-5 text-xs font-semibold uppercase tracking-[0.14em] transition ${
                tab === t.id
                  ? 'bg-brand-forest text-brand-white'
                  : 'border border-brand-stone bg-brand-white text-brand-ink hover:bg-brand-paper'
              }`}
            >
              {t.label}
            </button>
          ))}
        </nav>

        {tab === 'overview' ? <OverviewPanel data={dashboard.data} isLoading={dashboard.isLoading} /> : null}

        {tab === 'bookings' ? (
          <BookingsPanel
            data={bookingsQuery.data}
            isLoading={bookingsQuery.isLoading}
            onAction={(action, id, reason) => performBookingAction.mutate({ action, id, reason })}
            role={user?.role}
          />
        ) : null}

        {tab === 'rooms' ? (
          <RoomsPanel
            data={roomsQuery.data}
            isLoading={roomsQuery.isLoading}
            onDelete={(id) => deleteRoom.mutate(id)}
            onSaved={() => queryClient.invalidateQueries({ queryKey: ['admin', 'rooms'] })}
          />
        ) : null}

        {tab === 'inspections' ? <DroneInspectionPanel /> : null}
        {tab === 'maintenances' ? <MaintenancesPanel /> : null}

        {tab === 'catalog' ? <CatalogPanel /> : null}

        {tab === 'vouchers' ? (
          <VouchersPanel
            data={vouchersQuery.data}
            isLoading={vouchersQuery.isLoading}
            onChanged={() => queryClient.invalidateQueries({ queryKey: ['admin', 'vouchers'] })}
          />
        ) : null}

        {tab === 'promotions' ? <PromotionsPanel /> : null}
        {tab === 'reviews' ? <ReviewsModerationPanel /> : null}
        {tab === 'contacts' ? <ContactsPanel /> : null}
        {tab === 'analytics' ? <AnalyticsPanel /> : null}
        {tab === 'audit' ? <AuditLogsPanel /> : null}

        {tab === 'users' ? (
          <UsersPanel
            data={usersQuery.data}
            isLoading={usersQuery.isLoading}
            onUpdateRole={(id, role) => updateUserRole.mutate({ id, role })}
            onSetDisabled={(id, disabled, reason) => setUserDisabled.mutate({ id, disabled, reason })}
            onDelete={(id) => deleteUser.mutate(id)}
          />
        ) : null}
      </div>
    </section>
  );
};

function OverviewPanel({ data, isLoading }: { data?: AdminDashboard; isLoading: boolean }) {
  if (isLoading) {
    return <p className="mt-10 text-brand-ink/58">Loading dashboard...</p>;
  }
  if (!data) {
    return <p className="mt-10 text-brand-ink/58">No data available.</p>;
  }
  const metrics = [
    ['Total bookings', String(data.totalBookings), 'All time'],
    ['Pending', String(data.pendingBookings ?? 0), 'Awaiting review'],
    ['Rooms', `${data.availableRooms}/${data.totalRooms}`, 'Available'],
    ['Revenue (30d)', formatVnd(data.monthlyRevenue), 'Confirmed only'],
  ];
  return (
    <>
      <div className="mt-10 grid gap-5 md:grid-cols-4">
        {metrics.map(([label, value, note]) => (
          <article key={label} className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
            <p className="text-sm font-semibold text-brand-ink/58">{label}</p>
            <p className="mt-4 font-serif text-3xl text-brand-charcoal">{value}</p>
            <p className="mt-3 text-sm text-brand-ink/52">{note}</p>
          </article>
        ))}
      </div>
      <div className="mt-6 grid gap-5 md:grid-cols-3">
        <article className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
          <p className="text-sm font-semibold text-brand-ink/58">Users</p>
          <p className="mt-4 font-serif text-3xl text-brand-charcoal">{data.totalUsers ?? '—'}</p>
          <p className="mt-3 text-sm text-brand-ink/52">Registered</p>
        </article>
        <article className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
          <p className="text-sm font-semibold text-brand-ink/58">Reviews</p>
          <p className="mt-4 font-serif text-3xl text-brand-charcoal">{data.totalReviews ?? '—'}</p>
          <p className="mt-3 text-sm text-brand-ink/52">Across all villas</p>
        </article>
        <article className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_18px_60px_rgba(32,52,43,0.08)]">
          <p className="text-sm font-semibold text-brand-ink/58">Today check-ins</p>
          <p className="mt-4 font-serif text-3xl text-brand-charcoal">{data.todayBookings ?? 0}</p>
          <p className="mt-3 text-sm text-brand-ink/52">Scheduled</p>
        </article>
      </div>
      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <section className="rounded-[2rem] bg-brand-paper p-7">
          <h2 className="text-2xl text-brand-charcoal">Recent bookings</h2>
          {data.recentBookings.length === 0 ? (
            <p className="mt-4 text-sm text-brand-ink/58">No bookings yet.</p>
          ) : (
            <ul className="mt-4 space-y-3 text-sm text-brand-ink/72">
              {data.recentBookings.map((b: AdminBooking) => (
                <li key={b.id} className="flex items-center justify-between rounded-[1rem] bg-brand-white p-3">
                  <span>#{b.id} · Room {b.room?.roomNumber ?? '—'} · {b.guestName}</span>
                  <span className="text-brand-ink/58">{b.status}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="rounded-[2rem] bg-brand-paper p-7">
          <h2 className="text-2xl text-brand-charcoal">Top performing rooms</h2>
          <ul className="mt-4 space-y-2 text-sm text-brand-ink/72">
            {data.mostBookedRooms.slice(0, 5).map((r: MostBookedRoom) => (
              <li key={r.roomId} className="flex justify-between rounded-[1rem] bg-brand-white p-3">
                <span>Room {r.roomNumber}</span>
                <span>{r.totalBookings} bookings</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}

function BookingsPanel({
  data, isLoading, onAction, role,
}: {
  data?: { bookings: AdminBooking[]; statuses: string[] };
  isLoading: boolean;
  onAction: (action: BookingAction, id: number, reason?: string) => void;
  role?: 'USER' | 'STAFF' | 'MANAGER' | 'ADMIN';
}) {
  const [status, setStatus] = useState('all');

  const filtered = useMemo(() => {
    if (!data?.bookings) return [];
    if (status === 'all') return data.bookings;
    return data.bookings.filter((b) => String(b.status).toUpperCase() === status);
  }, [data?.bookings, status]);

  if (isLoading) return <p className="mt-10 text-brand-ink/58">Loading bookings...</p>;
  if (!data) return <p className="mt-10 text-brand-ink/58">No data.</p>;

  return (
    <div className="mt-10">
      <div className="flex flex-wrap items-center gap-2">
        {STATUS_FILTERS.map((s) => (
          <button
            key={s.value}
            type="button"
            onClick={() => setStatus(s.value)}
            className={`min-h-10 rounded-full px-4 text-xs font-semibold uppercase tracking-[0.14em] transition ${
              status === s.value
                ? 'bg-brand-forest text-brand-white'
                : 'border border-brand-stone bg-brand-white text-brand-ink/72 hover:bg-brand-paper'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>

      <div className="mt-6 overflow-x-auto rounded-[2rem] bg-brand-paper shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <table className="w-full min-w-[920px] text-left text-sm">
          <thead className="bg-brand-white text-xs uppercase tracking-[0.14em] text-brand-ink/58">
            <tr>
              <th className="p-4">ID</th>
              <th className="p-4">Room</th>
              <th className="p-4">Guest</th>
              <th className="p-4">Dates</th>
              <th className="p-4">Total</th>
              <th className="p-4">Status</th>
              <th className="p-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((b) => (
              <>
                <tr key={b.id} className="border-t border-brand-stone/40 align-top">
                  <td className="p-4 font-semibold text-brand-charcoal">#{b.id}</td>
                  <td className="p-4">{b.room?.roomNumber ?? '—'}</td>
                  <td className="p-4">
                    <div className="font-semibold text-brand-charcoal">{b.guestName}</div>
                    <div className="mt-1 text-xs text-brand-ink/58">
                      {b.guestEmail || b.user?.email || '—'}
                      {b.guestPhone ? ` · ${b.guestPhone}` : ''}
                      {b.guests ? ` · ${b.guests} pax` : ''}
                    </div>
                  </td>
                  <td className="p-4 text-brand-ink/62">
                    <div>{b.checkInDate} → {b.checkOutDate}</div>
                    {b.notes ? <div className="mt-1 text-xs text-brand-ink/48">📝 {b.notes}</div> : null}
                  </td>
                  <td className="p-4">{formatVnd(b.totalPrice)}</td>
                  <td className="p-4">
                    <span className="inline-block rounded-full bg-brand-forest/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest">
                      {b.status}
                    </span>
                  </td>
                  <td className="p-4">
                    <div className="flex flex-wrap gap-2">
                      {b.status === 'PAID' ? (
                        <>
                          <button type="button" onClick={() => onAction('checkin', b.id)} className="rounded-full border border-brand-sage px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">
                            Check in
                          </button>
                          <button type="button" onClick={() => onAction('no-show', b.id)} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">
                            No show
                          </button>
                        </>
                      ) : null}
                      {b.status === 'CHECKED_IN' ? (
                        <button type="button" onClick={() => onAction('checkout', b.id)} className="rounded-full border border-brand-sage px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">
                          Check out
                        </button>
                      ) : null}
                      {b.status === 'CHECKED_OUT' ? (
                        <button type="button" onClick={() => onAction('complete', b.id)} className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">
                          Complete
                        </button>
                      ) : null}
                      {role === 'ADMIN' && ['PENDING_PAYMENT', 'PAID'].includes(b.status) ? (
                        <button type="button" onClick={() => onAction('cancel', b.id, 'Cancelled by admin')} className="rounded-full border border-brand-stone px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-ink/72">
                          Cancel
                        </button>
                      ) : null}
                      {!['PAID', 'CHECKED_IN', 'CHECKED_OUT'].includes(b.status)
                        && !(role === 'ADMIN' && b.status === 'PENDING_PAYMENT') ? (
                        <span className="text-xs text-brand-ink/48">No available action</span>
                      ) : null}
                    </div>
                  </td>
                </tr>
              </>
            ))}
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="p-6 text-center text-sm text-brand-ink/58">
                  No bookings match the current filter.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function RoomsPanel({
  data, isLoading, onDelete, onSaved,
}: {
  data?: { rooms: AdminRoom[]; roomTypes: AdminRoomType[]; amenities: AdminAmenity[] };
  isLoading: boolean;
  onDelete: (id: number) => void;
  onSaved: () => void;
}) {
  const [editingId, setEditingId] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);

  if (isLoading) return <p className="mt-10 text-brand-ink/58">Loading rooms...</p>;
  if (!data) return <p className="mt-10 text-brand-ink/58">No data.</p>;
  return (
    <div className="mt-10 space-y-8">
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => { setCreating(true); setEditingId(null); }}
          className="inline-flex min-h-11 items-center rounded-full bg-brand-forest px-5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white"
        >
          New room
        </button>
      </div>

      {creating ? (
        <RoomForm
          roomTypes={data.roomTypes}
          amenities={data.amenities}
          onCancel={() => setCreating(false)}
          onSaved={() => { setCreating(false); onSaved(); }}
        />
      ) : null}

      <div className="overflow-x-auto rounded-[2rem] bg-brand-paper shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <table className="w-full min-w-[820px] text-left text-sm">
          <thead className="bg-brand-white text-xs uppercase tracking-[0.14em] text-brand-ink/58">
            <tr>
              <th className="p-4">Room</th>
              <th className="p-4">Type</th>
              <th className="p-4">Price / night</th>
              <th className="p-4">Capacity</th>
              <th className="p-4">Available</th>
              <th className="p-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.rooms.map((r) => (
              <>
                <tr key={r.id} className="border-t border-brand-stone/40 align-top">
                  <td className="p-4 font-semibold text-brand-charcoal">#{r.roomNumber}</td>
                  <td className="p-4">{r.roomType?.name ?? '—'}</td>
                  <td className="p-4">{formatVnd(r.pricePerNight)}</td>
                  <td className="p-4">{r.capacity ?? '—'} · {r.bedrooms ?? '—'} BR</td>
                  <td className="p-4">{r.isAvailable ? 'Yes' : 'No'}</td>
                  <td className="p-4">
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => { setEditingId(r.id); setCreating(false); }}
                        className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => onDelete(r.id)}
                        className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
                {editingId === r.id ? (
                  <tr className="border-t border-brand-stone/40 bg-brand-sand/40">
                    <td colSpan={6} className="p-5">
                      <RoomForm
                        existing={r}
                        roomTypes={data.roomTypes}
                        amenities={data.amenities}
                        onCancel={() => setEditingId(null)}
                        onSaved={() => { setEditingId(null); onSaved(); }}
                      />
                    </td>
                  </tr>
                ) : null}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function RoomForm({
  existing, roomTypes, amenities, onCancel, onSaved,
}: {
  existing?: AdminRoom;
  roomTypes: AdminRoomType[];
  amenities: AdminAmenity[];
  onCancel: () => void;
  onSaved: () => void;
}) {
  const [formTab, setFormTab] = useState<'basic' | 'amenities' | 'gallery' | 'location' | 'policies'>('basic');
  
  // Basic info
  const [roomNumber, setRoomNumber] = useState(existing?.roomNumber ?? '');
  const [roomTypeId, setRoomTypeId] = useState<number | undefined>(existing?.roomType?.id);
  const [pricePerNight, setPricePerNight] = useState(String(existing?.pricePerNight ?? ''));
  const [capacity, setCapacity] = useState(String(existing?.capacity ?? '2'));
  const [bedrooms, setBedrooms] = useState(String(existing?.bedrooms ?? '1'));
  const [description, setDescription] = useState(existing?.description ?? '');
  const [imageUrl, setImageUrl] = useState(existing?.imageUrl ?? '');
  const [isAvailable, setIsAvailable] = useState(existing?.isAvailable ?? true);

  // Amenities
  const [selectedAmenityIds, setSelectedAmenityIds] = useState<number[]>(
    existing?.amenities?.map((a) => a.id) ?? []
  );

  // Gallery
  const [galleryImages, setGalleryImages] = useState<string[]>(
    existing?.galleryImages && existing.galleryImages.length > 0
      ? existing.galleryImages
      : ['']
  );

  // Location & Surroundings
  const [latitude, setLatitude] = useState(String(existing?.latitude ?? '11.9056'));
  const [longitude, setLongitude] = useState(String(existing?.longitude ?? '108.4389'));
  const [directions, setDirections] = useState(existing?.directions ?? '');
  const [nearbyAirport, setNearbyAirport] = useState(existing?.nearbyAirport ?? 'Lien Khuong Airport (DLI) - 30 mins drive');
  const [nearbyRestaurants, setNearbyRestaurants] = useState(existing?.nearbyRestaurants ?? '');
  const [nearbyCafes, setNearbyCafes] = useState(existing?.nearbyCafes ?? '');
  const [nearbyAttractions, setNearbyAttractions] = useState(existing?.nearbyAttractions ?? '');

  // Policies & Stay Rules
  const [checkInTime, setCheckInTime] = useState(existing?.checkInTime ?? '14:00');
  const [checkOutTime, setCheckOutTime] = useState(existing?.checkOutTime ?? '12:00');
  const [minimumStay, setMinimumStay] = useState(String(existing?.minimumStay ?? '1'));
  const [maximumStay, setMaximumStay] = useState(String(existing?.maximumStay ?? '30'));
  const [houseRules, setHouseRules] = useState(
    existing?.houseRules ?? '• No smoking indoors.\n• Quiet hours from 22:00 to 07:00.\n• Pets allowed upon prior request.'
  );
  const [policies, setPolicies] = useState(
    existing?.policies ?? '• Free cancellation up to 3 days before check-in.\n• 50% refund between 1 and 3 days before check-in.\n• Non-refundable under 24 hours.'
  );

  const [error, setError] = useState<string | null>(null);

  const toggleAmenity = (id: number) => {
    setSelectedAmenityIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleAddGalleryImage = () => {
    setGalleryImages((prev) => [...prev, '']);
  };

  const handleUpdateGalleryImage = (index: number, val: string) => {
    setGalleryImages((prev) => {
      const copy = [...prev];
      copy[index] = val;
      return copy;
    });
  };

  const handleRemoveGalleryImage = (index: number) => {
    setGalleryImages((prev) => prev.filter((_, i) => i !== index));
  };

  const save = useMutation<{ message: string }, ApiError, void>({
    mutationFn: () => {
      const cleanGallery = galleryImages.map((s) => s.trim()).filter(Boolean);
      const payload = {
        id: existing?.id,
        roomNumber,
        roomTypeId,
        pricePerNight: Number(pricePerNight),
        capacity: capacity ? Number(capacity) : undefined,
        bedrooms: bedrooms ? Number(bedrooms) : undefined,
        description,
        imageUrl,
        isAvailable,
        amenityIds: selectedAmenityIds,
        galleryImages: cleanGallery,
        latitude: latitude ? Number(latitude) : undefined,
        longitude: longitude ? Number(longitude) : undefined,
        directions,
        nearbyAirport,
        nearbyRestaurants,
        nearbyCafes,
        nearbyAttractions,
        checkInTime,
        checkOutTime,
        minimumStay: minimumStay ? Number(minimumStay) : undefined,
        maximumStay: maximumStay ? Number(maximumStay) : undefined,
        houseRules,
        policies,
      };
      return api.post<{ message: string }>(API_PATHS.admin.saveRoom, payload);
    },
    onSuccess: onSaved,
    onError: (err) => setError(err.message),
  });

  return (
    <form
      className="space-y-6 rounded-[2rem] border border-brand-stone bg-brand-paper p-6 shadow-xl"
      onSubmit={(e) => {
        e.preventDefault();
        setError(null);
        save.mutate();
      }}
    >
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-brand-stone/60 pb-4">
        <div>
          <h3 className="text-xl font-medium text-brand-charcoal">
            {existing ? `Edit Villa #${existing.roomNumber}` : 'New Luxury Villa'}
          </h3>
          <p className="text-xs text-brand-ink/55">Configure pricing, amenities, gallery, map location and stay rules.</p>
        </div>
        <div className="flex flex-wrap gap-1.5 rounded-full bg-brand-white p-1 shadow-sm">
          {(
            [
              { id: 'basic', label: '1. Basic Info' },
              { id: 'amenities', label: `2. Amenities (${selectedAmenityIds.length})` },
              { id: 'gallery', label: `3. Gallery (${galleryImages.filter(Boolean).length})` },
              { id: 'location', label: '4. Location & Map' },
              { id: 'policies', label: '5. Policies & Rules' },
            ] as const
          ).map((t) => (
            <button
              key={t.id}
              type="button"
              onClick={() => setFormTab(t.id)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-semibold uppercase tracking-[0.12em] transition-all ${
                formTab === t.id
                  ? 'bg-brand-forest text-brand-white shadow-sm'
                  : 'text-brand-ink/65 hover:text-brand-charcoal'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab 1: Basic Info */}
      {formTab === 'basic' ? (
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Villa Number / Code *
              <input
                value={roomNumber}
                onChange={(e) => setRoomNumber(e.target.value)}
                placeholder="e.g. GV-01"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
                required
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Villa Type *
              <select
                value={roomTypeId ?? ''}
                onChange={(e) => setRoomTypeId(Number(e.target.value) || undefined)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
                required
              >
                <option value="">— Select Type —</option>
                {roomTypes.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Price per Night (VND) *
              <input
                type="number"
                value={pricePerNight}
                onChange={(e) => setPricePerNight(e.target.value)}
                placeholder="e.g. 5500000"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
                required
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Capacity (Guests)
              <input
                type="number"
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Bedrooms
              <input
                type="number"
                value={bedrooms}
                onChange={(e) => setBedrooms(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Primary Hero Image URL
              <input
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                placeholder="https://images.unsplash.com/..."
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
          </div>

          {imageUrl ? (
            <div className="flex items-center gap-4 rounded-[1.2rem] bg-brand-white p-3">
              <img
                src={imageUrl}
                alt="Preview"
                className="h-16 w-24 rounded-lg object-cover"
                onError={(e) => ((e.target as HTMLElement).style.display = 'none')}
              />
              <span className="text-xs text-brand-ink/55">Primary Villa Hero Image Preview</span>
            </div>
          ) : null}

          <label className="block text-xs font-semibold text-brand-charcoal">
            Description & Highlights
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 w-full rounded-[1.2rem] border border-brand-stone bg-brand-white p-3 text-sm"
              rows={3}
              placeholder="Private cliffside retreat with infinity pool, pine forest panoramic views..."
            />
          </label>

          <label className="flex items-center gap-2.5 text-sm font-semibold text-brand-charcoal">
            <input
              type="checkbox"
              checked={isAvailable}
              onChange={(e) => setIsAvailable(e.target.checked)}
              className="h-4 w-4 rounded border-brand-stone text-brand-forest focus:ring-brand-forest"
            />
            Available for immediate booking
          </label>
        </div>
      ) : null}

      {/* Tab 2: Amenities Picker */}
      {formTab === 'amenities' ? (
        <div className="space-y-4">
          <p className="text-xs text-brand-ink/65">
            Select the luxury amenities and fixtures provided at this villa:
          </p>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {amenities.map((a) => {
              const selected = selectedAmenityIds.includes(a.id);
              return (
                <button
                  key={a.id}
                  type="button"
                  onClick={() => toggleAmenity(a.id)}
                  className={`flex items-center gap-2.5 rounded-[1rem] p-3 text-left text-xs font-semibold transition-all ${
                    selected
                      ? 'border-2 border-brand-forest bg-brand-forest/10 text-brand-forest shadow-sm'
                      : 'border border-brand-stone bg-brand-white text-brand-charcoal hover:border-brand-forest/50'
                  }`}
                >
                  <span className="text-base">{a.iconCode || a.icon || '✨'}</span>
                  <span className="truncate">{a.name}</span>
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      {/* Tab 3: Gallery Images */}
      {formTab === 'gallery' ? (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-xs text-brand-ink/65">Add multiple high-resolution photo URLs for the villa gallery:</p>
            <button
              type="button"
              onClick={handleAddGalleryImage}
              className="rounded-full border border-brand-forest bg-brand-white px-3.5 py-1 text-xs font-semibold text-brand-forest hover:bg-brand-forest hover:text-brand-white"
            >
              + Add Image URL
            </button>
          </div>
          <div className="space-y-3">
            {galleryImages.map((url, idx) => (
              <div key={idx} className="flex items-center gap-2">
                <input
                  value={url}
                  onChange={(e) => handleUpdateGalleryImage(idx, e.target.value)}
                  placeholder={`https://images.nhuvillas.com/villa-${idx + 1}.jpg`}
                  className="h-11 flex-1 rounded-full border border-brand-stone bg-brand-white px-4 text-xs"
                />
                {url ? (
                  <img
                    src={url}
                    alt=""
                    className="h-11 w-11 shrink-0 rounded-lg object-cover"
                    onError={(e) => ((e.target as HTMLElement).style.display = 'none')}
                  />
                ) : null}
                <button
                  type="button"
                  onClick={() => handleRemoveGalleryImage(idx)}
                  className="rounded-full border border-brand-coral/40 px-3 py-2 text-xs font-semibold text-brand-coral hover:bg-brand-coral hover:text-brand-white"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {/* Tab 4: Location & Surroundings */}
      {formTab === 'location' ? (
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Latitude (GPS)
              <input
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Longitude (GPS)
              <input
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
          </div>

          <label className="block text-xs font-semibold text-brand-charcoal">
            Driving Directions / Access Notes
            <textarea
              value={directions}
              onChange={(e) => setDirections(e.target.value)}
              className="mt-1 w-full rounded-[1.2rem] border border-brand-stone bg-brand-white p-3 text-sm"
              rows={2}
              placeholder="From Da Lat city center, take Tran Thanh Tong street towards Tuyen Lam Lake (15 mins)..."
            />
          </label>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Nearby Airport
              <input
                value={nearbyAirport}
                onChange={(e) => setNearbyAirport(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Nearby Attractions
              <input
                value={nearbyAttractions}
                onChange={(e) => setNearbyAttractions(e.target.value)}
                placeholder="Tuyen Lam Lake (500m), Truc Lam Zen Monastery (2km)"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Nearby Fine Dining & Restaurants
              <input
                value={nearbyRestaurants}
                onChange={(e) => setNearbyRestaurants(e.target.value)}
                placeholder="The Lake House Restaurant, Pine Grill"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Nearby Cafes
              <input
                value={nearbyCafes}
                onChange={(e) => setNearbyCafes(e.target.value)}
                placeholder="Windmills Cafe, Cheo Veooo"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
          </div>
        </div>
      ) : null}

      {/* Tab 5: Policies & Stay Rules */}
      {formTab === 'policies' ? (
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <label className="block text-xs font-semibold text-brand-charcoal">
              Check-in Time
              <input
                value={checkInTime}
                onChange={(e) => setCheckInTime(e.target.value)}
                placeholder="14:00"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Check-out Time
              <input
                value={checkOutTime}
                onChange={(e) => setCheckOutTime(e.target.value)}
                placeholder="12:00"
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Minimum Stay (Nights)
              <input
                type="number"
                value={minimumStay}
                onChange={(e) => setMinimumStay(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
            <label className="block text-xs font-semibold text-brand-charcoal">
              Maximum Stay (Nights)
              <input
                type="number"
                value={maximumStay}
                onChange={(e) => setMaximumStay(e.target.value)}
                className="mt-1 h-11 w-full rounded-full border border-brand-stone bg-brand-white px-4 text-sm"
              />
            </label>
          </div>

          <label className="block text-xs font-semibold text-brand-charcoal">
            House Rules & Stay Etiquette
            <textarea
              value={houseRules}
              onChange={(e) => setHouseRules(e.target.value)}
              className="mt-1 w-full rounded-[1.2rem] border border-brand-stone bg-brand-white p-3 text-sm"
              rows={3}
            />
          </label>

          <label className="block text-xs font-semibold text-brand-charcoal">
            Booking & Cancellation Policies
            <textarea
              value={policies}
              onChange={(e) => setPolicies(e.target.value)}
              className="mt-1 w-full rounded-[1.2rem] border border-brand-stone bg-brand-white p-3 text-sm"
              rows={3}
            />
          </label>
        </div>
      ) : null}

      {error ? <p className="text-xs text-brand-coral">{error}</p> : null}

      <div className="flex items-center justify-between border-t border-brand-stone/60 pt-4">
        <div className="flex gap-2">
          {formTab !== 'basic' ? (
            <button
              type="button"
              onClick={() => {
                const tabs: Array<'basic' | 'amenities' | 'gallery' | 'location' | 'policies'> = [
                  'basic',
                  'amenities',
                  'gallery',
                  'location',
                  'policies',
                ];
                const prevIdx = tabs.indexOf(formTab) - 1;
                if (prevIdx >= 0) setFormTab(tabs[prevIdx]);
              }}
              className="rounded-full border border-brand-stone px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-brand-ink/72"
            >
              ← Previous Section
            </button>
          ) : null}
          {formTab !== 'policies' ? (
            <button
              type="button"
              onClick={() => {
                const tabs: Array<'basic' | 'amenities' | 'gallery' | 'location' | 'policies'> = [
                  'basic',
                  'amenities',
                  'gallery',
                  'location',
                  'policies',
                ];
                const nextIdx = tabs.indexOf(formTab) + 1;
                if (nextIdx < tabs.length) setFormTab(tabs[nextIdx]);
              }}
              className="rounded-full border border-brand-forest/60 px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-brand-forest"
            >
              Next Section →
            </button>
          ) : null}
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="min-h-11 rounded-full border border-brand-stone px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/72"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={save.isPending}
            className="min-h-11 rounded-full bg-brand-forest px-7 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50"
          >
            {save.isPending ? 'Saving Villa...' : existing ? 'Update Villa' : 'Create Villa'}
          </button>
        </div>
      </div>
    </form>
  );
}

function VouchersPanel({ data, isLoading, onChanged }: { data?: { vouchers: AdminVoucher[] }; isLoading: boolean; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [code, setCode] = useState('');
  const [amount, setAmount] = useState('10');
  const [quantity, setQuantity] = useState('1');
  const [percent, setPercent] = useState(false);
  const [expiryDate, setExpiryDate] = useState('');
  const [editing, setEditing] = useState<AdminVoucher | null>(null);

  const save = useMutation<{ message: string }, ApiError, void>({
    mutationFn: () => api.post<{ message: string }>(API_PATHS.admin.saveVoucher, {
      id: editing?.id,
      code: code || editing?.code,
      amount: Number(amount),
      quantity: Number(quantity),
      percent,
      expiryDate: expiryDate || undefined,
    }),
    onSuccess: () => {
      setCode('');
      setAmount('10');
      setQuantity('1');
      setPercent(false);
      setExpiryDate('');
      setEditing(null);
      onChanged();
    },
  });

  const del = useMutation<{ message: string }, ApiError, number>({
    mutationFn: (id) => api.delete<{ message: string }>(API_PATHS.admin.deleteVoucher(id)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'vouchers'] }),
  });

  function startEdit(v: AdminVoucher) {
    setEditing(v);
    setCode(v.code);
    setAmount(String(v.amount));
    setQuantity(String(v.quantity));
    setPercent(Boolean(v.percent));
    setExpiryDate(v.expiryDate ?? '');
  }

  if (isLoading) return <p className="mt-10 text-brand-ink/58">Loading vouchers...</p>;
  return (
    <div className="mt-10 grid gap-6 lg:grid-cols-[1fr_0.6fr]">
      <div className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <h2 className="text-2xl text-brand-charcoal">Vouchers</h2>
        {(!data || data.vouchers.length === 0) ? (
          <p className="mt-4 text-sm text-brand-ink/58">No vouchers yet.</p>
        ) : (
          <ul className="mt-4 space-y-2">
            {data.vouchers.map((v) => (
              <li key={v.id} className="flex items-center justify-between rounded-[1rem] bg-brand-white p-4 text-sm">
                <div>
                  <span className="font-semibold text-brand-charcoal">{v.code}</span>{' '}
                  · {v.percent ? `${v.amount}%` : formatVnd(v.amount)} · qty {v.quantity}
                  {' · used '}{v.usedCount ?? 0}
                  {v.expiryDate ? ` · exp ${v.expiryDate}` : ''}
                  {v.active === false ? ' · inactive' : ''}
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => startEdit(v)}
                    className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => del.mutate(v.id)}
                    className="rounded-full border border-brand-coral/30 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral"
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
      <form
        className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_20px_70px_rgba(32,52,43,0.08)]"
        onSubmit={(e) => { e.preventDefault(); save.mutate(); }}
      >
        <h2 className="text-2xl text-brand-charcoal">{editing ? 'Edit voucher' : 'New voucher'}</h2>
        <div className="mt-5 grid gap-4">
          <label>
            <span className="text-sm font-semibold text-brand-charcoal">Code</span>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              placeholder={editing ? '(unchanged)' : 'SUMMER25'}
              className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4"
            />
          </label>
          <label>
            <span className="text-sm font-semibold text-brand-charcoal">Amount</span>
            <input
              type="number"
              value={amount}
              min={0}
              onChange={(e) => setAmount(e.target.value)}
              className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4"
            />
          </label>
          <label>
            <span className="text-sm font-semibold text-brand-charcoal">Quantity</span>
            <input
              type="number"
              value={quantity}
              min={1}
              onChange={(e) => setQuantity(e.target.value)}
              className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4"
            />
          </label>
          <label>
            <span className="text-sm font-semibold text-brand-charcoal">Expiry date</span>
            <input
              type="date"
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              className="mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-4"
            />
          </label>
          <label className="flex items-center gap-3">
            <input type="checkbox" checked={percent} onChange={(e) => setPercent(e.target.checked)} />
            <span className="text-sm">Treat amount as percent (%)</span>
          </label>
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={save.isPending}
              className="min-h-12 flex-1 rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white disabled:opacity-50"
            >
              {save.isPending ? 'Saving...' : editing ? 'Update voucher' : 'Create voucher'}
            </button>
            {editing ? (
              <button
                type="button"
                onClick={() => { setEditing(null); setCode(''); setAmount('10'); setQuantity('1'); setPercent(false); setExpiryDate(''); }}
                className="min-h-12 rounded-full border border-brand-stone px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/72"
              >
                Cancel
              </button>
            ) : null}
          </div>
        </div>
      </form>
    </div>
  );
}

function UsersPanel({
  data, isLoading, onUpdateRole, onSetDisabled, onDelete,
}: {
  data?: AdminUser[];
  isLoading: boolean;
  onUpdateRole: (id: number, role: string) => void;
  onSetDisabled: (id: number, disabled: boolean, reason?: string) => void;
  onDelete: (id: number) => void;
}) {
  const [detailId, setDetailId] = useState<number | null>(null);
  const [reasonById, setReasonById] = useState<Record<number, string>>({});
  if (isLoading) return <p className="mt-10 text-brand-ink/58">Loading users...</p>;
  if (!data) return <p className="mt-10 text-brand-ink/58">No data.</p>;
  const selected = data.find((u) => u.id === detailId);
  return (
    <div className="mt-10 grid gap-6 xl:grid-cols-[1fr_360px]">
      <div className="overflow-x-auto rounded-[2rem] bg-brand-paper shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <table className="w-full min-w-[980px] text-left text-sm">
          <thead className="bg-brand-white text-xs uppercase tracking-[0.14em] text-brand-ink/58">
            <tr>
              <th className="p-4">Username</th>
              <th className="p-4">Email</th>
              <th className="p-4">Name</th>
              <th className="p-4">Verified</th>
              <th className="p-4">Status</th>
              <th className="p-4">Role</th>
              <th className="p-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.map((u) => (
              <tr key={u.id} className="border-t border-brand-stone/40 align-top">
                <td className="p-4 font-semibold text-brand-charcoal">{u.username}</td>
                <td className="p-4">{u.email}</td>
                <td className="p-4">{u.fullName || '-'}</td>
                <td className="p-4">{u.emailVerified ? 'Yes' : 'No'}</td>
                <td className="p-4">{u.disabled ? 'Disabled' : 'Active'}</td>
                <td className="p-4">
                  <select
                    defaultValue={u.role}
                    onChange={(e) => onUpdateRole(u.id, e.target.value)}
                    className="rounded-full border border-brand-stone bg-brand-white px-3 py-2 text-xs"
                  >
                    <option value="USER">USER</option>
                    <option value="STAFF">STAFF</option>
                    <option value="MANAGER">MANAGER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td className="p-4">
                  <div className="flex flex-wrap gap-2">
                    <button type="button" onClick={() => setDetailId(u.id)} className="rounded-full border border-brand-forest px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-forest">
                      Detail
                    </button>
                    <button type="button" onClick={() => onSetDisabled(u.id, !u.disabled, reasonById[u.id] || 'Updated by admin')} className="rounded-full border border-brand-stone px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-ink/70">
                      {u.disabled ? 'Enable' : 'Disable'}
                    </button>
                    <button type="button" onClick={() => onDelete(u.id)} className="rounded-full border border-brand-coral/40 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-brand-coral">
                      Delete
                    </button>
                  </div>
                  <input
                    value={reasonById[u.id] ?? ''}
                    onChange={(e) => setReasonById((prev) => ({ ...prev, [u.id]: e.target.value }))}
                    placeholder="Disable reason"
                    className="mt-2 h-9 w-full rounded-full border border-brand-stone bg-brand-white px-3 text-xs"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <aside className="rounded-[2rem] bg-brand-paper p-6 shadow-[0_20px_70px_rgba(32,52,43,0.08)]">
        <h2 className="text-xl text-brand-charcoal">User detail</h2>
        {selected ? (
          <dl className="mt-4 space-y-3 text-sm">
            {[
              ['ID', selected.id],
              ['Username', selected.username],
              ['Email', selected.email],
              ['Full name', selected.fullName || '-'],
              ['Role', selected.role],
              ['Verified', selected.emailVerified ? 'Yes' : 'No'],
              ['Status', selected.disabled ? 'Disabled' : 'Active'],
              ['Disabled reason', selected.disabledReason || '-'],
              ['Created', selected.createdAt || '-'],
            ].map(([label, value]) => (
              <div key={String(label)}>
                <dt className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-ink/45">{label}</dt>
                <dd className="mt-1 text-brand-charcoal">{value}</dd>
              </div>
            ))}
          </dl>
        ) : (
          <p className="mt-4 text-sm text-brand-ink/58">Select a user to inspect account details.</p>
        )}
      </aside>
    </div>
  );
}
