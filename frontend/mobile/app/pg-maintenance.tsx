import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  Modal,
  TextInput,
  FlatList,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────────── */

interface MaintenanceRequest {
  id: string;
  requestNumber: string;
  category: string;
  title: string;
  description: string;
  priority: string;
  status: string;
  assignedTo?: string;
  resolvedAt?: string;
  tenantRating?: number;
  createdAt: string;
}

interface NewRequestForm {
  category: string;
  title: string;
  description: string;
  priority: string;
}

/* ── Constants ─────────────────────────────────────────────────── */

const EMPTY_FORM: NewRequestForm = {
  category: '',
  title: '',
  description: '',
  priority: 'MEDIUM',
};

const FILTER_TABS = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED'] as const;

const CATEGORIES = [
  'PLUMBING',
  'ELECTRICAL',
  'FURNITURE',
  'APPLIANCE',
  'CLEANING',
  'PEST_CONTROL',
  'PAINTING',
  'CARPENTRY',
  'AC_REPAIR',
  'OTHER',
] as const;

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;

const CATEGORY_CONFIG: Record<string, { icon: string; bg: string; text: string }> = {
  PLUMBING:     { icon: '\uD83D\uDEB0', bg: '#dbeafe', text: '#1e40af' },
  ELECTRICAL:   { icon: '\u26A1',       bg: '#fef9c3', text: '#854d0e' },
  FURNITURE:    { icon: '\uD83E\uDE91', bg: '#f3e8ff', text: '#6b21a8' },
  APPLIANCE:    { icon: '\uD83C\uDF00', bg: '#e0e7ff', text: '#3730a3' },
  CLEANING:     { icon: '\uD83E\uDDF9', bg: '#d1fae5', text: '#065f46' },
  PEST_CONTROL: { icon: '\uD83D\uDC1B', bg: '#fee2e2', text: '#991b1b' },
  PAINTING:     { icon: '\uD83C\uDFA8', bg: '#fce7f3', text: '#9d174d' },
  CARPENTRY:    { icon: '\uD83D\uDD28', bg: '#ffedd5', text: '#9a3412' },
  AC_REPAIR:    { icon: '\u2744\uFE0F', bg: '#cffafe', text: '#155e75' },
  OTHER:        { icon: '\uD83D\uDD27', bg: '#f3f4f6', text: '#374151' },
};

const PRIORITY_COLORS: Record<string, { bg: string; text: string }> = {
  URGENT: { bg: '#fee2e2', text: '#dc2626' },
  HIGH:   { bg: '#ffedd5', text: '#ea580c' },
  MEDIUM: { bg: '#fef9c3', text: '#ca8a04' },
  LOW:    { bg: '#f3f4f6', text: '#6b7280' },
};

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  OPEN:        { bg: '#dbeafe', text: '#1e40af' },
  IN_PROGRESS: { bg: '#fef9c3', text: '#854d0e' },
  RESOLVED:    { bg: '#dcfce7', text: '#14532d' },
  CLOSED:      { bg: '#f3f4f6', text: '#374151' },
  CANCELLED:   { bg: '#fee2e2', text: '#991b1b' },
};

