import SearchBar from '@/components/SearchBar';
import PropertyTypeBar from '@/components/PropertyTypeBar';
import RecentSearches from '@/components/RecentSearches';
import StillInterested from '@/components/StillInterested';
import Recommendations from '@/components/Recommendations';
import TrendingListings from '@/components/TrendingListings';
import Link from 'next/link';

const TRUST_BADGES = [
  { icon: '0%', label: 'Zero commission', sub: 'Hosts keep everything' },
  { icon: '✓', label: 'Verified listings', sub: 'Every property checked' },
  { icon: '⚡', label: 'Instant UPI', sub: 'Pay & get paid instantly' },
  { icon: '🤖', label: 'AI-powered', sub: 'Smart pricing & messaging' },
];

const STATS = [
  { value: '10K+', label: 'Listings' },
  { value: '50+', label: 'Cities' },
  { value: '4.8★', label: 'Avg Rating' },
  { value: '₹0', label: 'Deposit' },
];

export default async function HomePage() {

  return (
    <>
      {/* ── Hero Section ── */}
      <section className="relative overflow-hidden">
        {/* Background: deep gradient with texture */}
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900 via-orange-950 to-slate-900" />
        {/* Decorative blobs */}
        <div className="absolute top-[-20%] right-[-10%] w-[500px] h-[500px] bg-orange-500/20 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-30%] left-[-10%] w-[400px] h-[400px] bg-pink-500/15 rounded-full blur-[100px]" />
        <div className="absolute top-[20%] left-[15%] w-[200px] h-[200px] bg-amber-400/10 rounded-full blur-[80px]" />

        <div className="relative z-10 max-w-5xl mx-auto px-4 pt-16 pb-20 sm:pt-24 sm:pb-28">
          {/* Eyebrow */}
          <div className="flex justify-center mb-6">
            <span className="inline-flex items-center gap-2 bg-white/10 backdrop-blur-sm border border-white/10 text-orange-300 text-xs font-semibold px-4 py-1.5 rounded-full tracking-wide uppercase">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              India&#39;s zero-commission travel platform
            </span>
          </div>

          {/* Headline */}
          <h1 className="text-center text-4xl sm:text-5xl md:text-6xl font-extrabold tracking-tight leading-[1.1]">
            <span className="text-white">Find your </span>
            <span className="bg-gradient-to-r from-orange-400 via-amber-300 to-orange-400 bg-clip-text text-transparent">
              perfect stay
            </span>
            <br className="hidden sm:block" />
            <span className="text-white"> in </span>
            <span className="bg-gradient-to-r from-orange-400 to-pink-400 bg-clip-text text-transparent">
              India
            </span>
          </h1>

          <p className="text-center text-white/60 text-base sm:text-lg mt-5 mb-10 max-w-xl mx-auto leading-relaxed">
            Homes, villas, PGs, hotels — book verified stays across 50+ cities.
            <br className="hidden sm:block" />
            <span className="text-orange-300/80">Hosts keep 100% of earnings.</span>
          </p>

          {/* Search Bar */}
          <div className="max-w-2xl mx-auto">
            <SearchBar />
          </div>

          {/* Trust Badges */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-10 max-w-2xl mx-auto">
            {TRUST_BADGES.map((badge) => (
              <div key={badge.label}
                className="flex items-center gap-2.5 bg-white/[0.06] backdrop-blur-sm border border-white/[0.08] rounded-xl px-3 py-2.5 hover:bg-white/[0.1] transition-colors">
                <div className="w-9 h-9 rounded-lg bg-orange-500/20 flex items-center justify-center text-orange-300 font-bold text-sm shrink-0">
                  {badge.icon}
                </div>
                <div className="min-w-0">
                  <p className="text-white text-xs font-semibold truncate">{badge.label}</p>
                  <p className="text-white/40 text-[10px] truncate">{badge.sub}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Stats Row */}
          <div className="flex justify-center gap-8 sm:gap-14 mt-10">
            {STATS.map((s) => (
              <div key={s.label} className="text-center">
                <p className="text-2xl sm:text-3xl font-extrabold text-white">{s.value}</p>
                <p className="text-white/40 text-xs mt-0.5">{s.label}</p>
              </div>
            ))}
          </div>

          {/* CTA Button */}
          <div className="mt-10 flex justify-center">
            <Link href="/search"
              className="bg-white text-slate-900 font-semibold px-8 py-3 rounded-xl hover:bg-orange-50 transition text-sm shadow-lg shadow-black/20">
              Explore stays
            </Link>
          </div>
        </div>
      </section>

      {/* Property Type Bar — Airbnb-style sticky category row */}
      <PropertyTypeBar />

      {/* Recent Searches */}
      <RecentSearches />

      {/* Still Interested */}
      <StillInterested />

      {/* Property type tabs + Trending listings */}
      <TrendingListings />

      {/* Recommendations */}
      <div className="bg-gray-50 py-4">
        <Recommendations />
      </div>

      {/* ── Why Safar Section ── */}
      <section className="bg-gradient-to-b from-slate-50 to-white py-20 px-4">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-14">
            <p className="text-orange-500 text-sm font-semibold tracking-wide uppercase mb-2">For hosts</p>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900">
              Why hosts choose Safar
            </h2>
            <p className="text-slate-500 mt-3 max-w-lg mx-auto">
              Zero commission. AI-powered tools. Full control over your property.
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              {
                icon: '💰',
                title: '0% commission',
                desc: 'Keep 100% of every booking. Pay a flat monthly subscription — ₹999/month.',
                accent: 'from-green-500/10 to-emerald-500/5',
                border: 'border-green-200',
              },
              {
                icon: '🤖',
                title: 'AI autopilot',
                desc: "Smart pricing, guest messaging, calendar optimization — AI runs your listing while you sleep.",
                accent: 'from-blue-500/10 to-indigo-500/5',
                border: 'border-blue-200',
              },
              {
                icon: '🛡️',
                title: 'Verified & insured',
                desc: 'Every listing is verified by our team. Zero deposit micro-insurance protects every booking.',
                accent: 'from-orange-500/10 to-amber-500/5',
                border: 'border-orange-200',
              },
            ].map((item) => (
              <div key={item.title}
                className={`relative overflow-hidden rounded-2xl border ${item.border} bg-gradient-to-br ${item.accent} p-8 hover:shadow-lg transition-shadow`}>
                <div className="text-4xl mb-5">{item.icon}</div>
                <h3 className="text-lg font-bold text-slate-900 mb-2">{item.title}</h3>
                <p className="text-slate-600 text-sm leading-relaxed">{item.desc}</p>
              </div>
            ))}
          </div>
          <div className="text-center mt-12">
            <a href="/host"
              className="inline-flex items-center gap-2 bg-slate-900 text-white px-8 py-3.5 rounded-xl font-semibold text-base hover:bg-slate-800 transition shadow-lg shadow-slate-900/20">
              Start your 90-day free trial
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </a>
          </div>
        </div>
      </section>
    </>
  );
}
