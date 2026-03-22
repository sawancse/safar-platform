'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';

interface KycData {
  id: string;
  status: string;
  fullLegalName: string | null;
  dateOfBirth: string | null;
  aadhaarNumber: string | null;
  aadhaarVerified: boolean;
  panNumber: string | null;
  panVerified: boolean;
  addressLine1: string | null;
  city: string | null;
  state: string | null;
  pincode: string | null;
  bankAccountName: string | null;
  bankAccountNumber: string | null;
  bankIfsc: string | null;
  bankName: string | null;
  bankVerified: boolean;
  gstin: string | null;
  gstVerified: boolean;
  businessName: string | null;
  businessType: string | null;
  rejectionReason: string | null;
  completionPercent: number;
  submittedAt: string | null;
  verifiedAt: string | null;
}

const STATUS_CONFIG: Record<string, { color: string; label: string; icon: string }> = {
  NOT_STARTED:     { color: 'bg-gray-100 text-gray-600', label: 'Not Started', icon: '○' },
  IDENTITY_PENDING:{ color: 'bg-yellow-100 text-yellow-700', label: 'Identity Pending', icon: '◔' },
  ADDRESS_PENDING: { color: 'bg-yellow-100 text-yellow-700', label: 'Address Pending', icon: '◑' },
  BANK_PENDING:    { color: 'bg-yellow-100 text-yellow-700', label: 'Bank Pending', icon: '◕' },
  SUBMITTED:       { color: 'bg-blue-100 text-blue-700', label: 'Under Review', icon: '◉' },
  VERIFIED:        { color: 'bg-green-100 text-green-700', label: 'Verified', icon: '✓' },
  REJECTED:        { color: 'bg-red-100 text-red-600', label: 'Rejected', icon: '✕' },
};

const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
  'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka',
  'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram',
  'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu',
  'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Delhi', 'Chandigarh', 'Puducherry', 'Jammu and Kashmir', 'Ladakh',
];

function StepCard({ step, title, desc, done, active, children }: {
  step: number; title: string; desc: string; done: boolean; active: boolean;
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(active);
  return (
    <div className={`border-2 rounded-2xl overflow-hidden transition ${
      done ? 'border-green-300 bg-green-50/30' : active ? 'border-orange-300' : 'border-gray-200'
    }`}>
      <button type="button" onClick={() => setOpen(!open)}
        className="w-full text-left px-5 py-4 flex items-center gap-4">
        <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 ${
          done ? 'bg-green-500 text-white' : active ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-500'
        }`}>
          {done ? '✓' : step}
        </div>
        <div className="flex-1">
          <p className="text-sm font-semibold text-gray-800">{title}</p>
          <p className="text-xs text-gray-500">{desc}</p>
        </div>
        {done && <span className="text-xs text-green-600 font-semibold">Complete</span>}
        <svg className={`w-4 h-4 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && <div className="px-5 pb-5 pt-1">{children}</div>}
    </div>
  );
}

