'use client';

import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Listing, PgPackage } from '@/types';

interface Props {
  token: string;
  listings: Listing[];
}

interface PackageFormData {
  name: string;
  description: string;
  monthlyPriceRupees: string;
  includesMeals: boolean;
  includesLaundry: boolean;
  includesWifi: boolean;
  includesHousekeeping: boolean;
}

const EMPTY_FORM: PackageFormData = {
  name: '',
  description: '',
  monthlyPriceRupees: '',
  includesMeals: false,
  includesLaundry: false,
  includesWifi: false,
  includesHousekeeping: false,
};

export default function HostPgPackagesTab({ token, listings }: Props) {
  const pgListings = listings.filter(l => l.type === 'PG' || l.type === 'COLIVING');
  const [selectedListingId, setSelectedListingId] = useState<string>('');
  const [packages, setPackages] = useState<PgPackage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<PackageFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (pgListings.length > 0 && !selectedListingId) {
      setSelectedListingId(pgListings[0].id);
    }
  }, [pgListings, selectedListingId]);

  const fetchPackages = useCallback(async () => {
    if (!selectedListingId) return;
    setLoading(true);
    setError('');
    try {
      const data = await api.getPgPackages(selectedListingId);
      setPackages(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load packages');
    } finally {
      setLoading(false);
    }
  }, [selectedListingId]);

  useEffect(() => { fetchPackages(); }, [fetchPackages]);

  function openAddModal() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setShowModal(true);
  }

  function openEditModal(pkg: PgPackage) {
    setEditingId(pkg.id);
    setForm({
      name: pkg.name,
      description: pkg.description || '',
      monthlyPriceRupees: String(pkg.monthlyPricePaise / 100),
      includesMeals: pkg.includesMeals,
      includesLaundry: pkg.includesLaundry,
      includesWifi: pkg.includesWifi,
      includesHousekeeping: pkg.includesHousekeeping,
    });
    setFormError('');
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) { setFormError('Name is required'); return; }
    if (!form.monthlyPriceRupees || Number(form.monthlyPriceRupees) < 0) { setFormError('Price must be 0 or more'); return; }

    setSaving(true);
    setFormError('');
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        monthlyPricePaise: Math.round(Number(form.monthlyPriceRupees) * 100),
        includesMeals: form.includesMeals,
        includesLaundry: form.includesLaundry,
        includesWifi: form.includesWifi,
        includesHousekeeping: form.includesHousekeeping,
      };

      if (editingId) {
        const updated = await api.updatePgPackage(selectedListingId, editingId, payload, token);
        setPackages(prev => prev.map(p => p.id === editingId ? updated : p));
      } else {
        const created = await api.createPgPackage(selectedListingId, payload, token);
        setPackages(prev => [...prev, created]);
      }
      closeModal();
    } catch (e: any) {
      setFormError(e.message || 'Failed to save package');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(packageId: string) {
    try {
      await api.deletePgPackage(selectedListingId, packageId, token);
      setPackages(prev => prev.filter(p => p.id !== packageId));
    } catch (e: any) {
      setError(e.message || 'Failed to delete package');
    }
  }

  if (pgListings.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <div className="text-4xl mb-4">🏠</div>
        <p>No PG/Co-living listings found. This feature is only for PG and Co-living properties.</p>
      </div>
    );
  }

  const INCLUDES_FLAGS = [
    { key: 'includesMeals' as const, label: 'Meals', icon: '🍽️' },
    { key: 'includesLaundry' as const, label: 'Laundry', icon: '👕' },
    { key: 'includesWifi' as const, label: 'WiFi', icon: '📶' },
    { key: 'includesHousekeeping' as const, label: 'Housekeeping', icon: '🧹' },
  ];

  return (
    <div className="space-y-6">
      {/* Listing Selector */}
      <div className="flex items-center gap-4 flex-wrap">
        <label className="text-sm font-medium text-gray-700">PG Listing:</label>
        <select value={selectedListingId}
          onChange={(e) => setSelectedListingId(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500">
          {pgListings.map((l) => (
            <option key={l.id} value={l.id}>{l.title} ({l.city})</option>
          ))}
        </select>
      </div>

      {/* Info banner */}
      <div className="bg-purple-50 border border-purple-200 rounded-xl p-4 text-sm text-purple-700">
        Packages are add-ons to the base room rent. Guests choose: Room Type (base rent) + Package (add-on).
        For example: &quot;2-Sharing AC&quot; at base rent + &quot;Standard Package&quot; with meals = total monthly rent.
      </div>

      {/* Add button */}
      <div className="flex justify-end">
        <button onClick={openAddModal}
          className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2 rounded-xl text-sm transition">
          + Add Package
        </button>
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading packages...</div>
      ) : packages.length === 0 ? (
        <div className="text-center py-16 bg-white border rounded-xl shadow-sm">
          <div className="text-4xl mb-4">📦</div>
          <h3 className="text-lg font-semibold text-gray-700 mb-2">No packages yet</h3>
          <p className="text-sm text-gray-500 mb-6 max-w-md mx-auto">
            Create packages like &quot;Basic&quot; (bed only), &quot;Standard&quot; (with meals), or &quot;Premium&quot; (all inclusive).
          </p>
          <button onClick={openAddModal}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm transition">
            + Add First Package
          </button>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {packages.map((pkg) => (
            <div key={pkg.id} className="bg-white border rounded-xl p-5 shadow-sm hover:shadow-md transition">
              <div className="flex items-start justify-between mb-3">
                <h4 className="font-semibold text-gray-800">{pkg.name}</h4>
                <div className="text-right">
                  <div className="text-lg font-bold text-orange-600">
                    {pkg.monthlyPricePaise === 0 ? 'Free' : `+${formatPaise(pkg.monthlyPricePaise)}`}
                  </div>
                  <div className="text-xs text-gray-400">/ month add-on</div>
                </div>
              </div>

              {pkg.description && (
                <p className="text-xs text-gray-500 mb-3 line-clamp-2">{pkg.description}</p>
              )}

              {/* Includes flags */}
              <div className="flex flex-wrap gap-2 mb-3">
                {pkg.includesMeals && (
                  <span className="text-xs bg-green-50 text-green-700 px-2 py-1 rounded-full">🍽️ Meals</span>
                )}
                {pkg.includesLaundry && (
                  <span className="text-xs bg-blue-50 text-blue-700 px-2 py-1 rounded-full">👕 Laundry</span>
                )}
                {pkg.includesWifi && (
                  <span className="text-xs bg-purple-50 text-purple-700 px-2 py-1 rounded-full">📶 WiFi</span>
                )}
                {pkg.includesHousekeeping && (
                  <span className="text-xs bg-amber-50 text-amber-700 px-2 py-1 rounded-full">🧹 Housekeeping</span>
                )}
                {!pkg.includesMeals && !pkg.includesLaundry && !pkg.includesWifi && !pkg.includesHousekeeping && (
                  <span className="text-xs text-gray-400">Bed only</span>
                )}
              </div>

              {/* Actions */}
              <div className="flex gap-2 pt-2 border-t">
                <button onClick={() => openEditModal(pkg)}
                  className="flex-1 text-sm font-medium text-orange-600 hover:text-orange-700 py-1.5 rounded-lg hover:bg-orange-50 transition">
                  Edit
                </button>
                <button onClick={() => handleDelete(pkg.id)}
                  className="flex-1 text-sm font-medium text-red-500 hover:text-red-600 py-1.5 rounded-lg hover:bg-red-50 transition">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Add/Edit Modal ──────────────────────────────────────── */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-5">
                <h3 className="text-lg font-semibold text-gray-800">
                  {editingId ? 'Edit Package' : 'Add Package'}
                </h3>
                <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 text-xl leading-none">
                  &times;
                </button>
              </div>

              {formError && <p className="text-red-600 text-sm mb-4">{formError}</p>}

              <form onSubmit={handleSave} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Package Name *</label>
                  <input type="text" value={form.name}
                    onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
                    placeholder="e.g. Basic, Standard, Premium"
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    required />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea value={form.description}
                    onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
                    placeholder="What's included in this package..."
                    rows={2}
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500" />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Monthly Add-on Price (INR) *</label>
                  <input type="number" min="0" step="1" value={form.monthlyPriceRupees}
                    onChange={(e) => setForm(f => ({ ...f, monthlyPriceRupees: e.target.value }))}
                    placeholder="0 for free / basic package"
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    required />
                  <p className="text-xs text-gray-400 mt-1">Use 0 for a &quot;Bed Only&quot; basic package</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">What&apos;s Included</label>
                  <div className="grid grid-cols-2 gap-3">
                    {INCLUDES_FLAGS.map((flag) => (
                      <label key={flag.key}
                        className={`flex items-center gap-2 p-3 border rounded-lg cursor-pointer transition ${
                          form[flag.key] ? 'border-orange-500 bg-orange-50' : 'border-gray-200 hover:bg-gray-50'
                        }`}>
                        <input type="checkbox" checked={form[flag.key]}
                          onChange={(e) => setForm(f => ({ ...f, [flag.key]: e.target.checked }))}
                          className="rounded border-gray-300 text-orange-500 focus:ring-orange-500" />
                        <span className="text-sm">{flag.icon} {flag.label}</span>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={closeModal}
                    className="flex-1 border border-gray-300 text-gray-600 font-semibold px-4 py-2.5 rounded-xl text-sm hover:bg-gray-50 transition">
                    Cancel
                  </button>
                  <button type="submit" disabled={saving}
                    className="flex-1 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2.5 rounded-xl text-sm transition disabled:opacity-50">
                    {saving ? 'Saving...' : editingId ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
