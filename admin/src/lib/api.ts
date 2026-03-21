import axios from 'axios';

const BASE = '/api/v1';

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

  verifyListing(id: string, token: string) {
    return axios.put(`${BASE}/admin/listings/${id}/verify`, {}, { headers: authHeaders(token) });
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
};