export default function HostKycTab({ token }: { token: string }) {
  const [kyc, setKyc] = useState<KycData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  // Form states
  const [name, setName] = useState('');
  const [dob, setDob] = useState('');
  const [aadhaar, setAadhaar] = useState('');
  const [pan, setPan] = useState('');
  const [addr1, setAddr1] = useState('');
  const [addr2, setAddr2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [pincode, setPincode] = useState('');
  const [bankName, setBankName] = useState('');
  const [accountName, setAccountName] = useState('');
  const [accountNum, setAccountNum] = useState('');
  const [ifsc, setIfsc] = useState('');
  const [gstin, setGstin] = useState('');
  const [bizName, setBizName] = useState('');
  const [bizType, setBizType] = useState('');
  const [hostType, setHostType] = useState('INDIVIDUAL');

  useEffect(() => {
    api.getKyc(token).then((data: KycData) => {
      setKyc(data);
      // Pre-fill forms
      if (data.fullLegalName) setName(data.fullLegalName);
      if (data.dateOfBirth) setDob(data.dateOfBirth);
      if (data.panNumber) setPan(data.panNumber);
      if (data.addressLine1) setAddr1(data.addressLine1);
      if (data.city) setCity(data.city);
      if (data.state) setState(data.state);
      if (data.pincode) setPincode(data.pincode);
      if (data.bankName) setBankName(data.bankName);
      if (data.bankAccountName) setAccountName(data.bankAccountName);
      if (data.bankIfsc) setIfsc(data.bankIfsc);
      if (data.gstin) setGstin(data.gstin);
      if (data.businessName) setBizName(data.businessName);
      if (data.businessType) setBizType(data.businessType);
    }).catch(() => {}).finally(() => setLoading(false));
  }, [token]);

  async function saveIdentity() {
    setSaving(true); setError('');
    try {
      const data = await api.updateKycIdentity({
        fullLegalName: name, dateOfBirth: dob || null,
        aadhaarNumber: aadhaar, panNumber: pan.toUpperCase(),
      }, token);
      setKyc(data);
    } catch (e: any) { setError(e.message); }
    finally { setSaving(false); }
  }

  async function saveAddress() {
    setSaving(true); setError('');
    try {
      const data = await api.updateKycAddress({
        addressLine1: addr1, addressLine2: addr2 || null,
        city, state, pincode,
      }, token);
      setKyc(data);
    } catch (e: any) { setError(e.message); }
    finally { setSaving(false); }
  }

  async function saveBank() {
    setSaving(true); setError('');
    try {
      const data = await api.updateKycBank({
        bankAccountName: accountName, bankAccountNumber: accountNum,
        bankIfsc: ifsc.toUpperCase(), bankName,
      }, token);
      setKyc(data);
    } catch (e: any) { setError(e.message); }
    finally { setSaving(false); }
  }

  async function saveBusiness() {
    setSaving(true); setError('');
    try {
      const data = await api.updateKycBusiness({
        gstin: gstin || null, businessName: bizName || null,
        businessType: bizType || null,
      }, token);
      setKyc(data);
    } catch (e: any) { setError(e.message); }
    finally { setSaving(false); }
  }

  if (loading) {
    return <div className="text-center py-20 text-gray-400"><p>Loading KYC status...</p></div>;
  }

  const status = STATUS_CONFIG[kyc?.status ?? 'NOT_STARTED'] ?? STATUS_CONFIG.NOT_STARTED;
  const identityDone = !!(kyc?.aadhaarVerified && kyc?.panVerified);
  const addressDone = !!(kyc?.addressLine1 && kyc?.city && kyc?.pincode);
  const bankDone = !!(kyc?.bankVerified);
  const isVerified = kyc?.status === 'VERIFIED';
  const isSubmitted = kyc?.status === 'SUBMITTED';
  const isRejected = kyc?.status === 'REJECTED';

  return (
    <div>
      {/* Status header */}
      <div className="mb-6 border rounded-2xl p-5 bg-white flex items-center justify-between">
        <div className="flex items-center gap-4">
          {/* Progress ring */}
          <div className="relative w-14 h-14">
            <svg className="w-14 h-14 -rotate-90" viewBox="0 0 56 56">
              <circle cx="28" cy="28" r="24" fill="none" stroke="#e5e7eb" strokeWidth="4" />
              <circle cx="28" cy="28" r="24" fill="none"
                stroke={isVerified ? '#22c55e' : '#f97316'}
                strokeWidth="4" strokeLinecap="round"
                strokeDasharray={`${(kyc?.completionPercent ?? 0) * 1.508} 150.8`} />
            </svg>
            <span className="absolute inset-0 flex items-center justify-center text-xs font-bold">
              {kyc?.completionPercent ?? 0}%
            </span>
          </div>
          <div>
            <p className="text-base font-bold text-gray-900">Identity Verification</p>
            <p className="text-sm text-gray-500">Complete your KYC to activate listings and receive payouts</p>
          </div>
        </div>
        <span className={`text-xs font-semibold px-3 py-1.5 rounded-full ${status.color}`}>
          {status.icon} {status.label}
        </span>
      </div>

      {/* Rejection banner */}
      {isRejected && kyc?.rejectionReason && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-2xl p-4">
          <p className="text-sm font-semibold text-red-700">Your KYC was rejected</p>
          <p className="text-sm text-red-600 mt-1">{kyc.rejectionReason}</p>
          <p className="text-xs text-red-500 mt-2">Please update your details and resubmit.</p>
        </div>
      )}

      {/* Verified banner */}
      {isVerified && (
        <div className="mb-6 bg-green-50 border border-green-200 rounded-2xl p-4 flex items-center gap-3">
          <span className="text-3xl">&#9989;</span>
          <div>
            <p className="text-sm font-semibold text-green-800">KYC Verified</p>
            <p className="text-xs text-green-600">Your identity has been verified. You can now receive payouts and have listings go live.</p>
          </div>
        </div>
      )}

      {/* Under review banner */}
      {isSubmitted && (
        <div className="mb-6 bg-blue-50 border border-blue-200 rounded-2xl p-4 flex items-center gap-3">
          <span className="text-3xl">&#128269;</span>
          <div>
            <p className="text-sm font-semibold text-blue-800">Under Review</p>
            <p className="text-xs text-blue-600">Your KYC documents are being verified. This usually takes 24-48 hours.</p>
          </div>
        </div>
      )}

      {error && (
        <div className="mb-4 bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm">{error}</div>
      )}

      {/* Host Type Selection */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">What type of host are you?</h3>
        <div className="grid grid-cols-2 gap-3">
          {[
            { value: 'INDIVIDUAL', label: 'Individual', desc: 'I own or rent a property', icon: '\u{1F3E0}' },
            { value: 'PROPERTY_MANAGER', label: 'Property Manager', desc: 'I manage properties for others', icon: '\u{1F3E2}' },
            { value: 'BUSINESS', label: 'Business / Hotel', desc: 'I run a hospitality business', icon: '\u{1F3E8}' },
            { value: 'NGO', label: 'NGO / Organization', desc: 'I represent an organization (Aashray)', icon: '\u{1F91D}' },
          ].map(type => (
            <button key={type.value}
              onClick={() => setHostType(type.value)}
              className={`p-4 rounded-xl border-2 text-left transition ${
                hostType === type.value ? 'border-orange-500 bg-orange-50' : 'border-gray-200 hover:border-gray-300'
              }`}>
              <span className="text-2xl">{type.icon}</span>
              <p className="font-semibold text-sm mt-1">{type.label}</p>
              <p className="text-xs text-gray-500">{type.desc}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Steps */}
      <div className="space-y-4">
        {/* Step 1: Identity */}
        <StepCard step={1} title="Government ID" desc="Aadhaar card and PAN card verification"
          done={identityDone} active={!identityDone}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-gray-600 mb-1">Full legal name (as on Aadhaar)</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={name} onChange={e => setName(e.target.value)}
                placeholder="Full name as per government ID" disabled={isVerified} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Date of birth</label>
              <input type="date" className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={dob} onChange={e => setDob(e.target.value)} disabled={isVerified} />
            </div>
            <div />
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Aadhaar number</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono"
                maxLength={12} placeholder="XXXX XXXX XXXX"
                value={aadhaar} onChange={e => setAadhaar(e.target.value.replace(/\D/g, ''))}
                disabled={isVerified || identityDone} />
              {kyc?.aadhaarVerified && (
                <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                  <span>&#9989;</span> Aadhaar verified
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">PAN number</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono uppercase"
                maxLength={10} placeholder="ABCDE1234F"
                value={pan} onChange={e => setPan(e.target.value.toUpperCase())}
                disabled={isVerified || identityDone} />
              {kyc?.panVerified && (
                <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                  <span>&#9989;</span> PAN verified
                </p>
              )}
            </div>
          </div>
          {!isVerified && !identityDone && (
            <button onClick={saveIdentity} disabled={saving || !name || aadhaar.length !== 12 || pan.length !== 10}
              className="mt-4 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm disabled:opacity-50 transition">
              {saving ? 'Verifying...' : 'Verify Identity'}
            </button>
          )}
        </StepCard>

        {/* Step 2: Address */}
        <StepCard step={2} title="Address" desc="Your registered address for correspondence"
          done={addressDone} active={identityDone && !addressDone}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-gray-600 mb-1">Address line 1</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={addr1} onChange={e => setAddr1(e.target.value)}
                placeholder="House/flat number, street" disabled={isVerified} />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-gray-600 mb-1">Address line 2 (optional)</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={addr2} onChange={e => setAddr2(e.target.value)}
                placeholder="Locality, landmark" disabled={isVerified} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">City</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={city} onChange={e => setCity(e.target.value)} placeholder="Mumbai" disabled={isVerified} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">State</label>
              <select className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 bg-white"
                value={state} onChange={e => setState(e.target.value)} disabled={isVerified}>
                <option value="">Select state</option>
                {INDIAN_STATES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Pincode</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono"
                maxLength={6} value={pincode} onChange={e => setPincode(e.target.value.replace(/\D/g, ''))}
                placeholder="400001" disabled={isVerified} />
            </div>
          </div>
          {!isVerified && !addressDone && (
            <button onClick={saveAddress} disabled={saving || !addr1 || !city || !state || pincode.length !== 6}
              className="mt-4 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm disabled:opacity-50 transition">
              {saving ? 'Saving...' : 'Save Address'}
            </button>
          )}
        </StepCard>

        {/* Step 3: Bank Details */}
        <StepCard step={3} title="Payout Account" desc="Bank account where you'll receive payouts"
          done={bankDone} active={addressDone && !bankDone}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-gray-600 mb-1">Account holder name</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={accountName} onChange={e => setAccountName(e.target.value)}
                placeholder="Name as per bank records" disabled={isVerified} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Account number</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono"
                value={accountNum} onChange={e => setAccountNum(e.target.value.replace(/\D/g, ''))}
                placeholder="Account number" disabled={isVerified || bankDone} />
              {kyc?.bankVerified && (
                <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                  <span>&#9989;</span> Account verified ({kyc.bankAccountNumber})
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">IFSC code</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono uppercase"
                maxLength={11} value={ifsc} onChange={e => setIfsc(e.target.value.toUpperCase())}
                placeholder="SBIN0001234" disabled={isVerified || bankDone} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Bank name</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={bankName} onChange={e => setBankName(e.target.value)}
                placeholder="State Bank of India" disabled={isVerified} />
            </div>
          </div>
          {!isVerified && !bankDone && (
            <button onClick={saveBank} disabled={saving || !accountName || !accountNum || ifsc.length !== 11 || !bankName}
              className="mt-4 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm disabled:opacity-50 transition">
              {saving ? 'Verifying...' : 'Verify Bank Account'}
            </button>
          )}
        </StepCard>

        {/* Step 4: Business (optional) */}
        <StepCard step={4} title="Business Details" desc="Optional — for commercial hosts with GST registration"
          done={!!(kyc?.gstVerified)} active={bankDone}>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">GSTIN (optional)</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono uppercase"
                maxLength={15} value={gstin} onChange={e => setGstin(e.target.value.toUpperCase())}
                placeholder="22AAAAA0000A1Z5" disabled={isVerified} />
              {kyc?.gstVerified && (
                <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                  <span>&#9989;</span> GST verified
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Business name</label>
              <input className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                value={bizName} onChange={e => setBizName(e.target.value)}
                placeholder="Business name" disabled={isVerified} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Business type</label>
              <select className="w-full border rounded-lg px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 bg-white"
                value={bizType} onChange={e => setBizType(e.target.value)} disabled={isVerified}>
                <option value="">Select type</option>
                <option value="INDIVIDUAL">Individual / Sole Proprietor</option>
                <option value="PARTNERSHIP">Partnership</option>
                <option value="PRIVATE_LIMITED">Private Limited</option>
                <option value="LLP">LLP</option>
                <option value="TRUST">Trust / Society</option>
              </select>
            </div>
          </div>
          {!isVerified && (
            <button onClick={saveBusiness} disabled={saving}
              className="mt-4 bg-gray-200 hover:bg-gray-300 text-gray-700 font-semibold px-6 py-2.5 rounded-xl text-sm disabled:opacity-50 transition">
              {saving ? 'Saving...' : 'Save Business Details'}
            </button>
          )}
        </StepCard>
      </div>

      {/* Info footer */}
      {!isVerified && !isSubmitted && (
        <div className="mt-6 bg-blue-50 border border-blue-200 rounded-2xl p-4">
          <p className="text-sm text-blue-800 font-medium">Why do we need this?</p>
          <ul className="text-xs text-blue-700 mt-2 space-y-1 list-disc list-inside">
            <li>Government regulations require identity verification for property hosts</li>
            <li>Bank details are needed to send you payouts after each booking</li>
            <li>Your information is encrypted and never shared with guests</li>
            <li>KYC verification usually takes 24-48 hours after submission</li>
          </ul>
        </div>
      )}
    </div>
  );
}
