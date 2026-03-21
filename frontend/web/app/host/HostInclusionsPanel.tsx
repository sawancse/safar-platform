'use client';

import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { RoomTypeInclusion, InclusionCategory, InclusionMode, ChargeType } from '@/types';

const CATEGORIES: { value: InclusionCategory; label: string; icon: string }[] = [
  { value: 'MEAL', label: 'Meals', icon: '🍽️' },
  { value: 'DISCOUNT', label: 'Discounts', icon: '🏷️' },
  { value: 'FLEXIBILITY', label: 'Flexibility', icon: '⏰' },
  { value: 'WELLNESS', label: 'Wellness', icon: '🧘' },
  { value: 'TRANSPORT', label: 'Transport', icon: '🚗' },
  { value: 'AMENITY', label: 'Amenities', icon: '🔑' },
  { value: 'EXPERIENCE', label: 'Experiences', icon: '🎭' },
];

const INCLUSION_MODES: { value: InclusionMode; label: string; color: string }[] = [
  { value: 'INCLUDED', label: 'Included in rate', color: 'bg-green-100 text-green-700' },
  { value: 'PAID_ADDON', label: 'Paid add-on', color: 'bg-blue-100 text-blue-700' },
  { value: 'COMPLIMENTARY', label: 'Complimentary', color: 'bg-purple-100 text-purple-700' },
];

const CHARGE_TYPES: { value: ChargeType; label: string }[] = [
  { value: 'PER_STAY', label: 'Per stay' },
  { value: 'PER_NIGHT', label: 'Per night' },
  { value: 'PER_PERSON', label: 'Per person' },
  { value: 'PER_HOUR', label: 'Per hour' },
  { value: 'PER_USE', label: 'Per use' },
];

const QUICK_ADD_TEMPLATES: { category: InclusionCategory; name: string; mode: InclusionMode; desc?: string }[] = [
  { category: 'MEAL', name: 'Breakfast', mode: 'INCLUDED', desc: 'Daily breakfast included' },
  { category: 'MEAL', name: 'Lunch', mode: 'PAID_ADDON', desc: 'Lunch available at extra charge' },
  { category: 'MEAL', name: 'Dinner', mode: 'PAID_ADDON', desc: 'Dinner available at extra charge' },
  { category: 'MEAL', name: 'Half Board (Breakfast + Dinner)', mode: 'INCLUDED' },
  { category: 'MEAL', name: 'Full Board (All Meals)', mode: 'INCLUDED' },
  { category: 'MEAL', name: 'All-Inclusive', mode: 'INCLUDED', desc: 'All meals, beverages & snacks' },
  { category: 'MEAL', name: 'Welcome Drink', mode: 'COMPLIMENTARY' },
  { category: 'MEAL', name: 'Mini Bar', mode: 'PAID_ADDON' },
  { category: 'DISCOUNT', name: '10% F&B Discount', mode: 'INCLUDED', desc: '10% discount on food & beverages' },
  { category: 'DISCOUNT', name: '20% Spa Discount', mode: 'INCLUDED', desc: '20% discount on spa services' },
  { category: 'DISCOUNT', name: '15% Laundry Discount', mode: 'INCLUDED', desc: '15% off laundry services' },
  { category: 'DISCOUNT', name: '10% Activity Discount', mode: 'INCLUDED', desc: '10% off curated activities' },
  { category: 'FLEXIBILITY', name: 'Early Check-in (2 hrs)', mode: 'COMPLIMENTARY', desc: 'Subject to availability' },
  { category: 'FLEXIBILITY', name: 'Late Checkout (2 hrs)', mode: 'COMPLIMENTARY', desc: 'Subject to availability' },
  { category: 'FLEXIBILITY', name: 'Guaranteed Early Check-in', mode: 'PAID_ADDON', desc: 'Guaranteed 2 hours early' },
  { category: 'FLEXIBILITY', name: 'Guaranteed Late Checkout', mode: 'PAID_ADDON', desc: 'Guaranteed 2 hours late' },
  { category: 'FLEXIBILITY', name: 'Express Check-in', mode: 'COMPLIMENTARY', desc: 'Skip the queue' },
  { category: 'FLEXIBILITY', name: 'Free Cancellation (48 hrs)', mode: 'INCLUDED', desc: 'Free cancel until 48 hrs before' },
  { category: 'FLEXIBILITY', name: 'Cancellation Charges Apply', mode: 'INCLUDED', desc: 'Cancellation fee as per policy' },
  { category: 'WELLNESS', name: 'Spa Access', mode: 'PAID_ADDON' },
  { category: 'WELLNESS', name: 'Gym Access', mode: 'COMPLIMENTARY' },
  { category: 'WELLNESS', name: 'Pool Access', mode: 'INCLUDED' },
  { category: 'WELLNESS', name: 'Yoga Session', mode: 'PAID_ADDON' },
  { category: 'TRANSPORT', name: 'Airport Pickup', mode: 'PAID_ADDON' },
  { category: 'TRANSPORT', name: 'Airport Drop', mode: 'PAID_ADDON' },
  { category: 'TRANSPORT', name: 'Railway Station Pickup', mode: 'PAID_ADDON' },
  { category: 'TRANSPORT', name: 'Local Sightseeing', mode: 'PAID_ADDON' },
  { category: 'AMENITY', name: 'Free WiFi', mode: 'INCLUDED' },
  { category: 'AMENITY', name: 'Free Parking', mode: 'INCLUDED' },
  { category: 'AMENITY', name: 'Daily Housekeeping', mode: 'INCLUDED' },
  { category: 'AMENITY', name: 'Laundry Service', mode: 'PAID_ADDON' },
  { category: 'AMENITY', name: 'Ironing Service', mode: 'PAID_ADDON' },
  { category: 'AMENITY', name: 'Room Service', mode: 'PAID_ADDON' },
  { category: 'EXPERIENCE', name: 'City Tour', mode: 'PAID_ADDON' },
  { category: 'EXPERIENCE', name: 'Cooking Class', mode: 'PAID_ADDON' },
  { category: 'EXPERIENCE', name: 'Cultural Show', mode: 'PAID_ADDON' },
  { category: 'EXPERIENCE', name: 'Nature Walk', mode: 'PAID_ADDON' },
];

