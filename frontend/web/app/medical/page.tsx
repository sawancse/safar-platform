'use client';

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Hospital, MedicalStayPackage } from '@/types';

const SPECIALTIES = [
  { key: '', label: 'All', icon: '🏥' },
  { key: 'Cardiology', label: 'Cardiology', icon: '❤️' },
  { key: 'Orthopedics', label: 'Orthopedics', icon: '🦴' },
  { key: 'Oncology', label: 'Oncology', icon: '🎗️' },
  { key: 'Neurology', label: 'Neurology', icon: '🧠' },
  { key: 'Dental', label: 'Dental', icon: '🦷' },
  { key: 'Ophthalmology', label: 'Eye Care', icon: '👁️' },
  { key: 'Fertility', label: 'Fertility', icon: '👶' },
  { key: 'Cosmetic', label: 'Cosmetic', icon: '✨' },
  { key: 'Ayurveda', label: 'Ayurveda', icon: '🌿' },
];

const TOP_PROCEDURES: Record<string, string[]> = {
  Cardiology: ['Bypass Surgery', 'Angioplasty', 'Valve Replacement', 'Pacemaker'],
  Orthopedics: ['Knee Replacement', 'Hip Replacement', 'Spine Surgery', 'ACL Repair'],
  Oncology: ['Chemotherapy', 'Radiation Therapy', 'Tumor Removal'],
  Dental: ['Dental Implants', 'Root Canal', 'Veneers', 'Teeth Whitening'],
  Ophthalmology: ['LASIK', 'Cataract Surgery', 'Glaucoma Treatment'],
  Fertility: ['IVF', 'IUI', 'Egg Freezing'],
  Cosmetic: ['Hair Transplant', 'Rhinoplasty', 'Liposuction', 'Facelift'],
  Ayurveda: ['Panchakarma', 'Detox Retreat', 'Yoga Therapy'],
};

const CITIES = ['Chennai', 'Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Kochi', 'Kolkata', 'Pune'];

