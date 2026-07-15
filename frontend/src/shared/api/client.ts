const API_BASE = '/api/v1';

type RequestOptions = Omit<RequestInit, 'body'> & {
    json?: unknown;
    body?: BodyInit;
};

class ApiError extends Error {
    status: number;
    code: string;
    requestId?: string;
    payload?: unknown;

    constructor(status: number, code: string, message: string, payload?: unknown, requestId?: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.code = code;
        this.payload = payload;
        this.requestId = requestId;
    }
}

export { ApiError };

async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
    const { json, headers, body, ...rest } = opts;
    const finalHeaders: Record<string, string> = {
        Accept: 'application/json',
        ...(headers as Record<string, string> | undefined),
    };
    let finalBody: BodyInit | undefined = body;
    if (json !== undefined) {
        finalHeaders['Content-Type'] = 'application/json';
        finalBody = JSON.stringify(json);
    }

    const res = await fetch(`${API_BASE}${path}`, {
        ...rest,
        credentials: 'include',
        headers: finalHeaders,
        body: finalBody,
    });

    const contentType = res.headers.get('content-type') || '';
    const isJson = contentType.includes('application/json');
    const payload = isJson ? await res.json().catch(() => null) : null;
    const requestId = res.headers.get('X-Request-Id') ?? undefined;

    if (!res.ok) {
        const err = payload?.error;
        if (res.status === 401 && !path.startsWith('/auth/')) {
            try {
                window.sessionStorage.removeItem('nhu.session');
            } catch {
                /* ignore */
            }
            if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
                const redirect = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.assign(`/login?redirect=${redirect}&reason=login_required`);
            }
        }
        throw new ApiError(
            res.status,
            err?.code ?? `HTTP_${res.status}`,
            err?.message ?? `Request failed (${res.status})`,
            payload,
            requestId,
        );
    }

    if (payload && typeof payload === 'object' && 'data' in payload) {
        return payload.data as T;
    }
    return payload as T;
}

export const api = {
    get: <T>(path: string, opts?: RequestOptions) => request<T>(path, { ...opts, method: 'GET' }),
    post: <T>(path: string, json?: unknown, opts?: RequestOptions) =>
        request<T>(path, { ...opts, method: 'POST', json }),
    put: <T>(path: string, json?: unknown, opts?: RequestOptions) =>
        request<T>(path, { ...opts, method: 'PUT', json }),
    patch: <T>(path: string, json?: unknown, opts?: RequestOptions) =>
        request<T>(path, { ...opts, method: 'PATCH', json }),
    delete: <T>(path: string, opts?: RequestOptions) => request<T>(path, { ...opts, method: 'DELETE' }),
};

export const API_PATHS = {
    auth: {
        login: '/auth/login',
        register: '/auth/register',
        logout: '/auth/logout',
        session: '/auth/session',
        forgotPassword: '/auth/forgot-password',
        resetPassword: '/auth/reset-password',
        verify: (token: string) => `/auth/verify?token=${encodeURIComponent(token)}`,
    },
    profile: '/profile',
    profilePassword: '/profile/password',
    rooms: '/rooms',
    room: (id: number | string) => `/rooms/${id}`,
    roomBookedDates: (id: number | string) => `/rooms/${id}/booked-dates`,
    bookings: '/bookings',
    booking: (id: number | string) => `/bookings/${id}`,
    bookingTimeline: (id: number | string) => `/bookings/${id}/timeline`,
    bookingCancel: (id: number | string) => `/bookings/${id}/cancel`,
    bookingSubmitPayment: (id: number | string) => `/bookings/${id}/payment`,
    validateVoucher: '/bookings/vouchers/validate',
    vouchers: {
        validate: '/vouchers/validate',
        preview: '/vouchers/preview',
    },
    wishlist: '/wishlist',
    wishlistToggle: '/wishlist/toggle',
    reviews: {
        room: (roomId: number | string) => `/reviews/room/${roomId}`,
        mine: '/reviews/mine',
        forBooking: (bookingId: number | string) => `/reviews/booking/${bookingId}`,
        submitForBooking: (bookingId: number | string) => `/reviews/booking/${bookingId}`,
        update: (reviewId: number | string) => `/reviews/${reviewId}`,
        delete: (reviewId: number | string) => `/reviews/${reviewId}`,
    },
    payments: {
        all: '/payments',
        create: '/payments',
        complete: (id: number | string) => `/payments/${id}/complete`,
        fail: (id: number | string) => `/payments/${id}/fail`,
        refund: (id: number | string) => `/payments/${id}/refund`,
        byBooking: (bookingId: number | string) => `/payments/booking/${bookingId}`,
    },
    blogs: '/blogs',
    blog: (id: number | string) => `/blogs/${id}`,
    info: (slug: string) => `/info/${slug}`,
    ai: {
        history: '/ai/history',
        recommend: '/ai/recommend',
        clear: '/ai/history',
    },
    admin: {
        dashboard: '/admin/dashboard',
        bookings: '/admin/bookings',
        updateBookingStatus: (id: number | string) => `/admin/bookings/${id}/status`,
        approveBooking: (id: number | string) => `/admin/bookings/${id}/approve`,
        rejectBooking: (id: number | string) => `/admin/bookings/${id}/reject`,
        completeBooking: (id: number | string) => `/admin/bookings/${id}/complete`,
        cancelBooking: (id: number | string) => `/admin/bookings/${id}/cancel`,
        rooms: '/admin/rooms',
        saveRoom: '/admin/rooms',
        roomDetail: (id: number | string) => `/admin/rooms/${id}`,
        deleteRoom: (id: number | string) => `/admin/rooms/${id}`,
        vouchers: '/admin/vouchers',
        saveVoucher: '/admin/vouchers',
        deleteVoucher: (id: number | string) => `/admin/vouchers/${id}`,
        roomTypes: '/admin/room-types',
        saveRoomType: '/admin/room-types',
        deleteRoomType: (id: number | string) => `/admin/room-types/${id}`,
        amenities: '/admin/amenities',
        saveAmenity: '/admin/amenities',
        deleteAmenity: (id: number | string) => `/admin/amenities/${id}`,
        users: '/admin/users',
        reports: '/admin/reports',
    },
    users: {
        me: '/users/me',
        list: '/users',
        updateRole: (id: number | string) => `/users/${id}/role`,
        delete: (id: number | string) => `/users/${id}`,
    },
    contact: '/contact',
    health: '/health',
    promotions: {
        all: '/promotions',
        featured: '/promotions/featured',
        byCategory: (category: string) => `/promotions/category/${category}`,
        expired: '/promotions/expired',
        upcoming: '/promotions/upcoming',
        countdown: '/promotions/countdown',
        grouped: '/promotions/grouped',
        validate: '/promotions/validate',
        preview: '/promotions/preview',
    },
    faqs: {
        all: '/faqs',
        byCategory: (category: string) => `/faqs/category/${category}`,
        categories: '/faqs/categories',
        search: '/faqs/search',
        grouped: '/faqs/grouped',
        helpful: (id: number | string) => `/faqs/${id}/helpful`,
    },
};