interface InclusionFormData {
  category: InclusionCategory;
  name: string;
  description: string;
  inclusionMode: InclusionMode;
  chargePaise: string;
  chargeType: ChargeType;
  discountPercent: string;
  terms: string;
  isHighlight: boolean;
}

const EMPTY_FORM: InclusionFormData = {
  category: 'MEAL',
  name: '',
  description: '',
  inclusionMode: 'INCLUDED',
  chargePaise: '',
  chargeType: 'PER_STAY',
  discountPercent: '',
  terms: '',
  isHighlight: false,
};

interface Props {
  token: string;
  listingId: string;
  roomTypeId: string;
  roomTypeName: string;
  inclusions: RoomTypeInclusion[];
  onUpdate: () => void;
}

export default function HostInclusionsPanel({ token, listingId, roomTypeId, roomTypeName, inclusions, onUpdate }: Props) {
  const [expanded, setExpanded] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<InclusionFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [showQuickAdd, setShowQuickAdd] = useState(false);
  const [quickAddCategory, setQuickAddCategory] = useState<InclusionCategory>('MEAL');

  function openAdd() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError('');
    setShowForm(true);
    setShowQuickAdd(false);
  }

  function openEdit(inc: RoomTypeInclusion) {
    setEditingId(inc.id);
    setForm({
      category: inc.category,
      name: inc.name,
      description: inc.description || '',
      inclusionMode: inc.inclusionMode,
      chargePaise: inc.chargePaise > 0 ? String(inc.chargePaise / 100) : '',
      chargeType: inc.chargeType,
      discountPercent: inc.discountPercent > 0 ? String(inc.discountPercent) : '',
      terms: inc.terms || '',
      isHighlight: inc.isHighlight,
    });
    setError('');
    setShowForm(true);
    setShowQuickAdd(false);
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) { setError('Name is required'); return; }

    setSaving(true);
    setError('');
    try {
      const payload = {
        category: form.category,
        name: form.name.trim(),
        description: form.description.trim() || null,
        inclusionMode: form.inclusionMode,
        chargePaise: form.chargePaise ? Math.round(Number(form.chargePaise) * 100) : 0,
        chargeType: form.chargeType,
        discountPercent: form.discountPercent ? Number(form.discountPercent) : 0,
        terms: form.terms.trim() || null,
        isHighlight: form.isHighlight,
      };

      if (editingId) {
        await api.updateRoomTypeInclusion(listingId, roomTypeId, editingId, payload, token);
      } else {
        await api.createRoomTypeInclusion(listingId, roomTypeId, payload, token);
      }
      setShowForm(false);
      setEditingId(null);
      onUpdate();
    } catch (e: any) {
      setError(e.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(inclusionId: string) {
    try {
      await api.deleteRoomTypeInclusion(listingId, roomTypeId, inclusionId, token);
      onUpdate();
    } catch (e: any) {
      setError(e.message || 'Failed to delete');
    }
  }

  async function handleQuickAdd(template: typeof QUICK_ADD_TEMPLATES[0]) {
    setSaving(true);
    try {
      await api.createRoomTypeInclusion(listingId, roomTypeId, {
        category: template.category,
        name: template.name,
        description: template.desc || null,
        inclusionMode: template.mode,
        chargePaise: 0,
        chargeType: 'PER_STAY',
        discountPercent: 0,
        terms: null,
        isHighlight: false,
      }, token);
      onUpdate();
    } catch (e: any) {
      setError(e.message || 'Failed to add');
    } finally {
      setSaving(false);
    }
  }

  const grouped = CATEGORIES.map(cat => ({
    ...cat,
    items: inclusions.filter(i => i.category === cat.value),
  })).filter(g => g.items.length > 0);

  const modeLabel = (mode: InclusionMode) =>
    INCLUSION_MODES.find(m => m.value === mode)?.label || mode;
  const modeColor = (mode: InclusionMode) =>
    INCLUSION_MODES.find(m => m.value === mode)?.color || 'bg-gray-100 text-gray-700';

  return (
    <div className="border-t mt-3 pt-3">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-800 w-full"
      >
        <span className={`transition-transform ${expanded ? 'rotate-90' : ''}`}>&#9654;</span>
        Inclusions & Perks
        {inclusions.length > 0 && (
          <span className="ml-auto text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full">
            {inclusions.length}
          </span>
        )}
      </button>

      {expanded && (
        <div className="mt-3 space-y-3">
          {/* Existing inclusions grouped by category */}
          {grouped.length > 0 ? (
            grouped.map(group => (
              <div key={group.value}>
                <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5">
                  {group.icon} {group.label}
                </div>
                <div className="space-y-1.5">
                  {group.items.map(inc => (
                    <div key={inc.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2 text-sm group">
                      <div className="flex items-center gap-2 flex-1 min-w-0">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${modeColor(inc.inclusionMode)}`}>
                          {modeLabel(inc.inclusionMode)}
                        </span>
                        <span className="font-medium text-gray-800 truncate">{inc.name}</span>
                        {inc.isHighlight && <span className="text-xs text-yellow-600">&#9733;</span>}
                        {inc.inclusionMode === 'PAID_ADDON' && inc.chargePaise > 0 && (
                          <span className="text-xs text-blue-600 font-medium">
                            +{formatPaise(inc.chargePaise)}/{inc.chargeType.replace('PER_', '').toLowerCase()}
                          </span>
                        )}
                        {inc.category === 'DISCOUNT' && inc.discountPercent > 0 && (
                          <span className="text-xs text-green-600 font-medium">{inc.discountPercent}% off</span>
                        )}
                      </div>
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition">
                        <button onClick={() => openEdit(inc)} className="text-xs text-orange-600 hover:underline">Edit</button>
                        <button onClick={() => handleDelete(inc.id)} className="text-xs text-red-500 hover:underline">Delete</button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))
          ) : (
            <p className="text-xs text-gray-400 italic">No inclusions configured yet</p>
          )}

          {error && <p className="text-xs text-red-600">{error}</p>}

          {/* Action buttons */}
          <div className="flex gap-2">
            <button onClick={openAdd}
              className="text-xs font-medium text-orange-600 hover:text-orange-700 border border-orange-200 px-3 py-1.5 rounded-lg hover:bg-orange-50 transition">
              + Custom
            </button>
            <button onClick={() => { setShowQuickAdd(!showQuickAdd); setShowForm(false); }}
              className="text-xs font-medium text-teal-600 hover:text-teal-700 border border-teal-200 px-3 py-1.5 rounded-lg hover:bg-teal-50 transition">
              Quick Add
            </button>
          </div>

          {/* Quick Add Panel */}
          {showQuickAdd && (
            <div className="bg-teal-50 border border-teal-200 rounded-xl p-3 space-y-2">
              <div className="flex flex-wrap gap-1.5 mb-2">
                {CATEGORIES.map(cat => (
                  <button key={cat.value}
                    onClick={() => setQuickAddCategory(cat.value)}
                    className={`text-xs px-2.5 py-1 rounded-full font-medium transition ${
                      quickAddCategory === cat.value
                        ? 'bg-teal-600 text-white'
                        : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
                    }`}>
                    {cat.icon} {cat.label}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                {QUICK_ADD_TEMPLATES
                  .filter(t => t.category === quickAddCategory)
                  .filter(t => !inclusions.some(i => i.name === t.name))
                  .map(t => (
                    <button key={t.name}
                      onClick={() => handleQuickAdd(t)}
                      disabled={saving}
                      className="text-left text-xs bg-white border rounded-lg px-2.5 py-2 hover:border-teal-400 hover:bg-teal-50 transition disabled:opacity-50">
                      <div className="font-medium text-gray-800">{t.name}</div>
                      <div className="text-gray-400 mt-0.5">
                        {INCLUSION_MODES.find(m => m.value === t.mode)?.label}
                      </div>
                    </button>
                  ))}
              </div>
            </div>
          )}

          {/* Custom Form */}
          {showForm && (
            <form onSubmit={handleSave} className="bg-orange-50 border border-orange-200 rounded-xl p-3 space-y-3">
              <div className="text-sm font-semibold text-gray-700">
                {editingId ? 'Edit Inclusion' : 'New Inclusion'}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Category</label>
                  <select value={form.category}
                    onChange={e => setForm(f => ({ ...f, category: e.target.value as InclusionCategory }))}
                    className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500">
                    {CATEGORIES.map(c => (
                      <option key={c.value} value={c.value}>{c.icon} {c.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Mode</label>
                  <select value={form.inclusionMode}
                    onChange={e => setForm(f => ({ ...f, inclusionMode: e.target.value as InclusionMode }))}
                    className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500">
                    {INCLUSION_MODES.map(m => (
                      <option key={m.value} value={m.value}>{m.label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Name *</label>
                <input type="text" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Breakfast, Spa Access, Airport Pickup"
                  className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500" required />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Description</label>
                <input type="text" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="Continental buffet served 7-10am"
                  className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500" />
              </div>

              {form.inclusionMode === 'PAID_ADDON' && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">Price (INR)</label>
                    <input type="number" min="0" step="1" value={form.chargePaise}
                      onChange={e => setForm(f => ({ ...f, chargePaise: e.target.value }))}
                      placeholder="500"
                      className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-600 mb-1">Charge per</label>
                    <select value={form.chargeType}
                      onChange={e => setForm(f => ({ ...f, chargeType: e.target.value as ChargeType }))}
                      className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500">
                      {CHARGE_TYPES.map(ct => (
                        <option key={ct.value} value={ct.value}>{ct.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
              )}

              {form.category === 'DISCOUNT' && (
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">Discount %</label>
                  <input type="number" min="0" max="100" value={form.discountPercent}
                    onChange={e => setForm(f => ({ ...f, discountPercent: e.target.value }))}
                    placeholder="10"
                    className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500" />
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Terms / Conditions</label>
                <input type="text" value={form.terms}
                  onChange={e => setForm(f => ({ ...f, terms: e.target.value }))}
                  placeholder="Subject to availability"
                  className="w-full border rounded-lg px-2.5 py-1.5 text-sm focus:ring-1 focus:ring-orange-500" />
              </div>

              <label className="flex items-center gap-2 text-xs text-gray-600">
                <input type="checkbox" checked={form.isHighlight}
                  onChange={e => setForm(f => ({ ...f, isHighlight: e.target.checked }))}
                  className="rounded border-gray-300 text-orange-500 focus:ring-orange-500" />
                Highlight on listing card
              </label>

              <div className="flex gap-2">
                <button type="button" onClick={() => setShowForm(false)}
                  className="flex-1 text-xs font-medium text-gray-600 border px-3 py-1.5 rounded-lg hover:bg-gray-50 transition">
                  Cancel
                </button>
                <button type="submit" disabled={saving}
                  className="flex-1 text-xs font-medium bg-orange-500 text-white px-3 py-1.5 rounded-lg hover:bg-orange-600 transition disabled:opacity-50">
                  {saving ? 'Saving...' : editingId ? 'Update' : 'Add'}
                </button>
              </div>
            </form>
          )}
        </div>
      )}
    </div>
  );
}
