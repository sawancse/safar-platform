import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  RefreshControl,
  Modal,
  TextInput,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────────── */

interface Listing {
  id: string;
  title: string;
}

interface RoomType {
  id: string;
  name: string;
  maxOccupancy: number;
  numberOfRooms: number;
  sharingType: string;
}

interface PgPackage {
  id: string;
  name: string;
  pricePaise: number;
}

interface Tenant {
  id: string;
  name: string;
  phone: string;
  email?: string;
  roomTypeId: string;
  roomTypeName?: string;
  bedNumber?: number;
  packageId?: string;
  packageName?: string;
  moveInDate: string;
  leaseEndDate?: string;
  monthlyRentPaise: number;
  securityDepositPaise?: number;
  emergencyContact?: string;
  status: 'ACTIVE' | 'NOTICE_PERIOD' | 'VACATED';
}

interface TenantFormData {
  name: string;
  phone: string;
  email: string;
  roomTypeId: string;
  packageId: string;
  moveInDate: string;
  leaseDurationMonths: string;
  monthlyRent: string;
  securityDeposit: string;
  emergencyContact: string;
}

/* ── Constants ─────────────────────────────────────────────────── */

const EMPTY_FORM: TenantFormData = {
  name: '',
  phone: '',
  email: '',
  roomTypeId: '',
  packageId: '',
  moveInDate: '',
  leaseDurationMonths: '',
  monthlyRent: '',
  securityDeposit: '',
  emergencyContact: '',
};

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  ACTIVE:        { bg: '#dcfce7', text: '#14532d' },
  NOTICE_PERIOD: { bg: '#fef9c3', text: '#854d0e' },
  VACATED:       { bg: '#f3f4f6', text: '#374151' },
};

