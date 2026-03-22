import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────── */

type RuleType = 'SEASONAL' | 'WEEKEND' | 'EARLY_BIRD' | 'LAST_MINUTE' | 'LONG_STAY';
type AdjustmentType = 'PERCENTAGE' | 'FIXED';

interface PricingRule {
  id: string;
  ruleType: RuleType;
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  minimumNights?: number;
  daysBeforeCheckin?: number;
  active: boolean;
}

interface AiSuggestion {
  date: string;
  suggestedPricePaise: number;
  currentPricePaise: number;
  reason: string;
  accepted?: boolean;
  dismissed?: boolean;
}

/* ── Constants ─────────────────────────────────────────────── */

const RULE_TYPES: { key: RuleType; label: string }[] = [
  { key: 'SEASONAL', label: 'Seasonal' },
  { key: 'WEEKEND', label: 'Weekend' },
  { key: 'EARLY_BIRD', label: 'Early Bird' },
  { key: 'LAST_MINUTE', label: 'Last Minute' },
  { key: 'LONG_STAY', label: 'Long Stay' },
];

const RULE_BADGE_COLORS: Record<RuleType, { bg: string; text: string }> = {
  SEASONAL:    { bg: '#fef3c7', text: '#92400e' },
  WEEKEND:     { bg: '#dbeafe', text: '#1e3a8a' },
  EARLY_BIRD:  { bg: '#dcfce7', text: '#14532d' },
  LAST_MINUTE: { bg: '#fee2e2', text: '#7f1d1d' },
  LONG_STAY:   { bg: '#f3e8ff', text: '#581c87' },
};

/* ── Helpers ───────────────────────────────────────────────── */

function formatAmount(paise: number): string {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

function formatDate(iso: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatAdjustment(type: AdjustmentType, value: number): string {
  if (type === 'PERCENTAGE') {
    return value > 0 ? `+${value}%` : `${value}%`;
  }
  const sign = value > 0 ? '+' : '';
  return `${sign}\u20B9${Math.abs(value / 100).toLocaleString('en-IN')}`;
}

function todayStr(): string {
  return new Date().toISOString().split('T')[0];
}

function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + days);
  return d.toISOString().split('T')[0];
}

/* ── Component ─────────────────────────────────────────────── */