export default function MedicalPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [hospitals, setHospitals] = useState<Hospital[]>([]);
  const [packages, setPackages] = useState<MedicalStayPackage[]>([]);
  const [loadingHospitals, setLoadingHospitals] = useState(true);
  const [loadingPackages, setLoadingPackages] = useState(true);
  const [selectedHospital, setSelectedHospital] = useState<Hospital | null>(null);

  const city = searchParams.get('city') ?? '';
  const specialty = searchParams.get('specialty') ?? '';
  const procedure = searchParams.get('procedure') ?? '';

  useEffect(() => {
    setLoadingHospitals(true);
    api.getHospitals()
      .then((data) => setHospitals((data ?? []).map((h: any) => ({
        ...h,
        specialties: typeof h.specialties === 'string' ? h.specialties.split(',').map((s: string) => s.trim()) : (h.specialties || []),
        accreditations: typeof h.accreditations === 'string' ? h.accreditations.split(',').map((s: string) => s.trim()) : (h.accreditations || []),
      }))))
      .catch(() => setHospitals([]))
      .finally(() => setLoadingHospitals(false));
  }, []);

  const fetchPackages = useCallback(async () => {
    setLoadingPackages(true);
    try {
      const params: { city?: string; specialty?: string } = {};
      if (city) params.city = city;
      if (specialty) params.specialty = specialty;
      const result = await api.getMedicalStaySearch(params);
      setPackages(result ?? []);
    } catch { setPackages([]); }
    finally { setLoadingPackages(false); }
  }, [city, specialty]);

  useEffect(() => { fetchPackages(); }, [fetchPackages]);

  function updateParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    router.push(`/medical?${params.toString()}`);
  }

  const filteredHospitals = hospitals.filter(h => {
    if (city && h.city.toLowerCase() !== city.toLowerCase()) return false;
    if (specialty && !h.specialties.some(s => s.toLowerCase().includes(specialty.toLowerCase()))) return false;
    return true;
  });

  const procedures = specialty ? (TOP_PROCEDURES[specialty] ?? []) : [];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Hero */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-2xl p-8 mb-8">
        <h1 className="text-3xl font-bold mb-2">Medical Tourism</h1>
        <p className="text-blue-100 text-lg mb-6">
          World-class treatment at 60-90% lower cost. Find hospital + stay packages across India.
        </p>

        {/* Search bar */}
        <div className="bg-white rounded-xl p-4 flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <label className="text-xs font-semibold text-gray-500 block mb-1">CITY</label>
            <select className="w-full text-sm text-gray-900 border-0 outline-none bg-transparent font-medium"
              value={city} onChange={e => updateParam('city', e.target.value)}>
              <option value="">All cities</option>
              {CITIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="hidden sm:block w-px bg-gray-200" />
          <div className="flex-1">
            <label className="text-xs font-semibold text-gray-500 block mb-1">SPECIALTY</label>
            <select className="w-full text-sm text-gray-900 border-0 outline-none bg-transparent font-medium"
              value={specialty} onChange={e => updateParam('specialty', e.target.value)}>
              {SPECIALTIES.map(s => <option key={s.key} value={s.key}>{s.icon} {s.label}</option>)}
            </select>
          </div>
          <div className="hidden sm:block w-px bg-gray-200" />
          <div className="flex-1">
            <label className="text-xs font-semibold text-gray-500 block mb-1">TRAVELERS</label>
            <p className="text-sm text-gray-900 font-medium">Patient + 1 caregiver</p>
          </div>
        </div>
      </div>

      {/* Specialty chips */}
      <div className="flex gap-2 overflow-x-auto pb-4 mb-6 scrollbar-hide">
        {SPECIALTIES.map(s => (
          <button key={s.key} onClick={() => updateParam('specialty', specialty === s.key ? '' : s.key)}
            className={`flex items-center gap-1.5 border rounded-full px-4 py-2 text-sm font-medium whitespace-nowrap transition ${
              specialty === s.key ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'
            }`}>
            <span>{s.icon}</span> {s.label}
          </button>
        ))}
      </div>

      {/* Procedures for selected specialty */}
      {procedures.length > 0 && (
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-3">Common {specialty} Procedures</h2>
          <div className="flex gap-2 flex-wrap">
            {procedures.map(p => (
              <button key={p} onClick={() => updateParam('procedure', procedure === p ? '' : p)}
                className={`border rounded-lg px-4 py-2 text-sm font-medium transition ${
                  procedure === p ? 'border-purple-500 bg-purple-50 text-purple-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'
                }`}>
                {p}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Hospitals */}
        <div className="lg:col-span-1">
          <h2 className="text-xl font-semibold mb-4">
            Hospital Partners
            {city && <span className="text-gray-400 text-base font-normal"> in {city}</span>}
          </h2>
          {loadingHospitals ? (
            <div className="space-y-4">
              {[1, 2, 3].map(i => <div key={i} className="rounded-xl bg-gray-100 h-36 animate-pulse" />)}
            </div>
          ) : filteredHospitals.length > 0 ? (
            <div className="space-y-4">
              {filteredHospitals.map(hospital => (
                <button key={hospital.id} type="button"
                  onClick={() => setSelectedHospital(selectedHospital?.id === hospital.id ? null : hospital)}
                  className={`w-full text-left border-2 rounded-2xl p-4 transition ${
                    selectedHospital?.id === hospital.id ? 'border-blue-500 bg-blue-50/50 shadow-sm' : 'border-gray-200 hover:border-gray-300'
                  }`}>
                  <div className="flex items-start gap-3">
                    <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center shrink-0">
                      <span className="text-xl">🏥</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-sm">{hospital.name}</h3>
                      <p className="text-xs text-gray-500 mt-0.5">{hospital.city}</p>
                      {hospital.rating != null && (
                        <div className="flex items-center gap-1 mt-1">
                          <span className="bg-blue-600 text-white text-xs font-bold px-1.5 py-0.5 rounded">
                            {hospital.rating.toFixed(1)}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-1 mt-3">
                    {hospital.specialties.slice(0, 4).map(s => (
                      <span key={s} className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full">{s}</span>
                    ))}
                  </div>
                  {hospital.accreditations.length > 0 && (
                    <div className="mt-2 flex gap-1 flex-wrap">
                      {hospital.accreditations.map(a => (
                        <span key={a} className="text-xs bg-green-50 text-green-700 px-2 py-0.5 rounded-full">{a}</span>
                      ))}
                    </div>
                  )}
                </button>
              ))}
            </div>
          ) : (
            <p className="text-gray-400 text-sm">No hospitals found for this criteria.</p>
          )}
        </div>

        {/* Stay Packages + Cost Estimate */}
        <div className="lg:col-span-2">
          {/* Selected hospital detail */}
          {selectedHospital && (
            <div className="bg-blue-50 border border-blue-200 rounded-2xl p-5 mb-6">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-bold text-blue-900">{selectedHospital.name}</h3>
                  <p className="text-sm text-blue-700">{selectedHospital.city}</p>
                </div>
                <button onClick={() => setSelectedHospital(null)} className="text-blue-400 hover:text-blue-600">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div className="flex gap-2 flex-wrap mt-3">
                {selectedHospital.specialties.map(s => (
                  <span key={s} className="text-xs bg-white text-blue-700 px-2.5 py-1 rounded-full border border-blue-200">{s}</span>
                ))}
              </div>
              {selectedHospital.accreditations.length > 0 && (
                <div className="mt-2 flex gap-1">
                  {selectedHospital.accreditations.map(a => (
                    <span key={a} className="text-xs bg-green-100 text-green-800 px-2 py-0.5 rounded-full font-semibold">{a}</span>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Cost estimate card */}
          {specialty && procedure && (
            <div className="bg-gradient-to-br from-purple-50 to-blue-50 border border-purple-200 rounded-2xl p-5 mb-6">
              <h3 className="font-bold text-purple-900 mb-3">Estimated Cost: {procedure}</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-purple-700">Treatment</span>
                  <span className="font-semibold text-purple-900">₹2,50,000 – ₹4,00,000</span>
                </div>
                <div className="flex justify-between text-gray-500">
                  <span>Recovery stay (14 nights)</span>
                  <span>₹35,000</span>
                </div>
                <div className="flex justify-between text-gray-500">
                  <span>Services (pickup, translator)</span>
                  <span>₹5,000</span>
                </div>
                <div className="flex justify-between font-bold text-purple-900 border-t border-purple-200 pt-2 text-base">
                  <span>Total estimate</span>
                  <span>₹2,90,000 – ₹4,40,000</span>
                </div>
              </div>
              <div className="mt-4 bg-white rounded-xl p-4">
                <h4 className="text-sm font-semibold text-gray-800 mb-2">Recovery Timeline</h4>
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center text-xs font-bold">1</span>
                    <div>
                      <p className="text-sm font-medium text-gray-800">Day 1-3: Hospital stay</p>
                      <p className="text-xs text-gray-500">Under doctor supervision</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center text-xs font-bold">2</span>
                    <div>
                      <p className="text-sm font-medium text-gray-800">Day 4-14: Recovery at nearby stay</p>
                      <p className="text-xs text-gray-500">Comfortable recovery with caregiver</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-full bg-green-100 text-green-700 flex items-center justify-center text-xs font-bold">3</span>
                    <div>
                      <p className="text-sm font-medium text-gray-800">Day 15: Follow-up & discharge</p>
                      <p className="text-xs text-gray-500">Final check-up, travel clearance</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Nearby stays */}
          <h2 className="text-xl font-semibold mb-4">
            Nearby Medical Stays
            {city && <span className="text-gray-400 text-base font-normal"> in {city}</span>}
          </h2>
          {loadingPackages ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[1, 2, 3, 4].map(i => <div key={i} className="rounded-xl bg-gray-100 h-48 animate-pulse" />)}
            </div>
          ) : packages.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {packages.map(pkg => (
                <div key={pkg.id} className="border rounded-2xl p-5 hover:shadow-md transition bg-white">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold line-clamp-2">{pkg.listingTitle}</h3>
                      <p className="text-sm text-gray-500 mt-1">{pkg.city}</p>
                    </div>
                    <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full whitespace-nowrap">
                      {pkg.specialties?.[0] || 'General'}
                    </span>
                  </div>
                  <div className="mt-3 flex items-center gap-3 text-sm text-gray-500">
                    <span>🏥 {pkg.hospitalName}</span>
                    <span>📍 {pkg.distanceKm?.toFixed(1) || '?'} km</span>
                  </div>
                  {/* Medical-friendly badges */}
                  <div className="flex gap-1.5 flex-wrap mt-3">
                    <span className="text-xs bg-green-50 text-green-700 px-2 py-0.5 rounded-full">♿ Accessible</span>
                    <span className="text-xs bg-green-50 text-green-700 px-2 py-0.5 rounded-full">🍳 Kitchen</span>
                    <span className="text-xs bg-green-50 text-green-700 px-2 py-0.5 rounded-full">👥 Caregiver OK</span>
                  </div>
                  {(pkg.amenities?.length ?? 0) > 0 && (
                    <div className="flex gap-1 flex-wrap mt-2">
                      {pkg.amenities!.slice(0, 3).map(a => (
                        <span key={a} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">{a}</span>
                      ))}
                    </div>
                  )}
                  <div className="mt-3 pt-3 border-t flex items-center justify-between">
                    <span className="font-bold text-orange-600">{formatPaise(pkg.basePricePaise || 0)}/night</span>
                    <Link href={`/listings/${pkg.listingId}`}
                      className="text-sm text-orange-500 font-semibold hover:underline">
                      View stay →
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-16 text-gray-400">
              <p className="text-5xl mb-4">🏥</p>
              <p className="text-lg font-medium">No medical stays found</p>
              <p className="text-sm mt-1">Try a different city or specialty</p>
            </div>
          )}
        </div>
      </div>

      {/* Why Medical Tourism in India */}
      <div className="mt-12 bg-gray-50 rounded-2xl p-8">
        <h2 className="text-2xl font-bold text-center mb-8">Why India for Medical Tourism?</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[
            { icon: '💰', title: '60-90% Savings', desc: 'Same quality treatment at fraction of US/UK cost' },
            { icon: '⭐', title: 'World-Class', desc: 'NABH & JCI accredited hospitals with top doctors' },
            { icon: '🏠', title: 'Recovery Stays', desc: 'Comfortable hospital-adjacent accommodation' },
            { icon: '🌍', title: 'No Wait Times', desc: 'Get treated within days, not months' },
          ].map(item => (
            <div key={item.title} className="text-center">
              <span className="text-4xl">{item.icon}</span>
              <h3 className="font-semibold mt-3">{item.title}</h3>
              <p className="text-sm text-gray-500 mt-1">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
