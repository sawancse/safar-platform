'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

interface GstInvoice {
  id: string;
  invoiceNumber: string;
  bookingId: string;
  guestName: string;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
  invoiceDate: string;
  pdfUrl: string;
}

interface HostInvoice {
  id: string;
  invoiceNumber: string;
  tier: string;
  amountPaise: number;
  gstAmountPaise: number;
  totalPaise: number;
  status: string;
  billingPeriodStart: string;
  billingPeriodEnd: string;
}

interface TdsReport {
  hostId: string;
  pan: string;
  period: string;
  totalRevenuePaise: number;
  tdsDeductedPaise: number;
  invoiceCount: number;
}

export default function HostInvoicesTab({ token }: { token: string }) {
  const [gstInvoices, setGstInvoices] = useState<GstInvoice[]>([]);
  const [subInvoices, setSubInvoices] = useState<HostInvoice[]>([]);
  const [tdsReport, setTdsReport] = useState<TdsReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeSection, setActiveSection] = useState<'gst' | 'subscription' | 'tds'>('gst');
  const [year, setYear] = useState(new Date().getFullYear());
  const [quarter, setQuarter] = useState(Math.ceil((new Date().getMonth() + 1) / 3));

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.getGstInvoices(token).catch(() => []),
      api.getSubscriptionInvoices(token).catch(() => []),
      api.getTdsReport(year, quarter, token).catch(() => null),
    ]).then(([gst, sub, tds]) => {
      setGstInvoices(Array.isArray(gst) ? gst : []);
      setSubInvoices(Array.isArray(sub) ? sub : []);
      setTdsReport(tds);
    }).finally(() => setLoading(false));
  }, [token, year, quarter]);

  if (loading) {
    return <div className="text-center py-20 text-gray-400"><div className="text-4xl mb-4 animate-spin">&#8987;</div><p>Loading invoices...</p></div>;
  }

  const fmtDate = (d: string) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '-';

  const STATUS_STYLE: Record<string, string> = {
    DRAFT: 'bg-gray-100 text-gray-600',
    ISSUED: 'bg-blue-100 text-blue-700',
    PAID: 'bg-green-100 text-green-700',
    OVERDUE: 'bg-red-100 text-red-600',
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">Invoices &amp; Tax</h2>
        <div className="flex gap-2">
          <select value={year} onChange={(e) => setYear(Number(e.target.value))}
            className="border rounded-lg px-3 py-1.5 text-sm">
            {[2024, 2025, 2026].map(y => <option key={y} value={y}>{y}</option>)}
          </select>
          {activeSection === 'tds' && (
            <select value={quarter} onChange={(e) => setQuarter(Number(e.target.value))}
              className="border rounded-lg px-3 py-1.5 text-sm">
              {[1, 2, 3, 4].map(q => <option key={q} value={q}>Q{q}</option>)}
            </select>
          )}
        </div>
      </div>

      {/* Section tabs */}
      <div className="flex gap-1 bg-gray-100 rounded-xl p-1">
        {[
          { key: 'gst' as const, label: `GST Invoices (${gstInvoices.length})` },
          { key: 'subscription' as const, label: `Subscription (${subInvoices.length})` },
          { key: 'tds' as const, label: 'TDS Report' },
        ].map(tab => (
          <button key={tab.key} onClick={() => setActiveSection(tab.key)}
            className={`flex-1 py-2 px-3 rounded-lg text-sm font-medium transition ${
              activeSection === tab.key ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
            }`}>
            {tab.label}
          </button>
        ))}
      </div>

      {/* GST Invoices */}
      {activeSection === 'gst' && (
        <div>
          {gstInvoices.length === 0 ? (
            <div className="text-center py-16 text-gray-400">
              <p className="text-5xl mb-3">&#128196;</p>
              <p className="font-medium">No GST invoices yet</p>
              <p className="text-sm mt-1">Invoices are generated when bookings are completed</p>
            </div>
          ) : (
            <div className="space-y-3">
              {gstInvoices.map(inv => (
                <div key={inv.id} className="border rounded-xl p-4 bg-white">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <p className="font-semibold text-sm">{inv.invoiceNumber}</p>
                      <p className="text-xs text-gray-400">{fmtDate(inv.invoiceDate)}</p>
                    </div>
                    <p className="font-bold text-lg">{formatPaise(inv.totalAmount)}</p>
                  </div>
                  <div className="grid grid-cols-4 gap-2 text-xs text-gray-500">
                    <div>Taxable: {formatPaise(inv.taxableAmount)}</div>
                    <div>CGST: {formatPaise(inv.cgstAmount)}</div>
                    <div>SGST: {formatPaise(inv.sgstAmount)}</div>
                    <div>IGST: {formatPaise(inv.igstAmount)}</div>
                  </div>
                  {inv.guestName && <p className="text-xs text-gray-400 mt-1">Guest: {inv.guestName}</p>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Subscription Invoices */}
      {activeSection === 'subscription' && (
        <div>
          {subInvoices.length === 0 ? (
            <div className="text-center py-16 text-gray-400">
              <p className="text-5xl mb-3">&#128179;</p>
              <p className="font-medium">No subscription invoices</p>
              <p className="text-sm mt-1">Subscribe to a plan to see invoices here</p>
            </div>
          ) : (
            <div className="space-y-3">
              {subInvoices.map(inv => (
                <div key={inv.id} className="border rounded-xl p-4 bg-white flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-sm">{inv.invoiceNumber}</p>
                    <p className="text-xs text-gray-400">{inv.tier} Plan — {fmtDate(inv.billingPeriodStart)} to {fmtDate(inv.billingPeriodEnd)}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold">{formatPaise(inv.totalPaise)}</p>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_STYLE[inv.status] || 'bg-gray-100'}`}>
                      {inv.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TDS Report */}
      {activeSection === 'tds' && (
        <div>
          {tdsReport ? (
            <div className="bg-white border rounded-xl p-6">
              <h3 className="font-semibold text-gray-900 mb-4">TDS Report — Q{quarter} {year}</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-600">PAN</span>
                  <span className="font-mono font-semibold">{tdsReport.pan}</span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-600">Total Revenue</span>
                  <span className="font-semibold">{formatPaise(tdsReport.totalRevenuePaise)}</span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-600">TDS Deducted (1%)</span>
                  <span className="font-semibold text-red-600">{formatPaise(tdsReport.tdsDeductedPaise)}</span>
                </div>
                <div className="flex justify-between py-2 border-b">
                  <span className="text-gray-600">Invoice Count</span>
                  <span className="font-semibold">{tdsReport.invoiceCount}</span>
                </div>
              </div>
              <p className="text-xs text-gray-400 mt-4">TDS certificates (Form 16A) will be available after quarterly filing.</p>
            </div>
          ) : (
            <div className="text-center py-16 text-gray-400">
              <p className="text-5xl mb-3">&#128203;</p>
              <p className="font-medium">No TDS data for Q{quarter} {year}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
