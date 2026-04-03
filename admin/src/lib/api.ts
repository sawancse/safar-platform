import axios from 'axios';

const BASE = (import.meta.env.VITE_API_URL || '') + '/api/v1';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

// Auto-refresh token on 401 — only if refresh token exists
axios.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true;
      const refreshToken = localStorage.getItem('admin_refresh_token');
      if (refreshToken) {
        try {
          const res = await axios.post(`${BASE}/auth/refresh`, { refreshToken });
          const { accessToken, refreshToken: newRefresh } = res.data;
          localStorage.setItem('admin_token', accessToken);
          if (newRefresh) localStorage.setItem('admin_refresh_token', newRefresh);
          original.headers.Authorization = `Bearer ${accessToken}`;
          return axios(original);
        } catch {
          localStorage.removeItem('admin_token');
          localStorage.removeItem('admin_refresh_token');
          window.location.href = '/login';
          return Promise.reject(error);
        }
      }
      // No refresh token — don't redirect, just let the error bubble up
      // Pages will show empty data; user can manually re-login
    }
    return Promise.reject(error);
  }
);

export const adminApi = {
  // Auth
  sendOtp(phone: string) {
    return axios.post(`${BASE}/auth/otp/send`, { phone });
  },

  verifyOtp(phone: string, otp: string) {
    return axios.post<{
      accessToken: string;
      refreshToken: string;
      accessTokenExpiresIn: number;
      user: { id: string; role: string; name: string };
    }>(`${BASE}/auth/otp/verify`, { phone, otp });
  },

  // Admin impersonation — login as a user for support
  impersonateUser(targetUserId: string, token: string) {
    return axios.post<{
      accessToken: string;
      refreshToken: string;
      accessTokenExpiresIn: number;
      user: { id: string; role: string; name: string; phone?: string; email?: string };
    }>(`${BASE}/auth/admin/impersonate`, { targetUserId }, { headers: authHeaders(token) });
  },

  // Dashboard analytics
  getAnalytics(token: string) {
    return axios.get<{
      totalListings: number;
      pendingListings: number;
      totalBookings: number;
      totalRevenuePaise: number;
      activeHosts: number;
      activeGuests: number;
    }>(`${BASE}/admin/analytics/summary`, { headers: authHeaders(token) });
  },

  // Listings moderation
  getPendingListings(token: string) {
    return axios.get<any[]>(`${BASE}/admin/listings/pending`, { headers: authHeaders(token) });
  },

  getListingsByStatus(token: string, status?: string) {
    const params = status ? `?status=${status}` : '';
    return axios.get<any[]>(`${BASE}/admin/listings${params}`, { headers: authHeaders(token) });
  },

  verifyListing(id: string, token: string, skipKycCheck = false) {
    return axios.put(`${BASE}/admin/listings/${id}/verify?skipKycCheck=${skipKycCheck}`, {}, { headers: authHeaders(token) });
  },

  getHostKyc(listingId: string, token: string) {
    return axios.get<any>(`${BASE}/admin/listings/${listingId}/host-kyc`, { headers: authHeaders(token) });
  },

  rejectListing(id: string, notes: string, token: string) {
    return axios.put(`${BASE}/admin/listings/${id}/reject`, { notes }, { headers: authHeaders(token) });
  },

  suspendListing(id: string, reason: string, note: string, token: string) {
    return axios.post(`${BASE}/admin/listings/${id}/suspend?reason=${reason}&note=${encodeURIComponent(note)}`, {}, { headers: authHeaders(token) });
  },

  restoreListing(id: string, token: string) {
    return axios.post(`${BASE}/admin/listings/${id}/restore`, {}, { headers: authHeaders(token) });
  },

  getArchivedListings(token: string) {
    return axios.get<any[]>(`${BASE}/admin/listings/archived`, { headers: authHeaders(token) });
  },

  // Listing media
  deleteListingMedia(listingId: string, mediaId: string, token: string) {
    return axios.delete(`${BASE}/listings/${listingId}/media/${mediaId}`, { headers: authHeaders(token) });
  },

  getListingMedia(id: string, token: string) {
    return axios.get<{ id: string; url: string; type: string; isPrimary: boolean }[]>(
      `${BASE}/listings/${id}/media`,
      { headers: authHeaders(token) },
    );
  },

  // Hosts
  getHosts(token: string) {
    return axios.get<any[]>(`${BASE}/admin/hosts`, { headers: authHeaders(token) });
  },
  suspendHost(hostId: string, reason: string, token: string) {
    return axios.post(`${BASE}/admin/hosts/${hostId}/suspend`, { reason }, { headers: authHeaders(token) });
  },
  unsuspendHost(hostId: string, token: string) {
    return axios.post(`${BASE}/admin/hosts/${hostId}/unsuspend`, {}, { headers: authHeaders(token) });
  },
  banHost(hostId: string, reason: string, token: string) {
    return axios.post(`${BASE}/admin/hosts/${hostId}/ban`, { reason }, { headers: authHeaders(token) });
  },

  // KYC Admin
  getPendingKycs(token: string) {
    return axios.get<any[]>(`${BASE}/admin/kyc?status=SUBMITTED`, { headers: authHeaders(token) });
  },

  getAllKycs(token: string, status?: string) {
    const params = status ? `?status=${status}` : '';
    return axios.get<any[]>(`${BASE}/admin/kyc${params}`, { headers: authHeaders(token) });
  },

  getKycDetail(kycId: string, token: string) {
    return axios.get<any>(`${BASE}/admin/kyc/${kycId}`, { headers: authHeaders(token) });
  },

  approveKyc(kycId: string, token: string) {
    return axios.post(`${BASE}/admin/kyc/${kycId}/approve`, {}, { headers: authHeaders(token) });
  },

  rejectKyc(kycId: string, reason: string, token: string) {
    return axios.post(`${BASE}/admin/kyc/${kycId}/reject?reason=${encodeURIComponent(reason)}`, {}, { headers: authHeaders(token) });
  },

  bulkApproveKyc(kycIds: string[], token: string) {
    return axios.post(`${BASE}/admin/kyc/bulk-approve`, kycIds, {
      headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
    });
  },

  // Revenue & Payouts
  getRevenueSummary(token: string) {
    return axios.get(`${BASE}/admin/analytics/summary`, { headers: authHeaders(token) }).then(r => r.data).catch(() => null);
  },

  getSettlements(token: string) {
    return axios.get(`${BASE}/admin/settlements`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  getRecentPayouts(token: string) {
    return axios.get(`${BASE}/admin/payouts`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  getReconciliation(token: string) {
    return axios.get(`${BASE}/admin/reconciliation`, { headers: authHeaders(token) }).then(r => r.data).catch(() => null);
  },

  getCommissionSummary(token: string) {
    return axios.get(`${BASE}/admin/commission/summary`, { headers: authHeaders(token) }).then(r => r.data).catch(() => null);
  },

  getCommissionByHost(token: string) {
    return axios.get(`${BASE}/admin/commission/by-host`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  // ── Admin Bookings ────────────────────────────────────────────────────────

  getBookings(token: string, params: {
    status?: string; hostId?: string; guestId?: string; listingId?: string;
    dateFrom?: string; dateTo?: string; search?: string;
    sortBy?: string; sortDir?: string; page?: number; size?: number;
  } = {}) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/admin/bookings?${qs}`, { headers: authHeaders(token) });
  },

  getBooking(id: string, token: string) {
    return axios.get(`${BASE}/admin/bookings/${id}`, { headers: authHeaders(token) });
  },

  getBookingsByHost(hostId: string, token: string) {
    return axios.get(`${BASE}/admin/bookings/by-host/${hostId}`, { headers: authHeaders(token) });
  },

  getBookingsByGuest(guestId: string, token: string) {
    return axios.get(`${BASE}/admin/bookings/by-guest/${guestId}`, { headers: authHeaders(token) });
  },

  adminCancelBooking(id: string, reason: string, token: string) {
    return axios.post(`${BASE}/admin/bookings/${id}/cancel?reason=${encodeURIComponent(reason)}`, {}, { headers: authHeaders(token) });
  },

  getBookingStats(token: string, days = 30) {
    return axios.get(`${BASE}/admin/bookings/stats?days=${days}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => null);
  },

  // ── Admin Guests ──────────────────────────────────────────────────────────

  getGuests(token: string) {
    return axios.get(`${BASE}/admin/guests`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  // ── Manual Settlement & Payout ────────────────────────────────────────────

  processSettlement(planId: string, token: string) {
    return axios.post(`${BASE}/admin/settlements/${planId}/process`, {}, { headers: authHeaders(token) });
  },

  processSettlementByBooking(bookingId: string, token: string) {
    return axios.post(`${BASE}/admin/settlements/by-booking/${bookingId}/process`, {}, { headers: authHeaders(token) });
  },

  retryPayout(payoutId: string, token: string) {
    return axios.post(`${BASE}/admin/payouts/${payoutId}/retry`, {}, { headers: authHeaders(token) });
  },

  initiateRefund(data: { paymentId: string; bookingId: string; amountPaise: number; reason: string; refundType: string }, token: string) {
    return axios.post(`${BASE}/payments/refund`, data, { headers: authHeaders(token) });
  },

  getRefunds(bookingId: string, token: string) {
    return axios.get(`${BASE}/payments/refunds/${bookingId}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  getPayoutsByHost(hostId: string, token: string) {
    return axios.get(`${BASE}/admin/payouts/by-host/${hostId}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  // ── Room Occupancy ─────────────────────────────────────────────────────
  getRoomTypes(listingId: string, token: string) {
    return axios.get(`${BASE}/listings/${listingId}/room-types`, { headers: authHeaders(token) });
  },

  getPgTenancies(params: string, token: string) {
    return axios.get(`${BASE}/pg-tenancies?${params}`, { headers: authHeaders(token) });
  },

  // ── Donations (Aashray) ────────────────────────────────────────────────
  getDonations: (token: string, status?: string) =>
    axios.get(`${BASE}/donations/admin${status ? '?status=' + status : ''}`, { headers: authHeaders(token) }).then(r => r.data?.content || r.data || []),

  getDonationStats: (token: string) =>
    axios.get(`${BASE}/donations/stats`, { headers: authHeaders(token) }).then(r => r.data),

  // ── Safar Cooks (Admin) ─────────────────────────────────────────────────
  getChefs(token: string) {
    return axios.get(`${BASE}/chefs/admin/all?size=200`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  getPendingChefs(token: string) {
    return axios.get(`${BASE}/chefs/admin/pending?size=200`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  verifyChef(chefId: string, token: string) {
    return axios.post(`${BASE}/chefs/admin/${chefId}/verify`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  rejectChef(chefId: string, reason: string, token: string) {
    return axios.post(`${BASE}/chefs/admin/${chefId}/reject?reason=${encodeURIComponent(reason)}`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  suspendChef(chefId: string, token: string) {
    return axios.post(`${BASE}/chefs/admin/${chefId}/suspend`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  getChefBookings(token: string) {
    return axios.get(`${BASE}/chef-bookings/chef`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  getChefEvents(token: string) {
    return axios.get(`${BASE}/chef-events/chef`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  getChefSubscriptions(token: string) {
    return axios.get(`${BASE}/chef-subscriptions/chef`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  // ── Experiences (Admin) ────────────────────────────────────────────────
  getExperiences(token: string, status?: string) {
    const qs = status ? `?status=${status}&size=200` : '?size=200';
    return axios.get(`${BASE}/admin/experiences${qs}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  getExperienceStats(token: string) {
    return axios.get(`${BASE}/admin/experiences/stats`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({}));
  },

  updateExperienceStatus(id: string, status: string, token: string) {
    return axios.patch(`${BASE}/admin/experiences/${id}/status`, { status }, { headers: authHeaders(token) }).then(r => r.data);
  },
};
