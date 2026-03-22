'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';
const SHARING_TYPES = ['PRIVATE', 'TWO_SHARING', 'THREE_SHARING', 'FOUR_SHARING', 'DORMITORY'];
const GENDER_OPTIONS = ['MALE_ONLY', 'FEMALE_ONLY', 'COED'];
const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Hyderabad', 'Pune', 'Kolkata', 'Goa', 'Jaipur'];

export default function LookingForPage() {
  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') ?? '' : '';
  const [tab, setTab] = useState<'create' | 'browse'>('browse');
  const [profiles, setProfiles] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterCity, setFilterCity] = useState('');

  // Form state
  const [form, setForm] = useState({
    preferredCity: 'Bangalore',
    preferredLocality: '',
    budgetMinPaise: 500000,
    budgetMaxPaise: 1500000,
    preferredSharing: 'TWO_SHARING',
    genderPreference: 'COED',
    preferredAmenities: 'wifi,ac',
    moveInDate: '',
    vegetarianOnly: false,
    petOwner: false,
    occupation: 'working_professional',
    companyOrCollege: '',
  });

  useEffect(() => { loadProfiles(); }, [filterCity]);

  async function loadProfiles() {
    setLoading(true);
    try {
      const params = new URLSearchParams({ status: 'ACTIVE', size: '20' });
      if (filterCity) params.set('city', filterCity);
      const data = await api.getSeekerProfiles(params.toString());
      setProfiles(data.content || []);
    } catch (e) { console.error(e); }
    setLoading(false);
  }

  async function createProfile() {
    if (!token) { alert('Please sign in first'); return; }
    try {
      await api.createSeekerProfile({
        ...form,
        name: 'Seeker',
        seekerType: 'PG_SEEKER',
      }, token);
      alert('Profile created! Hosts can now find you.');
      setTab('browse');
      loadProfiles();
    } catch (e: any) {
      alert('Failed: ' + (e.message || ''));
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-2">Looking For a PG / Room</h1>
        <p className="text-gray-500 mb-6">Create your profile or browse what others are looking for</p>

        {/* Tabs */}
        <div className="flex gap-4 mb-6">
          <button onClick={() => setTab('browse')}
            className={`px-4 py-2 rounded-lg font-medium ${tab === 'browse' ? 'bg-orange-500 text-white' : 'bg-white border'}`}>
            Browse Seekers
          </button>
          <button onClick={() => setTab('create')}
            className={`px-4 py-2 rounded-lg font-medium ${tab === 'create' ? 'bg-orange-500 text-white' : 'bg-white border'}`}>
            Create Profile
          </button>
        </div>

        {tab === 'create' && (
          <div className="bg-white rounded-xl border p-6 space-y-4">
            <h2 className="text-xl font-semibold">Your Preferences</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-gray-600 block mb-1">Preferred City</label>
                <select value={form.preferredCity} onChange={e => setForm({ ...form, preferredCity: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2">
                  {CITIES.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Locality</label>
                <input value={form.preferredLocality} onChange={e => setForm({ ...form, preferredLocality: e.target.value })}
                  placeholder="e.g. Koramangala, HSR Layout" className="w-full border rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Budget Range (₹/month)</label>
                <div className="flex gap-2">
                  <input type="number" value={form.budgetMinPaise / 100} onChange={e => setForm({ ...form, budgetMinPaise: Number(e.target.value) * 100 })}
                    className="w-1/2 border rounded-lg px-3 py-2" placeholder="Min" />
                  <input type="number" value={form.budgetMaxPaise / 100} onChange={e => setForm({ ...form, budgetMaxPaise: Number(e.target.value) * 100 })}
                    className="w-1/2 border rounded-lg px-3 py-2" placeholder="Max" />
                </div>
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Sharing Preference</label>
                <select value={form.preferredSharing} onChange={e => setForm({ ...form, preferredSharing: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2">
                  {SHARING_TYPES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                </select>
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Gender Preference</label>
                <select value={form.genderPreference} onChange={e => setForm({ ...form, genderPreference: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2">
                  {GENDER_OPTIONS.map(g => <option key={g} value={g}>{g.replace('_', ' ')}</option>)}
                </select>
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Move-in Date</label>
                <input type="date" value={form.moveInDate} onChange={e => setForm({ ...form, moveInDate: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Occupation</label>
                <select value={form.occupation} onChange={e => setForm({ ...form, occupation: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2">
                  <option value="student">Student</option>
                  <option value="working_professional">Working Professional</option>
                  <option value="freelancer">Freelancer</option>
                </select>
              </div>
              <div>
                <label className="text-sm text-gray-600 block mb-1">Company / College</label>
                <input value={form.companyOrCollege} onChange={e => setForm({ ...form, companyOrCollege: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2" placeholder="e.g. TCS, IIT Delhi" />
              </div>
            </div>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={form.vegetarianOnly} onChange={e => setForm({ ...form, vegetarianOnly: e.target.checked })} />
                Vegetarian only
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={form.petOwner} onChange={e => setForm({ ...form, petOwner: e.target.checked })} />
                Pet owner
              </label>
            </div>
            <button onClick={createProfile} className="bg-orange-500 text-white px-6 py-3 rounded-xl font-medium hover:bg-orange-600 w-full">
              Publish Profile
            </button>
          </div>
        )}

        {tab === 'browse' && (
          <>
            <div className="flex gap-3 mb-4">
              <select value={filterCity} onChange={e => setFilterCity(e.target.value)}
                className="border rounded-lg px-3 py-2 text-sm">
                <option value="">All Cities</option>
                {CITIES.map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
            {loading ? <div className="text-center text-gray-400 py-12">Loading...</div> : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {profiles.map((p: any) => (
                  <div key={p.id} className="bg-white rounded-xl border p-4 hover:shadow-md transition-shadow">
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <h3 className="font-semibold">{p.name}</h3>
                        <p className="text-sm text-gray-500">{p.occupation} · {p.companyOrCollege || 'Not specified'}</p>
                      </div>
                      <span className="text-xs bg-orange-100 text-orange-800 px-2 py-1 rounded-full">{p.seekerType?.replace('_', ' ')}</span>
                    </div>
                    <div className="text-sm text-gray-600 space-y-1">
                      <p>📍 {p.preferredCity}{p.preferredLocality ? `, ${p.preferredLocality}` : ''}</p>
                      <p>💰 ₹{(p.budgetMinPaise / 100).toLocaleString()} – ₹{(p.budgetMaxPaise / 100).toLocaleString()}/month</p>
                      <p>🏠 {p.preferredSharing?.replace('_', ' ')} · {p.genderPreference?.replace('_', ' ')}</p>
                      {p.moveInDate && <p>📅 Move-in: {p.moveInDate}</p>}
                      {p.vegetarianOnly && <span className="inline-block text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded mr-1">🌱 Veg</span>}
                      {p.petOwner && <span className="inline-block text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">🐾 Pet</span>}
                    </div>
                  </div>
                ))}
                {profiles.length === 0 && <div className="col-span-2 text-center text-gray-400 py-12">No seekers found</div>}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
