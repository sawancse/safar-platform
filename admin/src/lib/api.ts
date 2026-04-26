import axios from 'axios';

const BASE = (import.meta.env.VITE_API_URL || '') + '/api/v1';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

// Singleton guard — prevents concurrent refresh races from parallel 401s
let refreshPromise: Promise<string | null> | null = null;

// Auto-refresh token on 401 — only if refresh token exists
axios.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true;
      const refreshToken = localStorage.getItem('admin_refresh_token');
      if (refreshToken) {
        if (!refreshPromise) {
          refreshPromise = (async () => {
            try {
              const res = await axios.post(`${BASE}/auth/refresh`, { refreshToken });
              const { accessToken, refreshToken: newRefresh } = res.data;
              localStorage.setItem('admin_token', accessToken);
              if (newRefresh) localStorage.setItem('admin_refresh_token', newRefresh);
              return accessToken;
            } catch {
              localStorage.removeItem('admin_token');
              localStorage.removeItem('admin_refresh_token');
              window.location.href = '/login';
              return null;
            }
          })().finally(() => { refreshPromise = null; });
        }
        const newToken = await refreshPromise;
        if (newToken) {
          original.headers.Authorization = `Bearer ${newToken}`;
          return axios(original);
        }
        return Promise.reject(error);
      }
      // No refresh token — don't redirect, just let the error bubble up
      // Pages will show empty data; user can manually re-login
    }
    return Promise.reject(error);
  }
);

function tokenHeaders() {
  const t = localStorage.getItem('admin_token');
  return t ? { Authorization: `Bearer ${t}` } : {};
}

// Strip leading /api/v1 if a caller passes it in (BASE already includes it).
function url(path: string) {
  return path.startsWith('/api/v1') ? path.replace(/^\/api\/v1/, BASE) : `${BASE}${path}`;
}

