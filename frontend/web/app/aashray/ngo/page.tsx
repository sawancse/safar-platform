'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';

const ORG_TYPES = [
  { value: 'NGO', label: 'Non-Governmental Organization', icon: '🏛️' },
  { value: 'GOVERNMENT', label: 'Government Agency', icon: '🏢' },
  { value: 'INTERNATIONAL', label: 'International Organization', icon: '🌍' },
  { value: 'CSR', label: 'Corporate CSR Program', icon: '🏭' },
];

export default function NgoRegistrationPage() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const [name, setName] = useState('');
  const [type, setType] = useState('NGO');
  const [unhcrCode, setUnhcrCode] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [budgetLakhs, setBudgetLakhs] = useState('');
  const [description, setDescription] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const token = localStorage.getItem('access_token');
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      const res = await fetch(`${apiUrl}/api/v1/aashray/organizations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          name, type,
          unhcrPartnerCode: unhcrCode || null,
          contactEmail: email,
          contactPhone: phone || null,
          budgetPaise: Math.round(Number(budgetLakhs) * 10000000) || 0,
        }),
      });
      if (!res.ok) throw new Error(await res.text().catch(() => 'Registration failed'));
      setSuccess(true);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <span className="text-6xl">✅</span>
        <h1 className="text-2xl font-bold mt-4 mb-2">Registration Submitted</h1>
        <p className="text-gray-500 mb-6">
          Our team will review your application and reach out within 48 hours.
        </p>
        <button onClick={() => router.push('/aashray')}
          className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-6 py-3 rounded-xl transition">
          Back to Aashray
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">Register as NGO Partner</h1>
      <p className="text-gray-500 mb-8">
        Join Safar Aashray to find housing for displaced persons through our platform.
      </p>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Org type */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Organization Type</label>
          <div className="grid grid-cols-2 gap-3">
            {ORG_TYPES.map(t => (
              <button key={t.value} type="button" onClick={() => setType(t.value)}
                className={`text-left border-2 rounded-xl p-4 transition ${
                  type === t.value ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-gray-300'
                }`}>
                <span className="text-2xl">{t.icon}</span>
                <p className="text-sm font-semibold mt-2">{t.label}</p>
              </button>
            ))}
          </div>
        </div>

        {/* Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Organization Name *</label>
          <input required className="w-full border rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-400"
            placeholder="e.g. Jesuit Refugee Service India"
            value={name} onChange={e => setName(e.target.value)} />
        </div>

        {/* UNHCR code */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">UNHCR Partner Code (if applicable)</label>
          <input className="w-full border rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-400"
            placeholder="e.g. IND-NGO-0042"
            value={unhcrCode} onChange={e => setUnhcrCode(e.target.value)} />
        </div>

        {/* Contact */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contact Email *</label>
            <input required type="email" className="w-full border rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-400"
              placeholder="contact@org.com"
              value={email} onChange={e => setEmail(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
            <input className="w-full border rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-400"
              placeholder="+91 98765 43210"
              value={phone} onChange={e => setPhone(e.target.value)} />
          </div>
        </div>

        {/* Budget */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Annual Housing Budget (Lakhs INR)</label>
          <input type="number" className="w-full border rounded-xl px-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-blue-400"
            placeholder="e.g. 50"
            value={budgetLakhs} onChange={e => setBudgetLakhs(e.target.value)} />
          <p className="text-xs text-gray-400 mt-1">Approximate annual budget for refugee housing</p>
        </div>

        {error && <div className="bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm">{error}</div>}

        <button type="submit" disabled={submitting || !name || !email}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-xl disabled:opacity-50 transition">
          {submitting ? 'Submitting...' : 'Submit Application'}
        </button>

        <p className="text-xs text-gray-400 text-center">
          Applications are reviewed within 48 hours. We may contact you for additional verification.
        </p>
      </form>
    </div>
  );
}