function formatPrice(paise: number): string {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

function formatDate(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/* ── Component ─────────────────────────────────────────────────── */

export default function HostTenantsScreen() {
  const router = useRouter();

  /* ── State ─────────────────────────────────────────────── */
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [listings, setListings] = useState<Listing[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showListingPicker, setShowListingPicker] = useState(false);

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [packages, setPackages] = useState<PgPackage[]>([]);

  // Tenant modal
  const [showTenantModal, setShowTenantModal] = useState(false);
  const [editingTenantId, setEditingTenantId] = useState<string | null>(null);
  const [form, setForm] = useState<TenantFormData>(EMPTY_FORM);

  // Room/Package picker modals inside form
  const [showRoomPicker, setShowRoomPicker] = useState(false);
  const [showPackagePicker, setShowPackagePicker] = useState(false);

  /* ── Data loading ──────────────────────────────────────── */

  const loadListings = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setLoading(false); return; }
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
  }, [selectedListingId]);

  const loadTenants = useCallback(async () => {
    if (!selectedListingId) { setTenants([]); return; }
    const token = await getAccessToken();
    if (!token) return;
    try {
      const data = await api.getTenants(selectedListingId, token);
      setTenants(data);
    } catch {
      setTenants([]);
    }
  }, [selectedListingId]);

  const loadRoomTypes = useCallback(async () => {
    if (!selectedListingId) { setRoomTypes([]); return; }
    const token = await getAccessToken();
    if (!token) return;
    try {
      const data = await api.getRoomTypes(selectedListingId, token);
      setRoomTypes(data);
    } catch {
      setRoomTypes([]);
    }
  }, [selectedListingId]);

  const loadPackages = useCallback(async () => {
    if (!selectedListingId) { setPackages([]); return; }
    const token = await getAccessToken();
    if (!token) return;
    try {
      const data = await api.getPgPackages(selectedListingId, token);
      setPackages(data);
    } catch {
      setPackages([]);
    }
  }, [selectedListingId]);

  const loadAll = useCallback(async () => {
    await Promise.all([loadTenants(), loadRoomTypes(), loadPackages()]);
  }, [loadTenants, loadRoomTypes, loadPackages]);

  useEffect(() => { loadListings(); }, [loadListings]);
  useEffect(() => { if (selectedListingId) loadAll(); }, [selectedListingId, loadAll]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadAll();
    setRefreshing(false);
  }, [loadAll]);

  /* ── Summary calculations ──────────────────────────────── */

  const activeTenants = tenants.filter((t) => t.status === 'ACTIVE' || t.status === 'NOTICE_PERIOD');
  const totalBeds = roomTypes.reduce((sum, rt) => sum + rt.maxOccupancy * rt.numberOfRooms, 0);
  const occupiedBeds = activeTenants.length;
  const occupancyPct = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0;

  /* ── Tenant CRUD ────────────────────────────────────────── */

  const openAddTenant = () => {
    setEditingTenantId(null);
    setForm(EMPTY_FORM);
    setShowTenantModal(true);
  };

  const openEditTenant = (t: Tenant) => {
    setEditingTenantId(t.id);
    setForm({
      name: t.name,
      phone: t.phone,
      email: t.email || '',
      roomTypeId: t.roomTypeId || '',
      packageId: t.packageId || '',
      moveInDate: t.moveInDate ? t.moveInDate.split('T')[0] : '',
      leaseDurationMonths: '',
      monthlyRent: String(t.monthlyRentPaise / 100),
      securityDeposit: t.securityDepositPaise ? String(t.securityDepositPaise / 100) : '',
      emergencyContact: t.emergencyContact || '',
    });
    setShowTenantModal(true);
  };

  const saveTenant = async () => {
    if (!selectedListingId) return;
    if (!form.name.trim()) { Alert.alert('Error', 'Name is required'); return; }
    if (!form.phone.trim()) { Alert.alert('Error', 'Phone is required'); return; }
    if (!form.roomTypeId) { Alert.alert('Error', 'Room assignment is required'); return; }
    if (!form.monthlyRent || Number(form.monthlyRent) <= 0) {
      Alert.alert('Error', 'Monthly rent must be greater than 0');
      return;
    }

    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(true);

    const payload = {
      name: form.name.trim(),
      phone: form.phone.trim(),
      email: form.email.trim() || null,
      roomTypeId: form.roomTypeId,
      packageId: form.packageId || null,
      moveInDate: form.moveInDate || null,
      leaseDurationMonths: form.leaseDurationMonths ? Number(form.leaseDurationMonths) : null,
      monthlyRentPaise: Math.round(Number(form.monthlyRent) * 100),
      securityDepositPaise: form.securityDeposit ? Math.round(Number(form.securityDeposit) * 100) : null,
      emergencyContact: form.emergencyContact.trim() || null,
    };

    try {
      if (editingTenantId) {
        await api.updateTenant(selectedListingId, editingTenantId, payload, token);
      } else {
        await api.addTenant(selectedListingId, payload, token);
      }
      setShowTenantModal(false);
      await loadTenants();
    } catch (err: any) {
      Alert.alert('Error', err?.response?.data?.detail || 'Failed to save tenant');
    } finally {
      setActionLoading(false);
    }
  };

  const removeTenant = (t: Tenant) => {
    Alert.alert(
      'Remove Tenant',
      `Remove "${t.name}" from this property?\n\nThis will mark the tenant as vacated with today as the vacate date.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: async () => {
            if (!selectedListingId) return;
            const token = await getAccessToken();
            if (!token) return;
            try {
              await api.removeTenant(selectedListingId, t.id, token);
              await loadTenants();
            } catch (err: any) {
              Alert.alert('Error', err?.response?.data?.detail || 'Failed to remove tenant');
            }
          },
        },
      ],
    );
  };

  /* ── Helpers ────────────────────────────────────────────── */

  const selectedListing = listings.find((l) => l.id === selectedListingId);
  const getRoomName = (id: string) => roomTypes.find((r) => r.id === id)?.name || 'Unknown Room';
  const getPackageName = (id?: string) => {
    if (!id) return null;
    return packages.find((p) => p.id === id)?.name || null;
  };

  /* ── Loading / Auth gates ───────────────────────────────── */

  if (loading) {
    return (
      <View style={s.centered}>
        <ActivityIndicator size="large" color="#f97316" />
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

  /* ── Main render ─────────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>PG Tenants</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Listing Selector */}
      <TouchableOpacity style={s.selector} onPress={() => setShowListingPicker(true)}>
        <Text style={s.selectorLabel}>Listing</Text>
        <Text style={s.selectorValue} numberOfLines={1}>
          {selectedListing?.title || 'Select a listing'}
        </Text>
        <Text style={s.selectorArrow}>▼</Text>
      </TouchableOpacity>

      {/* Listing Picker Modal */}
      <Modal visible={showListingPicker} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.pickerModal}>
            <Text style={s.modalTitle}>Select Listing</Text>
            <ScrollView style={{ maxHeight: 400 }}>
              {listings.map((l) => (
                <TouchableOpacity
                  key={l.id}
                  style={[s.pickerItem, l.id === selectedListingId && s.pickerItemActive]}
                  onPress={() => {
                    setSelectedListingId(l.id);
                    setShowListingPicker(false);
                  }}
                >
                  <Text
                    style={[s.pickerItemText, l.id === selectedListingId && s.pickerItemTextActive]}
                    numberOfLines={1}
                  >
                    {l.title}
                  </Text>
                </TouchableOpacity>
              ))}
              {listings.length === 0 && (
                <Text style={s.emptyText}>No listings found</Text>
              )}
            </ScrollView>
            <TouchableOpacity style={s.cancelBtn} onPress={() => setShowListingPicker(false)}>
              <Text style={s.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Tenant Summary */}
      {selectedListingId && (
        <View style={s.summaryCard}>
          <View style={s.summaryRow}>
            <View style={s.summaryItem}>
              <Text style={s.summaryValue}>{occupiedBeds} / {totalBeds}</Text>
              <Text style={s.summaryLabel}>Beds Occupied</Text>
            </View>
            <View style={s.summaryItem}>
              <Text style={s.summaryValue}>{activeTenants.length}</Text>
              <Text style={s.summaryLabel}>Active Tenants</Text>
            </View>
            <View style={s.summaryItem}>
              <Text style={[s.summaryValue, { color: '#f97316' }]}>{occupancyPct}%</Text>
              <Text style={s.summaryLabel}>Occupancy</Text>
            </View>
          </View>
          {/* Occupancy bar */}
          <View style={s.occupancyBarBg}>
            <View style={[s.occupancyBarFill, { width: `${Math.min(occupancyPct, 100)}%` }]} />
          </View>
        </View>
      )}

      {/* Add Tenant Button */}
      {selectedListingId && (
        <TouchableOpacity style={s.addBar} onPress={openAddTenant}>
          <Text style={s.addBarText}>+ Add Tenant</Text>
        </TouchableOpacity>
      )}

      {/* Tenant List */}
      <ScrollView
        style={s.list}
        contentContainerStyle={s.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
      >
        {!selectedListingId && (
          <Text style={s.emptyText}>Select a listing to manage tenants</Text>
        )}
        {selectedListingId && tenants.length === 0 && (
          <Text style={s.emptyText}>No tenants yet. Add one above.</Text>
        )}
        {tenants.map((t) => {
          const statusColor = STATUS_COLORS[t.status] || STATUS_COLORS.ACTIVE;
          const pkgName = t.packageName || getPackageName(t.packageId);
          return (
            <View key={t.id} style={s.card}>
              {/* Name + Status */}
              <View style={s.cardHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={s.cardName}>{t.name}</Text>
                  <Text style={s.cardPhone}>{t.phone}</Text>
                  {t.email ? <Text style={s.cardEmail}>{t.email}</Text> : null}
                </View>
                <View style={[s.statusBadge, { backgroundColor: statusColor.bg }]}>
                  <Text style={[s.statusText, { color: statusColor.text }]}>
                    {t.status.replace('_', ' ')}
                  </Text>
                </View>
              </View>

              {/* Details */}
              <View style={s.detailsSection}>
                <View style={s.detailRow}>
                  <Text style={s.detailLabel}>Room</Text>
                  <Text style={s.detailValue}>
                    {t.roomTypeName || getRoomName(t.roomTypeId)}
                    {t.bedNumber ? ` — Bed ${t.bedNumber}` : ''}
                  </Text>
                </View>
                {pkgName && (
                  <View style={s.detailRow}>
                    <Text style={s.detailLabel}>Package</Text>
                    <Text style={s.detailValue}>{pkgName}</Text>
                  </View>
                )}
                <View style={s.detailRow}>
                  <Text style={s.detailLabel}>Move-in</Text>
                  <Text style={s.detailValue}>{formatDate(t.moveInDate)}</Text>
                </View>
                {t.leaseEndDate && (
                  <View style={s.detailRow}>
                    <Text style={s.detailLabel}>Lease End</Text>
                    <Text style={s.detailValue}>{formatDate(t.leaseEndDate)}</Text>
                  </View>
                )}
                <View style={s.detailRow}>
                  <Text style={s.detailLabel}>Rent</Text>
                  <Text style={s.rentValue}>{formatPrice(t.monthlyRentPaise)}/mo</Text>
                </View>
              </View>

              {/* Penalty Config */}
              <View style={[s.detailRow, { marginTop: 6 }]}>
                <Text style={[s.detailLabel, { fontSize: 11, color: '#9ca3af' }]}>
                  Grace: {(t as any).gracePeriodDays ?? 5}d · Penalty: {(((t as any).latePenaltyBps ?? 200) / 100).toFixed(1)}%/day · Cap: {(t as any).maxPenaltyPercent ?? 25}%
                </Text>
              </View>

              {/* Actions */}
              {t.status !== 'VACATED' && (
                <View style={s.cardActions}>
                  <TouchableOpacity style={s.editBtn} onPress={() => openEditTenant(t)}>
                    <Text style={s.editBtnText}>Edit</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={s.removeBtn} onPress={() => removeTenant(t)}>
                    <Text style={s.removeBtnText}>Remove</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          );
        })}
        <View style={{ height: 100 }} />
      </ScrollView>

      {/* ── Add/Edit Tenant Modal ─────────────────────────── */}
      <Modal visible={showTenantModal} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.formModal}>
            <ScrollView showsVerticalScrollIndicator={false}>
              <Text style={s.modalTitle}>
                {editingTenantId ? 'Edit Tenant' : 'Add Tenant'}
              </Text>

              <Text style={s.label}>Name *</Text>
              <TextInput
                style={s.input}
                placeholder="Tenant full name"
                placeholderTextColor="#9ca3af"
                value={form.name}
                onChangeText={(v) => setForm((p) => ({ ...p, name: v }))}
              />

              <Text style={s.label}>Phone *</Text>
              <TextInput
                style={s.input}
                placeholder="+91 98765 43210"
                placeholderTextColor="#9ca3af"
                keyboardType="phone-pad"
                value={form.phone}
                onChangeText={(v) => setForm((p) => ({ ...p, phone: v }))}
              />

              <Text style={s.label}>Email</Text>
              <TextInput
                style={s.input}
                placeholder="tenant@email.com"
                placeholderTextColor="#9ca3af"
                keyboardType="email-address"
                autoCapitalize="none"
                value={form.email}
                onChangeText={(v) => setForm((p) => ({ ...p, email: v }))}
              />

              {/* Room Assignment */}
              <Text style={s.label}>Room Assignment *</Text>
              <TouchableOpacity
                style={s.pickerInput}
                onPress={() => setShowRoomPicker(true)}
              >
                <Text style={form.roomTypeId ? s.pickerInputValue : s.pickerInputPlaceholder}>
                  {form.roomTypeId
                    ? roomTypes.find((r) => r.id === form.roomTypeId)?.name || 'Select room'
                    : 'Select room'}
                </Text>
                <Text style={s.selectorArrow}>▼</Text>
              </TouchableOpacity>

              {/* Package Selection */}
              <Text style={s.label}>Package</Text>
              <TouchableOpacity
                style={s.pickerInput}
                onPress={() => setShowPackagePicker(true)}
              >
                <Text style={form.packageId ? s.pickerInputValue : s.pickerInputPlaceholder}>
                  {form.packageId
                    ? packages.find((p) => p.id === form.packageId)?.name || 'Select package'
                    : 'None'}
                </Text>
                <Text style={s.selectorArrow}>▼</Text>
              </TouchableOpacity>

              <Text style={s.label}>Move-in Date</Text>
              <TextInput
                style={s.input}
                placeholder="YYYY-MM-DD"
                placeholderTextColor="#9ca3af"
                value={form.moveInDate}
                onChangeText={(v) => setForm((p) => ({ ...p, moveInDate: v }))}
              />

              <Text style={s.label}>Lease Duration (months)</Text>
              <TextInput
                style={s.input}
                placeholder="e.g. 6"
                placeholderTextColor="#9ca3af"
                keyboardType="number-pad"
                value={form.leaseDurationMonths}
                onChangeText={(v) => setForm((p) => ({ ...p, leaseDurationMonths: v }))}
              />

              <View style={s.row}>
                <View style={s.halfField}>
                  <Text style={s.label}>Monthly Rent (₹) *</Text>
                  <TextInput
                    style={s.input}
                    placeholder="8000"
                    placeholderTextColor="#9ca3af"
                    keyboardType="decimal-pad"
                    value={form.monthlyRent}
                    onChangeText={(v) => setForm((p) => ({ ...p, monthlyRent: v }))}
                  />
                </View>
                <View style={s.halfField}>
                  <Text style={s.label}>Security Deposit (₹)</Text>
                  <TextInput
                    style={s.input}
                    placeholder="16000"
                    placeholderTextColor="#9ca3af"
                    keyboardType="decimal-pad"
                    value={form.securityDeposit}
                    onChangeText={(v) => setForm((p) => ({ ...p, securityDeposit: v }))}
                  />
                </View>
              </View>

              <Text style={s.label}>Emergency Contact</Text>
              <TextInput
                style={s.input}
                placeholder="+91 98765 43210"
                placeholderTextColor="#9ca3af"
                keyboardType="phone-pad"
                value={form.emergencyContact}
                onChangeText={(v) => setForm((p) => ({ ...p, emergencyContact: v }))}
              />

              <View style={s.modalActions}>
                <TouchableOpacity
                  style={s.cancelBtn}
                  onPress={() => setShowTenantModal(false)}
                >
                  <Text style={s.cancelBtnText}>Cancel</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[s.primaryBtn, actionLoading && { opacity: 0.6 }]}
                  onPress={saveTenant}
                  disabled={actionLoading}
                >
                  {actionLoading ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={s.primaryBtnText}>
                      {editingTenantId ? 'Update' : 'Add'}
                    </Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* ── Room Picker Modal (inside form) ───────────────── */}
      <Modal visible={showRoomPicker} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.pickerModal}>
            <Text style={s.modalTitle}>Select Room</Text>
            <ScrollView style={{ maxHeight: 400 }}>
              {roomTypes.map((rt) => (
                <TouchableOpacity
                  key={rt.id}
                  style={[s.pickerItem, rt.id === form.roomTypeId && s.pickerItemActive]}
                  onPress={() => {
                    setForm((p) => ({ ...p, roomTypeId: rt.id }));
                    setShowRoomPicker(false);
                  }}
                >
                  <Text
                    style={[s.pickerItemText, rt.id === form.roomTypeId && s.pickerItemTextActive]}
                  >
                    {rt.name}
                  </Text>
                  <Text style={s.pickerItemSub}>
                    {rt.sharingType} — Max {rt.maxOccupancy} — {rt.numberOfRooms} rooms
                  </Text>
                </TouchableOpacity>
              ))}
              {roomTypes.length === 0 && (
                <Text style={s.emptyText}>No rooms configured for this listing</Text>
              )}
            </ScrollView>
            <TouchableOpacity style={s.cancelBtn} onPress={() => setShowRoomPicker(false)}>
              <Text style={s.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* ── Package Picker Modal (inside form) ────────────── */}
      <Modal visible={showPackagePicker} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.pickerModal}>
            <Text style={s.modalTitle}>Select Package</Text>
            <ScrollView style={{ maxHeight: 400 }}>
              <TouchableOpacity
                style={[s.pickerItem, !form.packageId && s.pickerItemActive]}
                onPress={() => {
                  setForm((p) => ({ ...p, packageId: '' }));
                  setShowPackagePicker(false);
                }}
              >
                <Text style={[s.pickerItemText, !form.packageId && s.pickerItemTextActive]}>
                  None
                </Text>
              </TouchableOpacity>
              {packages.map((pkg) => (
                <TouchableOpacity
                  key={pkg.id}
                  style={[s.pickerItem, pkg.id === form.packageId && s.pickerItemActive]}
                  onPress={() => {
                    setForm((p) => ({ ...p, packageId: pkg.id }));
                    setShowPackagePicker(false);
                  }}
                >
                  <Text
                    style={[s.pickerItemText, pkg.id === form.packageId && s.pickerItemTextActive]}
                  >
                    {pkg.name}
                  </Text>
                  <Text style={s.pickerItemSub}>{formatPrice(pkg.pricePaise)}/mo</Text>
                </TouchableOpacity>
              ))}
              {packages.length === 0 && (
                <Text style={s.emptyText}>No packages configured for this listing</Text>
              )}
            </ScrollView>
            <TouchableOpacity style={s.cancelBtn} onPress={() => setShowPackagePicker(false)}>
              <Text style={s.cancelBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────────── */

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f9fafb' },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: Platform.OS === 'ios' ? 56 : 16,
    paddingHorizontal: 16,
    paddingBottom: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 24, color: '#f97316', fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },

  /* Listing selector */
  selector: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 16,
    marginTop: 12,
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  selectorLabel: { fontSize: 13, color: '#6b7280', marginRight: 8 },
  selectorValue: { flex: 1, fontSize: 15, fontWeight: '600', color: '#111827' },
  selectorArrow: { fontSize: 12, color: '#6b7280' },

  /* Summary card */
  summaryCard: {
    marginHorizontal: 16,
    marginTop: 12,
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  summaryItem: { alignItems: 'center', flex: 1 },
  summaryValue: { fontSize: 22, fontWeight: '800', color: '#111827' },
  summaryLabel: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  occupancyBarBg: {
    height: 8,
    backgroundColor: '#f3f4f6',
    borderRadius: 4,
    overflow: 'hidden',
  },
  occupancyBarFill: {
    height: 8,
    backgroundColor: '#f97316',
    borderRadius: 4,
  },

  /* Add bar */
  addBar: {
    marginHorizontal: 16,
    marginTop: 12,
    backgroundColor: '#f97316',
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  addBarText: { color: '#fff', fontSize: 15, fontWeight: '700' },

  /* List */
  list: { flex: 1, marginTop: 8 },
  listContent: { paddingHorizontal: 16, paddingTop: 4 },

  /* Card */
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cardName: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 2 },
  cardPhone: { fontSize: 14, color: '#374151' },
  cardEmail: { fontSize: 13, color: '#6b7280', marginTop: 1 },

  /* Status badge */
  statusBadge: {
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
    marginLeft: 8,
  },
  statusText: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase' },

  /* Details */
  detailsSection: { marginTop: 12, gap: 6 },
  detailRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  detailLabel: { fontSize: 13, color: '#6b7280' },
  detailValue: { fontSize: 14, fontWeight: '500', color: '#111827' },
  rentValue: { fontSize: 16, fontWeight: '800', color: '#f97316' },

  /* Card actions */
  cardActions: { flexDirection: 'row', marginTop: 14, gap: 10 },
  editBtn: {
    backgroundColor: '#f97316',
    borderRadius: 8,
    paddingHorizontal: 20,
    paddingVertical: 9,
  },
  editBtnText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  removeBtn: {
    backgroundColor: '#fee2e2',
    borderRadius: 8,
    paddingHorizontal: 20,
    paddingVertical: 9,
  },
  removeBtnText: { color: '#dc2626', fontSize: 13, fontWeight: '600' },

  /* Modals */
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  pickerModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    maxHeight: '60%',
  },
  formModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    maxHeight: '90%',
  },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  pickerItem: {
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderRadius: 10,
    marginBottom: 4,
  },
  pickerItemActive: { backgroundColor: '#fff7ed' },
  pickerItemText: { fontSize: 15, color: '#374151' },
  pickerItemTextActive: { color: '#f97316', fontWeight: '600' },
  pickerItemSub: { fontSize: 12, color: '#9ca3af', marginTop: 2 },

  /* Form */
  label: { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 12, marginBottom: 4 },
  input: {
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 15,
    color: '#111827',
  },
  pickerInput: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  pickerInputValue: { flex: 1, fontSize: 15, color: '#111827' },
  pickerInputPlaceholder: { flex: 1, fontSize: 15, color: '#9ca3af' },
  row: { flexDirection: 'row', gap: 12 },
  halfField: { flex: 1 },

  /* Buttons */
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 20, marginBottom: 10 },
  primaryBtn: {
    backgroundColor: '#f97316',
    borderRadius: 10,
    paddingHorizontal: 24,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  cancelBtn: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 20,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelBtnText: { color: '#374151', fontSize: 15, fontWeight: '600' },

  /* Empty states */
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 12 },
  emptyText: { fontSize: 14, color: '#9ca3af', textAlign: 'center', marginTop: 40 },
});
