import type {
  ActivateSubscriptionResponse,
  AuthResponse,
  AvailabilityDayDto,
  AvailabilityResponse,
  Booking,
  BucketListItem,
  BulkAvailabilityRequest,
  ChatMessage,
  Conversation,
  CoTraveler,
  SavedPaymentMethod,
  CreateBookingRequest,
  Experience,
  Hospital,
  HostSubscription,
  Listing,
  MediaItem,
  MedicalStayPackage,
  MilesBalance,
  MilesHistoryResponse,
  NomadComment,
  NomadPost,
  PaymentOrder,
  PgPackage,
  QuickReplyTemplate,
  Review,
  ReviewStats,
  HostReviewStats,
  ICalFeed,
  OccupancyReport,
  PricingRule,
  RoomType,
  RoomTypeInclusion,
  SearchResponse,
  SubscriptionTier,
  UserProfile,
} from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Check if JWT token is about to expire (within 2 minutes) and proactively refresh.
 */
async function ensureFreshToken(options?: RequestInit): Promise<RequestInit | undefined> {
  if (typeof window === 'undefined') return options;
  const headers = options?.headers as Record<string, string> | undefined;
  if (!headers?.Authorization) return options;

  const token = headers.Authorization.replace('Bearer ', '');
  try {
    // Decode JWT payload (base64)
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expiresAt = payload.exp * 1000;
    const now = Date.now();
    // If token expires within 2 minutes, refresh proactively
    if (expiresAt - now < 120000) {
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        const res = await fetch(`${API_URL}/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        if (res.ok) {
          const data = await res.json();
          localStorage.setItem('access_token', data.accessToken);
          if (data.refreshToken) localStorage.setItem('refresh_token', data.refreshToken);
          document.cookie = `access_token=${data.accessToken}; path=/; max-age=${30 * 24 * 60 * 60}; SameSite=Lax`;
          return {
            ...options,
            headers: { ...headers, Authorization: `Bearer ${data.accessToken}` },
          };
        }
        // Proactive refresh failed — don't redirect here, let the 401 handler catch it
      }
    }
  } catch { /* token decode failed — let it proceed, 401 handler will catch */ }
  return options;
}

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  // Proactively refresh token if about to expire
  const freshOptions = await ensureFreshToken(options);

  const res = await fetch(`${API_URL}${path}`, {
    ...freshOptions,
    headers: {
      'Content-Type': 'application/json',
      ...freshOptions?.headers,
    },
    credentials: 'include',
    cache: 'no-store',
  });

  // Auto-refresh token on 401 — only for authenticated endpoints (those with Authorization header)
  const hasAuthHeader = freshOptions?.headers && ('Authorization' in (freshOptions.headers as Record<string, string>));
  if (res.status === 401 && typeof window !== 'undefined' && hasAuthHeader && !path.includes('/auth/')) {
    const refreshToken = localStorage.getItem('refresh_token');
    if (refreshToken) {
      let refreshRes: Response | null = null;
      try {
        refreshRes = await fetch(`${API_URL}/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // Network error on refresh — redirect to login
      }

      if (refreshRes?.ok) {
        const data = await refreshRes.json();
        localStorage.setItem('access_token', data.accessToken);
        if (data.refreshToken) localStorage.setItem('refresh_token', data.refreshToken);
        document.cookie = `access_token=${data.accessToken}; path=/; max-age=${30 * 24 * 60 * 60}; SameSite=Lax`;
        // Retry original request with new token
        const retryHeaders = { ...freshOptions?.headers } as Record<string, string>;
        retryHeaders['Authorization'] = `Bearer ${data.accessToken}`;
        const retryRes = await fetch(`${API_URL}${path}`, {
          ...freshOptions,
          headers: { 'Content-Type': 'application/json', ...retryHeaders },
          credentials: 'include',
        });
        if (!retryRes.ok) {
          const text = await retryRes.text().catch(() => 'Unknown error');
          throw new Error(text || `HTTP ${retryRes.status}`);
        }
        const text = await retryRes.text();
        return text ? JSON.parse(text) : (undefined as T);
      }
    }
    // Refresh failed or no refresh token — clear and redirect to login
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    document.cookie = 'access_token=; path=/; max-age=0';
    window.location.href = '/auth';
    throw new Error('Session expired. Please sign in again.');
  }

  if (!res.ok) {
    const text = await res.text().catch(() => 'Unknown error');
    throw new Error(text || `HTTP ${res.status}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

export const api = {
  /* ── Search ───────────────────────────────────────────────── */
  search: async (params: Record<string, string>): Promise<SearchResponse> => {
    try {
      const raw = await apiFetch<any>(
        `/api/v1/search/listings?${new URLSearchParams(params)}`
      );
      // Normalise search-service response to SearchResponse shape
      return {
        content: raw.content ?? raw.listings ?? [],
        totalElements: raw.totalElements ?? raw.totalHits ?? 0,
        totalPages: raw.totalPages ?? Math.ceil((raw.totalHits ?? 0) / (Number(params.size) || 20)),
        number: raw.number ?? raw.page ?? 0,
        size: raw.size ?? (Number(params.size) || 20),
        aggregations: raw.aggregations ?? undefined,
      };
    } catch {
      // If geo search (lat/lng), try /nearby endpoint as backup
      if (params.lat && params.lng) {
        console.warn('ES search failed for geo query — trying /nearby endpoint');
        try {
          const nearbyParams = new URLSearchParams();
          nearbyParams.set('lat', params.lat);
          nearbyParams.set('lng', params.lng);
          if (params.radiusKm) nearbyParams.set('radiusKm', params.radiusKm);
          if (params.type) nearbyParams.set('type', params.type);
          nearbyParams.set('limit', params.size || '20');
          const nearby = await apiFetch<any[]>(`/api/v1/search/nearby?${nearbyParams}`);
          return {
            content: nearby ?? [],
            totalElements: nearby?.length ?? 0,
            totalPages: 1,
            number: 0,
            size: Number(params.size) || 20,
          };
        } catch {
          console.warn('Nearby endpoint also failed — returning empty');
          return { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 };
        }
      }
      // Fallback to listing-service when search-service/Elasticsearch is unavailable
      console.warn('ES search failed — falling back to listing-service DB query');
      const listingParams = new URLSearchParams();
      if (params.city) listingParams.set('city', params.city);
      if (params.query) listingParams.set('city', params.query); // text search → city filter fallback
      if (params.size) listingParams.set('size', params.size);
      if (params.page) listingParams.set('page', params.page);
      if (params.type) listingParams.set('type', params.type);
      const minP = params.minPricePaise || params.priceMin;
      const maxP = params.maxPricePaise || params.priceMax;
      if (minP) listingParams.set('minPricePaise', minP);
      if (maxP) listingParams.set('maxPricePaise', maxP);
      return apiFetch<SearchResponse>(
        `/api/v1/listings?${listingParams}`
      );
    }
  },

  autocomplete: (q: string) =>
    apiFetch<string[]>(`/api/v1/search/autocomplete?q=${encodeURIComponent(q)}`),

  locationSuggest: (q: string) =>
    apiFetch<Record<string, import('@/types').LocationSuggestion[]>>(`/api/v1/locations/suggest?q=${encodeURIComponent(q)}`),

  /* ── Listings ─────────────────────────────────────────────── */
  getListing: (id: string) =>
    apiFetch<Listing>(`/api/v1/listings/${id}`),

  getListingMedia: (id: string) =>
    apiFetch<MediaItem[]>(
      `/api/v1/listings/${id}/media`
    ),

  getListingAvailability: (id: string, from: string, to: string) =>
    apiFetch<{ date: string; isAvailable: boolean }[]>(
      `/api/v1/listings/${id}/availability?from=${from}&to=${to}`
    ),

  getRoomTypes: (listingId: string) =>
    apiFetch<RoomType[]>(`/api/v1/listings/${listingId}/room-types`),

  getAvailableRoomTypes: (listingId: string, checkIn: string, checkOut: string) =>
    apiFetch<RoomType[]>(
      `/api/v1/listings/${listingId}/room-types/available?checkIn=${checkIn}&checkOut=${checkOut}`
    ),

  /* ── Auth ─────────────────────────────────────────────────── */
  sendOtp: (phone: string) =>
    apiFetch<void>('/api/v1/auth/otp/send', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    }),

  verifyOtp: (phone: string, otp: string, name?: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/otp/verify', {
      method: 'POST',
      body: JSON.stringify({ phone, otp, name }),
    }),

  googleSignIn: (idToken: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/google/signin', {
      method: 'POST',
      body: JSON.stringify({ idToken }),
    }),

  appleSignIn: (identityToken: string, name?: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/apple/signin', {
      method: 'POST',
      body: JSON.stringify({ identityToken, name }),
    }),

  sendWhatsAppOtp: (phone: string) =>
    apiFetch<void>('/api/v1/auth/otp/whatsapp/send', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    }),

  trustDevice: (deviceFingerprint: string, deviceName: string, token: string) =>
    apiFetch<{ status: string }>('/api/v1/auth/device/trust', {
      method: 'POST',
      body: JSON.stringify({ deviceFingerprint, deviceName }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': localStorage.getItem('user_id') || '' },
    }),

  checkTrustedDevice: (deviceFingerprint: string, phone?: string, email?: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/device/check', {
      method: 'POST',
      body: JSON.stringify({ deviceFingerprint, phone, email }),
    }),

  removeTrustedDevice: (deviceFingerprint: string, token: string) =>
    apiFetch<void>('/api/v1/auth/device/trust', {
      method: 'DELETE',
      body: JSON.stringify({ deviceFingerprint }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': localStorage.getItem('user_id') || '' },
    }),

  sendEmailOtp: (email: string) =>
    apiFetch<void>('/api/v1/auth/otp/email/send', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  verifyEmailOtp: (email: string, otp: string, name?: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/otp/email/verify', {
      method: 'POST',
      body: JSON.stringify({ email, otp, name }),
    }),

  checkAuthMethod: (email: string) =>
    apiFetch<{ exists: boolean; hasPassword: boolean; methods: string[] }>(
      `/api/v1/auth/check-method?email=${encodeURIComponent(email)}`
    ),

  passwordSignIn: (email: string, password: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/password/signin', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  setPassword: (password: string, token: string) =>
    apiFetch<{ status: string }>('/api/v1/auth/password/set', {
      method: 'POST',
      body: JSON.stringify({ password }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': localStorage.getItem('user_id') || '' },
    }),

  changePassword: (oldPassword: string, newPassword: string, token: string) =>
    apiFetch<{ status: string }>('/api/v1/auth/password/change', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': localStorage.getItem('user_id') || '' },
    }),

  resetPassword: (email: string, otp: string, newPassword: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/password/reset', {
      method: 'POST',
      body: JSON.stringify({ email, otp, newPassword }),
    }),

  refreshToken: (refreshToken: string) =>
    apiFetch<AuthResponse>('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),

  logoutAll: () =>
    apiFetch<void>('/api/v1/auth/logout-all', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') || '' }),
    }),

  /* ── Profile ─────────────────────────────────────────────── */
  getMyProfile: (token: string) =>
    apiFetch<UserProfile>('/api/v1/users/me', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateMyProfile: (data: Partial<UserProfile>, token: string) =>
    apiFetch<UserProfile>('/api/v1/users/me', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  uploadAvatar: async (file: File, token: string) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API_URL}/api/v1/users/me/avatar`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    });
    if (!res.ok) throw new Error('Upload failed');
    return res.json() as Promise<{ avatarUrl: string }>;
  },

  getHostProfile: (hostId: string) =>
    apiFetch<{
      id: string;
      name: string;
      avatarUrl: string;
      bio: string;
      languages: string;
      verificationLevel: string;
      trustScore: number;
      trustBadge: string;
      responseRate: number;
      avgResponseMinutes: number;
      totalHostReviews: number;
      hostType: string;
      selfieVerified: boolean;
      createdAt: string;
      lastActiveAt: string;
    }>(`/api/v1/users/hosts/${hostId}`),

  /* ── Co-Travelers ──────────────────────────────────────────── */
  getCoTravelers: (token: string) =>
    apiFetch<CoTraveler[]>('/api/v1/users/me/co-travelers', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  createCoTraveler: (data: { firstName: string; lastName: string; dateOfBirth?: string; gender?: string }, token: string) =>
    apiFetch<CoTraveler>('/api/v1/users/me/co-travelers', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateCoTraveler: (id: string, data: { firstName: string; lastName: string; dateOfBirth?: string; gender?: string }, token: string) =>
    apiFetch<CoTraveler>(`/api/v1/users/me/co-travelers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteCoTraveler: (id: string, token: string) =>
    apiFetch<void>(`/api/v1/users/me/co-travelers/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Payment Methods ───────────────────────────────────────── */
  getPaymentMethods: (token: string) =>
    apiFetch<SavedPaymentMethod[]>('/api/v1/users/me/payment-methods', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  createPaymentMethod: (data: Partial<SavedPaymentMethod>, token: string) =>
    apiFetch<SavedPaymentMethod>('/api/v1/users/me/payment-methods', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updatePaymentMethod: (id: string, data: Partial<SavedPaymentMethod>, token: string) =>
    apiFetch<SavedPaymentMethod>(`/api/v1/users/me/payment-methods/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  setDefaultPaymentMethod: (id: string, token: string) =>
    apiFetch<void>(`/api/v1/users/me/payment-methods/${id}/default`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}` },
    }),

  deletePaymentMethod: (id: string, token: string) =>
    apiFetch<void>(`/api/v1/users/me/payment-methods/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── PG Packages ─────────────────────────────────────────── */
  getPgPackages: (listingId: string) =>
    apiFetch<PgPackage[]>(`/api/v1/listings/${listingId}/packages`),

  createPgPackage: (listingId: string, data: any, token: string) =>
    apiFetch<PgPackage>(`/api/v1/listings/${listingId}/packages`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updatePgPackage: (listingId: string, packageId: string, data: any, token: string) =>
    apiFetch<PgPackage>(`/api/v1/listings/${listingId}/packages/${packageId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deletePgPackage: (listingId: string, packageId: string, token: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/packages/${packageId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Bookings ─────────────────────────────────────────────── */
  createBooking: (data: CreateBookingRequest, token: string) =>
    apiFetch<Booking>('/api/v1/bookings', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getMyBookings: (token: string) =>
    apiFetch<Booking[]>('/api/v1/bookings/me', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getBooking: (id: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  cancelBooking: (id: string, reason: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}/cancel?reason=${encodeURIComponent(reason)}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ── Booking Guest Management ──────────────────────────────
  getBookingGuests: (bookingId: string, token: string) =>
    apiFetch<any[]>(`/api/v1/bookings/${bookingId}/guests`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  addBookingGuest: (bookingId: string, guest: {
    fullName: string; email?: string; phone?: string; age?: number;
    idType?: string; idNumber?: string; roomAssignment?: string; isPrimary?: boolean;
  }, token: string) =>
    apiFetch<any>(`/api/v1/bookings/${bookingId}/guests`, {
      method: 'POST',
      body: JSON.stringify(guest),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateBookingGuest: (bookingId: string, guestId: string, guest: {
    fullName: string; email?: string; phone?: string; age?: number;
    idType?: string; idNumber?: string; roomAssignment?: string; isPrimary?: boolean;
  }, token: string) =>
    apiFetch<any>(`/api/v1/bookings/${bookingId}/guests/${guestId}`, {
      method: 'PUT',
      body: JSON.stringify(guest),
      headers: { Authorization: `Bearer ${token}` },
    }),

  removeBookingGuest: (bookingId: string, guestId: string, token: string) =>
    apiFetch<void>(`/api/v1/bookings/${bookingId}/guests/${guestId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  confirmBooking: (id: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}/confirm`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Host Bookings ─────────────────────────────────────── */
  getHostBookings: (token: string) =>
    apiFetch<Booking[]>('/api/v1/bookings/host', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  checkInBooking: (id: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}/check-in`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  completeBooking: (id: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}/complete`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  markNoShow: (id: string, token: string) =>
    apiFetch<Booking>(`/api/v1/bookings/${id}/no-show`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Payments ─────────────────────────────────────────────── */
  createPaymentOrder: (bookingId: string, amountPaise: number, token: string) =>
    apiFetch<PaymentOrder>(
      `/api/v1/payments/order?bookingId=${bookingId}&amountPaise=${amountPaise}`,
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }
    ),

  verifyPayment: (
    data: {
      bookingId: string;
      razorpayOrderId: string;
      razorpayPaymentId: string;
      razorpaySignature: string;
    },
    token: string
  ) =>
    apiFetch<void>('/api/v1/payments/verify', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Reviews ──────────────────────────────────────────────── */
  getListingReviews: async (listingId: string): Promise<Review[]> => {
    const raw = await apiFetch<any>(`/api/v1/reviews/listing/${listingId}?size=50&sort=createdAt,desc`);
    return Array.isArray(raw) ? raw : (raw?.content ?? []);
  },

  getListingReviewStats: (listingId: string): Promise<ReviewStats> =>
    apiFetch<ReviewStats>(`/api/v1/reviews/listing/${listingId}/stats`),

  createReview: (
    data: {
      bookingId: string; rating: number; comment?: string;
      guestPhotoUrls?: string[];
      ratingCleanliness?: number; ratingLocation?: number; ratingValue?: number;
      ratingCommunication?: number; ratingCheckIn?: number; ratingAccuracy?: number;
      ratingStaff?: number; ratingFacilities?: number; ratingComfort?: number; ratingFreeWifi?: number;
      categoryComments?: string;
    },
    token: string
  ) =>
    apiFetch<Review>('/api/v1/reviews', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateReview: (reviewId: string, data: { rating?: number; comment?: string }, token: string) =>
    apiFetch<Review>(`/api/v1/reviews/${reviewId}?${new URLSearchParams(
      Object.entries(data).filter(([, v]) => v != null).map(([k, v]) => [k, String(v)])
    )}`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteReview: (reviewId: string, token: string) =>
    apiFetch<void>(`/api/v1/reviews/${reviewId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  // Host review management
  getHostReviews: async (token: string, filter = 'all', page = 0, size = 10): Promise<any> =>
    apiFetch<any>(`/api/v1/reviews/host/me?filter=${filter}&page=${page}&size=${size}&sort=createdAt,desc`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getHostReviewStats: (token: string): Promise<HostReviewStats> =>
    apiFetch<HostReviewStats>('/api/v1/reviews/host/me/stats', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  addHostReply: (reviewId: string, reply: string, token: string): Promise<Review> =>
    apiFetch<Review>(`/api/v1/reviews/${reviewId}/reply?reply=${encodeURIComponent(reply)}`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteHostReply: (reviewId: string, token: string): Promise<void> =>
    apiFetch<void>(`/api/v1/reviews/${reviewId}/reply`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Media Management ─────────────────────────────────────── */
  presignUpload: (listingId: string, mediaType: string, contentType: string) =>
    apiFetch<{ mediaId: string; uploadUrl: string; s3Key: string; cdnUrl: string }>(
      `/api/v1/media/upload/presign?listingId=${listingId}&mediaType=${encodeURIComponent(mediaType)}&contentType=${encodeURIComponent(contentType)}`,
      { method: 'POST' }
    ),

  confirmUpload: (data: { mediaId: string; listingId: string; s3Key: string; mediaType: string; durationSeconds: number }) =>
    apiFetch<void>(`/api/v1/media/upload/confirm`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  deleteMedia: (listingId: string, mediaId: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/media/${mediaId}`, {
      method: 'DELETE',
    }),

  reorderMedia: (listingId: string, mediaIds: string[]) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/media/reorder`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(mediaIds),
    }),

  updateMediaCategory: (listingId: string, mediaId: string, category: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/media/${mediaId}/category?category=${encodeURIComponent(category)}`, {
      method: 'PUT',
    }),

  /* ── Host ─────────────────────────────────────────────────── */
  getMyListings: (token: string) =>
    apiFetch<Listing[]>('/api/v1/listings/mine', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateListing: (id: string, data: any, token: string) =>
    apiFetch<Listing>(`/api/v1/listings/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  toggleAashray: (id: string, body: { aashrayReady: boolean; aashrayDiscountPercent?: number; longTermMonthlyPaise?: number; minStayDays?: number }, token: string) =>
    apiFetch<Listing>(`/api/v1/listings/${id}/aashray`, {
      method: 'POST',
      body: JSON.stringify(body),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Host KYC ─────────────────────────────────────────────── */
  getKyc: (token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc', {
      headers: { Authorization: `Bearer ${token}` },
    }),
  updateKycIdentity: (data: any, token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc/identity', {
      method: 'PUT', body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),
  updateKycAddress: (data: any, token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc/address', {
      method: 'PUT', body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),
  updateKycBank: (data: any, token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc/bank', {
      method: 'PUT', body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),
  updateKycBusiness: (data: any, token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc/business', {
      method: 'PUT', body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),
  submitKyc: (token: string) =>
    apiFetch<any>('/api/v1/users/me/kyc/submit', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Miles ────────────────────────────────────────────────── */
  getMilesBalance: (token: string) =>
    apiFetch<MilesBalance>('/api/v1/miles/balance', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getMilesHistory: (token: string, page = 0) =>
    apiFetch<MilesHistoryResponse>(
      `/api/v1/miles/history?page=${page}&size=20`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  redeemMiles: (bookingId: string, miles: number, token: string) =>
    apiFetch<void>('/api/v1/miles/redeem', {
      method: 'POST',
      body: JSON.stringify({ bookingId, miles }),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Experiences ──────────────────────────────────────────── */
  getExperiences: (params?: { city?: string; category?: string }) => {
    const qs = new URLSearchParams();
    if (params?.city) qs.set('city', params.city);
    if (params?.category) qs.set('category', params.category);
    const query = qs.toString();
    return apiFetch<Experience[]>(
      `/api/v1/experiences${query ? `?${query}` : ''}`
    );
  },

  getExperience: (id: string) =>
    apiFetch<Experience>(`/api/v1/experiences/${id}`),

  /* ── Medical Tourism ──────────────────────────────────────── */
  getMedicalStaySearch: (params?: { city?: string; specialty?: string }) => {
    const qs = new URLSearchParams();
    if (params?.city) qs.set('city', params.city);
    if (params?.specialty) qs.set('specialty', params.specialty);
    const query = qs.toString();
    return apiFetch<MedicalStayPackage[]>(
      `/api/v1/medical-stay/search${query ? `?${query}` : ''}`
    );
  },

  getHospitals: async () => {
    const res = await apiFetch<{ content: Hospital[] }>('/api/v1/medical-stay/hospitals?size=100');
    return Array.isArray(res) ? res : res?.content ?? [];
  },

  /* ── Nomad Network ────────────────────────────────────────── */
  getNomadFeed: (city: string, category?: string) => {
    const qs = new URLSearchParams({ city });
    if (category) qs.set('category', category);
    return apiFetch<NomadPost[]>(`/api/v1/nomad/feed?${qs}`);
  },

  createNomadPost: (
    data: { title: string; body: string; category: string; city: string },
    token: string
  ) =>
    apiFetch<NomadPost>('/api/v1/nomad/posts', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getNomadPostComments: (postId: string) =>
    apiFetch<NomadComment[]>(`/api/v1/nomad/posts/${postId}/comments`),

  addComment: (postId: string, body: string, token: string) =>
    apiFetch<NomadComment>(`/api/v1/nomad/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ body }),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Bucket List ──────────────────────────────────────────── */
  getBucketList: (token: string) =>
    apiFetch<BucketListItem[]>('/api/v1/bucket-list', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  addToBucketList: (listingId: string, token: string) =>
    apiFetch<BucketListItem>(`/api/v1/bucket-list/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  removeFromBucketList: (listingId: string, token: string) =>
    apiFetch<void>(`/api/v1/bucket-list/${listingId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Host Subscription ──────────────────────────────────────── */
  getSubscription: (token: string) =>
    apiFetch<HostSubscription>('/api/v1/users/me/subscription', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  startTrial: (tier: SubscriptionTier, token: string) =>
    apiFetch<HostSubscription>(`/api/v1/users/me/subscription/trial?tier=${tier}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  upgradeSubscription: (tier: SubscriptionTier, token: string) =>
    apiFetch<HostSubscription>(`/api/v1/users/me/subscription/upgrade?tier=${tier}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  activateSubscription: (tier: SubscriptionTier, token: string) =>
    apiFetch<ActivateSubscriptionResponse>(`/api/v1/users/me/subscription/activate?tier=${tier}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Room Type Management ─────────────────────────────────────── */
  createRoomType: (listingId: string, data: any, token: string) =>
    apiFetch<RoomType>(`/api/v1/listings/${listingId}/room-types`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateRoomType: (listingId: string, roomTypeId: string, data: any, token: string) =>
    apiFetch<RoomType>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteRoomType: (listingId: string, roomTypeId: string, token: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateRoomTypePhotos: (listingId: string, roomTypeId: string, data: { primaryPhotoUrl?: string; photoUrls?: string[] }, token: string) =>
    apiFetch<RoomType>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/photos`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Room Type Inclusions & Perks ────────────────────────────── */
  getRoomTypeInclusions: (listingId: string, roomTypeId: string) =>
    apiFetch<RoomTypeInclusion[]>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions`),

  createRoomTypeInclusion: (listingId: string, roomTypeId: string, data: any, token: string) =>
    apiFetch<RoomTypeInclusion>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  updateRoomTypeInclusion: (listingId: string, roomTypeId: string, inclusionId: string, data: any, token: string) =>
    apiFetch<RoomTypeInclusion>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions/${inclusionId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteRoomTypeInclusion: (listingId: string, roomTypeId: string, inclusionId: string, token: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions/${inclusionId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  bulkReplaceRoomTypeInclusions: (listingId: string, roomTypeId: string, data: any[], token: string) =>
    apiFetch<RoomTypeInclusion[]>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions/bulk`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Host Calendar / Availability ──────────────────────────────── */
  getAvailabilityMonth: (listingId: string, year: number, month: number, token: string) =>
    apiFetch<AvailabilityDayDto[]>(
      `/api/v1/listings/${listingId}/availability/month?year=${year}&month=${month}`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  bulkUpdateAvailability: (listingId: string, data: BulkAvailabilityRequest, token: string) =>
    apiFetch<AvailabilityResponse[]>(`/api/v1/listings/${listingId}/availability/bulk`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Host Earnings & Invoices ───────────────────────────────── */
  getGstInvoices: (token: string) =>
    apiFetch<any[]>('/api/v1/host/gst-invoices', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getTdsReport: (year: number, quarter: number, token: string) =>
    apiFetch<any>(`/api/v1/host/tds-report?year=${year}&quarter=${quarter}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getPnl: (year: number, token: string) =>
    apiFetch<any>(`/api/v1/host/pnl?year=${year}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getSubscriptionInvoices: (token: string) =>
    apiFetch<any[]>('/api/v1/payments/invoices', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getHostExpenses: (year: number, token: string) =>
    apiFetch<any[]>(`/api/v1/host/expenses?year=${year}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Occupancy Report ──────────────────────────────────────── */
  getOccupancyReport: (from: string, to: string, token: string) =>
    apiFetch<OccupancyReport>(`/api/v1/bookings/host/occupancy?from=${from}&to=${to}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── iCal Feeds ────────────────────────────────────────────── */
  getICalFeeds: (listingId: string, token: string) =>
    apiFetch<ICalFeed[]>(`/api/v1/listings/${listingId}/ical/feeds`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  addICalFeed: (listingId: string, data: { feedUrl: string; feedName: string }, token: string) =>
    apiFetch<ICalFeed>(`/api/v1/listings/${listingId}/ical/feeds`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  exportICal: (listingId: string, token: string) =>
    apiFetch<string>(`/api/v1/listings/${listingId}/ical/export`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'text/calendar' },
    }),

  syncICalFeed: (listingId: string, feedId: string, token: string) =>
    apiFetch<{ status: string }>(`/api/v1/listings/${listingId}/ical/feeds/${feedId}/sync`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteICalFeed: (listingId: string, feedId: string, token: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/ical/feeds/${feedId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  /* ── Pricing Rules ─────────────────────────────────────────── */
  getPricingRules: (listingId: string, token: string) =>
    apiFetch<PricingRule[]>(`/api/v1/listings/${listingId}/pricing-rules`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  createPricingRule: (listingId: string, data: any, token: string) =>
    apiFetch<PricingRule>(`/api/v1/listings/${listingId}/pricing-rules`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deletePricingRule: (listingId: string, ruleId: string, token: string) =>
    apiFetch<void>(`/api/v1/listings/${listingId}/pricing-rules/${ruleId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  previewPricingRule: (listingId: string, date: string, token: string) =>
    apiFetch<{ date: string; basePricePaise: number; effectivePricePaise: number; rulesApplied: string[] }>(
      `/api/v1/listings/${listingId}/pricing-rules/preview?date=${date}`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  /* ── Commission Rate ─────────────────────────────────────── */
  getCommissionRate: (token: string, tier?: string) =>
    apiFetch<{ tier: string; commissionRate: number; commissionPercent: number }>(
      `/api/v1/payments/commission-rate${tier ? `?tier=${tier}` : ''}`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  /* ── Messaging ──────────────────────────────────────────── */
  sendMessage: (data: { listingId: string; recipientId: string; bookingId?: string; content: string }, token: string) =>
    apiFetch<ChatMessage>('/api/v1/messages', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getConversations: (token: string) =>
    apiFetch<Conversation[]>('/api/v1/messages/conversations', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getMessages: (conversationId: string, token: string, page = 0) =>
    apiFetch<{ content: ChatMessage[]; totalPages: number; number: number }>(
      `/api/v1/messages/conversations/${conversationId}?page=${page}&size=50`,
      { headers: { Authorization: `Bearer ${token}` } }
    ),

  markAsRead: (conversationId: string, token: string) =>
    apiFetch<void>(`/api/v1/messages/conversations/${conversationId}/read`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  getUnreadCount: (token: string) =>
    apiFetch<{ count: number }>('/api/v1/messages/unread-count', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getQuickReplies: (token: string) =>
    apiFetch<QuickReplyTemplate[]>('/api/v1/messages/quick-replies', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  createQuickReply: (content: string, token: string) =>
    apiFetch<QuickReplyTemplate>('/api/v1/messages/quick-replies', {
      method: 'POST',
      body: JSON.stringify({ content }),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteQuickReply: (id: string, token: string) =>
    apiFetch<void>(`/api/v1/messages/quick-replies/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ──────────────────────────────────────────
  // AI Pricing
  // ──────────────────────────────────────────
  aiPricingSuggest: (listingId: string, basePricePaise: number, city: string, propertyType: string, targetDate: string) =>
    apiFetch<any>(`/api/v1/ai/pricing/suggest?listing_id=${listingId}&base_price_paise=${basePricePaise}&city=${city}&property_type=${propertyType}&target_date=${targetDate}`, {
      method: 'POST',
    }),

  aiPricingCalendar: (listingId: string, basePricePaise: number, city: string, propertyType: string) =>
    apiFetch<any>(`/api/v1/ai/pricing/calendar/${listingId}?base_price_paise=${basePricePaise}&city=${city}&property_type=${propertyType}`),

  aiPricingRules: (rule: any) =>
    apiFetch<any>('/api/v1/ai/pricing/rules', {
      method: 'POST',
      body: JSON.stringify(rule),
    }),

  aiPricingAnalytics: (listingId: string, basePricePaise: number, city: string) =>
    apiFetch<any>(`/api/v1/ai/pricing/analytics/${listingId}?base_price_paise=${basePricePaise}&city=${city}`),

  // AI Messaging
  aiMessageTemplate: (templateType: string, language: string, params: Record<string, string>) =>
    apiFetch<any>('/api/v1/ai/messaging/template', {
      method: 'POST',
      body: JSON.stringify({ template_type: templateType, language, params }),
    }),

  aiReplySuggest: (messages: any[], listingContext: Record<string, string>) =>
    apiFetch<any>('/api/v1/ai/messaging/reply-suggest', {
      method: 'POST',
      body: JSON.stringify({ messages, listing_context: listingContext }),
    }),

  aiAutoResponse: (query: string, listingContext: Record<string, string>) =>
    apiFetch<any>('/api/v1/ai/messaging/auto-response', {
      method: 'POST',
      body: JSON.stringify({ query, listing_context: listingContext }),
    }),

  // AI Calendar
  aiCalendarOptimize: (data: any) =>
    apiFetch<any>('/api/v1/ai/calendar/optimize', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  aiCalendarInsights: (listingId: string, basePricePaise: number, city: string) =>
    apiFetch<any>(`/api/v1/ai/calendar/insights/${listingId}?base_price_paise=${basePricePaise}&city=${city}`),

  // ──────────────────────────────────────────
  // Aashray Cases
  // ──────────────────────────────────────────
  createAashrayCase: (data: any, token: string) =>
    apiFetch<any>('/api/v1/aashray/cases', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getAashrayCases: (params: string, token: string) =>
    apiFetch<any>(`/api/v1/aashray/cases?${params}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getAashrayCase: (id: string, token: string) =>
    apiFetch<any>(`/api/v1/aashray/cases/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  matchAashrayCase: (id: string, token: string) =>
    apiFetch<any>(`/api/v1/aashray/cases/${id}/match`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  assignAashrayCase: (caseId: string, listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/aashray/cases/${caseId}/assign?listingId=${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  getAashrayStats: (token: string) =>
    apiFetch<any>('/api/v1/aashray/cases/stats', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ──────────────────────────────────────────
  // Channel Manager
  // ──────────────────────────────────────────
  connectChannelManager: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/connect/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  disconnectChannelManager: (listingId: string, token: string) =>
    apiFetch<void>(`/api/v1/channel-manager/disconnect/${listingId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  syncChannelRates: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/sync/rates/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  syncChannelAvailability: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/sync/availability/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  pullChannelBookings: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/sync/pull-bookings/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  getChannelStatus: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/status/${listingId}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getChannelSyncLogs: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/channel-manager/logs/${listingId}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getAvailableChannels: () =>
    apiFetch<string[]>('/api/v1/channel-manager/channels'),

  createChannelMapping: (data: any, token: string) =>
    apiFetch<any>('/api/v1/channel-manager/mappings', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ──────────────────────────────────────────
  // PG Tenancy
  // ──────────────────────────────────────────
  createPgTenancy: (data: any, token: string) =>
    apiFetch<any>('/api/v1/pg-tenancies', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getPgTenancies: (params: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies?${params}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getPgTenancy: (id: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  giveTenancyNotice: (id: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies/${id}/notice`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  vacateTenancy: (id: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies/${id}/vacate`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  getTenancyInvoices: (tenancyId: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies/${tenancyId}/invoices`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  getOverdueInvoices: (token: string) =>
    apiFetch<any>('/api/v1/pg-tenancies/invoices/overdue', {
      headers: { Authorization: `Bearer ${token}` },
    }),

  markInvoicePaid: (invoiceId: string, paymentId: string, token: string) =>
    apiFetch<any>(`/api/v1/pg-tenancies/invoices/${invoiceId}/pay?razorpayPaymentId=${paymentId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ──────────────────────────────────────────
  // Seeker Profiles (Reverse Search)
  // ──────────────────────────────────────────
  createSeekerProfile: (data: any, token: string) =>
    apiFetch<any>('/api/v1/seekers', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  getSeekerProfiles: (params: string) =>
    apiFetch<any>(`/api/v1/seekers?${params}`),

  getSeekerProfile: (id: string) =>
    apiFetch<any>(`/api/v1/seekers/${id}`),

  updateSeekerProfile: (id: string, data: any, token: string) =>
    apiFetch<any>(`/api/v1/seekers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    }),

  deleteSeekerProfile: (id: string, token: string) =>
    apiFetch<void>(`/api/v1/seekers/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    }),

  getSeekerMatches: (seekerId: string) =>
    apiFetch<any>(`/api/v1/seekers/${seekerId}/matches`),

  getSeekersForListing: (listingId: string, token: string) =>
    apiFetch<any>(`/api/v1/seekers/for-listing/${listingId}`, {
      headers: { Authorization: `Bearer ${token}` },
    }),

  // ──────────────────────────────────────────
  // Localities (Polygon Search)
  // ──────────────────────────────────────────
  getLocalities: (city?: string) =>
    apiFetch<any>(`/api/v1/localities${city ? `?city=${city}` : ''}`),

  getLocalityPolygon: (id: string) =>
    apiFetch<any>(`/api/v1/localities/${id}`),

  getLocalitiesByCity: (city: string) =>
    apiFetch<any>(`/api/v1/localities/city/${city}`),

  getLocalityByName: (name: string, city: string) =>
    apiFetch<any>(`/api/v1/localities/by-name?name=${name}&city=${city}`),

  importLocality: (name: string, city: string, token: string) =>
    apiFetch<any>(`/api/v1/localities/import?name=${name}&city=${city}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }),
};
