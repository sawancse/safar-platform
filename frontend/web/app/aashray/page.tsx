'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Listing } from '@/types';

const IMPACT_STATS = [
  { label: 'Families Housed', value: '—', icon: '🏠' },
  { label: 'Aashray Hosts', value: '—', icon: '🤝' },
  { label: 'Partner NGOs', value: '—', icon: '🏛️' },
  { label: 'Cities Covered', value: '—', icon: '🗺️' },
];

const REFUGEE_GROUPS = [
  { name: 'Tibetan', count: '~100K', region: 'Dharamshala, Bylakuppe', icon: '🏔️' },
  { name: 'Afghan', count: '~15K', region: 'Delhi, Hyderabad', icon: '🕌' },
  { name: 'Rohingya', count: '~40K', region: 'Jammu, Hyderabad, Delhi', icon: '🌏' },
  { name: 'Sri Lankan Tamil', count: '~60K', region: 'Tamil Nadu', icon: '🏝️' },
  { name: 'Myanmar Chin', count: '~40K', region: 'Mizoram, Delhi', icon: '🌿' },
  { name: 'Climate Displaced', count: '25M+/yr', region: 'Nationwide', icon: '🌊' },
];

const HOW_IT_WORKS_HOST = [
  { step: 1, title: 'Opt in your property', desc: 'Toggle "Aashray Ready" on your listing. Set a discounted monthly rate.' },
  { step: 2, title: 'Get matched', desc: 'NGO case workers find your listing and book for refugee families.' },
  { step: 3, title: 'Host with purpose', desc: 'Receive monthly payments from the NGO. Get tax benefits under Section 80G.' },
];

const HOW_IT_WORKS_NGO = [
  { step: 1, title: 'Create org account', desc: 'Register your organization with UNHCR partner code or government ID.' },
  { step: 2, title: 'Add case workers', desc: 'Your team gets access to search Aashray-ready listings and book stays.' },
  { step: 3, title: 'Book & manage', desc: 'Book 30-360 day stays. Track placements. Monthly invoicing.' },
];