function formatDate(iso: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/* ── Component ─────────────────────────────────────────────────── */

export default function PgMaintenanceScreen() {
  const router = useRouter();

  /* ── State ─────────────────────────────────────────────── */
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [tenancyId, setTenancyId] = useState<string | null>(null);
  const [requests, setRequests] = useState<MaintenanceRequest[]>([]);
  const [activeFilter, setActiveFilter] = useState<string>('ALL');

  // New request modal
  const [showNewModal, setShowNewModal] = useState(false);
  const [form, setForm] = useState<NewRequestForm>(EMPTY_FORM);
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);
  const [showPriorityPicker, setShowPriorityPicker] = useState(false);

  // Rate modal
  const [showRateModal, setShowRateModal] = useState(false);
  const [ratingRequestId, setRatingRequestId] = useState<string | null>(null);
  const [ratingValue, setRatingValue] = useState(5);
  const [ratingFeedback, setRatingFeedback] = useState('');

  /* ── Data loading ──────────────────────────────────────── */

  const loadDashboard = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setLoading(false); return; }
    setAuthed(true);
    try {
      const dashboard = await api.getTenantDashboard(token);
      if (dashboard?.tenancyId) {
        setTenancyId(dashboard.tenancyId);
      } else if (dashboard?.id) {
        setTenancyId(dashboard.id);
      }
    } catch {
      setTenancyId(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadRequests = useCallback(async () => {
    if (!tenancyId) { setRequests([]); return; }
    const token = await getAccessToken();
    if (!token) return;
    try {
      const statusParam = activeFilter === 'ALL' ? undefined : activeFilter;
      const data = await api.getMaintenanceRequests(tenancyId, token, statusParam);
      setRequests(data?.content ?? (Array.isArray(data) ? data : []));
    } catch {
      setRequests([]);
    }
  }, [tenancyId, activeFilter]);

  useEffect(() => { loadDashboard(); }, [loadDashboard]);
  useEffect(() => { if (tenancyId) loadRequests(); }, [tenancyId, loadRequests]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadRequests();
    setRefreshing(false);
  }, [loadRequests]);

  /* ── Create Request ────────────────────────────────────── */

  const openNewRequest = () => {
    setForm(EMPTY_FORM);
    setShowNewModal(true);
  };

  const submitNewRequest = async () => {
    if (!tenancyId) return;
    if (!form.category) { Alert.alert('Error', 'Please select a category'); return; }
    if (!form.title.trim()) { Alert.alert('Error', 'Title is required'); return; }
    if (!form.description.trim()) { Alert.alert('Error', 'Description is required'); return; }

    const token = await getAccessToken();
    if (!token) return;
    setSubmitting(true);

    try {
      await api.createMaintenanceRequest(tenancyId, {
        category: form.category,
        title: form.title.trim(),
        description: form.description.trim(),
        priority: form.priority,
      }, token);
      setShowNewModal(false);
      await loadRequests();
      Alert.alert('Success', 'Maintenance request submitted');
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Failed to submit request');
    } finally {
      setSubmitting(false);
    }
  };

  /* ── Rate Request ──────────────────────────────────────── */

  const openRateModal = (requestId: string) => {
    setRatingRequestId(requestId);
    setRatingValue(5);
    setRatingFeedback('');
    setShowRateModal(true);
  };

  const submitRating = async () => {
    if (!tenancyId || !ratingRequestId) return;
    const token = await getAccessToken();
    if (!token) return;
    setSubmitting(true);

    try {
      await api.rateMaintenanceRequest(tenancyId, ratingRequestId, ratingValue, ratingFeedback.trim(), token);
      setShowRateModal(false);
      await loadRequests();
      Alert.alert('Thank you', 'Your rating has been submitted');
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Failed to submit rating');
    } finally {
      setSubmitting(false);
    }
  };

  /* ── Filtered list ─────────────────────────────────────── */

  const filteredRequests = requests;

  /* ── Loading / Auth gates ───────────────────────────────── */

  if (loading) {
    return (
      <View style={s.centered}>
        <ActivityIndicator size="large" color="#F97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={s.centered}>
        <Text style={s.emptyTitle}>Sign in required</Text>
        <TouchableOpacity style={s.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={s.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (!tenancyId) {
    return (
      <View style={s.container}>
        <View style={s.header}>
          <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
            <Text style={s.backText}>{'<'}</Text>
          </TouchableOpacity>
          <Text style={s.headerTitle}>Maintenance Requests</Text>
          <View style={{ width: 40 }} />
        </View>
        <View style={s.centered}>
          <Text style={s.emptyIcon}>{'\uD83C\uDFE0'}</Text>
          <Text style={s.emptyTitle}>No active tenancy</Text>
          <Text style={s.emptySubtitle}>You need an active PG tenancy to submit maintenance requests.</Text>
        </View>
      </View>
    );
  }

  /* ── Render helpers ────────────────────────────────────── */

  const renderStars = (count: number, interactive: boolean, onSelect?: (n: number) => void) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <TouchableOpacity
          key={i}
          disabled={!interactive}
          onPress={() => onSelect?.(i)}
          style={s.starTouch}
        >
          <Text style={[s.star, i <= count ? s.starFilled : s.starEmpty]}>
            {'\u2605'}
          </Text>
        </TouchableOpacity>
      );
    }
    return <View style={s.starsRow}>{stars}</View>;
  };

  const renderRequestCard = ({ item }: { item: MaintenanceRequest }) => {
    const catConfig = CATEGORY_CONFIG[item.category] || CATEGORY_CONFIG.OTHER;
    const prioColor = PRIORITY_COLORS[item.priority] || PRIORITY_COLORS.MEDIUM;
    const statusColor = STATUS_COLORS[item.status] || STATUS_COLORS.OPEN;
    const isResolved = item.status === 'RESOLVED';
    const canRate = isResolved && !item.tenantRating;

    return (
      <TouchableOpacity
        style={s.card}
        activeOpacity={canRate ? 0.7 : 1}
        onPress={() => canRate && openRateModal(item.id)}
      >
        {/* Top row: category + priority */}
        <View style={s.cardTopRow}>
          <View style={[s.categoryBadge, { backgroundColor: catConfig.bg }]}>
            <Text style={s.categoryIcon}>{catConfig.icon}</Text>
            <Text style={[s.categoryText, { color: catConfig.text }]}>
              {item.category.replace(/_/g, ' ')}
            </Text>
          </View>
          <View style={[s.priorityBadge, { backgroundColor: prioColor.bg }]}>
            <Text style={[s.priorityText, { color: prioColor.text }]}>
              {item.priority}
            </Text>
          </View>
        </View>

        {/* Title + description */}
        <Text style={s.cardTitle} numberOfLines={1}>{item.title}</Text>
        <Text style={s.cardDescription} numberOfLines={2}>{item.description}</Text>

        {/* Bottom row: status + date */}
        <View style={s.cardBottomRow}>
          <View style={[s.statusBadge, { backgroundColor: statusColor.bg }]}>
            <Text style={[s.statusText, { color: statusColor.text }]}>
              {item.status.replace(/_/g, ' ')}
            </Text>
          </View>
          {item.requestNumber ? (
            <Text style={s.requestNumber}>#{item.requestNumber}</Text>
          ) : null}
          <Text style={s.cardDate}>{formatDate(item.createdAt)}</Text>
        </View>

        {/* Assigned to */}
        {item.assignedTo ? (
          <Text style={s.assignedText}>Assigned to: {item.assignedTo}</Text>
        ) : null}

        {/* Resolved date */}
        {item.resolvedAt ? (
          <Text style={s.resolvedText}>Resolved: {formatDate(item.resolvedAt)}</Text>
        ) : null}

        {/* Rating display or prompt */}
        {item.tenantRating ? (
          <View style={s.ratingRow}>
            <Text style={s.ratingLabel}>Your rating:</Text>
            {renderStars(item.tenantRating, false)}
          </View>
        ) : canRate ? (
          <View style={s.rateCta}>
            <Text style={s.rateCtaText}>Tap to rate resolution</Text>
          </View>
        ) : null}
      </TouchableOpacity>
    );
  };

  /* ── Main render ─────────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backText}>{'<'}</Text>
        </TouchableOpacity>
        <View style={s.headerCenter}>
          <Text style={s.headerTitle}>Maintenance Requests</Text>
          {requests.length > 0 && (
            <View style={s.countBadge}>
              <Text style={s.countBadgeText}>{requests.length}</Text>
            </View>
          )}
        </View>
        <View style={{ width: 40 }} />
      </View>

      {/* Filter chips */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={s.filterBar}
        contentContainerStyle={s.filterBarContent}
      >
        {FILTER_TABS.map((tab) => {
          const isActive = activeFilter === tab;
          return (
            <TouchableOpacity
              key={tab}
              style={[s.filterChip, isActive && s.filterChipActive]}
              onPress={() => setActiveFilter(tab)}
            >
              <Text style={[s.filterChipText, isActive && s.filterChipTextActive]}>
                {tab.replace(/_/g, ' ')}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>

      {/* Request list */}
      <FlatList
        data={filteredRequests}
        keyExtractor={(item) => item.id}
        renderItem={renderRequestCard}
        contentContainerStyle={s.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#F97316" />
        }
        ListEmptyComponent={
          <View style={s.emptyContainer}>
            <Text style={s.emptyIcon}>{'\uD83D\uDD27'}</Text>
            <Text style={s.emptyTitle}>No maintenance requests yet</Text>
            <Text style={s.emptySubtitle}>
              Tap the + button to report an issue with your accommodation.
            </Text>
          </View>
        }
      />

      {/* FAB */}
      <TouchableOpacity style={s.fab} onPress={openNewRequest} activeOpacity={0.8}>
        <Text style={s.fabText}>+</Text>
      </TouchableOpacity>

      {/* ── New Request Modal ─────────────────────────────── */}
      <Modal visible={showNewModal} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.formModal}>
            <ScrollView showsVerticalScrollIndicator={false}>
              <Text style={s.modalTitle}>New Maintenance Request</Text>

              {/* Category */}
              <Text style={s.label}>Category *</Text>
              <TouchableOpacity
                style={s.pickerInput}
                onPress={() => setShowCategoryPicker(true)}
              >
                <Text style={form.category ? s.pickerInputValue : s.pickerInputPlaceholder}>
                  {form.category
                    ? `${(CATEGORY_CONFIG[form.category] || CATEGORY_CONFIG.OTHER).icon}  ${form.category.replace(/_/g, ' ')}`
                    : 'Select category'}
                </Text>
                <Text style={s.selectorArrow}>{'\u25BC'}</Text>
              </TouchableOpacity>

              {/* Title */}
              <Text style={s.label}>Title *</Text>
              <TextInput
                style={s.input}
                placeholder="Brief summary of the issue"
                placeholderTextColor="#9ca3af"
                value={form.title}
                onChangeText={(v) => setForm((p) => ({ ...p, title: v }))}
              />

              {/* Description */}
              <Text style={s.label}>Description *</Text>
              <TextInput
                style={[s.input, s.textArea]}
                placeholder="Describe the issue in detail..."
                placeholderTextColor="#9ca3af"
                multiline
                numberOfLines={4}
                textAlignVertical="top"
                value={form.description}
                onChangeText={(v) => setForm((p) => ({ ...p, description: v }))}
              />

              {/* Priority */}
              <Text style={s.label}>Priority</Text>
              <TouchableOpacity
                style={s.pickerInput}
                onPress={() => setShowPriorityPicker(true)}
              >
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  {form.priority ? (
                    <View style={[s.priorityDot, { backgroundColor: (PRIORITY_COLORS[form.priority] || PRIORITY_COLORS.MEDIUM).text }]} />
                  ) : null}
                  <Text style={s.pickerInputValue}>
                    {form.priority || 'Select priority'}
                  </Text>
                </View>
                <Text style={s.selectorArrow}>{'\u25BC'}</Text>
              </TouchableOpacity>

              {/* Actions */}
              <View style={s.modalActions}>
                <TouchableOpacity
                  style={s.cancelBtn}
                  onPress={() => setShowNewModal(false)}
                >
                  <Text style={s.cancelBtnText}>Cancel</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[s.submitBtn, submitting && s.submitBtnDisabled]}
                  onPress={submitNewRequest}
                  disabled={submitting}
                >
                  {submitting ? (
                    <ActivityIndicator size="small" color="#fff" />
                  ) : (
                    <Text style={s.submitBtnText}>Submit Request</Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* ── Category Picker Modal ─────────────────────────── */}
      <Modal visible={showCategoryPicker} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.pickerModal}>
            <Text style={s.modalTitle}>Select Category</Text>
            <ScrollView style={{ maxHeight: 400 }}>
              {CATEGORIES.map((cat) => {
                const cfg = CATEGORY_CONFIG[cat] || CATEGORY_CONFIG.OTHER;
                const isSelected = form.category === cat;
                return (
                  <TouchableOpacity
                    key={cat}
                    style={[s.pickerItem, isSelected && s.pickerItemActive]}
                    onPress={() => {
                      setForm((p) => ({ ...p, category: cat }));
                      setShowCategoryPicker(false);
                    }}
                  >
                    <Text style={s.pickerItemIcon}>{cfg.icon}</Text>
                    <Text style={[s.pickerItemText, isSelected && s.pickerItemTextActive]}>
                      {cat.replace(/_/g, ' ')}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
            <TouchableOpacity style={s.cancelBtn} onPress={() => setShowCategoryPicker(false)}>
              <Text style={s.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* ── Priority Picker Modal ─────────────────────────── */}
      <Modal visible={showPriorityPicker} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.pickerModal}>
            <Text style={s.modalTitle}>Select Priority</Text>
            {PRIORITIES.map((prio) => {
              const color = PRIORITY_COLORS[prio];
              const isSelected = form.priority === prio;
              return (
                <TouchableOpacity
                  key={prio}
                  style={[s.pickerItem, isSelected && s.pickerItemActive]}
                  onPress={() => {
                    setForm((p) => ({ ...p, priority: prio }));
                    setShowPriorityPicker(false);
                  }}
                >
                  <View style={[s.priorityDot, { backgroundColor: color.text }]} />
                  <Text style={[s.pickerItemText, isSelected && s.pickerItemTextActive]}>
                    {prio}
                  </Text>
                </TouchableOpacity>
              );
            })}
            <TouchableOpacity style={s.cancelBtn} onPress={() => setShowPriorityPicker(false)}>
              <Text style={s.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* ── Rate Modal ────────────────────────────────────── */}
      <Modal visible={showRateModal} transparent animationType="fade">
        <View style={s.modalOverlay}>
          <View style={s.rateModal}>
            <Text style={s.modalTitle}>Rate Resolution</Text>
            <Text style={s.rateSubtitle}>How was the maintenance handled?</Text>

            {renderStars(ratingValue, true, setRatingValue)}

            <Text style={s.rateValueLabel}>
              {ratingValue === 1 ? 'Poor' :
               ratingValue === 2 ? 'Below Average' :
               ratingValue === 3 ? 'Average' :
               ratingValue === 4 ? 'Good' : 'Excellent'}
            </Text>

            <Text style={s.label}>Feedback (optional)</Text>
            <TextInput
              style={[s.input, s.textArea]}
              placeholder="Any additional comments..."
              placeholderTextColor="#9ca3af"
              multiline
              numberOfLines={3}
              textAlignVertical="top"
              value={ratingFeedback}
              onChangeText={setRatingFeedback}
            />

            <View style={s.modalActions}>
              <TouchableOpacity
                style={s.cancelBtn}
                onPress={() => setShowRateModal(false)}
              >
                <Text style={s.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[s.submitBtn, submitting && s.submitBtnDisabled]}
                onPress={submitRating}
                disabled={submitting}
              >
                {submitting ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={s.submitBtnText}>Submit Rating</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

/* ── Styles ─────────────────────────────────────────────────────── */

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 54,
    paddingBottom: 14,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 22, fontWeight: '600', color: '#111827' },
  headerCenter: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  countBadge: {
    marginLeft: 8,
    backgroundColor: '#F97316',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 2,
    minWidth: 24,
    alignItems: 'center',
  },
  countBadgeText: { fontSize: 12, fontWeight: '700', color: '#fff' },

  /* Filter bar */
  filterBar: { backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  filterBarContent: { paddingHorizontal: 12, paddingVertical: 10, gap: 8, flexDirection: 'row' },
  filterChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
    marginRight: 8,
  },
  filterChipActive: { backgroundColor: '#F97316' },
  filterChipText: { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  filterChipTextActive: { color: '#fff' },

  /* List */
  listContent: { padding: 16, paddingBottom: 100 },

  /* Card */
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  cardTopRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  categoryBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
  },
  categoryIcon: { fontSize: 14, marginRight: 6 },
  categoryText: { fontSize: 12, fontWeight: '700', textTransform: 'capitalize' },
  priorityBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  priorityText: { fontSize: 11, fontWeight: '700' },
  priorityDot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  cardTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 4 },
  cardDescription: { fontSize: 13, color: '#6b7280', lineHeight: 18, marginBottom: 10 },
  cardBottomRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  statusText: { fontSize: 11, fontWeight: '700' },
  requestNumber: { fontSize: 12, color: '#9ca3af', fontWeight: '600' },
  cardDate: { fontSize: 12, color: '#9ca3af', marginLeft: 'auto' },
  assignedText: { fontSize: 12, color: '#6b7280', marginTop: 8 },
  resolvedText: { fontSize: 12, color: '#14532d', marginTop: 4 },

  /* Rating display */
  ratingRow: { flexDirection: 'row', alignItems: 'center', marginTop: 10 },
  ratingLabel: { fontSize: 12, color: '#6b7280', marginRight: 6 },
  starsRow: { flexDirection: 'row', alignItems: 'center' },
  starTouch: { padding: 2 },
  star: { fontSize: 20 },
  starFilled: { color: '#F97316' },
  starEmpty: { color: '#d1d5db' },
  rateCta: {
    marginTop: 10,
    paddingVertical: 8,
    backgroundColor: '#fff7ed',
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#fed7aa',
  },
  rateCtaText: { fontSize: 13, fontWeight: '600', color: '#F97316' },

  /* Empty state */
  emptyContainer: { flex: 1, alignItems: 'center', paddingTop: 80 },
  emptyIcon: { fontSize: 56, marginBottom: 16 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#374151', marginBottom: 6, textAlign: 'center' },
  emptySubtitle: { fontSize: 14, color: '#9ca3af', textAlign: 'center', lineHeight: 20, paddingHorizontal: 32 },

  /* FAB */
  fab: {
    position: 'absolute',
    bottom: 32,
    right: 20,
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: '#F97316',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#F97316',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 6,
  },
  fabText: { fontSize: 28, fontWeight: '600', color: '#fff', marginTop: -2 },

  /* Modal shared */
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  formModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    maxHeight: '85%',
  },
  pickerModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
    maxHeight: '70%',
  },
  rateModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 24,
  },
  modalTitle: { fontSize: 20, fontWeight: '700', color: '#111827', marginBottom: 20, textAlign: 'center' },
  rateSubtitle: { fontSize: 14, color: '#6b7280', textAlign: 'center', marginBottom: 20 },
  rateValueLabel: { fontSize: 15, fontWeight: '600', color: '#F97316', textAlign: 'center', marginTop: 8, marginBottom: 20 },

  /* Form */
  label: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6, marginTop: 14 },
  input: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 10,
    padding: 12,
    fontSize: 15,
    color: '#111827',
  },
  textArea: { minHeight: 100, textAlignVertical: 'top' },
  pickerInput: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 10,
    padding: 12,
  },
  pickerInputValue: { fontSize: 15, color: '#111827' },
  pickerInputPlaceholder: { fontSize: 15, color: '#9ca3af' },
  selectorArrow: { fontSize: 12, color: '#9ca3af', marginLeft: 8 },

  /* Picker items */
  pickerItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 10,
    marginBottom: 4,
  },
  pickerItemActive: { backgroundColor: '#fff7ed' },
  pickerItemIcon: { fontSize: 18, marginRight: 12 },
  pickerItemText: { fontSize: 15, color: '#374151', fontWeight: '500' },
  pickerItemTextActive: { color: '#F97316', fontWeight: '700' },

  /* Modal actions */
  modalActions: { flexDirection: 'row', gap: 12, marginTop: 24 },
  cancelBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: '#f3f4f6',
    alignItems: 'center',
  },
  cancelBtnText: { fontSize: 15, fontWeight: '600', color: '#374151' },
  submitBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: '#F97316',
    alignItems: 'center',
  },
  submitBtnDisabled: { opacity: 0.6 },
  submitBtnText: { fontSize: 15, fontWeight: '700', color: '#fff' },

  /* Auth */
  primaryBtn: {
    marginTop: 16,
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 12,
    backgroundColor: '#F97316',
  },
  primaryBtnText: { fontSize: 15, fontWeight: '700', color: '#fff' },
});