export const adminApi = {
  // Generic verbs — auto-attach admin bearer token from localStorage. Use these
  // when a page just needs a quick REST call without a dedicated typed wrapper.
  get<T = any>(path: string) {
    return axios.get<T>(url(path), { headers: tokenHeaders() });
  },
  post<T = any>(path: string, body?: any) {
    return axios.post<T>(url(path), body, { headers: tokenHeaders() });
  },
  put<T = any>(path: string, body?: any) {
    return axios.put<T>(url(path), body, { headers: tokenHeaders() });
  },
  delete<T = any>(path: string) {
    return axios.delete<T>(url(path), { headers: tokenHeaders() });
  },

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

  adminConfirmCashBooking(id: string, token: string) {
    return axios.post(`${BASE}/admin/bookings/${id}/confirm-cash`, {}, { headers: authHeaders(token) });
  },

  adminRecordCashPayment(id: string, amountPaise: number, note: string, token: string) {
    return axios.post(`${BASE}/admin/bookings/${id}/record-cash-payment?amountPaise=${amountPaise}&note=${encodeURIComponent(note)}`, {}, { headers: authHeaders(token) });
  },

  getBookingStats(token: string, days = 30) {
    return axios.get(`${BASE}/admin/bookings/stats?days=${days}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => null);
  },

  // ── Admin Guests ──────────────────────────────────────────────────────────

  getGuests(token: string) {
    return axios.get(`${BASE}/admin/guests`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  mergeGuests(keeperId: string, loserId: string, token: string) {
    return axios.post(`${BASE}/admin/guests/merge`, { keeperId, loserId },
      { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Manual Settlement & Payout ────────────────────────────────────────────

  processSettlement(planId: string, token: string) {
    return axios.post(`${BASE}/admin/settlements/${planId}/process`, {}, { headers: authHeaders(token) });
  },

  processSettlementByBooking(bookingId: string, token: string, params?: { totalAmountPaise?: number; hostId?: string; bookingType?: string; hostTier?: string }) {
    const qs = new URLSearchParams();
    if (params?.totalAmountPaise) qs.set('totalAmountPaise', String(params.totalAmountPaise));
    if (params?.hostId) qs.set('hostId', params.hostId);
    if (params?.bookingType) qs.set('bookingType', params.bookingType);
    if (params?.hostTier) qs.set('hostTier', params.hostTier || 'STARTER');
    const q = qs.toString() ? `?${qs}` : '';
    return axios.post(`${BASE}/admin/settlements/by-booking/${bookingId}/process${q}`, {}, { headers: authHeaders(token) });
  },

  retryPayout(payoutId: string, token: string) {
    return axios.post(`${BASE}/admin/payouts/${payoutId}/retry`, {}, { headers: authHeaders(token) });
  },

  initiateRefund(data: { paymentId: string; bookingId: string; amountPaise: number; reason: string; refundType: string }, token: string) {
    return axios.post(`${BASE}/payments/refund`, data, { headers: authHeaders(token) });
  },

  // ── Deposit Refund ──────────────────────────────────────────────────────
  getPendingDeposits(page: number, size: number, token: string) {
    return axios.get(`${BASE}/admin/bookings/pending-deposits`, { params: { page, size }, headers: authHeaders(token) }).then(r => r.data);
  },

  adminDepositRefund(bookingId: string, refundType: string, deductionPaise: number | null, deductionReason: string, token: string) {
    const params: any = { refundType };
    if (refundType === 'PARTIAL' && deductionPaise) params.deductionPaise = deductionPaise;
    if (deductionReason) params.deductionReason = deductionReason;
    return axios.post(`${BASE}/admin/bookings/${bookingId}/deposit-refund`, null, { params, headers: authHeaders(token) });
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
    return axios.get(`${BASE}/chef-bookings/admin/all?size=200&sort=createdAt,desc`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  getChefEvents(token: string) {
    return axios.get(`${BASE}/chef-events/admin/all?size=200&sort=createdAt,desc`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  getChefSubscriptions(token: string) {
    return axios.get(`${BASE}/chef-subscriptions/admin/all?size=200&sort=createdAt,desc`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  assignChefToBooking(bookingId: string, chefId: string, token: string) {
    return axios.post(`${BASE}/chef-bookings/admin/${bookingId}/assign?chefId=${chefId}`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  assignChefToEvent(eventId: string, chefId: string, token: string) {
    return axios.post(`${BASE}/chef-events/admin/${eventId}/assign?chefId=${chefId}`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  adminCancelChefBooking(bookingId: string, reason: string, token: string) {
    return axios.post(`${BASE}/chef-bookings/admin/${bookingId}/cancel?reason=${encodeURIComponent(reason)}`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  adminCompleteChefBooking(bookingId: string, token: string) {
    return axios.post(`${BASE}/chef-bookings/admin/${bookingId}/complete`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  adminCancelChefEvent(eventId: string, reason: string, token: string) {
    return axios.post(`${BASE}/chef-events/admin/${eventId}/cancel?reason=${encodeURIComponent(reason)}`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  adminCompleteChefEvent(eventId: string, token: string) {
    return axios.post(`${BASE}/chef-events/admin/${eventId}/complete`, null, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Dish Catalog (Admin) ──────────────────────────────────────────────
  getDishCatalog(token: string) {
    return axios.get(`${BASE}/dishes/admin/all`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  createDish(dish: any, token: string) {
    return axios.post(`${BASE}/dishes/admin`, dish, { headers: authHeaders(token) }).then(r => r.data);
  },

  updateDish(dishId: string, dish: any, token: string) {
    return axios.put(`${BASE}/dishes/admin/${dishId}`, dish, { headers: authHeaders(token) }).then(r => r.data);
  },

  deleteDish(dishId: string, token: string) {
    return axios.delete(`${BASE}/dishes/admin/${dishId}`, { headers: authHeaders(token) }).then(r => r.data);
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

  // ── Builder Projects (Admin) ────────────────────────────────────────────
  getBuilderProjects(token: string, params?: Record<string, any>) {
    const qs = new URLSearchParams();
    qs.set('size', '200');
    qs.set('sort', 'createdAt,desc');
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/builder-projects/admin/list?${qs}`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({ content: [] }));
  },

  verifyBuilderProject(id: string, token: string) {
    return axios.post(`${BASE}/builder-projects/${id}/verify`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },

  verifyBuilderRera(id: string, token: string) {
    return axios.post(`${BASE}/builder-projects/${id}/verify-rera`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ══ VAS: Agreements ══
  getAgreements(token: string, params?: any) {
    const qs = new URLSearchParams();
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/agreements/admin/list?${qs}`, { headers: authHeaders(token) });
  },
  getAgreement(id: string, token: string) {
    return axios.get(`${BASE}/agreements/${id}`, { headers: authHeaders(token) });
  },
  updateAgreementStatus(id: string, status: string, token: string) {
    return axios.patch(`${BASE}/agreements/${id}/status`, null, { params: { status }, headers: authHeaders(token) });
  },

  // ══ VAS: Home Loan ══
  getLoanApplications(token: string, params?: any) {
    const qs = new URLSearchParams();
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/homeloan/applications/my?${qs}`, { headers: authHeaders(token) });
  },
  getLoanApplication(id: string, token: string) {
    return axios.get(`${BASE}/homeloan/applications/${id}`, { headers: authHeaders(token) });
  },
  updateLoanStatus(id: string, status: string, token: string) {
    return axios.patch(`${BASE}/homeloan/applications/${id}/status`, null, { params: { status }, headers: authHeaders(token) });
  },
  getPartnerBanks(token: string) {
    return axios.get(`${BASE}/homeloan/banks`, { headers: authHeaders(token) });
  },

  // ══ VAS: Legal ══
  getLegalCases(token: string, params?: any) {
    const qs = new URLSearchParams();
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/legal/cases?${qs}`, { headers: authHeaders(token) });
  },
  getLegalCase(id: string, token: string) {
    return axios.get(`${BASE}/legal/cases/${id}`, { headers: authHeaders(token) });
  },
  updateLegalCaseStatus(id: string, status: string, token: string) {
    return axios.patch(`${BASE}/legal/cases/${id}/status`, null, { params: { status }, headers: authHeaders(token) });
  },
  assignAdvocate(caseId: string, advocateId: string, token: string) {
    return axios.post(`${BASE}/legal/cases/${caseId}/assign`, null, { params: { advocateId }, headers: authHeaders(token) });
  },
  generateLegalReport(caseId: string, token: string) {
    return axios.post(`${BASE}/legal/cases/${caseId}/generate-report`, null, { headers: authHeaders(token) });
  },
  getAdvocates(token: string) {
    return axios.get(`${BASE}/legal/advocates`, { headers: authHeaders(token) });
  },

  // ══ VAS: Interiors ══
  getInteriorProjects(token: string, params?: any) {
    const qs = new URLSearchParams();
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/interiors/admin/projects?${qs}`, { headers: authHeaders(token) });
  },
  getInteriorProject(id: string, token: string) {
    return axios.get(`${BASE}/interiors/projects/${id}`, { headers: authHeaders(token) });
  },
  updateInteriorStatus(id: string, status: string, token: string) {
    return axios.patch(`${BASE}/interiors/projects/${id}/status`, null, { params: { status }, headers: authHeaders(token) });
  },
  assignDesigner(projectId: string, designerId: string, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/designer`, null, { params: { designerId }, headers: authHeaders(token) });
  },
  getDesigners(token: string) {
    return axios.get(`${BASE}/interiors/designers`, { headers: authHeaders(token) });
  },
  getInteriorMilestones(projectId: string, token: string) {
    return axios.get(`${BASE}/interiors/projects/${projectId}/milestones`, { headers: authHeaders(token) });
  },
  getQualityChecks(projectId: string, token: string) {
    return axios.get(`${BASE}/interiors/projects/${projectId}/quality-checks`, { headers: authHeaders(token) });
  },
  addInteriorMilestone(projectId: string, params: { name: string; description?: string; scheduledDate: string; paymentAmountPaise?: number }, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/milestones`, null, { params, headers: authHeaders(token) });
  },
  completeMilestone(milestoneId: string, token: string) {
    return axios.post(`${BASE}/interiors/milestones/${milestoneId}/complete`, null, { headers: authHeaders(token) });
  },
  addRoomDesign(projectId: string, body: { roomType: string; areaSqft?: number; designStyle?: string }, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/rooms`, body, { headers: authHeaders(token) });
  },
  getRoomDesigns(projectId: string, token: string) {
    return axios.get(`${BASE}/interiors/projects/${projectId}/rooms`, { headers: authHeaders(token) });
  },
  approveRoomDesign(projectId: string, roomId: string, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/rooms/${roomId}/approve`, null, { headers: authHeaders(token) });
  },
  generateInteriorQuote(projectId: string, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/quote`, null, { headers: authHeaders(token) });
  },
  addQualityCheck(projectId: string, params: { checkpointName: string; category?: string; status?: string; notes?: string; milestoneId?: string }, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/quality-checks`, null, { params, headers: authHeaders(token) });
  },
  addMaterialSelection(projectId: string, body: { roomDesignId?: string; catalogItemId?: string; category: string; materialName: string; brand?: string; quantity: number; unit?: string; unitPricePaise: number }, token: string) {
    return axios.post(`${BASE}/interiors/projects/${projectId}/materials`, body, { headers: authHeaders(token) });
  },
  getMaterials(projectId: string, token: string) {
    return axios.get(`${BASE}/interiors/projects/${projectId}/materials`, { headers: authHeaders(token) });
  },

  // ══ Users & Leads ══
  getUsers(token: string, params?: Record<string, any>) {
    const qs = new URLSearchParams();
    qs.set('size', '50');
    qs.set('sort', 'createdAt,desc');
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/admin/users?${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getUserStats(token: string) {
    return axios.get(`${BASE}/admin/users/stats`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getLeads(token: string, params?: Record<string, any>) {
    const qs = new URLSearchParams();
    qs.set('size', '50');
    qs.set('sort', 'createdAt,desc');
    if (params) Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') qs.set(k, String(v)); });
    return axios.get(`${BASE}/admin/leads?${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },

  getLeadStats(token: string) {
    return axios.get(`${BASE}/admin/leads/stats`, { headers: authHeaders(token) }).then(r => r.data).catch(() => ({}));
  },

  getLeadCampaigns(token: string) {
    return axios.get(`${BASE}/admin/leads/campaigns`, { headers: authHeaders(token) }).then(r => r.data).catch(() => []);
  },

  toggleCampaign(campaignId: string, token: string) {
    return axios.post(`${BASE}/admin/leads/campaigns/${campaignId}/toggle`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Platform staff pool (Phase B) ────────────────────────────────────
  listStaffPool(token: string, params?: { activeOnly?: boolean; role?: string }) {
    const qs = new URLSearchParams();
    if (params?.activeOnly != null) qs.set('activeOnly', String(params.activeOnly));
    if (params?.role) qs.set('role', params.role);
    return axios.get(`${BASE}/staff/admin/pool${qs.toString() ? `?${qs}` : ''}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createPoolStaff(data: any, token: string) {
    return axios.post(`${BASE}/staff/admin/pool`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  updatePoolStaff(staffId: string, data: any, token: string) {
    return axios.put(`${BASE}/staff/admin/pool/${staffId}`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  deletePoolStaff(staffId: string, token: string) {
    return axios.delete(`${BASE}/staff/admin/pool/${staffId}`, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Event pricing defaults (admin) ───────────────────────────────────
  listEventPricing(token: string) {
    return axios.get(`${BASE}/chef-events/pricing/admin`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createEventPricing(data: any, token: string) {
    return axios.post(`${BASE}/chef-events/pricing/admin`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  updateEventPricing(itemKey: string, data: any, token: string) {
    return axios.put(`${BASE}/chef-events/pricing/admin/${itemKey}`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  deactivateEventPricing(itemKey: string, token: string) {
    return axios.delete(`${BASE}/chef-events/pricing/admin/${itemKey}`, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Partner Vendors (admin-onboarded providers for bespoke services) ──
  listVendors(token: string, serviceType: string, activeOnly = false) {
    const qs = new URLSearchParams({ serviceType, activeOnly: String(activeOnly) });
    return axios.get(`${BASE}/vendors/admin?${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getVendor(id: string, token: string) {
    return axios.get(`${BASE}/vendors/admin/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  listEligibleVendors(token: string, serviceType: string, city?: string) {
    const qs = new URLSearchParams({ serviceType });
    if (city) qs.set('city', city);
    return axios.get(`${BASE}/vendors/admin/eligible?${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createVendor(data: any, token: string) {
    return axios.post(`${BASE}/vendors/admin`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  updateVendor(id: string, data: any, token: string) {
    return axios.put(`${BASE}/vendors/admin/${id}`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  setVendorActive(id: string, value: boolean, token: string) {
    return axios.post(`${BASE}/vendors/admin/${id}/active?value=${value}`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  verifyVendorKyc(id: string, verified: boolean, notes: string | null, token: string) {
    return axios.post(`${BASE}/vendors/admin/${id}/kyc`, { verified, notes }, { headers: authHeaders(token) }).then(r => r.data);
  },
  deleteVendor(id: string, token: string) {
    return axios.delete(`${BASE}/vendors/admin/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Services-leg listings (admin queue + lifecycle) ──
  // Vendors self-onboard via /vendor/onboard/{type} on safar-web; admin only
  // approves/rejects here. Replaces the manual partner-vendor onboarding flow
  // for new vendors going forward.
  listServiceListings(token: string, status: string = 'PENDING_REVIEW') {
    return axios.get(`${BASE}/services/admin/listings?status=${status}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getServiceListing(id: string, token: string) {
    return axios.get(`${BASE}/services/admin/listings/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  approveServiceListing(id: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${id}/approve`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  rejectServiceListing(id: string, reason: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${id}/reject`, { reason }, { headers: authHeaders(token) }).then(r => r.data);
  },
  suspendServiceListing(id: string, reason: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${id}/suspend`, { reason }, { headers: authHeaders(token) }).then(r => r.data);
  },
  restoreServiceListing(id: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${id}/restore`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  listServiceListingKyc(id: string, token: string) {
    return axios.get(`${BASE}/services/listings/${id}/kyc-documents`, { headers: authHeaders(token) }).then(r => r.data);
  },
  // ── Commission rates (platform-flexible monetization) ──
  listCommissionRates(token: string, serviceType?: string) {
    const qs = serviceType ? `?serviceType=${encodeURIComponent(serviceType)}` : '';
    return axios.get(`${BASE}/services/admin/commission-rates${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  updateCommissionRate(serviceType: string, tier: string, body: { commissionPct: number; promotionThreshold?: number; notes?: string }, token: string) {
    return axios.put(`${BASE}/services/admin/commission-rates/${serviceType}/${tier}`, body, { headers: authHeaders(token) }).then(r => r.data);
  },
  setVendorCommissionOverride(listingId: string, body: { commissionPctOverride: number | null; commissionOverrideReason?: string }, token: string) {
    return axios.put(`${BASE}/services/admin/listings/${listingId}/commission-override`, body, { headers: authHeaders(token) }).then(r => r.data);
  },
  // ── Vendor invites (Pattern E — WhatsApp BD outreach) ──
  listVendorInvites(token: string, serviceType?: string) {
    const qs = serviceType ? `?serviceType=${encodeURIComponent(serviceType)}` : '';
    return axios.get(`${BASE}/services/admin/invites${qs}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createVendorInvite(body: { phone: string; serviceType: string; businessName?: string; notes?: string; sentVia?: string }, token: string) {
    return axios.post(`${BASE}/services/admin/invites`, body, { headers: authHeaders(token) }).then(r => r.data);
  },
  cancelVendorInvite(id: string, token: string) {
    return axios.post(`${BASE}/services/admin/invites/${id}/cancel`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  // ── Pending changes (post-VERIFIED re-review) ──
  listPendingChanges(token: string) {
    return axios.get(`${BASE}/services/admin/listings/pending-changes`, { headers: authHeaders(token) }).then(r => r.data);
  },
  approvePendingChanges(listingId: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${listingId}/approve-changes`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  rejectPendingChanges(listingId: string, token: string) {
    return axios.post(`${BASE}/services/admin/listings/${listingId}/reject-changes`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },

  // Vendor assignment on a specific event booking
  getActiveBookingVendor(bookingId: string, token: string) {
    return axios.get(`${BASE}/chef-events/${bookingId}/vendor`, { headers: authHeaders(token) })
      .then(r => r.data).catch(() => null);
  },
  listBookingVendorHistory(bookingId: string, token: string) {
    return axios.get(`${BASE}/chef-events/${bookingId}/vendors`, { headers: authHeaders(token) }).then(r => r.data);
  },
  assignBookingVendor(bookingId: string, data: { vendorId: string; payoutPaise?: number; adminNotes?: string }, token: string) {
    return axios.post(`${BASE}/chef-events/${bookingId}/assign-vendor`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  confirmBookingVendor(bookingId: string, assignmentId: string, token: string) {
    return axios.post(`${BASE}/chef-events/${bookingId}/vendor/${assignmentId}/confirm`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  markBookingVendorDelivered(bookingId: string, assignmentId: string, token: string) {
    return axios.post(`${BASE}/chef-events/${bookingId}/vendor/${assignmentId}/delivered`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  cancelBookingVendor(bookingId: string, assignmentId: string, reason: string, token: string) {
    const qs = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return axios.post(`${BASE}/chef-events/${bookingId}/vendor/${assignmentId}/cancel${qs}`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  markBookingVendorPaid(bookingId: string, assignmentId: string, data: { payoutRef: string; payoutPaise?: number }, token: string) {
    return axios.post(`${BASE}/chef-events/${bookingId}/vendor/${assignmentId}/mark-paid`, data, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Supply Chain: Suppliers ──────────────────────────────────────────
  listSuppliers(token: string, activeOnly = false) {
    return axios.get(`${BASE}/suppliers/admin?activeOnly=${activeOnly}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getSupplier(id: string, token: string) {
    return axios.get(`${BASE}/suppliers/admin/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createSupplier(data: any, token: string) {
    return axios.post(`${BASE}/suppliers/admin`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  updateSupplier(id: string, data: any, token: string) {
    return axios.put(`${BASE}/suppliers/admin/${id}`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  setSupplierActive(id: string, value: boolean, token: string) {
    return axios.post(`${BASE}/suppliers/admin/${id}/active?value=${value}`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  verifySupplierKyc(id: string, verified: boolean, notes: string | null, token: string) {
    return axios.post(`${BASE}/suppliers/admin/${id}/kyc`, { verified, notes }, { headers: authHeaders(token) }).then(r => r.data);
  },
  deleteSupplier(id: string, token: string) {
    return axios.delete(`${BASE}/suppliers/admin/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  // Catalog
  listSupplierCatalog(supplierId: string, token: string, activeOnly = true) {
    return axios.get(`${BASE}/suppliers/admin/${supplierId}/catalog?activeOnly=${activeOnly}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  addCatalogItem(supplierId: string, data: any, token: string) {
    return axios.post(`${BASE}/suppliers/admin/${supplierId}/catalog`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  updateCatalogItem(supplierId: string, itemId: string, data: any, token: string) {
    return axios.put(`${BASE}/suppliers/admin/${supplierId}/catalog/${itemId}`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  deleteCatalogItem(supplierId: string, itemId: string, token: string) {
    return axios.delete(`${BASE}/suppliers/admin/${supplierId}/catalog/${itemId}`, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Supply Chain: Purchase Orders ────────────────────────────────────
  listPurchaseOrders(token: string, params?: { status?: string; supplierId?: string }) {
    const qs = new URLSearchParams();
    if (params?.status) qs.set('status', params.status);
    if (params?.supplierId) qs.set('supplierId', params.supplierId);
    return axios.get(`${BASE}/purchase-orders/admin${qs.toString() ? `?${qs}` : ''}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  listOverduePurchaseOrders(token: string) {
    return axios.get(`${BASE}/purchase-orders/admin/overdue`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getPurchaseOrder(id: string, token: string) {
    return axios.get(`${BASE}/purchase-orders/admin/${id}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getPurchaseOrderItems(id: string, token: string) {
    return axios.get(`${BASE}/purchase-orders/admin/${id}/items`, { headers: authHeaders(token) }).then(r => r.data);
  },
  createPurchaseOrder(data: any, token: string) {
    return axios.post(`${BASE}/purchase-orders/admin`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  transitionPurchaseOrder(id: string, action: 'issue' | 'ack' | 'in-transit' | 'deliver', token: string) {
    return axios.post(`${BASE}/purchase-orders/admin/${id}/${action}`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },
  invoicePurchaseOrder(id: string, data: { invoiceNumber: string; invoicePaise: number }, token: string) {
    return axios.post(`${BASE}/purchase-orders/admin/${id}/invoice`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  payPurchaseOrder(id: string, paymentRef: string, token: string) {
    return axios.post(`${BASE}/purchase-orders/admin/${id}/pay`, { paymentRef }, { headers: authHeaders(token) }).then(r => r.data);
  },
  cancelPurchaseOrder(id: string, reason: string, token: string) {
    const qs = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return axios.post(`${BASE}/purchase-orders/admin/${id}/cancel${qs}`, {}, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Supply Chain: Stock ──────────────────────────────────────────────
  listStock(token: string, params?: { category?: string; lowOnly?: boolean }) {
    const qs = new URLSearchParams();
    if (params?.category) qs.set('category', params.category);
    if (params?.lowOnly != null) qs.set('lowOnly', String(params.lowOnly));
    return axios.get(`${BASE}/stock/admin${qs.toString() ? `?${qs}` : ''}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getStockItem(itemKey: string, token: string) {
    return axios.get(`${BASE}/stock/admin/items/${itemKey}`, { headers: authHeaders(token) }).then(r => r.data);
  },
  getStockMovements(itemKey: string, token: string) {
    return axios.get(`${BASE}/stock/admin/items/${itemKey}/movements`, { headers: authHeaders(token) }).then(r => r.data);
  },
  upsertStockItem(data: any, token: string) {
    return axios.post(`${BASE}/stock/admin/items`, data, { headers: authHeaders(token) }).then(r => r.data);
  },
  adjustStock(itemKey: string, data: { qtyDelta: number; reason: string; notes?: string }, token: string) {
    return axios.post(`${BASE}/stock/admin/items/${itemKey}/adjust`, data, { headers: authHeaders(token) }).then(r => r.data);
  },

  // ── Generic S3 upload via presigned PUT (matches safar-web helper) ────
  async uploadFile(file: File, folder: string, token: string): Promise<string> {
    const presign = await axios.post<{ uploadUrl: string; publicUrl: string }>(
      `${BASE}/media/upload/generic-presign?folder=${encodeURIComponent(folder)}&contentType=${encodeURIComponent(file.type)}`,
      null,
      { headers: authHeaders(token) },
    ).then(r => r.data);
    await fetch(presign.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type },
    });
    return presign.publicUrl;
  },
};