export default function AashrayPage() {
  const [aashrayListings, setAashrayListings] = useState<Listing[]>([]);
  const [aashrayCount, setAashrayCount] = useState(0);
  const [loadingListings, setLoadingListings] = useState(true);

  useEffect(() => {
    api.search({ aashrayReady: 'true', size: '12' })
      .then(res => {
        setAashrayListings(res.content);
        setAashrayCount(res.totalElements);
      })
      .catch(() => {})
      .finally(() => setLoadingListings(false));
  }, []);

  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-br from-teal-600 via-blue-600 to-purple-700 text-white py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-block bg-white/20 backdrop-blur rounded-full px-4 py-1.5 text-sm font-medium mb-6">
            Safar Aashray — Shelter for Every Journey
          </div>
          <h1 className="text-4xl md:text-5xl font-bold mb-4">
            Everyone deserves a safe place to stay
          </h1>
          <p className="text-lg text-blue-100 mb-8 max-w-2xl mx-auto">
            Connecting displaced persons with dignified housing through NGO-funded stays.
            India hosts 300,000+ refugees — let's house them with care.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/aashray/host"
              className="bg-white text-blue-700 font-semibold px-8 py-3 rounded-xl hover:bg-blue-50 transition text-sm">
              Become an Aashray Host
            </Link>
            <Link href="/aashray/ngo"
              className="border-2 border-white text-white font-semibold px-8 py-3 rounded-xl hover:bg-white/10 transition text-sm">
              Register as NGO Partner
            </Link>
          </div>
        </div>
      </section>

      {/* Impact stats */}
      <section className="max-w-5xl mx-auto px-4 -mt-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {IMPACT_STATS.map(s => (
            <div key={s.label} className="bg-white rounded-2xl shadow-lg p-5 text-center">
              <span className="text-3xl">{s.icon}</span>
              <p className="text-2xl font-bold text-gray-900 mt-2">{s.value}</p>
              <p className="text-xs text-gray-500 mt-1">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* What is Aashray */}
      <section className="max-w-5xl mx-auto px-4 py-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-3">What is Safar Aashray?</h2>
          <p className="text-gray-500 max-w-2xl mx-auto">
            A module within Safar that connects three groups: property hosts who have spare rooms,
            refugee families who need safe housing, and NGOs/governments who fund the stays.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="text-center p-6 border rounded-2xl">
            <span className="text-5xl">🏠</span>
            <h3 className="text-lg font-semibold mt-4 mb-2">Hosts</h3>
            <p className="text-sm text-gray-500">
              List your spare room or property at a discounted rate. Receive guaranteed monthly payments from NGOs. Get 80G tax benefits.
            </p>
          </div>
          <div className="text-center p-6 border rounded-2xl">
            <span className="text-5xl">👨‍👩‍👧‍👦</span>
            <h3 className="text-lg font-semibold mt-4 mb-2">Refugees</h3>
            <p className="text-sm text-gray-500">
              Verified through UNHCR or government documents. Matched with suitable housing near schools, hospitals, and community.
            </p>
          </div>
          <div className="text-center p-6 border rounded-2xl">
            <span className="text-5xl">🏛️</span>
            <h3 className="text-lg font-semibold mt-4 mb-2">NGOs & Funders</h3>
            <p className="text-sm text-gray-500">
              Organizational accounts with case workers. Budget management, monthly invoicing, impact reporting.
            </p>
          </div>
        </div>
      </section>

      {/* How it works — Host */}
      <section className="bg-teal-50 py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-10">For Hosts — How It Works</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {HOW_IT_WORKS_HOST.map(item => (
              <div key={item.step} className="text-center">
                <div className="w-12 h-12 rounded-full bg-teal-600 text-white flex items-center justify-center text-lg font-bold mx-auto mb-4">
                  {item.step}
                </div>
                <h3 className="font-semibold mb-2">{item.title}</h3>
                <p className="text-sm text-gray-600">{item.desc}</p>
              </div>
            ))}
          </div>
          <div className="text-center mt-8">
            <Link href="/aashray/host"
              className="bg-teal-600 hover:bg-teal-700 text-white font-semibold px-8 py-3 rounded-xl transition text-sm inline-block">
              Open your home →
            </Link>
          </div>
        </div>
      </section>

      {/* How it works — NGO */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-10">For NGOs — How It Works</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {HOW_IT_WORKS_NGO.map(item => (
              <div key={item.step} className="text-center">
                <div className="w-12 h-12 rounded-full bg-blue-600 text-white flex items-center justify-center text-lg font-bold mx-auto mb-4">
                  {item.step}
                </div>
                <h3 className="font-semibold mb-2">{item.title}</h3>
                <p className="text-sm text-gray-600">{item.desc}</p>
              </div>
            ))}
          </div>
          <div className="text-center mt-8">
            <Link href="/aashray/ngo"
              className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-8 py-3 rounded-xl transition text-sm inline-block">
              Partner with us →
            </Link>
          </div>
        </div>
      </section>

      {/* Refugee communities in India */}
      <section className="bg-gray-50 py-16 px-4">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-3">Who We Serve</h2>
          <p className="text-gray-500 text-center mb-10">India hosts diverse displaced communities across the country</p>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {REFUGEE_GROUPS.map(g => (
              <div key={g.name} className="bg-white rounded-2xl p-5 border">
                <span className="text-3xl">{g.icon}</span>
                <h3 className="font-semibold mt-3">{g.name} Refugees</h3>
                <p className="text-sm text-gray-500 mt-1">{g.count} in India</p>
                <p className="text-xs text-gray-400 mt-0.5">{g.region}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ID acceptance */}
      <section className="py-16 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-2xl font-bold mb-3">Inclusive Verification</h2>
          <p className="text-gray-500 mb-8">We accept alternative identity documents for displaced persons</p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { icon: '🪪', label: 'Aadhaar / PAN', desc: 'Indian residents' },
              { icon: '🌐', label: 'UNHCR Certificate', desc: 'Registered refugees' },
              { icon: '📋', label: 'Long Term Visa', desc: 'Afghan & others with LTV' },
              { icon: '🏛️', label: 'NGO Vouching', desc: 'Organizational guarantee' },
            ].map(item => (
              <div key={item.label} className="border rounded-xl p-4">
                <span className="text-2xl">{item.icon}</span>
                <p className="font-semibold text-sm mt-2">{item.label}</p>
                <p className="text-xs text-gray-500 mt-1">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Why India */}
      <section className="bg-gradient-to-r from-orange-500 to-amber-500 text-white py-16 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-2xl font-bold mb-3">
            "अतिथि देवो भव" — The Guest is God
          </h2>
          <p className="text-orange-100 mb-8 max-w-2xl mx-auto">
            India's ancient tradition of hospitality meets modern technology.
            Every spare room can become a safe haven. Every host can change a life.
          </p>
          <div className="grid grid-cols-3 gap-6">
            {[
              { num: '300K+', label: 'Refugees in India' },
              { num: '25M+', label: 'Climate displaced/year' },
              { num: '₹0', label: 'Commission on Aashray stays' },
            ].map(item => (
              <div key={item.label}>
                <p className="text-3xl font-bold">{item.num}</p>
                <p className="text-sm text-orange-100 mt-1">{item.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Browse Aashray Listings */}
      <section className="py-16 px-4">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-2">Browse Aashray-Ready Listings</h2>
          <p className="text-gray-500 text-center mb-8">
            {aashrayCount > 0
              ? `${aashrayCount} propert${aashrayCount === 1 ? 'y' : 'ies'} available for displaced persons`
              : 'Properties opened by generous hosts for displaced persons'}
          </p>
          {loadingListings ? (
            <div className="text-center py-12 text-gray-400">Loading Aashray listings...</div>
          ) : aashrayListings.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-4xl mb-3">🏠</p>
              <p className="text-gray-500">No Aashray-ready listings yet. Be the first host!</p>
              <Link href="/aashray/host"
                className="mt-4 inline-block bg-teal-600 hover:bg-teal-700 text-white font-semibold px-6 py-2 rounded-xl transition text-sm">
                Become an Aashray Host
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {aashrayListings.map(listing => (
                <Link key={listing.id} href={`/listings/${listing.id}`}
                  className="border rounded-2xl overflow-hidden hover:shadow-lg transition group">
                  <div className="relative h-40 bg-gray-100">
                    {listing.primaryPhotoUrl ? (
                      <img
                        src={listing.primaryPhotoUrl.startsWith('http') ? listing.primaryPhotoUrl : `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${listing.primaryPhotoUrl}`}
                        alt={listing.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-4xl text-gray-300">🏠</div>
                    )}
                    <span className="absolute top-2 left-2 bg-teal-600 text-white text-xs font-semibold px-2 py-0.5 rounded-full">
                      Aashray Ready
                    </span>
                  </div>
                  <div className="p-3">
                    <p className="font-semibold text-sm truncate">{listing.title}</p>
                    <p className="text-xs text-gray-500">{listing.city}, {listing.state}</p>
                    <p className="text-sm font-bold text-gray-800 mt-1">{formatPaise(listing.basePricePaise)} / night</p>
                    {listing.avgRating != null && listing.avgRating > 0 && (
                      <p className="text-xs text-gray-500 mt-0.5">★ {listing.avgRating.toFixed(1)}</p>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          )}
          {aashrayListings.length > 0 && aashrayCount > 12 && (
            <div className="text-center mt-6">
              <Link href="/search?aashrayReady=true"
                className="text-teal-600 hover:text-teal-700 font-semibold text-sm">
                View all {aashrayCount} Aashray listings →
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 px-4 text-center">
        <h2 className="text-3xl font-bold mb-4">Ready to make a difference?</h2>
        <p className="text-gray-500 mb-8">Whether you have a spare room or represent an organization, you can help.</p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link href="/aashray/host"
            className="bg-teal-600 hover:bg-teal-700 text-white font-semibold px-8 py-3 rounded-xl transition">
            Become an Aashray Host
          </Link>
          <Link href="/aashray/ngo"
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold px-8 py-3 rounded-xl transition">
            NGO Partnership
          </Link>
          <Link href="/aashray/donate"
            className="border-2 border-gray-300 text-gray-700 font-semibold px-8 py-3 rounded-xl hover:bg-gray-50 transition">
            Fund a Stay
          </Link>
        </div>
      </section>
    </div>
  );
}
