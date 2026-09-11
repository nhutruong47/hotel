const API_BASE = '/api/v1';
const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';

type RequestOptions = Omit<RequestInit, 'body'> & {
    json?: unknown;
    body?: BodyInit;
    csrfRetry?: boolean;
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

function readCookie(name: string): string | undefined {
    if (typeof document === 'undefined') return undefined;
    const prefix = `${encodeURIComponent(name)}=`;
    return document.cookie
        .split(';')
        .map((part) => part.trim())
        .find((part) => part.startsWith(prefix))
        ?.slice(prefix.length);
}

function isUnsafeMethod(method?: string): boolean {
    const normalized = (method ?? 'GET').toUpperCase();
    return !['GET', 'HEAD', 'OPTIONS'].includes(normalized);
}

async function ensureCsrfToken(): Promise<string | undefined> {
    let token = readCookie(CSRF_COOKIE);
    if (token || typeof document === 'undefined') {
        return token ? decodeURIComponent(token) : undefined;
    }

    await fetch(`${API_BASE}/health`, {
        credentials: 'include',
        headers: { Accept: 'application/json' },
    }).catch(() => undefined);

    token = readCookie(CSRF_COOKIE);
    return token ? decodeURIComponent(token) : undefined;
}

async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
    const { json, headers, body, csrfRetry = false, ...rest } = opts;
    const finalHeaders: Record<string, string> = {
        Accept: 'application/json',
        ...(headers as Record<string, string> | undefined),
    };
    let finalBody: BodyInit | undefined = body;
    if (json !== undefined) {
        finalHeaders['Content-Type'] = 'application/json';
        finalBody = JSON.stringify(json);
    }
    if (isUnsafeMethod(rest.method) && !finalHeaders[CSRF_HEADER]) {
        const token = await ensureCsrfToken();
        if (token) {
            finalHeaders[CSRF_HEADER] = token;
        }
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
        if (
            !csrfRetry &&
            res.status === 403 &&
            err?.code === 'CSRF_TOKEN_INVALID' &&
            isUnsafeMethod(rest.method)
        ) {
            return request<T>(path, { ...opts, csrfRetry: true });
        }
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

async function download(path: string, opts: RequestOptions = {}): Promise<{ blob: Blob; filename?: string }> {
    const { json, headers, body, csrfRetry = false, ...rest } = opts;
    const finalHeaders: Record<string, string> = {
        Accept: '*/*',
        ...(headers as Record<string, string> | undefined),
    };
    let finalBody: BodyInit | undefined = body;
    if (json !== undefined) {
        finalHeaders['Content-Type'] = 'application/json';
        finalBody = JSON.stringify(json);
    }
    if (isUnsafeMethod(rest.method) && !finalHeaders[CSRF_HEADER]) {
        const token = await ensureCsrfToken();
        if (token) {
            finalHeaders[CSRF_HEADER] = token;
        }
    }

    const res = await fetch(`${API_BASE}${path}`, {
        ...rest,
        credentials: 'include',
        headers: finalHeaders,
        body: finalBody,
    });

    if (!res.ok) {
        const payload = await res.json().catch(() => null);
        const err = payload?.error;
        if (
            !csrfRetry &&
            res.status === 403 &&
            err?.code === 'CSRF_TOKEN_INVALID' &&
            isUnsafeMethod(rest.method)
        ) {
            return download(path, { ...opts, csrfRetry: true });
        }
        throw new ApiError(
            res.status,
            err?.code ?? `HTTP_${res.status}`,
            err?.message ?? `Download failed (${res.status})`,
            payload,
            res.headers.get('X-Request-Id') ?? undefined,
        );
    }

    const disposition = res.headers.get('content-disposition') ?? '';
    const filename = disposition.match(/filename="?([^";]+)"?/i)?.[1];
    return { blob: await res.blob(), filename };
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
    download,
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
    profilePreferences: '/profile/preferences',
    rooms: '/rooms',
    room: (id: number | string) => `/rooms/${id}`,
    roomBookedDates: (id: number | string) => `/rooms/${id}/booked-dates`,
    bookings: {
        list: '/bookings',
        booking: (id: number | string) => `/bookings/${id}`,
        timeline: (id: number | string) => `/bookings/${id}/timeline`,
        cancel: (id: number | string) => `/bookings/${id}/cancel`,
        validateVoucher: '/bookings/vouchers/validate',
    },
    vouchers: {
        validate: '/vouchers/validate',
        preview: '/vouchers/preview',
    },
    wishlist: '/wishlist',
    wishlistToggle: '/wishlist/toggle',
    wishlistItem: (roomId: number | string) => `/wishlist/${roomId}`,
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
        stripeCheckout: '/payments/stripe-checkout',
        stripeSession: (sessionId: string) => `/payments/stripe-session/${sessionId}`,
        intent: '/payments/intent',
    },
    blogs: '/blogs',
    blog: (id: number | string) => `/blogs/${id}`,
    ai: {
        history: '/ai/history',
        recommend: '/ai/recommend',
        clear: '/ai/history',
    },
    admin: {
        dashboard: '/admin/dashboard',
        bookings: '/admin/bookings',
        todayCheckin: '/admin/bookings/today-checkin',
        todayCheckout: '/admin/bookings/today-checkout',
        completeBooking: (id: number | string) => `/admin/bookings/${id}/complete`,
        cancelBooking: (id: number | string) => `/admin/bookings/${id}/cancel`,
        checkInBooking: (id: number | string) => `/admin/bookings/${id}/checkin`,
        checkOutBooking: (id: number | string) => `/admin/bookings/${id}/checkout`,
        noShowBooking: (id: number | string) => `/admin/bookings/${id}/no-show`,
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
        reports: '/admin/reports',
        reportExcel: '/admin/reports/excel',
        reportPdf: '/admin/reports/pdf',
        reportExcelByRoom: '/admin/reports/excel/room',
        reportPdfByRoom: '/admin/reports/pdf/room',
        contacts: '/admin/contacts',
        updateContactStatus: (id: number | string) => `/admin/contacts/${id}/status`,
        promotions: '/admin/promotions',
        savePromotion: '/admin/promotions',
        deletePromotion: (id: number | string) => `/admin/promotions/${id}`,
        reviews: '/admin/reviews',
        replyReview: (id: number | string) => `/admin/reviews/${id}/reply`,
        hideReview: (id: number | string) => `/admin/reviews/${id}/hide`,
        deleteReview: (id: number | string) => `/admin/reviews/${id}`,
        auditLogs: '/admin/audit-logs',
        revenue: '/admin/revenue',
        occupancy: '/admin/occupancy',
        refunds: '/admin/refunds',
    },
    users: {
        list: '/users',
        detail: (id: number | string) => `/users/${id}`,
        updateRole: (id: number | string) => `/users/${id}/role`,
        setDisabled: (id: number | string) => `/users/${id}/disabled`,
        delete: (id: number | string) => `/users/${id}`,
    },
    contact: '/contact',
    accountDashboard: '/account/dashboard',
    notifications: {
        list: '/notifications',
        markRead: (id: number | string) => `/notifications/${id}/read`,
        markAllRead: '/notifications/read-all',
        delete: (id: number | string) => `/notifications/${id}`,
    },
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
    inspections: {
        all: '/inspections',
        stats: '/inspections/stats',
        byRoom: (roomId: number | string) => `/inspections/room/${roomId}`,
        latestByRoom: (roomId: number | string) => `/inspections/room/${roomId}/latest`,
        detail: (id: number | string) => `/inspections/${id}`,
        create: '/inspections',
        update: (id: number | string) => `/inspections/${id}`,
        delete: (id: number | string) => `/inspections/${id}`,
    },
    maintenances: {
        all: '/admin/maintenances',
        schedule: '/admin/maintenances',
        cancel: (id: number | string) => `/admin/maintenances/${id}`,
    },
};