export default function HostPricingScreen() {
  const router = useRouter();

  /* ── Listings ────────────────────────────────────────────── */
  const [listings, setListings] = useState<any[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);

  /* ── Rules ───────────────────────────────────────────────── */
  const [rules, setRules] = useState<PricingRule[]>([]);

  /* ── AI suggestions ──────────────────────────────────────── */
  const [suggestions, setSuggestions] = useState<AiSuggestion[]>([]);
  const [aiLoading, setAiLoading] = useState(false);

  /* ── Modal state ─────────────────────────────────────────── */
  const [modalVisible, setModalVisible] = useState(false);
  const [formRuleType, setFormRuleType] = useState<RuleType>('SEASONAL');
  const [formName, setFormName] = useState('');
  const [formStartDate, setFormStartDate] = useState('');
  const [formEndDate, setFormEndDate] = useState('');
  const [formAdjType, setFormAdjType] = useState<AdjustmentType>('PERCENTAGE');
  const [formAdjValue, setFormAdjValue] = useState('');
  const [formMinNights, setFormMinNights] = useState('');
  const [formDaysBefore, setFormDaysBefore] = useState('');
  const [saving, setSaving] = useState(false);

  /* ── Loading ─────────────────────────────────────────────── */
  const [loading, setLoading] = useState(true);
  const [rulesLoading, setRulesLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);

  /* ── Load listings ───────────────────────────────────────── */

  const loadListings = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    try {
      const data = await api.getMyListings(token);
      setListings(data);
      if (data.length > 0 && !selectedListingId) {
        setSelectedListingId(data[0].id);
      }
    } catch {
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadListings();
  }, [loadListings]);

  /* ── Load rules when listing changes ─────────────────────── */

  const loadRules = useCallback(async () => {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;
    setRulesLoading(true);
    try {
      const data = await api.getPricingRules(selectedListingId, token);
      setRules(Array.isArray(data) ? data : []);
    } catch {
      setRules([]);
    } finally {
      setRulesLoading(false);
    }
  }, [selectedListingId]);

  useEffect(() => {
    loadRules();
    setSuggestions([]);
  }, [loadRules]);

  /* ── Refresh ─────────────────────────────────────────────── */

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadRules();
    setRefreshing(false);
  }, [loadRules]);

  /* ── Delete rule ─────────────────────────────────────────── */

  async function handleDeleteRule(ruleId: string) {
    Alert.alert('Delete Rule', 'Are you sure you want to delete this pricing rule?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          if (!selectedListingId) return;
          const token = await getAccessToken();
          if (!token) return;
          try {
            await api.deletePricingRule(selectedListingId, ruleId, token);
            setRules((prev) => prev.filter((r) => r.id !== ruleId));
          } catch {
            Alert.alert('Error', 'Failed to delete rule');
          }
        },
      },
    ]);
  }

  /* ── Create rule ─────────────────────────────────────────── */

  function openModal() {
    setFormRuleType('SEASONAL');
    setFormName('');
    setFormStartDate('');
    setFormEndDate('');
    setFormAdjType('PERCENTAGE');
    setFormAdjValue('');
    setFormMinNights('');
    setFormDaysBefore('');
    setModalVisible(true);
  }

  async function handleCreateRule() {
    if (!selectedListingId || !formName.trim() || !formAdjValue.trim()) {
      Alert.alert('Missing fields', 'Please fill in name and adjustment value.');
      return;
    }

    const token = await getAccessToken();
    if (!token) return;

    const payload: any = {
      ruleType: formRuleType,
      name: formName.trim(),
      adjustmentType: formAdjType,
      adjustmentValue: Number(formAdjValue),
    };

    if (formRuleType === 'SEASONAL') {
      if (!formStartDate.trim() || !formEndDate.trim()) {
        Alert.alert('Missing dates', 'Seasonal rules require start and end dates (YYYY-MM-DD).');
        return;
      }
      payload.startDate = formStartDate.trim();
      payload.endDate = formEndDate.trim();
    }

    if (formRuleType === 'LONG_STAY' && formMinNights.trim()) {
      payload.minimumNights = Number(formMinNights);
    }

    if ((formRuleType === 'EARLY_BIRD' || formRuleType === 'LAST_MINUTE') && formDaysBefore.trim()) {
      payload.daysBeforeCheckin = Number(formDaysBefore);
    }

    setSaving(true);
    try {
      await api.createPricingRule(selectedListingId, payload, token);
      setModalVisible(false);
      await loadRules();
    } catch {
      Alert.alert('Error', 'Failed to create pricing rule.');
    } finally {
      setSaving(false);
    }
  }

  /* ── AI suggestions ──────────────────────────────────────── */

  async function fetchAiSuggestions() {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;

    setAiLoading(true);
    setSuggestions([]);
    try {
      const today = todayStr();
      const dates = Array.from({ length: 7 }, (_, i) => addDays(today, i));
      const results: AiSuggestion[] = [];

      for (const date of dates) {
        try {
          const data = await api.aiPricingSuggest(selectedListingId, date, token);
          results.push({ ...data, date, accepted: false, dismissed: false });
        } catch {
          // skip failed dates
        }
      }

      setSuggestions(results);
    } catch {
      Alert.alert('Error', 'Failed to get AI suggestions.');
    } finally {
      setAiLoading(false);
    }
  }

  function acceptSuggestion(index: number) {
    setSuggestions((prev) =>
      prev.map((s, i) => (i === index ? { ...s, accepted: true, dismissed: false } : s)),
    );
  }

  function dismissSuggestion(index: number) {
    setSuggestions((prev) =>
      prev.map((s, i) => (i === index ? { ...s, dismissed: true, accepted: false } : s)),
    );
  }

  /* ── Selected listing label ──────────────────────────────── */

  const selectedListing = listings.find((l) => l.id === selectedListingId);
  const selectedLabel = selectedListing?.title || 'Select listing';

  /* ── Auth gate ───────────────────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyTitle}>Sign in required</Text>
        <Text style={styles.emptySubtitle}>Please sign in to manage pricing rules.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={styles.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  /* ── Render ──────────────────────────────────────────────── */

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backBtnText}>{'\u2190'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Pricing Rules</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView
        style={styles.body}
        contentContainerStyle={styles.bodyContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#f97316']} />}
      >
        {/* ── Listing Selector ───────────────────────────────── */}
        <Text style={styles.sectionLabel}>Listing</Text>
        <TouchableOpacity style={styles.dropdown} onPress={() => setShowDropdown(!showDropdown)}>
          <Text style={styles.dropdownText} numberOfLines={1}>{selectedLabel}</Text>
          <Text style={styles.dropdownArrow}>{showDropdown ? '\u25B2' : '\u25BC'}</Text>
        </TouchableOpacity>

        {showDropdown && (
          <View style={styles.dropdownList}>
            {listings.map((l) => (
              <TouchableOpacity
                key={l.id}
                style={[styles.dropdownItem, l.id === selectedListingId && styles.dropdownItemActive]}
                onPress={() => {
                  setSelectedListingId(l.id);
                  setShowDropdown(false);
                }}
              >
                <Text
                  style={[styles.dropdownItemText, l.id === selectedListingId && styles.dropdownItemTextActive]}
                  numberOfLines={1}
                >
                  {l.title}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}

        {listings.length === 0 && (
          <View style={styles.emptyBox}>
            <Text style={styles.emptyTitle}>No listings yet</Text>
            <Text style={styles.emptySubtitle}>Create a listing first to set pricing rules.</Text>
          </View>
        )}

        {/* ── Active Rules ───────────────────────────────────── */}
        {selectedListingId && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Active Rules</Text>
              <TouchableOpacity style={styles.addBtn} onPress={openModal}>
                <Text style={styles.addBtnText}>+ Add Rule</Text>
              </TouchableOpacity>
            </View>

            {rulesLoading ? (
              <ActivityIndicator size="small" color="#f97316" style={{ marginVertical: 20 }} />
            ) : rules.length === 0 ? (
              <View style={styles.emptyBox}>
                <Text style={styles.emptySubtitle}>No pricing rules for this listing.</Text>
              </View>
            ) : (
              rules.map((rule) => {
                const badge = RULE_BADGE_COLORS[rule.ruleType];
                return (
                  <View key={rule.id} style={styles.ruleCard}>
                    <View style={styles.ruleTopRow}>
                      <View style={[styles.ruleBadge, { backgroundColor: badge.bg }]}>
                        <Text style={[styles.ruleBadgeText, { color: badge.text }]}>
                          {rule.ruleType.replace('_', ' ')}
                        </Text>
                      </View>
                      <TouchableOpacity onPress={() => handleDeleteRule(rule.id)} style={styles.deleteBtn}>
                        <Text style={styles.deleteBtnText}>{'\u2715'}</Text>
                      </TouchableOpacity>
                    </View>

                    <Text style={styles.ruleName}>{rule.name}</Text>
                    {rule.description ? (
                      <Text style={styles.ruleDesc}>{rule.description}</Text>
                    ) : null}

                    <View style={styles.ruleDetails}>
                      {rule.startDate && rule.endDate && (
                        <Text style={styles.ruleDetail}>
                          {formatDate(rule.startDate)} - {formatDate(rule.endDate)}
                        </Text>
                      )}

                      <Text style={styles.ruleAdjustment}>
                        {formatAdjustment(rule.adjustmentType, rule.adjustmentValue)}
                      </Text>

                      {rule.minimumNights != null && (
                        <Text style={styles.ruleDetail}>Min {rule.minimumNights} nights</Text>
                      )}
                      {rule.daysBeforeCheckin != null && (
                        <Text style={styles.ruleDetail}>{rule.daysBeforeCheckin} days before check-in</Text>
                      )}
                    </View>
                  </View>
                );
              })
            )}

            {/* ── AI Pricing Suggestions ─────────────────────── */}
            <View style={styles.aiSection}>
              <Text style={styles.sectionTitle}>AI Pricing Suggestions</Text>
              <Text style={styles.aiSubtitle}>
                Get AI-powered price recommendations for the next 7 days.
              </Text>

              <TouchableOpacity
                style={[styles.primaryBtn, aiLoading && styles.disabledBtn]}
                onPress={fetchAiSuggestions}
                disabled={aiLoading}
              >
                {aiLoading ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.primaryBtnText}>Get AI Suggestions</Text>
                )}
              </TouchableOpacity>

              {suggestions.map((s, idx) => {
                if (s.dismissed) return null;
                return (
                  <View
                    key={s.date}
                    style={[styles.suggestionCard, s.accepted && styles.suggestionAccepted]}
                  >
                    <View style={styles.suggestionTop}>
                      <Text style={styles.suggestionDate}>{formatDate(s.date)}</Text>
                      {s.accepted && (
                        <View style={styles.acceptedBadge}>
                          <Text style={styles.acceptedBadgeText}>Accepted</Text>
                        </View>
                      )}
                    </View>

                    <View style={styles.suggestionPrices}>
                      <View style={styles.priceCol}>
                        <Text style={styles.priceLabel}>Current</Text>
                        <Text style={styles.priceValue}>{formatAmount(s.currentPricePaise)}</Text>
                      </View>
                      <Text style={styles.priceArrow}>{'\u2192'}</Text>
                      <View style={styles.priceCol}>
                        <Text style={styles.priceLabel}>Suggested</Text>
                        <Text style={[styles.priceValue, styles.suggestedPrice]}>
                          {formatAmount(s.suggestedPricePaise)}
                        </Text>
                      </View>
                    </View>

                    {s.reason ? <Text style={styles.suggestionReason}>{s.reason}</Text> : null}

                    {!s.accepted && (
                      <View style={styles.suggestionActions}>
                        <TouchableOpacity
                          style={styles.acceptBtn}
                          onPress={() => acceptSuggestion(idx)}
                        >
                          <Text style={styles.acceptBtnText}>Accept</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          style={styles.dismissBtn}
                          onPress={() => dismissSuggestion(idx)}
                        >
                          <Text style={styles.dismissBtnText}>Dismiss</Text>
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                );
              })}
            </View>
          </>
        )}
      </ScrollView>

      {/* ── Add Rule Modal ─────────────────────────────────── */}
      <Modal visible={modalVisible} animationType="slide" transparent>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <ScrollView showsVerticalScrollIndicator={false}>
              <View style={styles.modalHeader}>
                <Text style={styles.modalTitle}>Add Pricing Rule</Text>
                <TouchableOpacity onPress={() => setModalVisible(false)}>
                  <Text style={styles.modalClose}>{'\u2715'}</Text>
                </TouchableOpacity>
              </View>

              {/* Rule type chips */}
              <Text style={styles.fieldLabel}>Rule Type</Text>
              <View style={styles.chipRow}>
                {RULE_TYPES.map((rt) => (
                  <TouchableOpacity
                    key={rt.key}
                    style={[styles.chip, formRuleType === rt.key && styles.chipActive]}
                    onPress={() => setFormRuleType(rt.key)}
                  >
                    <Text style={[styles.chipText, formRuleType === rt.key && styles.chipTextActive]}>
                      {rt.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Name */}
              <Text style={styles.fieldLabel}>Name</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g., Summer Sale"
                placeholderTextColor="#9ca3af"
                value={formName}
                onChangeText={setFormName}
              />

              {/* Dates (SEASONAL) */}
              {formRuleType === 'SEASONAL' && (
                <>
                  <Text style={styles.fieldLabel}>Start Date (YYYY-MM-DD)</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="2026-04-01"
                    placeholderTextColor="#9ca3af"
                    value={formStartDate}
                    onChangeText={setFormStartDate}
                  />
                  <Text style={styles.fieldLabel}>End Date (YYYY-MM-DD)</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="2026-06-30"
                    placeholderTextColor="#9ca3af"
                    value={formEndDate}
                    onChangeText={setFormEndDate}
                  />
                </>
              )}

              {/* Adjustment type */}
              <Text style={styles.fieldLabel}>Adjustment Type</Text>
              <View style={styles.chipRow}>
                <TouchableOpacity
                  style={[styles.chip, formAdjType === 'PERCENTAGE' && styles.chipActive]}
                  onPress={() => setFormAdjType('PERCENTAGE')}
                >
                  <Text style={[styles.chipText, formAdjType === 'PERCENTAGE' && styles.chipTextActive]}>
                    Percentage
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.chip, formAdjType === 'FIXED' && styles.chipActive]}
                  onPress={() => setFormAdjType('FIXED')}
                >
                  <Text style={[styles.chipText, formAdjType === 'FIXED' && styles.chipTextActive]}>
                    Fixed (paise)
                  </Text>
                </TouchableOpacity>
              </View>

              {/* Adjustment value */}
              <Text style={styles.fieldLabel}>
                Adjustment Value{' '}
                {formAdjType === 'PERCENTAGE' ? '(e.g., -10 for 10% discount)' : '(e.g., 50000 for +\u20B9500)'}
              </Text>
              <TextInput
                style={styles.input}
                placeholder={formAdjType === 'PERCENTAGE' ? '-10' : '50000'}
                placeholderTextColor="#9ca3af"
                value={formAdjValue}
                onChangeText={setFormAdjValue}
                keyboardType="number-pad"
              />

              {/* Min nights (LONG_STAY) */}
              {formRuleType === 'LONG_STAY' && (
                <>
                  <Text style={styles.fieldLabel}>Minimum Nights</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="7"
                    placeholderTextColor="#9ca3af"
                    value={formMinNights}
                    onChangeText={setFormMinNights}
                    keyboardType="number-pad"
                  />
                </>
              )}

              {/* Days before check-in (EARLY_BIRD / LAST_MINUTE) */}
              {(formRuleType === 'EARLY_BIRD' || formRuleType === 'LAST_MINUTE') && (
                <>
                  <Text style={styles.fieldLabel}>Days Before Check-in</Text>
                  <TextInput
                    style={styles.input}
                    placeholder={formRuleType === 'EARLY_BIRD' ? '30' : '3'}
                    placeholderTextColor="#9ca3af"
                    value={formDaysBefore}
                    onChangeText={setFormDaysBefore}
                    keyboardType="number-pad"
                  />
                </>
              )}

              {/* Submit */}
              <TouchableOpacity
                style={[styles.primaryBtn, styles.modalSubmitBtn, saving && styles.disabledBtn]}
                onPress={handleCreateRule}
                disabled={saving}
              >
                {saving ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.primaryBtnText}>Create Rule</Text>
                )}
              </TouchableOpacity>
            </ScrollView>
          </View>
        </View>
      </Modal>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f9fafb', padding: 24 },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#f97316',
    paddingTop: 52,
    paddingBottom: 14,
    paddingHorizontal: 16,
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backBtnText: { fontSize: 22, color: '#fff', fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#fff' },

  /* Body */
  body: { flex: 1 },
  bodyContent: { padding: 16, paddingBottom: 40 },

  /* Section */
  sectionLabel: { fontSize: 13, fontWeight: '600', color: '#6b7280', marginBottom: 6, textTransform: 'uppercase' },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 24, marginBottom: 12 },
  sectionTitle: { fontSize: 17, fontWeight: '700', color: '#111827' },

  /* Dropdown */
  dropdown: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  dropdownText: { fontSize: 15, color: '#111827', flex: 1 },
  dropdownArrow: { fontSize: 12, color: '#6b7280', marginLeft: 8 },
  dropdownList: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    marginTop: 4,
    overflow: 'hidden',
  },
  dropdownItem: { paddingHorizontal: 14, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  dropdownItemActive: { backgroundColor: '#fff7ed' },
  dropdownItemText: { fontSize: 15, color: '#374151' },
  dropdownItemTextActive: { color: '#f97316', fontWeight: '600' },

  /* Empty */
  emptyBox: { alignItems: 'center', paddingVertical: 32 },
  emptyTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 6 },
  emptySubtitle: { fontSize: 14, color: '#6b7280', textAlign: 'center' },

  /* Add button */
  addBtn: { backgroundColor: '#f97316', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 8 },
  addBtnText: { color: '#fff', fontSize: 14, fontWeight: '600' },

  /* Rule card */
  ruleCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  ruleTopRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  ruleBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 6 },
  ruleBadgeText: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase' },
  deleteBtn: { width: 32, height: 32, justifyContent: 'center', alignItems: 'center' },
  deleteBtnText: { fontSize: 16, color: '#ef4444', fontWeight: '600' },
  ruleName: { fontSize: 15, fontWeight: '600', color: '#111827', marginBottom: 4 },
  ruleDesc: { fontSize: 13, color: '#6b7280', marginBottom: 8 },
  ruleDetails: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginTop: 4 },
  ruleDetail: { fontSize: 13, color: '#6b7280' },
  ruleAdjustment: { fontSize: 14, fontWeight: '700', color: '#f97316' },

  /* AI section */
  aiSection: { marginTop: 32, paddingTop: 20, borderTopWidth: 1, borderTopColor: '#e5e7eb' },
  aiSubtitle: { fontSize: 13, color: '#6b7280', marginTop: 4, marginBottom: 16 },

  /* Suggestion card */
  suggestionCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    marginTop: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  suggestionAccepted: { borderColor: '#22c55e', backgroundColor: '#f0fdf4' },
  suggestionTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  suggestionDate: { fontSize: 14, fontWeight: '600', color: '#111827' },
  acceptedBadge: { backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6 },
  acceptedBadgeText: { fontSize: 11, fontWeight: '700', color: '#14532d' },
  suggestionPrices: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  priceCol: { alignItems: 'center' },
  priceLabel: { fontSize: 11, color: '#6b7280', marginBottom: 2 },
  priceValue: { fontSize: 16, fontWeight: '700', color: '#374151' },
  suggestedPrice: { color: '#f97316' },
  priceArrow: { fontSize: 18, color: '#9ca3af', marginHorizontal: 16 },
  suggestionReason: { fontSize: 12, color: '#6b7280', fontStyle: 'italic', marginBottom: 10 },
  suggestionActions: { flexDirection: 'row', gap: 10 },
  acceptBtn: { flex: 1, backgroundColor: '#f97316', paddingVertical: 8, borderRadius: 8, alignItems: 'center' },
  acceptBtnText: { color: '#fff', fontSize: 14, fontWeight: '600' },
  dismissBtn: { flex: 1, backgroundColor: '#f3f4f6', paddingVertical: 8, borderRadius: 8, alignItems: 'center' },
  dismissBtnText: { color: '#374151', fontSize: 14, fontWeight: '600' },

  /* Primary button */
  primaryBtn: { backgroundColor: '#f97316', paddingVertical: 14, borderRadius: 10, alignItems: 'center' },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  disabledBtn: { opacity: 0.6 },

  /* Modal */
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    maxHeight: '85%',
  },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  modalClose: { fontSize: 20, color: '#6b7280', padding: 4 },
  modalSubmitBtn: { marginTop: 24, marginBottom: 12 },

  /* Form */
  fieldLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 16, marginBottom: 6 },
  input: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: '#111827',
  },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  chipActive: { backgroundColor: '#fff7ed', borderColor: '#f97316' },
  chipText: { fontSize: 13, color: '#374151', fontWeight: '500' },
  chipTextActive: { color: '#f97316', fontWeight: '700' },
});
