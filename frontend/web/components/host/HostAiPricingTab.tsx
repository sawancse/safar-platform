'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

interface PricingDay {
  date: string;
  base_price_paise: number;
  suggested_price_paise: number;
  factors: { name: string; adjustment_percent: number; description: string }[];
  is_event_day: boolean;
  event_name?: string;
}

export default function HostAiPricingTab({ listingId, basePricePaise, city, propertyType }: {
  listingId: string; basePricePaise: number; city: string; propertyType: string;
}) {
  const [calendar, setCalendar] = useState<PricingDay[]>([]);
  const [analytics, setAnalytics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [selectedDay, setSelectedDay] = useState<PricingDay | null>(null);
  const [autoPricing, setAutoPricing] = useState(false);
  const [aggressiveness, setAggressiveness] = useState('moderate');

  useEffect(() => {
    loadData();
  }, [listingId]);

  async function loadData() {
    setLoading(true);
    try {
      const [cal, anal] = await Promise.all([
        api.aiPricingCalendar(listingId, basePricePaise, city, propertyType),
        api.aiPricingAnalytics(listingId, basePricePaise, city),
      ]);
      setCalendar(cal.prices || []);
      setAnalytics(anal);
    } catch (e) {
      console.error('Failed to load AI pricing:', e);
    }
    setLoading(false);
  }

  async function saveRules() {
    try {
      await api.aiPricingRules({
        listing_id: listingId,
        min_price_paise: Math.floor(basePricePaise * 0.5),
        max_price_paise: Math.floor(basePricePaise * 3),
        aggressiveness,
        auto_apply: autoPricing,
        weekend_boost_percent: 20,
        low_demand_discount_percent: 10,
      });
      loadData();
    } catch (e) {
      console.error('Failed to save rules:', e);
    }
  }

  if (loading) return <div className="p-6 text-center text-gray-500">Loading AI pricing...</div>;

  const pctChange = analytics?.suggested_avg_price_paise && basePricePaise
    ? ((analytics.suggested_avg_price_paise - basePricePaise) / basePricePaise * 100).toFixed(1)
    : '0';

  return (
    <div className="space-y-6">
      {/* Revenue Summary */}
      {analytics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl border p-4">
            <p className="text-sm text-gray-500">Current Avg Price</p>
            <p className="text-2xl font-bold">₹{(analytics.current_avg_price_paise / 100).toLocaleString()}</p>
          </div>
          <div className="bg-white rounded-xl border p-4">
            <p className="text-sm text-gray-500">AI Suggested Avg</p>
            <p className="text-2xl font-bold text-orange-600">₹{(analytics.suggested_avg_price_paise / 100).toLocaleString()}</p>
            <p className={`text-sm ${Number(pctChange) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {Number(pctChange) >= 0 ? '+' : ''}{pctChange}%
            </p>
          </div>
          <div className="bg-white rounded-xl border p-4">
            <p className="text-sm text-gray-500">Market Position</p>
            <p className="text-2xl font-bold capitalize">{analytics.price_position?.replace('_', ' ')}</p>
          </div>
          <div className="bg-white rounded-xl border p-4">
            <p className="text-sm text-gray-500">Est. Monthly Revenue</p>
            <p className="text-2xl font-bold text-green-600">₹{(analytics.revenue_potential_monthly_paise / 100).toLocaleString()}</p>
          </div>
        </div>
      )}

      {/* Pricing Rules */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="font-semibold text-lg mb-4">Pricing Rules</h3>
        <div className="flex items-center gap-6 flex-wrap">
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={autoPricing} onChange={(e) => setAutoPricing(e.target.checked)}
              className="rounded border-gray-300" />
            <span className="text-sm">Auto-apply AI prices</span>
          </label>
          <div>
            <label className="text-sm text-gray-500 block mb-1">Aggressiveness</label>
            <select value={aggressiveness} onChange={(e) => setAggressiveness(e.target.value)}
              className="border rounded-lg px-3 py-1.5 text-sm">
              <option value="conservative">Conservative</option>
              <option value="moderate">Moderate</option>
              <option value="aggressive">Aggressive</option>
            </select>
          </div>
          <button onClick={saveRules}
            className="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm hover:bg-orange-600">
            Save Rules
          </button>
        </div>
      </div>

      {/* 30-Day Price Calendar */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="font-semibold text-lg mb-4">30-Day Price Calendar</h3>
        <div className="grid grid-cols-7 gap-2">
          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(d => (
            <div key={d} className="text-xs font-medium text-gray-400 text-center">{d}</div>
          ))}
          {calendar.slice(0, 30).map((day) => {
            const diff = day.suggested_price_paise - day.base_price_paise;
            const isUp = diff > 0;
            const isEvent = day.is_event_day;
            return (
              <button key={day.date} onClick={() => setSelectedDay(day)}
                className={`p-2 rounded-lg text-xs border transition-all hover:ring-2 hover:ring-orange-300
                  ${isEvent ? 'bg-purple-50 border-purple-200' : isUp ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'}
                  ${selectedDay?.date === day.date ? 'ring-2 ring-orange-500' : ''}`}>
                <div className="font-medium">{new Date(day.date).getDate()}</div>
                <div className={`font-bold ${isUp ? 'text-green-700' : 'text-red-700'}`}>
                  ₹{(day.suggested_price_paise / 100).toLocaleString()}
                </div>
                {isEvent && <div className="text-purple-600 truncate">{day.event_name}</div>}
              </button>
            );
          })}
        </div>
      </div>

      {/* Selected Day Detail */}
      {selectedDay && (
        <div className="bg-white rounded-xl border p-6">
          <h3 className="font-semibold mb-3">
            {new Date(selectedDay.date).toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
            {selectedDay.event_name && <span className="ml-2 text-purple-600">({selectedDay.event_name})</span>}
          </h3>
          <div className="flex gap-8 mb-4">
            <div><span className="text-gray-500 text-sm">Base</span><br/>
              <span className="text-lg font-bold">₹{(selectedDay.base_price_paise / 100).toLocaleString()}</span></div>
            <div><span className="text-gray-500 text-sm">Suggested</span><br/>
              <span className="text-lg font-bold text-orange-600">₹{(selectedDay.suggested_price_paise / 100).toLocaleString()}</span></div>
          </div>
          <div className="space-y-2">
            {selectedDay.factors.map((f, i) => (
              <div key={i} className="flex justify-between items-center text-sm">
                <span className="text-gray-600">{f.description}</span>
                <span className={f.adjustment_percent >= 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'}>
                  {f.adjustment_percent >= 0 ? '+' : ''}{f.adjustment_percent.toFixed(1)}%
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
