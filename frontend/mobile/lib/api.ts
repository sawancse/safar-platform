import Constants from 'expo-constants';
import * as SecureStore from 'expo-secure-store';

const BASE_URL: string =
  (Constants.expoConfig?.extra?.apiUrl as string) ?? 'http://localhost:8080';

async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
  });
  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      msg = body.message ?? body.error ?? msg;
    } catch {}
    throw new Error(msg);
  }
  return res.json() as Promise<T>;
}

export const api = {
  search(params: Record<string, string>) {
    const qs = new URLSearchParams(params).toString();
    return apiFetch<{ listings: any[]; totalHits: number; page: number; size: number }>(`/api/v1/search/listings?${qs}`);
  },

  autocomplete(q: string) {
    return apiFetch<string[]>(`/api/v1/search/autocomplete?q=${encodeURIComponent(q)}`);
  },

  getListing(id: string) {
    return apiFetch<any>(`/api/v1/listings/${id}`);
  },

  sendOtp(phone: string) {
    return apiFetch<void>('/api/v1/auth/otp/send', {
      method: 'POST',
      body: JSON.stringify({ phone }),
    });
  },

  verifyOtp(phone: string, otp: string, name?: string) {
    return apiFetch<{ accessToken: string; refreshToken: string; userId: string; role: string }>(
      '/api/v1/auth/otp/verify',
      {
        method: 'POST',
        body: JSON.stringify({ phone, otp, name }),
      }
    );
  },

  googleSignIn(idToken: string) {
    return apiFetch<any>('/api/v1/auth/google/signin', {
      method: 'POST',
      body: JSON.stringify({ idToken }),
    });
  },

  /* ── Email OTP ─────────────────────────────────────────── */
  sendEmailOtp(email: string) {
    return apiFetch<void>('/api/v1/auth/otp/email/send', {
      method: 'POST',
      body: JSON.stringify({ email }),
    });
  },

  verifyEmailOtp(email: string, otp: string, name?: string) {
    return apiFetch<{ accessToken: string; refreshToken: string; userId: string; role: string }>(
      '/api/v1/auth/otp/email/verify',
      {
        method: 'POST',
        body: JSON.stringify({ email, otp, name }),
      }
    );
  },

  /* ── Password Auth ─────────────────────────────────────── */
  checkAuthMethod(email: string) {
    return apiFetch<{ exists: boolean; hasPassword: boolean; methods: string[] }>(
      `/api/v1/auth/check-method?email=${encodeURIComponent(email)}`
    );
  },

  passwordSignIn(email: string, password: string) {
    return apiFetch<{ accessToken: string; refreshToken: string; userId: string; role: string }>(
      '/api/v1/auth/password/signin',
      {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }
    );
  },

  async setPassword(password: string, token: string) {
    const userId = await SecureStore.getItemAsync('safar_user_id');
    return apiFetch<{ status: string }>('/api/v1/auth/password/set', {
      method: 'POST',
      body: JSON.stringify({ password }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': userId || '' },
    });
  },

  async changePassword(oldPassword: string, newPassword: string, token: string) {
    const userId = await SecureStore.getItemAsync('safar_user_id');
    return apiFetch<{ status: string }>('/api/v1/auth/password/change', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword }),
      headers: { Authorization: `Bearer ${token}`, 'X-User-Id': userId || '' },
    });
  },

  resetPassword(email: string, otp: string, newPassword: string) {
    return apiFetch<{ accessToken: string; refreshToken: string; userId: string; role: string }>(
      '/api/v1/auth/password/reset',
      {
        method: 'POST',
        body: JSON.stringify({ email, otp, newPassword }),
      }
    );
  },

  getMyBookings(token: string) {
    return apiFetch<any[]>('/api/v1/bookings/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createBooking(body: object, token: string) {
    return apiFetch<any>('/api/v1/bookings', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
  },

  createPaymentOrder(bookingId: string, token: string) {
    return apiFetch<any>('/api/v1/payments/orders', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: JSON.stringify({ bookingId }),
    });
  },

  cancelBooking(id: string, reason: string, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${id}/cancel?reason=${encodeURIComponent(reason)}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  confirmBooking(id: string, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${id}/confirm`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createReview(data: {
    bookingId: string; rating: number; comment?: string;
    ratingCleanliness?: number; ratingLocation?: number; ratingValue?: number;
    ratingCommunication?: number; ratingCheckIn?: number; ratingAccuracy?: number;
  }, token: string) {
    return apiFetch<any>('/api/v1/reviews', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getListingReviews(listingId: string) {
    return apiFetch<any[]>(`/api/v1/reviews/listing/${listingId}`);
  },

  getListingReviewStats(listingId: string) {
    return apiFetch<any>(`/api/v1/reviews/listing/${listingId}/stats`);
  },

  getMyReviews(token: string) {
    return apiFetch<any[]>('/api/v1/reviews/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getMyListings(token: string) {
    return apiFetch<any[]>('/api/v1/listings/mine', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Miles ────────────────────────────────────────────────── */
  getMilesBalance(token: string) {
    return apiFetch<any>('/api/v1/miles/balance', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getMilesHistory(token: string, page = 0) {
    return apiFetch<any>(`/api/v1/miles/history?page=${page}&size=20`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  redeemMiles(bookingId: string, miles: number, token: string) {
    return apiFetch<void>('/api/v1/miles/redeem', {
      method: 'POST',
      body: JSON.stringify({ bookingId, miles }),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Experiences ──────────────────────────────────────────── */
  getExperiences(params?: { city?: string; category?: string }) {
    const qs = new URLSearchParams();
    if (params?.city) qs.set('city', params.city);
    if (params?.category) qs.set('category', params.category);
    const query = qs.toString();
    return apiFetch<any[]>(`/api/v1/experiences${query ? `?${query}` : ''}`);
  },

  getExperience(id: string) {
    return apiFetch<any>(`/api/v1/experiences/${id}`);
  },

  /* ── Medical Tourism ──────────────────────────────────────── */
  getMedicalStaySearch(params?: { city?: string; specialty?: string }) {
    const qs = new URLSearchParams();
    if (params?.city) qs.set('city', params.city);
    if (params?.specialty) qs.set('specialty', params.specialty);
    const query = qs.toString();
    return apiFetch<any[]>(`/api/v1/medical-stay/search${query ? `?${query}` : ''}`);
  },

  getHospitals() {
    return apiFetch<any[]>('/api/v1/medical-stay/hospitals');
  },

  /* ── Nomad Network ────────────────────────────────────────── */
  getNomadFeed(city: string, category?: string) {
    const qs = new URLSearchParams({ city });
    if (category) qs.set('category', category);
    return apiFetch<any[]>(`/api/v1/nomad/feed?${qs}`);
  },

  createNomadPost(data: { title: string; body: string; category: string; city: string }, token: string) {
    return apiFetch<any>('/api/v1/nomad/posts', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addComment(postId: string, body: string, token: string) {
    return apiFetch<any>(`/api/v1/nomad/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ body }),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Host Bookings ─────────────────────────────────────── */
  getHostBookings(token: string) {
    return apiFetch<any[]>('/api/v1/bookings/host', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  checkInBooking(id: string, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${id}/check-in`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  completeBooking(id: string, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${id}/complete`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  markNoShow(id: string, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${id}/no-show`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── User Profile ────────────────────────────────────────── */
  getMyProfile(token: string) {
    return apiFetch<any>('/api/v1/users/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateMyProfile(data: object, token: string) {
    return apiFetch<any>('/api/v1/users/me', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Co-travelers ────────────────────────────────────────── */
  getCoTravelers(token: string) {
    return apiFetch<any[]>('/api/v1/users/me/co-travelers', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addCoTraveler(data: { firstName: string; lastName: string; dateOfBirth?: string; gender?: string }, token: string) {
    return apiFetch<any>('/api/v1/users/me/co-travelers', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  removeCoTraveler(id: string, token: string) {
    return apiFetch<void>(`/api/v1/users/me/co-travelers/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Host Subscription ──────────────────────────────────── */
  getMySubscription(token: string) {
    return apiFetch<any>('/api/v1/users/me/subscription', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  startSubscriptionTrial(tier: string, token: string) {
    return apiFetch<any>(`/api/v1/users/me/subscription/trial?tier=${tier}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  upgradeSubscription(tier: string, token: string) {
    return apiFetch<any>(`/api/v1/users/me/subscription/upgrade?tier=${tier}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Avatar ──────────────────────────────────────────────── */
  async uploadAvatar(uri: string, token: string): Promise<{ avatarUrl: string }> {
    const filename = uri.split('/').pop() ?? 'avatar.jpg';
    const match = /\.(\w+)$/.exec(filename);
    const type = match ? `image/${match[1]}` : 'image/jpeg';

    const formData = new FormData();
    formData.append('file', { uri, name: filename, type } as any);

    const res = await fetch(`${BASE_URL}/api/v1/users/me/avatar`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data',
      },
      body: formData,
    });
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try { const b = await res.json(); msg = b.message ?? b.error ?? msg; } catch {}
      throw new Error(msg);
    }
    return res.json() as Promise<{ avatarUrl: string }>;
  },

  /* ── Messaging ──────────────────────────────────────────── */
  sendMessage(data: { listingId: string; recipientId: string; bookingId?: string; content: string }, token: string) {
    return apiFetch<any>('/api/v1/messages', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getConversations(token: string) {
    return apiFetch<any[]>('/api/v1/messages/conversations', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getMessages(conversationId: string, token: string, page = 0) {
    return apiFetch<any>(`/api/v1/messages/conversations/${conversationId}?page=${page}&size=50`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  markAsRead(conversationId: string, token: string) {
    return apiFetch<void>(`/api/v1/messages/conversations/${conversationId}/read`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getUnreadCount(token: string) {
    return apiFetch<any>('/api/v1/messages/unread-count', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Notifications ────────────────────────────────────────── */
  getNotifications(token: string, page = 0, size = 20) {
    return apiFetch<{ content: any[]; totalElements: number }>(
      `/api/v1/notifications?page=${page}&size=${size}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
  },

  getUnreadNotificationCount(token: string) {
    return apiFetch<{ count: number }>('/api/v1/notifications/unread-count', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  markNotificationRead(id: string, token: string) {
    return apiFetch<void>(`/api/v1/notifications/${id}/read`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  markAllNotificationsRead(token: string) {
    return apiFetch<void>('/api/v1/notifications/read-all', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Bucket List ──────────────────────────────────────────── */
  getBucketList(token: string) {
    return apiFetch<any[]>('/api/v1/bucket-list', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addToBucketList(listingId: string, token: string) {
    return apiFetch<any>(`/api/v1/bucket-list/${listingId}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  removeFromBucketList(listingId: string, token: string) {
    return apiFetch<void>(`/api/v1/bucket-list/${listingId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Listing CRUD ────────────────────────────────────────── */
  createListing(data: object, token: string) {
    return apiFetch<any>('/api/v1/listings', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateListing(id: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  submitForVerification(id: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/submit`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  archiveListing(id: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/archive`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  restoreListing(id: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/restore`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  pauseListing(id: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/pause`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  unpauseListing(id: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/unpause`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  toggleAashray(id: string, enabled: boolean, token: string) {
    return apiFetch<any>(`/api/v1/listings/${id}/aashray?enabled=${enabled}`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Room Types ──────────────────────────────────────────── */
  getRoomTypes(listingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/room-types`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createRoomType(listingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/room-types`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateRoomType(listingId: string, roomTypeId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deleteRoomType(listingId: string, roomTypeId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Room Inclusions ─────────────────────────────────────── */
  getRoomTypeInclusions(listingId: string, roomTypeId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createRoomTypeInclusion(listingId: string, roomTypeId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deleteRoomTypeInclusion(listingId: string, roomTypeId: string, inclusionId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/room-types/${roomTypeId}/inclusions/${inclusionId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Calendar & Availability ─────────────────────────────── */
  getAvailabilityMonth(listingId: string, year: number, month: number, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/availability?year=${year}&month=${month}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  bulkUpdateAvailability(listingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/availability/bulk`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── KYC ─────────────────────────────────────────────────── */
  getKyc(token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateKycIdentity(data: object, token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc/identity', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateKycAddress(data: object, token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc/address', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateKycBank(data: object, token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc/bank', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateKycBusiness(data: object, token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc/business', {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  submitKyc(token: string) {
    return apiFetch<any>('/api/v1/users/me/kyc/submit', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Host Earnings & Reports ─────────────────────────────── */
  getHostEarnings(token: string, params?: { from?: string; to?: string }) {
    const qs = new URLSearchParams();
    if (params?.from) qs.set('from', params.from);
    if (params?.to) qs.set('to', params.to);
    const query = qs.toString();
    return apiFetch<any>(`/api/v1/bookings/host/earnings${query ? `?${query}` : ''}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getGstInvoices(token: string, page = 0) {
    return apiFetch<any>(`/api/v1/payments/host/gst-invoices?page=${page}&size=20`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getTdsReport(token: string, financialYear?: string) {
    const qs = financialYear ? `?financialYear=${financialYear}` : '';
    return apiFetch<any>(`/api/v1/payments/host/tds-report${qs}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getPnl(token: string, params?: { from?: string; to?: string }) {
    const qs = new URLSearchParams();
    if (params?.from) qs.set('from', params.from);
    if (params?.to) qs.set('to', params.to);
    const query = qs.toString();
    return apiFetch<any>(`/api/v1/payments/host/pnl${query ? `?${query}` : ''}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getHostTransactions(token: string, page = 0) {
    return apiFetch<any>(`/api/v1/payments/host/transactions?page=${page}&size=20`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getOccupancyReport(token: string, listingId?: string) {
    const qs = listingId ? `?listingId=${listingId}` : '';
    return apiFetch<any>(`/api/v1/bookings/host/occupancy${qs}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Host Reviews ────────────────────────────────────────── */
  getHostReviews(token: string, params?: { listingId?: string; page?: number }) {
    const qs = new URLSearchParams();
    if (params?.listingId) qs.set('listingId', params.listingId);
    if (params?.page !== undefined) qs.set('page', String(params.page));
    qs.set('size', '20');
    const query = qs.toString();
    return apiFetch<any>(`/api/v1/reviews/host${query ? `?${query}` : ''}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getHostReviewStats(token: string) {
    return apiFetch<any>('/api/v1/reviews/host/stats', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addHostReply(reviewId: string, reply: string, token: string) {
    return apiFetch<any>(`/api/v1/reviews/${reviewId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ reply }),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deleteHostReply(reviewId: string, token: string) {
    return apiFetch<void>(`/api/v1/reviews/${reviewId}/reply`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Pricing Rules ───────────────────────────────────────── */
  getPricingRules(listingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/pricing-rules`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createPricingRule(listingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/pricing-rules`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deletePricingRule(listingId: string, ruleId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/pricing-rules/${ruleId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── PG Packages ─────────────────────────────────────────── */
  getPgPackages(listingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/pg-packages`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createPgPackage(listingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/pg-packages`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updatePgPackage(listingId: string, packageId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/pg-packages/${packageId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deletePgPackage(listingId: string, packageId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/pg-packages/${packageId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── iCal Feeds ──────────────────────────────────────────── */
  getICalFeeds(listingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/ical-feeds`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addICalFeed(listingId: string, data: { name: string; url: string }, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/ical-feeds`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  syncICalFeed(listingId: string, feedId: string, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/ical-feeds/${feedId}/sync`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deleteICalFeed(listingId: string, feedId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/ical-feeds/${feedId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  exportICal(listingId: string, token: string) {
    return apiFetch<{ url: string }>(`/api/v1/listings/${listingId}/ical-export`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── AI Pricing ──────────────────────────────────────────── */
  aiPricingSuggest(listingId: string, date: string, token: string) {
    return apiFetch<any>(`/api/v1/ai/pricing/suggest?listingId=${listingId}&date=${date}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  aiPricingCalendar(listingId: string, month: string, token: string) {
    return apiFetch<any>(`/api/v1/ai/pricing/calendar?listingId=${listingId}&month=${month}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  aiPricingAnalytics(listingId: string, token: string) {
    return apiFetch<any>(`/api/v1/ai/pricing/analytics?listingId=${listingId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Quick Replies ───────────────────────────────────────── */
  getQuickReplies(token: string) {
    return apiFetch<any[]>('/api/v1/messages/quick-replies', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createQuickReply(data: { label: string; text: string }, token: string) {
    return apiFetch<any>('/api/v1/messages/quick-replies', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deleteQuickReply(id: string, token: string) {
    return apiFetch<void>(`/api/v1/messages/quick-replies/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Media Management ────────────────────────────────────── */
  getListingMedia(listingId: string) {
    return apiFetch<any[]>(`/api/v1/media/listing/${listingId}`);
  },

  async uploadListingPhoto(listingId: string, uri: string, category: string, token: string): Promise<any> {
    const filename = uri.split('/').pop() ?? 'photo.jpg';
    const match = /\.(\w+)$/.exec(filename);
    const type = match ? `image/${match[1]}` : 'image/jpeg';
    const formData = new FormData();
    formData.append('file', { uri, name: filename, type } as any);
    formData.append('listingId', listingId);
    formData.append('category', category);
    const res = await fetch(`${BASE_URL}/api/v1/media/upload`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' },
      body: formData,
    });
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try { const b = await res.json(); msg = b.message ?? b.error ?? msg; } catch {}
      throw new Error(msg);
    }
    return res.json();
  },

  deleteMedia(mediaId: string, token: string) {
    return apiFetch<void>(`/api/v1/media/${mediaId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  reorderMedia(listingId: string, mediaIds: string[], token: string) {
    return apiFetch<void>(`/api/v1/media/listing/${listingId}/reorder`, {
      method: 'PUT',
      body: JSON.stringify({ mediaIds }),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Booking Guests ──────────────────────────────────────── */
  getBookingGuests(bookingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/bookings/${bookingId}/guests`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addBookingGuest(bookingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/bookings/${bookingId}/guests`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  removeBookingGuest(bookingId: string, guestId: string, token: string) {
    return apiFetch<void>(`/api/v1/bookings/${bookingId}/guests/${guestId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Payment Methods ─────────────────────────────────────── */
  getPaymentMethods(token: string) {
    return apiFetch<any[]>('/api/v1/payments/methods', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  createPaymentMethod(data: object, token: string) {
    return apiFetch<any>('/api/v1/payments/methods', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  deletePaymentMethod(id: string, token: string) {
    return apiFetch<void>(`/api/v1/payments/methods/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  setDefaultPaymentMethod(id: string, token: string) {
    return apiFetch<any>(`/api/v1/payments/methods/${id}/default`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getCommissionRate(token: string) {
    return apiFetch<any>('/api/v1/payments/commission-rate', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── PG Tenancy ──────────────────────────────────────────── */
  getTenants(listingId: string, token: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/tenants`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  addTenant(listingId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/tenants`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  updateTenant(listingId: string, tenantId: string, data: object, token: string) {
    return apiFetch<any>(`/api/v1/listings/${listingId}/tenants/${tenantId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  removeTenant(listingId: string, tenantId: string, token: string) {
    return apiFetch<void>(`/api/v1/listings/${listingId}/tenants/${tenantId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Reverse Search / Looking For ────────────────────────── */
  createLookingFor(data: object, token: string) {
    return apiFetch<any>('/api/v1/looking-for', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getMyLookingFor(token: string) {
    return apiFetch<any[]>('/api/v1/looking-for/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  getLookingForMatches(id: string, token: string) {
    return apiFetch<any[]>(`/api/v1/looking-for/${id}/matches`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Sign out all ────────────────────────────────────────── */
  logoutAll(token: string) {
    return apiFetch<void>('/api/v1/auth/logout-all', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Subscription Invoices ───────────────────────────────── */
  getSubscriptionInvoices(token: string) {
    return apiFetch<any[]>('/api/v1/users/me/subscription/invoices', {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Host Expenses ───────────────────────────────────────── */
  getHostExpenses(token: string, params?: { from?: string; to?: string }) {
    const qs = new URLSearchParams();
    if (params?.from) qs.set('from', params.from);
    if (params?.to) qs.set('to', params.to);
    const query = qs.toString();
    return apiFetch<any[]>(`/api/v1/payments/host/expenses${query ? `?${query}` : ''}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /* ── Listing Available Room Types ────────────────────────── */
  getAvailableRoomTypes(listingId: string, checkIn: string, checkOut: string) {
    return apiFetch<any[]>(`/api/v1/listings/${listingId}/room-types/available?checkIn=${checkIn}&checkOut=${checkOut}`);
  },

  /* ── Host Profile (public) ───────────────────────────────── */
  getHostProfile(userId: string) {
    return apiFetch<any>(`/api/v1/users/${userId}/host-profile`);
  },
};
