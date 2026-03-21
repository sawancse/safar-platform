'use client';

import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Listing, PricingRule, PricingRuleType, PriceAdjustmentType } from '@/types';

const RULE_TYPE_LABELS: Record<PricingRuleType, string> = {
  SEASONAL: 'Seasonal',
  WEEKEND: 'Weekend',
  LAST_MINUTE: 'Last Minute',
  EARLY_BIRD: 'Early Bird',
};

const RULE_TYPE_COLORS: Record<PricingRuleType, string> = {
  SEASONAL: 'bg-purple-100 text-purple-700',
  WEEKEND: 'bg-blue-100 text-blue-700',
  LAST_MINUTE: 'bg-red-100 text-red-700',
  EARLY_BIRD: 'bg-green-100 text-green-700',
};

const DAYS_LABELS = [
  { key: 'MONDAY', label: 'Mon' },
  { key: 'TUESDAY', label: 'Tue' },
  { key: 'WEDNESDAY', label: 'Wed' },
  { key: 'THURSDAY', label: 'Thu' },
  { key: 'FRIDAY', label: 'Fri' },
  { key: 'SATURDAY', label: 'Sat' },
  { key: 'SUNDAY', label: 'Sun' },
];

interface Props {
  token: string;
  listings: Listing[];
}

