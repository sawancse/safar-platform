'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';
const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  NOTICE_PERIOD: 'bg-yellow-100 text-yellow-800',
  VACATED: 'bg-gray-100 text-gray-600',
  TERMINATED: 'bg-red-100 text-red-800',
};

const INVOICE_COLORS: Record<string, string> = {
  GENERATED: 'bg-blue-100 text-blue-800',
  SENT: 'bg-purple-100 text-purple-800',
  PAID: 'bg-green-100 text-green-800',
  OVERDUE: 'bg-red-100 text-red-800',
  WAIVED: 'bg-gray-100 text-gray-600',
};

export default function HostPgTenancyTab({ listingId }: { listingId: string }) {
  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') ?? '' : '';
  const [tenancies, setTenancies] = useState<any[]>([]);
  const [overdue, setOverdue] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [selectedTenancy, setSelectedTenancy] = useState<any>(null);
  const [invoices, setInvoices] = useState<any[]>([]);

  useEffect(() => { loadData(); }, [listingId]);

  async function loadData() {
    setLoading(true);
    try {
      const [t, o] = await Promise.all([
        api.getPgTenancies(`listingId=${listingId}`, token!),
        api.getOverdueInvoices(token!),
      ]);
      setTenancies(t.content || []);
      setOverdue(o || []);
    } catch (e) { console.error(e); }
    setLoading(false);
  }

  async function loadInvoices(tenancyId: string) {
    const inv = await api.getTenancyInvoices(tenancyId, token!);
    setInvoices(inv.content || []);
  }

  async function giveNotice(id: string) {
    if (!confirm('Give 30-day notice? The tenant will be expected to vacate.')) return;
    await api.giveTenancyNotice(id, token!);
    loadData();
  }

  async function vacate(id: string) {
    if (!confirm('Mark as vacated?')) return;
    await api.vacateTenancy(id, token!);
    loadData();
  }

  if (loading) return <div className="p-6 text-center text-gray-500">Loading tenancies...</div>;

  return (
    <div className="space-y-6">
      {/* Overdue Alert */}
      {overdue.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4">
          <h3 className="font-semibold text-red-800 mb-2">⚠️ {overdue.length} Overdue Invoices</h3>
          {overdue.map((inv: any) => (
            <div key={inv.id} className="text-sm text-red-700">
              {inv.invoiceNumber} — ₹{(inv.grandTotalPaise / 100).toLocaleString()} due {inv.dueDate}
            </div>
          ))}
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl border p-4">
          <p className="text-sm text-gray-500">Active Tenants</p>
          <p className="text-2xl font-bold">{tenancies.filter(t => t.status === 'ACTIVE').length}</p>
        </div>
        <div className="bg-white rounded-xl border p-4">
          <p className="text-sm text-gray-500">Notice Period</p>
          <p className="text-2xl font-bold text-yellow-600">{tenancies.filter(t => t.status === 'NOTICE_PERIOD').length}</p>
        </div>
        <div className="bg-white rounded-xl border p-4">
          <p className="text-sm text-gray-500">Monthly Revenue</p>
          <p className="text-2xl font-bold text-green-600">
            ₹{(tenancies.filter(t => t.status === 'ACTIVE').reduce((s, t) => s + t.totalMonthlyPaise, 0) / 100).toLocaleString()}
          </p>
        </div>
        <div className="bg-white rounded-xl border p-4">
          <p className="text-sm text-gray-500">Overdue</p>
          <p className="text-2xl font-bold text-red-600">{overdue.length}</p>
        </div>
      </div>

      {/* Add Tenant Button */}
      <div className="flex justify-end">
        <button onClick={() => setShowForm(!showForm)}
          className="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm hover:bg-orange-600">
          + Add Tenant
        </button>
      </div>

      {/* Tenant List */}
      <div className="bg-white rounded-xl border divide-y">
        {tenancies.length === 0 ? (
          <div className="p-8 text-center text-gray-400">No tenants yet</div>
        ) : tenancies.map((t: any) => (
          <div key={t.id} className="p-4 flex items-center justify-between hover:bg-gray-50">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-medium">{t.tenancyRef}</span>
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[t.status]}`}>
                  {t.status}
                </span>
              </div>
              <div className="text-sm text-gray-500 mt-1">
                Bed {t.bedNumber || '—'} · {t.sharingType} · Move-in: {t.moveInDate}
                {t.moveOutDate && ` · Move-out: ${t.moveOutDate}`}
              </div>
              <div className="text-sm font-medium mt-1">
                ₹{(t.totalMonthlyPaise / 100).toLocaleString()}/month
                {t.mealsIncluded && ' · 🍽️ Meals'}
                {t.wifiIncluded && ' · 📶 WiFi'}
                {t.laundryIncluded && ' · 👕 Laundry'}
              </div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => { setSelectedTenancy(t); loadInvoices(t.id); }}
                className="text-sm text-blue-600 hover:underline">Invoices</button>
              {t.status === 'ACTIVE' && (
                <button onClick={() => giveNotice(t.id)}
                  className="text-sm text-yellow-600 hover:underline">Notice</button>
              )}
              {(t.status === 'ACTIVE' || t.status === 'NOTICE_PERIOD') && (
                <button onClick={() => vacate(t.id)}
                  className="text-sm text-red-600 hover:underline">Vacate</button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Invoice Modal */}
      {selectedTenancy && (
        <div className="bg-white rounded-xl border p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold">Invoices — {selectedTenancy.tenancyRef}</h3>
            <button onClick={() => setSelectedTenancy(null)} className="text-gray-400 hover:text-gray-600">✕</button>
          </div>
          {invoices.length === 0 ? (
            <p className="text-gray-400 text-sm">No invoices generated yet</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-gray-500 border-b">
                <th className="pb-2">Invoice</th><th>Period</th><th>Amount</th><th>Status</th><th>Due</th>
              </tr></thead>
              <tbody>
                {invoices.map((inv: any) => (
                  <tr key={inv.id} className="border-b">
                    <td className="py-2 font-medium">{inv.invoiceNumber}</td>
                    <td>{inv.billingMonth}/{inv.billingYear}</td>
                    <td>₹{(inv.grandTotalPaise / 100).toLocaleString()}</td>
                    <td><span className={`px-2 py-0.5 rounded-full text-xs ${INVOICE_COLORS[inv.status]}`}>{inv.status}</span></td>
                    <td>{inv.dueDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
