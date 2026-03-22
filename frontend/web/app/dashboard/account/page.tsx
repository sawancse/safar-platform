'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import type { UserProfile, CoTraveler, SavedPaymentMethod } from '@/types';

type SidebarKey = 'personal' | 'security' | 'travelers' | 'display' | 'payments' | 'privacy';

const SIDEBAR_ITEMS: { key: SidebarKey; icon: string; label: string }[] = [
  { key: 'personal', icon: '&#x1F464;', label: 'Personal details' },
  { key: 'security', icon: '&#x1F512;', label: 'Security settings' },
  { key: 'travelers', icon: '&#x1F465;', label: 'Other travelers' },
  { key: 'display', icon: '&#x1F3A8;', label: 'Display settings' },
  { key: 'payments', icon: '&#x1F4B3;', label: 'Payment methods' },
  { key: 'privacy', icon: '&#x1F6E1;', label: 'Privacy and data management' },
];

type EditField = 'name' | 'displayName' | 'email' | 'phone' | 'dob' | 'nationality' | 'gender' | 'address' | 'passport';

export default function AccountPage() {
  const router = useRouter();
  const [activeSection, setActiveSection] = useState<SidebarKey>('personal');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profile, setProfile] = useState<UserProfile | null>(null);

  // Edit state
  const [editingField, setEditingField] = useState<EditField | null>(null);

  // Temp edit values
  const [editFirstName, setEditFirstName] = useState('');
  const [editLastName, setEditLastName] = useState('');
  const [editDisplayName, setEditDisplayName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editPhone, setEditPhone] = useState('');
  const [editDob, setEditDob] = useState('');
  const [editNationality, setEditNationality] = useState('');
  const [editGender, setEditGender] = useState('');
  const [editAddress, setEditAddress] = useState('');
  const [editPassportName, setEditPassportName] = useState('');
  const [editPassportNumber, setEditPassportNumber] = useState('');
  const [editPassportExpiry, setEditPassportExpiry] = useState('');

  // Co-travelers
  const [coTravelers, setCoTravelers] = useState<CoTraveler[]>([]);
  const [showTravelerForm, setShowTravelerForm] = useState(false);
  const [editingTravelerId, setEditingTravelerId] = useState<string | null>(null);
  const [travFirstName, setTravFirstName] = useState('');
  const [travLastName, setTravLastName] = useState('');
  const [travDob, setTravDob] = useState('');
  const [travGender, setTravGender] = useState('');
  const [travConsent, setTravConsent] = useState(false);
  const [travSaving, setTravSaving] = useState(false);

  // Payment methods
  const [paymentMethods, setPaymentMethods] = useState<SavedPaymentMethod[]>([]);
  const [showPaymentForm, setShowPaymentForm] = useState(false);
  const [editingPaymentId, setEditingPaymentId] = useState<string | null>(null);
  const [pmType, setPmType] = useState<'UPI' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'NET_BANKING'>('UPI');
  const [pmUpiId, setPmUpiId] = useState('');
  const [pmCardNumber, setPmCardNumber] = useState('');
  const [pmCardNetwork, setPmCardNetwork] = useState('');
  const [pmCardHolder, setPmCardHolder] = useState('');
  const [pmCardExpMonth, setPmCardExpMonth] = useState('');
  const [pmCardExpYear, setPmCardExpYear] = useState('');
  const [pmBankName, setPmBankName] = useState('');
  const [pmBankAccount, setPmBankAccount] = useState('');
  const [pmLabel, setPmLabel] = useState('');
  const [pmDefault, setPmDefault] = useState(false);
  const [pmSaving, setPmSaving] = useState(false);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  const loadProfile = useCallback(async (t: string) => {
    try {
      const p = await api.getMyProfile(t);
      setProfile(p);
    } catch {
      // Profile may not exist yet — use defaults
      setProfile({
        userId: localStorage.getItem('user_id') || '',
        name: localStorage.getItem('user_name') || '',
        phone: '',
        role: localStorage.getItem('user_role') || 'GUEST',
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) {
      router.push('/auth?redirect=/dashboard/account');
      return;
    }
    setToken(t);
    loadProfile(t);
    api.getCoTravelers(t).then(setCoTravelers).catch(() => {});
    api.getPaymentMethods(t).then(setPaymentMethods).catch(() => {});
  }, [router, loadProfile]);

  if (loading || !profile) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#x23F3;</div>
        <p>Loading your account...</p>
      </div>
    );
  }

  const nameParts = (profile.name || '').split(' ');
  const firstName = nameParts[0] || '';
  const lastName = nameParts.slice(1).join(' ') || '';

  const avatarSrc = profile.avatarUrl
    ? (profile.avatarUrl.startsWith('http') ? profile.avatarUrl : `${apiUrl}${profile.avatarUrl}`)
    : '';

  const initials = profile.name
    ? profile.name.split(' ').map((w) => w[0]).join('').toUpperCase().slice(0, 2)
    : '?';

  function startEdit(field: EditField) {
    setEditFirstName(firstName);
    setEditLastName(lastName);
    setEditDisplayName(profile!.displayName || '');
    setEditEmail(profile!.email || '');
    setEditPhone((profile!.phone || '').replace('+91', ''));
    setEditDob(profile!.dateOfBirth || '');
    setEditNationality(profile!.nationality || 'India');
    setEditGender(profile!.gender || '');
    setEditAddress(profile!.address || 'India');
    setEditPassportName(profile!.passportName || '');
    setEditPassportNumber(profile!.passportNumber || '');
    setEditPassportExpiry(profile!.passportExpiry || '');
    setEditingField(field);
  }

  async function saveField(field: EditField) {
    setSaving(true);
    try {
      let data: Partial<UserProfile> = {};
      switch (field) {
        case 'name':
          data = { name: `${editFirstName.trim()} ${editLastName.trim()}`.trim() };
          break;
        case 'displayName':
          data = { displayName: editDisplayName.trim() };
          break;
        case 'email':
          data = { email: editEmail.trim() };
          break;
        case 'phone':
          data = { phone: editPhone.trim() ? `+91${editPhone.trim()}` : undefined };
          break;
        case 'dob':
          data = { dateOfBirth: editDob || undefined };
          break;
        case 'nationality':
          data = { nationality: editNationality };
          break;
        case 'gender':
          data = { gender: editGender };
          break;
        case 'address':
          data = { address: editAddress.trim() };
          break;
        case 'passport':
          data = {
            passportName: editPassportName.trim(),
            passportNumber: editPassportNumber.trim(),
            passportExpiry: editPassportExpiry || undefined,
          };
          break;
      }

      const updated = await api.updateMyProfile(data, token);
      setProfile(updated);

      // Keep localStorage in sync for Navbar
      if (data.name) localStorage.setItem('user_name', data.name);

      setEditingField(null);
    } catch (e: any) {
      alert(e.message || 'Failed to save. Please try again.');
    } finally {
      setSaving(false);
    }
  }

  function formatPhone(p?: string) {
    if (!p) return '';
    const digits = p.replace(/\D/g, '');
    if (digits.length >= 10) {
      const last10 = digits.slice(-10);
      return `+91 ${last10.slice(0, 5)} ${last10.slice(5)}`;
    }
    return p;
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <Link href="/dashboard" className="hover:text-orange-500">Dashboard</Link>
        <span>/</span>
        <span className="text-gray-800 font-medium">My Account</span>
      </div>

      <h1 className="text-2xl font-bold mb-2">My Account</h1>
      <p className="text-sm text-gray-500 mb-8">Manage your personal info, security, and account preferences</p>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Sidebar */}
        <div className="lg:w-64 shrink-0">
          {/* Avatar card */}
          <div className="border rounded-2xl p-5 mb-4 text-center">
            {avatarSrc ? (
              <img src={avatarSrc} alt={profile.name} className="w-16 h-16 rounded-full object-cover mx-auto mb-3" />
            ) : (
              <div className="w-16 h-16 rounded-full bg-orange-500 text-white flex items-center justify-center text-xl font-bold mx-auto mb-3">
                {initials}
              </div>
            )}
            <p className="font-semibold text-sm">{profile.name || 'Traveller'}</p>
            <p className="text-xs text-gray-400 capitalize">{(profile.role || 'guest').toLowerCase()}</p>
          </div>

          {/* Nav */}
          <nav className="border rounded-2xl overflow-hidden">
            {SIDEBAR_ITEMS.map((item) => (
              <button
                key={item.key}
                onClick={() => setActiveSection(item.key)}
                className={`w-full flex items-center gap-3 px-4 py-3 text-sm text-left border-b last:border-b-0 transition ${
                  activeSection === item.key
                    ? 'bg-orange-50 text-orange-600 font-medium'
                    : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <span className="w-5 text-center text-base" dangerouslySetInnerHTML={{ __html: item.icon }} />
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Main content */}
        <div className="flex-1 min-w-0">
          {/* ── Personal details ──────────────────────────── */}
          {activeSection === 'personal' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Personal details</h2>
              <p className="text-sm text-gray-500 mb-6">Update your info and find out how it&apos;s used.</p>

              {/* Profile Photo */}
              <div className="border rounded-2xl p-5 mb-4">
                <div className="flex items-center gap-5">
                  <div className="relative group">
                    <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-gray-200">
                      {profile.avatarUrl ? (
                        <img
                          src={profile.avatarUrl.startsWith('http') ? profile.avatarUrl : `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${profile.avatarUrl}`}
                          alt={profile.name || 'Avatar'}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-2xl font-bold text-white"
                          style={{ backgroundColor: ['#f97316','#3b82f6','#10b981','#8b5cf6','#ef4444','#ec4899'][(profile.name || '').split('').reduce((a: number, c: string) => a + c.charCodeAt(0), 0) % 6] }}>
                          {(profile.name || '?').split(' ').map((w: string) => w[0]).join('').substring(0, 2).toUpperCase()}
                        </div>
                      )}
                    </div>
                    <label className="absolute inset-0 rounded-full cursor-pointer flex items-center justify-center bg-black/0 group-hover:bg-black/30 transition-colors">
                      <span className="text-white opacity-0 group-hover:opacity-100 text-sm font-medium transition-opacity">
                        {profile.avatarUrl ? 'Change' : 'Upload'}
                      </span>
                      <input
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={async (e) => {
                          const file = e.target.files?.[0];
                          if (!file) return;
                          if (file.size > 5 * 1024 * 1024) { alert('Image must be under 5MB'); return; }
                          try {
                            const t = localStorage.getItem('access_token') || '';
                            const result = await api.uploadAvatar(file, t);
                            setProfile((prev: any) => prev ? { ...prev, avatarUrl: result.avatarUrl } : prev);
                            localStorage.setItem('user_avatar', result.avatarUrl);
                          } catch { alert('Failed to upload photo'); }
                          if (e.target) e.target.value = '';
                        }}
                      />
                    </label>
                  </div>
                  <div>
                    <p className="font-semibold text-gray-900">{profile.name || 'Your Name'}</p>
                    <p className="text-sm text-gray-500 mt-0.5">
                      {profile.avatarUrl ? 'Hover over photo to change it' : 'Add a profile photo so hosts and guests can recognize you'}
                    </p>
                    {profile.avatarUrl && (
                      <button
                        onClick={async () => {
                          try {
                            const t = localStorage.getItem('access_token') || '';
                            await api.updateMyProfile({ avatarUrl: '' } as any, t);
                            setProfile((prev: any) => prev ? { ...prev, avatarUrl: null } : prev);
                            localStorage.removeItem('user_avatar');
                          } catch { alert('Failed to remove photo'); }
                        }}
                        className="text-xs text-red-500 hover:text-red-600 font-medium mt-1"
                      >
                        Remove photo
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <div className="border rounded-2xl divide-y">
                {/* Name */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Name</p>
                      {editingField === 'name' ? (
                        <div className="space-y-3 mt-2">
                          <div className="grid grid-cols-2 gap-3">
                            <div>
                              <label className="block text-xs text-gray-500 mb-1">First name</label>
                              <input type="text" value={editFirstName} onChange={(e) => setEditFirstName(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" />
                            </div>
                            <div>
                              <label className="block text-xs text-gray-500 mb-1">Last name</label>
                              <input type="text" value={editLastName} onChange={(e) => setEditLastName(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" />
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <button onClick={() => saveField('name')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">{profile.name || <span className="text-gray-400">Not set</span>}</p>
                      )}
                    </div>
                    {editingField !== 'name' && (
                      <button onClick={() => startEdit('name')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Display name */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Display name</p>
                      {editingField === 'displayName' ? (
                        <div className="space-y-3 mt-2">
                          <input type="text" value={editDisplayName} onChange={(e) => setEditDisplayName(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="Choose a display name" />
                          <div className="flex gap-2">
                            <button onClick={() => saveField('displayName')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">{profile.displayName || <span className="text-gray-400">Choose a display name</span>}</p>
                      )}
                    </div>
                    {editingField !== 'displayName' && (
                      <button onClick={() => startEdit('displayName')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Email */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Email address</p>
                      {editingField === 'email' ? (
                        <div className="space-y-3 mt-2">
                          <input type="email" value={editEmail} onChange={(e) => setEditEmail(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="you@example.com" />
                          <div className="flex gap-2">
                            <button onClick={() => saveField('email')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="text-sm text-gray-800">{profile.email || <span className="text-gray-400">Not provided</span>}</p>
                            {profile.email && (
                              <span className="text-xs bg-green-100 text-green-700 font-medium px-2 py-0.5 rounded-full">Verified</span>
                            )}
                          </div>
                          {profile.email && (
                            <p className="text-xs text-gray-400 mt-1">This is the email address you use to sign in. It&apos;s also where we send your booking confirmations.</p>
                          )}
                        </div>
                      )}
                    </div>
                    {editingField !== 'email' && (
                      <button onClick={() => startEdit('email')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                  {editingField !== 'email' && profile.email && (
                    <button className="text-xs text-orange-500 hover:underline mt-2">Change email with phone verification</button>
                  )}
                </div>

                {/* Phone */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Phone number</p>
                      {editingField === 'phone' ? (
                        <div className="space-y-3 mt-2">
                          <div className="flex gap-2">
                            <div className="flex items-center border rounded-lg px-3 py-2 text-sm bg-gray-50 text-gray-600 shrink-0">+91</div>
                            <input type="tel" value={editPhone} onChange={(e) => setEditPhone(e.target.value.replace(/\D/g, ''))} maxLength={10} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="9876543210" />
                          </div>
                          <div className="flex gap-2">
                            <button onClick={() => saveField('phone')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <p className="text-sm text-gray-800">{profile.phone ? formatPhone(profile.phone) : <span className="text-gray-400">Not provided</span>}</p>
                          {profile.phone && (
                            <p className="text-xs text-gray-400 mt-1">Properties or attractions you book will use this number if they need to contact you.</p>
                          )}
                        </div>
                      )}
                    </div>
                    {editingField !== 'phone' && (
                      <button onClick={() => startEdit('phone')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Date of birth */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Date of birth</p>
                      {editingField === 'dob' ? (
                        <div className="space-y-3 mt-2">
                          <input type="date" value={editDob} onChange={(e) => setEditDob(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" max={new Date().toISOString().split('T')[0]} />
                          <div className="flex gap-2">
                            <button onClick={() => saveField('dob')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">
                          {profile.dateOfBirth
                            ? new Date(profile.dateOfBirth).toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })
                            : <span className="text-gray-400">Enter your date of birth</span>}
                        </p>
                      )}
                    </div>
                    {editingField !== 'dob' && (
                      <button onClick={() => startEdit('dob')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Nationality */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Nationality</p>
                      {editingField === 'nationality' ? (
                        <div className="space-y-3 mt-2">
                          <select value={editNationality} onChange={(e) => setEditNationality(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                            <option value="India">India</option>
                            <option value="United States">United States</option>
                            <option value="United Kingdom">United Kingdom</option>
                            <option value="Canada">Canada</option>
                            <option value="Australia">Australia</option>
                            <option value="Germany">Germany</option>
                            <option value="France">France</option>
                            <option value="Japan">Japan</option>
                            <option value="Singapore">Singapore</option>
                            <option value="UAE">UAE</option>
                            <option value="Other">Other</option>
                          </select>
                          <div className="flex gap-2">
                            <button onClick={() => saveField('nationality')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">{profile.nationality || 'India'}</p>
                      )}
                    </div>
                    {editingField !== 'nationality' && (
                      <button onClick={() => startEdit('nationality')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Gender */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Gender</p>
                      {editingField === 'gender' ? (
                        <div className="space-y-3 mt-2">
                          <select value={editGender} onChange={(e) => setEditGender(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                            <option value="">Select your gender</option>
                            <option value="Male">Male</option>
                            <option value="Female">Female</option>
                            <option value="Non-binary">Non-binary</option>
                            <option value="Prefer not to say">Prefer not to say</option>
                          </select>
                          <div className="flex gap-2">
                            <button onClick={() => saveField('gender')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">{profile.gender || <span className="text-gray-400">Select your gender</span>}</p>
                      )}
                    </div>
                    {editingField !== 'gender' && (
                      <button onClick={() => startEdit('gender')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Address */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Address</p>
                      {editingField === 'address' ? (
                        <div className="space-y-3 mt-2">
                          <textarea value={editAddress} onChange={(e) => setEditAddress(e.target.value)} rows={3} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 resize-none" placeholder="Enter your full address" />
                          <div className="flex gap-2">
                            <button onClick={() => saveField('address')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">{profile.address || 'India'}</p>
                      )}
                    </div>
                    {editingField !== 'address' && (
                      <button onClick={() => startEdit('address')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">Edit</button>
                    )}
                  </div>
                </div>

                {/* Passport */}
                <div className="p-5">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-500 mb-1">Passport details</p>
                      {editingField === 'passport' ? (
                        <div className="space-y-3 mt-2">
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">Full name (as on passport)</label>
                            <input type="text" value={editPassportName} onChange={(e) => setEditPassportName(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" />
                          </div>
                          <div className="grid grid-cols-2 gap-3">
                            <div>
                              <label className="block text-xs text-gray-500 mb-1">Passport number</label>
                              <input type="text" value={editPassportNumber} onChange={(e) => setEditPassportNumber(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" />
                            </div>
                            <div>
                              <label className="block text-xs text-gray-500 mb-1">Expiry date</label>
                              <input type="date" value={editPassportExpiry} onChange={(e) => setEditPassportExpiry(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" min={new Date().toISOString().split('T')[0]} />
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <button onClick={() => saveField('passport')} disabled={saving} className="bg-orange-500 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button>
                            <button onClick={() => setEditingField(null)} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-700">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-800">
                          {profile.passportNumber
                            ? <>{profile.passportName} &middot; {profile.passportNumber}</>
                            : <span className="text-gray-400">Not provided</span>}
                        </p>
                      )}
                    </div>
                    {editingField !== 'passport' && (
                      <button onClick={() => startEdit('passport')} className="text-sm text-orange-500 font-medium hover:underline shrink-0">
                        {profile.passportNumber ? 'Edit' : 'Add passport'}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ── Security settings ─────────────────────────── */}
          {activeSection === 'security' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Security settings</h2>
              <p className="text-sm text-gray-500 mb-6">Manage your account security and sign-in preferences.</p>

              <div className="border rounded-2xl divide-y">
                <div className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Password</p>
                    <p className="text-xs text-gray-400 mt-0.5">Your account uses OTP-based authentication. No password needed.</p>
                  </div>
                  <span className="text-xs bg-green-100 text-green-700 font-medium px-2.5 py-1 rounded-full">Secure</span>
                </div>
                <div className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Two-factor authentication</p>
                    <p className="text-xs text-gray-400 mt-0.5">OTP sent to your phone each time you sign in.</p>
                  </div>
                  <span className="text-xs bg-green-100 text-green-700 font-medium px-2.5 py-1 rounded-full">Enabled</span>
                </div>
                <div className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Active sessions</p>
                    <p className="text-xs text-gray-400 mt-0.5">You&apos;re currently signed in on this device.</p>
                  </div>
                  <button
                    className="text-sm text-red-500 font-medium hover:underline"
                    onClick={async () => {
                      if (!confirm('Sign out from all devices? You will need to sign in again.')) return;
                      try {
                        await api.logoutAll();
                        localStorage.removeItem('access_token');
                        localStorage.removeItem('refresh_token');
                        localStorage.removeItem('user_id');
                        localStorage.removeItem('user_role');
                        localStorage.removeItem('user_name');
                        localStorage.removeItem('user_avatar');
                        localStorage.removeItem('last_login_phone');
                        localStorage.removeItem('last_login_email');
                        document.cookie = 'access_token=; path=/; max-age=0';
                        router.push('/auth');
                      } catch (e: any) {
                        alert(e.message || 'Failed to sign out all devices');
                      }
                    }}
                  >Sign out all devices</button>
                </div>
                <div className="p-5">
                  <p className="text-sm font-medium text-gray-800 mb-1">Delete account</p>
                  <p className="text-xs text-gray-400 mb-3">Permanently delete your account and all associated data. This action cannot be undone.</p>
                  <button className="text-sm text-red-500 border border-red-200 px-4 py-2 rounded-lg hover:bg-red-50 transition font-medium">Delete my account</button>
                </div>
              </div>
            </div>
          )}

          {/* ── Other travelers ───────────────────────────── */}
          {activeSection === 'travelers' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Other travelers</h2>
              <p className="text-sm text-gray-500 mb-6">Add or edit info about the people you&apos;re traveling with.</p>

              {/* Existing travelers list */}
              {coTravelers.length > 0 && (
                <div className="space-y-3 mb-6">
                  {coTravelers.map((t) => (
                    <div key={t.id} className="border rounded-2xl p-5 flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-gray-800">{t.firstName} {t.lastName}</p>
                        <div className="flex gap-3 mt-1 text-xs text-gray-400">
                          {t.dateOfBirth && (
                            <span>Born: {new Date(t.dateOfBirth).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })}</span>
                          )}
                          {t.gender && <span>{t.gender}</span>}
                        </div>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <button
                          onClick={() => {
                            setEditingTravelerId(t.id);
                            setTravFirstName(t.firstName);
                            setTravLastName(t.lastName);
                            setTravDob(t.dateOfBirth || '');
                            setTravGender(t.gender || '');
                            setTravConsent(true);
                            setShowTravelerForm(true);
                          }}
                          className="text-sm text-orange-500 font-medium hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          onClick={async () => {
                            if (!confirm(`Remove ${t.firstName} ${t.lastName}?`)) return;
                            try {
                              await api.deleteCoTraveler(t.id, token);
                              setCoTravelers((prev) => prev.filter((x) => x.id !== t.id));
                            } catch {}
                          }}
                          className="text-sm text-red-400 font-medium hover:underline"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Add/Edit form */}
              {showTravelerForm ? (
                <div className="border rounded-2xl p-5 space-y-4">
                  <h3 className="font-semibold text-sm">{editingTravelerId ? 'Edit traveler' : 'Add a traveler'}</h3>

                  {/* Name */}
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Name</p>
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">First name(s) <span className="text-red-500">*</span></label>
                        <input
                          type="text"
                          value={travFirstName}
                          onChange={(e) => setTravFirstName(e.target.value)}
                          className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Last name(s) <span className="text-red-500">*</span></label>
                        <input
                          type="text"
                          value={travLastName}
                          onChange={(e) => setTravLastName(e.target.value)}
                          className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                        />
                      </div>
                    </div>
                    <p className="text-xs text-gray-400 mt-1.5">Enter this person&apos;s name exactly as it&apos;s written on their passport or other official travel document</p>
                  </div>

                  {/* Date of birth */}
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Date of birth</p>
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Date of birth <span className="text-red-500">*</span></label>
                      <input
                        type="date"
                        value={travDob}
                        onChange={(e) => setTravDob(e.target.value)}
                        className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                        max={new Date().toISOString().split('T')[0]}
                      />
                    </div>
                    <p className="text-xs text-gray-400 mt-1.5">It&apos;s important to enter a correct date of birth because these details can be used for booking or ticketing purposes</p>
                  </div>

                  {/* Gender */}
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Gender</p>
                    <select
                      value={travGender}
                      onChange={(e) => setTravGender(e.target.value)}
                      className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                    >
                      <option value="">Select gender</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Non-binary">Non-binary</option>
                      <option value="Prefer not to say">Prefer not to say</option>
                    </select>
                    <p className="text-xs text-gray-400 mt-1.5">Select the gender listed on this person&apos;s passport or other official travel document</p>
                  </div>

                  {/* Consent checkbox */}
                  <label className="flex items-start gap-2.5 cursor-pointer border-t pt-4">
                    <input
                      type="checkbox"
                      checked={travConsent}
                      onChange={(e) => setTravConsent(e.target.checked)}
                      className="mt-0.5 accent-orange-500"
                    />
                    <span className="text-xs text-gray-600 leading-relaxed">
                      I confirm that I&apos;m authorized to provide the personal data of any co-traveler (including children) to Safar for this service. In addition, I confirm that I&apos;ve informed the other travelers that I&apos;m providing their personal data to Safar.
                    </span>
                  </label>

                  {/* Actions */}
                  <div className="flex gap-3 pt-2">
                    <button
                      onClick={async () => {
                        if (!travFirstName.trim() || !travLastName.trim() || !travConsent) return;
                        setTravSaving(true);
                        try {
                          const data = {
                            firstName: travFirstName.trim(),
                            lastName: travLastName.trim(),
                            dateOfBirth: travDob || undefined,
                            gender: travGender || undefined,
                          };
                          if (editingTravelerId) {
                            const updated = await api.updateCoTraveler(editingTravelerId, data, token);
                            setCoTravelers((prev) => prev.map((x) => x.id === editingTravelerId ? updated : x));
                          } else {
                            const created = await api.createCoTraveler(data, token);
                            setCoTravelers((prev) => [created, ...prev]);
                          }
                          setShowTravelerForm(false);
                          setEditingTravelerId(null);
                          setTravFirstName(''); setTravLastName(''); setTravDob(''); setTravGender(''); setTravConsent(false);
                        } catch (e: any) {
                          alert(e.message || 'Failed to save traveler');
                        } finally {
                          setTravSaving(false);
                        }
                      }}
                      disabled={travSaving || !travFirstName.trim() || !travLastName.trim() || !travConsent}
                      className="bg-orange-500 text-white text-sm font-medium px-6 py-2.5 rounded-xl hover:bg-orange-600 transition disabled:opacity-50"
                    >
                      {travSaving ? 'Saving...' : 'Save'}
                    </button>
                    <button
                      onClick={() => {
                        setShowTravelerForm(false);
                        setEditingTravelerId(null);
                        setTravFirstName(''); setTravLastName(''); setTravDob(''); setTravGender(''); setTravConsent(false);
                      }}
                      className="text-sm text-gray-500 px-4 py-2.5 hover:text-gray-700"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => {
                    setEditingTravelerId(null);
                    setTravFirstName(''); setTravLastName(''); setTravDob(''); setTravGender(''); setTravConsent(false);
                    setShowTravelerForm(true);
                  }}
                  className="w-full border-2 border-dashed border-gray-300 rounded-2xl p-6 text-center hover:border-orange-400 hover:bg-orange-50/50 transition cursor-pointer"
                >
                  <p className="text-2xl mb-1">+</p>
                  <p className="text-sm font-medium text-gray-600">Add a traveler</p>
                </button>
              )}
            </div>
          )}

          {/* ── Display settings ──────────────────────────── */}
          {activeSection === 'display' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Display settings</h2>
              <p className="text-sm text-gray-500 mb-6">Customize how you see and interact with Safar.</p>

              <div className="border rounded-2xl divide-y">
                <div className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Language</p>
                    <p className="text-xs text-gray-400 mt-0.5">Choose your preferred language for the interface.</p>
                  </div>
                  <select className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-500">
                    <option value="en">English</option>
                    <option value="hi">Hindi</option>
                  </select>
                </div>
                <div className="p-5 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">Currency</p>
                    <p className="text-xs text-gray-400 mt-0.5">Display prices in your preferred currency.</p>
                  </div>
                  <select className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-500">
                    <option value="INR">INR (&#x20B9;)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (&euro;)</option>
                    <option value="GBP">GBP (&pound;)</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* ── Payment methods ───────────────────────────── */}
          {activeSection === 'payments' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Payment methods</h2>
              <p className="text-sm text-gray-500 mb-6">Securely save your payment details for faster checkout.</p>

              {/* Saved methods list */}
              {paymentMethods.length > 0 && (
                <div className="space-y-3 mb-6">
                  {paymentMethods.map((pm) => (
                    <div key={pm.id} className={`border rounded-2xl p-4 flex items-center justify-between ${pm.isDefault ? 'border-orange-300 bg-orange-50/30' : ''}`}>
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center text-lg shrink-0">
                          {pm.type === 'UPI' && <span>&#x1F4F1;</span>}
                          {pm.type === 'CREDIT_CARD' && <span>&#x1F4B3;</span>}
                          {pm.type === 'DEBIT_CARD' && <span>&#x1F4B3;</span>}
                          {pm.type === 'NET_BANKING' && <span>&#x1F3E6;</span>}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-semibold text-gray-800">{pm.label}</p>
                            {pm.isDefault && (
                              <span className="text-xs bg-orange-100 text-orange-600 font-medium px-2 py-0.5 rounded-full">Default</span>
                            )}
                          </div>
                          <p className="text-xs text-gray-400 capitalize">{pm.type.replace('_', ' ').toLowerCase()}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        {!pm.isDefault && (
                          <button
                            onClick={async () => {
                              try {
                                await api.setDefaultPaymentMethod(pm.id, token);
                                setPaymentMethods((prev) => prev.map((x) => ({ ...x, isDefault: x.id === pm.id })));
                              } catch {}
                            }}
                            className="text-xs text-orange-500 hover:underline"
                          >
                            Set default
                          </button>
                        )}
                        <button
                          onClick={() => {
                            setEditingPaymentId(pm.id);
                            setPmType(pm.type);
                            setPmUpiId(pm.upiId || '');
                            setPmCardNumber('');
                            setPmCardNetwork(pm.cardNetwork || '');
                            setPmCardHolder(pm.cardHolder || '');
                            setPmCardExpMonth(pm.cardExpiry ? pm.cardExpiry.split('/')[0] : '');
                            setPmCardExpYear(pm.cardExpiry ? pm.cardExpiry.split('/')[1] : '');
                            setPmBankName(pm.bankName || '');
                            setPmBankAccount('');
                            setPmLabel(pm.label || '');
                            setPmDefault(!!pm.isDefault);
                            setShowPaymentForm(true);
                          }}
                          className="text-sm text-orange-500 font-medium hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          onClick={async () => {
                            if (!confirm('Remove this payment method?')) return;
                            try {
                              await api.deletePaymentMethod(pm.id, token);
                              setPaymentMethods((prev) => prev.filter((x) => x.id !== pm.id));
                            } catch {}
                          }}
                          className="text-sm text-red-400 font-medium hover:underline"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Add/Edit form */}
              {showPaymentForm ? (
                <div className="border rounded-2xl p-5 space-y-5">
                  <h3 className="font-semibold text-sm">{editingPaymentId ? 'Edit payment method' : 'Add a payment method'}</h3>

                  {/* Type selector */}
                  <div>
                    <p className="text-sm font-medium text-gray-700 mb-2">Payment type</p>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                      {([
                        { key: 'UPI' as const, label: 'UPI', icon: '&#x1F4F1;' },
                        { key: 'CREDIT_CARD' as const, label: 'Credit Card', icon: '&#x1F4B3;' },
                        { key: 'DEBIT_CARD' as const, label: 'Debit Card', icon: '&#x1F4B3;' },
                        { key: 'NET_BANKING' as const, label: 'Net Banking', icon: '&#x1F3E6;' },
                      ]).map((opt) => (
                        <button
                          key={opt.key}
                          onClick={() => setPmType(opt.key)}
                          className={`border rounded-xl p-3 text-center transition ${
                            pmType === opt.key
                              ? 'border-orange-500 bg-orange-50 text-orange-600'
                              : 'border-gray-200 text-gray-600 hover:border-gray-300'
                          }`}
                        >
                          <span className="text-xl block mb-1" dangerouslySetInnerHTML={{ __html: opt.icon }} />
                          <span className="text-xs font-medium">{opt.label}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* UPI fields */}
                  {pmType === 'UPI' && (
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">UPI ID <span className="text-red-500">*</span></label>
                      <input
                        type="text"
                        value={pmUpiId}
                        onChange={(e) => setPmUpiId(e.target.value)}
                        className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                        placeholder="name@upi or name@paytm"
                      />
                      <p className="text-xs text-gray-400 mt-1">Enter your UPI ID linked to Google Pay, PhonePe, Paytm, or any UPI app</p>
                    </div>
                  )}

                  {/* Credit/Debit Card fields */}
                  {(pmType === 'CREDIT_CARD' || pmType === 'DEBIT_CARD') && (
                    <div className="space-y-4">
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Card number <span className="text-red-500">*</span></label>
                        <input
                          type="text"
                          value={pmCardNumber}
                          onChange={(e) => setPmCardNumber(e.target.value.replace(/\D/g, '').slice(0, 16))}
                          className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 font-mono tracking-wider"
                          placeholder="1234 5678 9012 3456"
                        />
                        <p className="text-xs text-gray-400 mt-1">Only the last 4 digits will be stored for your security</p>
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Cardholder name <span className="text-red-500">*</span></label>
                        <input
                          type="text"
                          value={pmCardHolder}
                          onChange={(e) => setPmCardHolder(e.target.value)}
                          className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                          placeholder="Name as printed on card"
                        />
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Expiry date <span className="text-red-500">*</span></label>
                          <div className="flex gap-2">
                            <select value={pmCardExpMonth} onChange={(e) => setPmCardExpMonth(e.target.value)} className="flex-1 border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                              <option value="">MM</option>
                              {Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0')).map((m) => (
                                <option key={m} value={m}>{m}</option>
                              ))}
                            </select>
                            <select value={pmCardExpYear} onChange={(e) => setPmCardExpYear(e.target.value)} className="flex-1 border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                              <option value="">YYYY</option>
                              {Array.from({ length: 10 }, (_, i) => String(new Date().getFullYear() + i)).map((y) => (
                                <option key={y} value={y}>{y}</option>
                              ))}
                            </select>
                          </div>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500 mb-1">Card network</label>
                          <select value={pmCardNetwork} onChange={(e) => setPmCardNetwork(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                            <option value="">Select</option>
                            <option value="Visa">Visa</option>
                            <option value="Mastercard">Mastercard</option>
                            <option value="RuPay">RuPay</option>
                            <option value="Amex">American Express</option>
                            <option value="Diners">Diners Club</option>
                          </select>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Net Banking fields */}
                  {pmType === 'NET_BANKING' && (
                    <div className="space-y-4">
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Bank name <span className="text-red-500">*</span></label>
                        <select value={pmBankName} onChange={(e) => setPmBankName(e.target.value)} className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500">
                          <option value="">Select your bank</option>
                          <option value="State Bank of India">State Bank of India</option>
                          <option value="HDFC Bank">HDFC Bank</option>
                          <option value="ICICI Bank">ICICI Bank</option>
                          <option value="Axis Bank">Axis Bank</option>
                          <option value="Kotak Mahindra Bank">Kotak Mahindra Bank</option>
                          <option value="Bank of Baroda">Bank of Baroda</option>
                          <option value="Punjab National Bank">Punjab National Bank</option>
                          <option value="Canara Bank">Canara Bank</option>
                          <option value="Union Bank of India">Union Bank of India</option>
                          <option value="IndusInd Bank">IndusInd Bank</option>
                          <option value="Yes Bank">Yes Bank</option>
                          <option value="IDBI Bank">IDBI Bank</option>
                          <option value="Federal Bank">Federal Bank</option>
                          <option value="Indian Bank">Indian Bank</option>
                          <option value="Other">Other</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 mb-1">Account number (last 4 digits)</label>
                        <input
                          type="text"
                          value={pmBankAccount}
                          onChange={(e) => setPmBankAccount(e.target.value.replace(/\D/g, '').slice(0, 4))}
                          maxLength={4}
                          className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 font-mono"
                          placeholder="Last 4 digits"
                        />
                        <p className="text-xs text-gray-400 mt-1">Optional — helps you identify the account later</p>
                      </div>
                    </div>
                  )}

                  {/* Label (optional) */}
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">Label (optional)</label>
                    <input
                      type="text"
                      value={pmLabel}
                      onChange={(e) => setPmLabel(e.target.value)}
                      className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500"
                      placeholder="e.g. Personal UPI, Work card"
                    />
                  </div>

                  {/* Set as default */}
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={pmDefault} onChange={(e) => setPmDefault(e.target.checked)} className="accent-orange-500" />
                    <span className="text-sm text-gray-700">Set as default payment method</span>
                  </label>

                  {/* Security note */}
                  <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex items-start gap-2">
                    <span className="text-green-600 text-sm mt-0.5">&#x1F512;</span>
                    <p className="text-xs text-green-700">Your payment information is encrypted and stored securely. We never store your full card number or CVV.</p>
                  </div>

                  {/* Actions */}
                  <div className="flex gap-3">
                    <button
                      onClick={async () => {
                        setPmSaving(true);
                        try {
                          const data: any = {
                            type: pmType,
                            label: pmLabel || undefined,
                            isDefault: pmDefault,
                          };

                          if (pmType === 'UPI') {
                            if (!pmUpiId.trim()) { alert('Please enter a UPI ID'); setPmSaving(false); return; }
                            data.upiId = pmUpiId.trim();
                          } else if (pmType === 'CREDIT_CARD' || pmType === 'DEBIT_CARD') {
                            if (!pmCardHolder.trim()) { alert('Please enter cardholder name'); setPmSaving(false); return; }
                            const num = pmCardNumber || '';
                            data.cardLast4 = num.length >= 4 ? num.slice(-4) : (editingPaymentId ? undefined : num);
                            data.cardNetwork = pmCardNetwork || undefined;
                            data.cardHolder = pmCardHolder.trim();
                            data.cardExpiry = pmCardExpMonth && pmCardExpYear ? `${pmCardExpMonth}/${pmCardExpYear}` : undefined;
                          } else if (pmType === 'NET_BANKING') {
                            if (!pmBankName) { alert('Please select a bank'); setPmSaving(false); return; }
                            data.bankName = pmBankName;
                            data.bankAccountLast4 = pmBankAccount || undefined;
                          }

                          if (editingPaymentId) {
                            const updated = await api.updatePaymentMethod(editingPaymentId, data, token);
                            setPaymentMethods((prev) => prev.map((x) => x.id === editingPaymentId ? updated : (pmDefault ? { ...x, isDefault: false } : x)));
                          } else {
                            const created = await api.createPaymentMethod(data, token);
                            if (pmDefault) {
                              setPaymentMethods((prev) => [created, ...prev.map((x) => ({ ...x, isDefault: false }))]);
                            } else {
                              setPaymentMethods((prev) => [created, ...prev]);
                            }
                          }
                          setShowPaymentForm(false);
                          setEditingPaymentId(null);
                        } catch (e: any) {
                          alert(e.message || 'Failed to save payment method');
                        } finally {
                          setPmSaving(false);
                        }
                      }}
                      disabled={pmSaving}
                      className="bg-orange-500 text-white text-sm font-medium px-6 py-2.5 rounded-xl hover:bg-orange-600 transition disabled:opacity-50"
                    >
                      {pmSaving ? 'Saving...' : 'Save'}
                    </button>
                    <button
                      onClick={() => { setShowPaymentForm(false); setEditingPaymentId(null); }}
                      className="text-sm text-gray-500 px-4 py-2.5 hover:text-gray-700"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => {
                    setEditingPaymentId(null);
                    setPmType('UPI'); setPmUpiId(''); setPmCardNumber(''); setPmCardNetwork(''); setPmCardHolder('');
                    setPmCardExpMonth(''); setPmCardExpYear(''); setPmBankName(''); setPmBankAccount('');
                    setPmLabel(''); setPmDefault(false);
                    setShowPaymentForm(true);
                  }}
                  className="w-full border-2 border-dashed border-gray-300 rounded-2xl p-6 text-center hover:border-orange-400 hover:bg-orange-50/50 transition cursor-pointer"
                >
                  <p className="text-2xl mb-1">+</p>
                  <p className="text-sm font-medium text-gray-600">Add a payment method</p>
                  <p className="text-xs text-gray-400 mt-1">UPI, Credit Card, Debit Card, Net Banking</p>
                </button>
              )}
            </div>
          )}

          {/* ── Privacy and data management ────────────────── */}
          {activeSection === 'privacy' && (
            <div>
              <h2 className="text-xl font-bold mb-1">Privacy and data management</h2>
              <p className="text-sm text-gray-500 mb-6">Control how your data is used and shared.</p>

              <div className="border rounded-2xl divide-y">
                <div className="p-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-800">Marketing emails</p>
                      <p className="text-xs text-gray-400 mt-0.5">Receive offers, travel inspiration, and updates from Safar.</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" defaultChecked className="sr-only peer" />
                      <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-orange-500"></div>
                    </label>
                  </div>
                </div>
                <div className="p-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-800">SMS notifications</p>
                      <p className="text-xs text-gray-400 mt-0.5">Receive booking updates and reminders via SMS.</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" defaultChecked className="sr-only peer" />
                      <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-orange-500"></div>
                    </label>
                  </div>
                </div>
                <div className="p-5">
                  <p className="text-sm font-medium text-gray-800 mb-1">Download your data</p>
                  <p className="text-xs text-gray-400 mb-3">Request a copy of all the data Safar holds about you.</p>
                  <button className="text-sm text-orange-500 border border-orange-200 px-4 py-2 rounded-lg hover:bg-orange-50 transition font-medium">Request data download</button>
                </div>
                <div className="p-5">
                  <p className="text-sm font-medium text-gray-800 mb-1">Cookie preferences</p>
                  <p className="text-xs text-gray-400">We use essential cookies to make our site work. We&apos;d also like to set analytics cookies to help us improve it.</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
