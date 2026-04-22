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