export default function HostPricingRulesTab({ token, listings }: Props) {
  const [selectedListingId, setSelectedListingId] = useState<string>('');
  const [rules, setRules] = useState<PricingRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState('');
  const [formRuleType, setFormRuleType] = useState<PricingRuleType>('SEASONAL');
  const [formFromDate, setFormFromDate] = useState('');
  const [formToDate, setFormToDate] = useState('');
  const [formDaysOfWeek, setFormDaysOfWeek] = useState<string[]>([]);
  const [formAdjustmentType, setFormAdjustmentType] = useState<PriceAdjustmentType>('PERCENT_INCREASE');
  const [formValue, setFormValue] = useState('');
  const [formPriority, setFormPriority] = useState('10');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  // Delete state
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Preview state
  const [previewDate, setPreviewDate] = useState('');
  const [previewResult, setPreviewResult] = useState<{
    date: string;
    basePricePaise: number;
    effectivePricePaise: number;
    rulesApplied: string[];
  } | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState('');

  // Set first listing as default
  useEffect(() => {
    if (listings.length > 0 && !selectedListingId) {
      setSelectedListingId(listings[0].id);
    }
  }, [listings, selectedListingId]);

  const fetchRules = useCallback(async () => {
    if (!selectedListingId || !token) return;
    setLoading(true);
    setError('');
    try {
      const data = await api.getPricingRules(selectedListingId, token);
      setRules(data ?? []);
    } catch (e: any) {
      setError(e.message || 'Failed to load pricing rules');
    } finally {
      setLoading(false);
    }
  }, [selectedListingId, token]);

  useEffect(() => {
    fetchRules();
  }, [fetchRules]);

  function resetForm() {
    setFormName('');
    setFormRuleType('SEASONAL');
    setFormFromDate('');
    setFormToDate('');
    setFormDaysOfWeek([]);
    setFormAdjustmentType('PERCENT_INCREASE');
    setFormValue('');
    setFormPriority('10');
    setFormError('');
  }

  function toggleDay(day: string) {
    setFormDaysOfWeek(prev =>
      prev.includes(day) ? prev.filter(d => d !== day) : [...prev, day]
    );
  }

  async function handleCreateRule(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedListingId || !token) return;

    // Validate
    if (!formName.trim()) { setFormError('Name is required'); return; }
    if (!formValue.trim()) { setFormError('Adjustment value is required'); return; }
    if (formRuleType === 'SEASONAL' && (!formFromDate || !formToDate)) {
      setFormError('Date range is required for seasonal rules');
      return;
    }
    if (formRuleType === 'WEEKEND' && formDaysOfWeek.length === 0) {
      setFormError('Select at least one day for weekend rules');
      return;
    }

    setSubmitting(true);
    setFormError('');
    try {
      const adjustmentValue = formAdjustmentType === 'FIXED_PRICE'
        ? Math.round(parseFloat(formValue) * 100) // Convert INR to paise
        : parseFloat(formValue); // Percentage

      await api.createPricingRule(selectedListingId, {
        name: formName.trim(),
        ruleType: formRuleType,
        fromDate: formRuleType === 'SEASONAL' ? formFromDate : null,
        toDate: formRuleType === 'SEASONAL' ? formToDate : null,
        daysOfWeek: formRuleType === 'WEEKEND' ? formDaysOfWeek.join(',') : null,
        priceAdjustmentType: formAdjustmentType,
        adjustmentValue,
        priority: parseInt(formPriority) || 10,
        isActive: true,
      }, token);
      resetForm();
      setShowForm(false);
      await fetchRules();
    } catch (e: any) {
      setFormError(e.message || 'Failed to create rule');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteRule(ruleId: string) {
    if (!selectedListingId || !token) return;
    if (!confirm('Delete this pricing rule?')) return;
    setDeletingId(ruleId);
    try {
      await api.deletePricingRule(selectedListingId, ruleId, token);
      await fetchRules();
    } catch (e: any) {
      setError(e.message || 'Failed to delete rule');
    } finally {
      setDeletingId(null);
    }
  }

  async function handlePreview() {
    if (!selectedListingId || !token || !previewDate) return;
    setPreviewLoading(true);
    setPreviewError('');
    setPreviewResult(null);
    try {
      const result = await api.previewPricingRule(selectedListingId, previewDate, token);
      setPreviewResult(result);
    } catch (e: any) {
      setPreviewError(e.message || 'Failed to preview price');
    } finally {
      setPreviewLoading(false);
    }
  }

  function formatAdjustment(rule: PricingRule) {
    if (rule.priceAdjustmentType === 'FIXED_PRICE') {
      return formatPaise(rule.adjustmentValue);
    }
    if (rule.priceAdjustmentType === 'PERCENT_INCREASE') {
      return `+${rule.adjustmentValue}%`;
    }
    return `-${rule.adjustmentValue}%`;
  }

  function formatRuleDetails(rule: PricingRule) {
    if (rule.ruleType === 'SEASONAL' && rule.fromDate && rule.toDate) {
      const from = new Date(rule.fromDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
      const to = new Date(rule.toDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
      return `${from} - ${to}`;
    }
    if (rule.ruleType === 'WEEKEND' && rule.daysOfWeek) {
      const days = rule.daysOfWeek.split(',');
      const labels = days.map(d => {
        const found = DAYS_LABELS.find(dl => dl.key === d);
        return found ? found.label : d;
      });
      return labels.join(', ');
    }
    if (rule.ruleType === 'LAST_MINUTE') return 'Applies to last-minute bookings';
    if (rule.ruleType === 'EARLY_BIRD') return 'Applies to early bookings';
    return '';
  }

  const selectedListing = listings.find(l => l.id === selectedListingId);

  if (listings.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No listings found. Create a listing first to manage pricing rules.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Listing Selector */}
      <div className="flex items-center gap-4 flex-wrap">
        <label className="text-sm font-medium text-gray-700">Listing:</label>
        <select
          value={selectedListingId}
          onChange={e => { setSelectedListingId(e.target.value); setPreviewResult(null); }}
          className="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
        >
          {listings.map(l => (
            <option key={l.id} value={l.id}>{l.title} ({l.city})</option>
          ))}
        </select>
        {selectedListing && (
          <span className="text-sm text-gray-500">
            Base price: {formatPaise(selectedListing.basePricePaise)}/{selectedListing.pricingUnit === 'HOUR' ? 'hr' : 'night'}
          </span>
        )}
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      {/* Add Rule Button */}
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-gray-800">
          Pricing Rules {rules.length > 0 && <span className="text-gray-400 font-normal">({rules.length})</span>}
        </h3>
        <button
          onClick={() => { setShowForm(!showForm); if (showForm) resetForm(); }}
          className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
            showForm
              ? 'bg-gray-200 text-gray-600 hover:bg-gray-300'
              : 'bg-orange-500 text-white hover:bg-orange-600'
          }`}
        >
          {showForm ? 'Cancel' : '+ Add Pricing Rule'}
        </button>
      </div>

      {/* Add Rule Form */}
      {showForm && (
        <form onSubmit={handleCreateRule} className="border rounded-xl p-5 bg-orange-50 space-y-4">
          <h4 className="text-sm font-semibold text-gray-800">New Pricing Rule</h4>

          {formError && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg">{formError}</p>}

          {/* Name */}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Rule Name</label>
            <input
              type="text"
              value={formName}
              onChange={e => setFormName(e.target.value)}
              placeholder="e.g. Diwali Season Surge"
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
            />
          </div>

          {/* Rule Type */}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Rule Type</label>
            <select
              value={formRuleType}
              onChange={e => setFormRuleType(e.target.value as PricingRuleType)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
            >
              <option value="SEASONAL">Seasonal</option>
              <option value="WEEKEND">Weekend</option>
              <option value="LAST_MINUTE">Last Minute</option>
              <option value="EARLY_BIRD">Early Bird</option>
            </select>
          </div>

          {/* Date Range for SEASONAL */}
          {formRuleType === 'SEASONAL' && (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">From Date</label>
                <input
                  type="date"
                  value={formFromDate}
                  onChange={e => setFormFromDate(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">To Date</label>
                <input
                  type="date"
                  value={formToDate}
                  onChange={e => setFormToDate(e.target.value)}
                  min={formFromDate}
                  className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
            </div>
          )}

          {/* Days of Week for WEEKEND */}
          {formRuleType === 'WEEKEND' && (
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-2">Days of Week</label>
              <div className="flex gap-2 flex-wrap">
                {DAYS_LABELS.map(({ key, label }) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => toggleDay(key)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition ${
                      formDaysOfWeek.includes(key)
                        ? 'bg-orange-500 text-white border-orange-500'
                        : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Adjustment Type */}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-2">Adjustment Type</label>
            <div className="flex gap-4 flex-wrap">
              {([
                { value: 'FIXED_PRICE', label: 'Fixed Price' },
                { value: 'PERCENT_INCREASE', label: '% Increase' },
                { value: 'PERCENT_DECREASE', label: '% Decrease' },
              ] as const).map(({ value, label }) => (
                <label key={value} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="adjustmentType"
                    value={value}
                    checked={formAdjustmentType === value}
                    onChange={e => setFormAdjustmentType(e.target.value as PriceAdjustmentType)}
                    className="text-orange-500 focus:ring-orange-500"
                  />
                  <span className="text-sm text-gray-700">{label}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Value */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                {formAdjustmentType === 'FIXED_PRICE' ? 'Price (INR)' : 'Percentage (%)'}
              </label>
              <input
                type="number"
                value={formValue}
                onChange={e => setFormValue(e.target.value)}
                placeholder={formAdjustmentType === 'FIXED_PRICE' ? 'e.g. 3500' : 'e.g. 20'}
                min="0"
                step={formAdjustmentType === 'FIXED_PRICE' ? '1' : '0.1'}
                className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Priority</label>
              <input
                type="number"
                value={formPriority}
                onChange={e => setFormPriority(e.target.value)}
                placeholder="10"
                min="1"
                className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
              />
              <p className="text-[10px] text-gray-400 mt-0.5">Higher priority = applied later</p>
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-5 py-2.5 rounded-xl text-sm transition disabled:opacity-50"
          >
            {submitting ? 'Creating...' : 'Create Rule'}
          </button>
        </form>
      )}

      {/* Rules List */}
      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading pricing rules...</div>
      ) : rules.length === 0 ? (
        <div className="text-center py-12 text-gray-400 border rounded-xl bg-gray-50">
          <p className="text-sm">No pricing rules yet.</p>
          <p className="text-xs mt-1">Create rules to automatically adjust prices for seasons, weekends, and more.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {rules.map(rule => (
            <div key={rule.id} className={`border rounded-xl p-4 bg-white shadow-sm space-y-3 ${
              !rule.isActive ? 'opacity-60' : ''
            }`}>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-800">{rule.name}</span>
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${RULE_TYPE_COLORS[rule.ruleType]}`}>
                    {RULE_TYPE_LABELS[rule.ruleType]}
                  </span>
                </div>
                <button
                  onClick={() => handleDeleteRule(rule.id)}
                  disabled={deletingId === rule.id}
                  className="text-xs text-red-500 hover:text-red-700 font-medium transition disabled:opacity-50"
                >
                  {deletingId === rule.id ? '...' : 'Delete'}
                </button>
              </div>

              <div className="text-xs text-gray-500">{formatRuleDetails(rule)}</div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className={`text-sm font-bold ${
                    rule.priceAdjustmentType === 'PERCENT_DECREASE' ? 'text-green-600' :
                    rule.priceAdjustmentType === 'PERCENT_INCREASE' ? 'text-red-600' :
                    'text-gray-800'
                  }`}>
                    {formatAdjustment(rule)}
                  </span>
                  <span className="text-[10px] text-gray-400">Priority: {rule.priority}</span>
                </div>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${
                  rule.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                }`}>
                  {rule.isActive ? 'Active' : 'Inactive'}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Price Preview Section */}
      <div className="border rounded-xl p-5 bg-white shadow-sm space-y-4">
        <h3 className="text-base font-semibold text-gray-800">Price Preview</h3>
        <p className="text-xs text-gray-500">Check the effective price for a specific date after all rules are applied.</p>

        <div className="flex items-end gap-3 flex-wrap">
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Date</label>
            <input
              type="date"
              value={previewDate}
              onChange={e => setPreviewDate(e.target.value)}
              className="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
            />
          </div>
          <button
            onClick={handlePreview}
            disabled={previewLoading || !previewDate || !selectedListingId}
            className="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-lg text-sm transition disabled:opacity-50"
          >
            {previewLoading ? 'Loading...' : 'Preview'}
          </button>
        </div>

        {previewError && <p className="text-red-600 text-sm">{previewError}</p>}

        {previewResult && (
          <div className="border rounded-lg p-4 bg-gray-50 space-y-2">
            <div className="flex items-center gap-4 flex-wrap text-sm">
              <div className="text-gray-600">
                Base price: <span className="font-semibold text-gray-800">{formatPaise(previewResult.basePricePaise)}</span>
              </div>
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
              <div className={`font-bold text-lg ${
                previewResult.effectivePricePaise > previewResult.basePricePaise ? 'text-red-600' :
                previewResult.effectivePricePaise < previewResult.basePricePaise ? 'text-green-600' :
                'text-gray-800'
              }`}>
                {formatPaise(previewResult.effectivePricePaise)}
              </div>
            </div>
            {previewResult.rulesApplied && previewResult.rulesApplied.length > 0 ? (
              <div className="text-xs text-gray-500">
                Rules applied: {previewResult.rulesApplied.join(', ')}
              </div>
            ) : (
              <div className="text-xs text-gray-400">No rules applied for this date.</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
