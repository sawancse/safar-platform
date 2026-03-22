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
  Switch,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Constants ─────────────────────────────────────────────────── */

const AMENITY_KEYS = [
  { key: 'meals', label: 'Meals' },
  { key: 'laundry', label: 'Laundry' },
  { key: 'wifi', label: 'WiFi' },
  { key: 'housekeeping', label: 'Housekeeping' },
  { key: 'ac', label: 'AC' },
  { key: 'parking', label: 'Parking' },
  { key: 'gym', label: 'Gym' },
] as const;

type AmenityKey = (typeof AMENITY_KEYS)[number]['key'];

function formatPrice(paise: number) {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

/* ── Types ─────────────────────────────────────────────────────── */

interface Listing {
  id: string;
  title: string;
}

interface PgPackage {
  id: string;
  name: string;
  description?: string;
  monthlyPricePaise: number;
  securityDepositPaise: number;
  lockInPeriodMonths: number;
  meals: boolean;
  laundry: boolean;
  wifi: boolean;
  housekeeping: boolean;
  ac: boolean;
  parking: boolean;
  gym: boolean;
}

interface PackageFormData {
  name: string;
  description: string;
  monthlyPrice: string;
  securityDeposit: string;
  lockInPeriod: string;
  meals: boolean;
  laundry: boolean;
  wifi: boolean;
  housekeeping: boolean;
  ac: boolean;
  parking: boolean;
  gym: boolean;
}

const EMPTY_FORM: PackageFormData = {
  name: '',
  description: '',
  monthlyPrice: '',
  securityDeposit: '',
  lockInPeriod: '',
  meals: false,
  laundry: false,
  wifi: false,
  housekeeping: false,
  ac: false,
  parking: false,
  gym: false,
};

/* ── Component ─────────────────────────────────────────────────── */

export default function HostPackagesScreen() {
  const router = useRouter();

  /* ── State ─────────────────────────────────────────────── */
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [listings, setListings] = useState<Listing[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showListingPicker, setShowListingPicker] = useState(false);

  const [packages, setPackages] = useState<PgPackage[]>([]);

  // Package modal
  const [showModal, setShowModal] = useState(false);
  const [editingPackageId, setEditingPackageId] = useState<string | null>(null);
  const [form, setForm] = useState<PackageFormData>(EMPTY_FORM);

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

  useEffect(() => { loadListings(); }, [loadListings]);
  useEffect(() => { if (selectedListingId) loadPackages(); }, [selectedListingId, loadPackages]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadPackages();
    setRefreshing(false);
  }, [loadPackages]);

  /* ── CRUD ─────────────────────────────────────────────── */

  const openAdd = () => {
    setEditingPackageId(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  const openEdit = (pkg: PgPackage) => {
    setEditingPackageId(pkg.id);
    setForm({
      name: pkg.name,
      description: pkg.description || '',
      monthlyPrice: String(pkg.monthlyPricePaise / 100),
      securityDeposit: String(pkg.securityDepositPaise / 100),
      lockInPeriod: String(pkg.lockInPeriodMonths),
      meals: pkg.meals,
      laundry: pkg.laundry,
      wifi: pkg.wifi,
      housekeeping: pkg.housekeeping,
      ac: pkg.ac,
      parking: pkg.parking,
      gym: pkg.gym,
    });
    setShowModal(true);
  };

  const savePackage = async () => {
    if (!selectedListingId) return;
    if (!form.name.trim()) { Alert.alert('Error', 'Package name is required'); return; }
    if (!form.monthlyPrice || Number(form.monthlyPrice) <= 0) {
      Alert.alert('Error', 'Monthly price must be greater than 0');
      return;
    }

    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(true);

    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      monthlyPricePaise: Math.round(Number(form.monthlyPrice) * 100),
      securityDepositPaise: Math.round(Number(form.securityDeposit || '0') * 100),
      lockInPeriodMonths: Number(form.lockInPeriod || '0'),
      meals: form.meals,
      laundry: form.laundry,
      wifi: form.wifi,
      housekeeping: form.housekeeping,
      ac: form.ac,
      parking: form.parking,
      gym: form.gym,
    };

    try {
      if (editingPackageId) {
        await api.updatePgPackage(selectedListingId, editingPackageId, payload, token);
      } else {
        await api.createPgPackage(selectedListingId, payload, token);
      }
      setShowModal(false);
      await loadPackages();
    } catch (err: any) {
      Alert.alert('Error', err?.response?.data?.detail || 'Failed to save package');
    } finally {
      setActionLoading(false);
    }
  };

  const deletePackage = (pkg: PgPackage) => {
    Alert.alert('Delete Package', `Delete "${pkg.name}"? This cannot be undone.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          if (!selectedListingId) return;
          const token = await getAccessToken();
          if (!token) return;
          try {
            await api.deletePgPackage(selectedListingId, pkg.id, token);
            await loadPackages();
          } catch (err: any) {
            Alert.alert('Error', err?.response?.data?.detail || 'Failed to delete package');
          }
        },
      },
    ]);
  };

  /* ── Helpers ─────────────────────────────────────────────── */

  const getIncludedAmenities = (pkg: PgPackage): string[] => {
    return AMENITY_KEYS.filter((a) => pkg[a.key]).map((a) => a.label);
  };

  const selectedListing = listings.find((l) => l.id === selectedListingId);

  /* ── Render ──────────────────────────────────────────────── */

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

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>PG Packages</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Listing Selector */}
      <TouchableOpacity style={s.selector} onPress={() => setShowListingPicker(true)}>
        <Text style={s.selectorLabel}>Listing</Text>
        <Text style={s.selectorValue} numberOfLines={1}>
          {selectedListing?.title || 'Select a listing'}
        </Text>
        <Text style={s.selectorArrow}>{'\u25BC'}</Text>
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

      {/* Add Package Button */}
      {selectedListingId && (
        <TouchableOpacity style={s.addBar} onPress={openAdd}>
          <Text style={s.addBarText}>+ Add Package</Text>
        </TouchableOpacity>
      )}

      {/* Package List */}
      <ScrollView
        style={s.list}
        contentContainerStyle={s.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
      >
        {!selectedListingId && (
          <Text style={s.emptyText}>Select a listing to manage packages</Text>
        )}
        {selectedListingId && packages.length === 0 && (
          <Text style={s.emptyText}>No packages yet. Add one above.</Text>
        )}
        {packages.map((pkg) => {
          const amenities = getIncludedAmenities(pkg);
          return (
            <View key={pkg.id} style={s.card}>
              {/* Card header */}
              <View style={s.cardHeader}>
                <Text style={s.cardName}>{pkg.name}</Text>
                <Text style={s.cardPrice}>{formatPrice(pkg.monthlyPricePaise)}/mo</Text>
              </View>
              {pkg.description ? <Text style={s.cardDesc}>{pkg.description}</Text> : null}

              {/* Included amenities */}
              {amenities.length > 0 && (
                <View style={s.amenitiesRow}>
                  {amenities.map((a) => (
                    <View key={a} style={s.amenityTag}>
                      <Text style={s.amenityText}>{'\u2713'} {a}</Text>
                    </View>
                  ))}
                </View>
              )}

              {/* Meta row */}
              <View style={s.metaRow}>
                {pkg.securityDepositPaise > 0 && (
                  <View style={s.badge}>
                    <Text style={s.badgeText}>Deposit: {formatPrice(pkg.securityDepositPaise)}</Text>
                  </View>
                )}
                {pkg.lockInPeriodMonths > 0 && (
                  <View style={s.badge}>
                    <Text style={s.badgeText}>Lock-in: {pkg.lockInPeriodMonths} mo</Text>
                  </View>
                )}
              </View>

              {/* Actions */}
              <View style={s.cardActions}>
                <TouchableOpacity style={s.editBtn} onPress={() => openEdit(pkg)}>
                  <Text style={s.editBtnText}>Edit</Text>
                </TouchableOpacity>
                <TouchableOpacity style={s.deleteBtn} onPress={() => deletePackage(pkg)}>
                  <Text style={s.deleteBtnText}>Delete</Text>
                </TouchableOpacity>
              </View>
            </View>
          );
        })}
        <View style={{ height: 40 }} />
      </ScrollView>

      {/* Package Form Modal */}
      <Modal visible={showModal} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.formModal}>
            <Text style={s.modalTitle}>
              {editingPackageId ? 'Edit Package' : 'Add Package'}
            </Text>
            <ScrollView showsVerticalScrollIndicator={false}>
              {/* Name */}
              <Text style={s.label}>Package Name *</Text>
              <TextInput
                style={s.input}
                placeholder="e.g. Basic, Standard, Premium"
                placeholderTextColor="#9ca3af"
                value={form.name}
                onChangeText={(v) => setForm((p) => ({ ...p, name: v }))}
              />

              {/* Price row */}
              <View style={s.row}>
                <View style={s.halfField}>
                  <Text style={s.label}>Monthly Price ({'\u20B9'}) *</Text>
                  <TextInput
                    style={s.input}
                    placeholder="e.g. 8000"
                    placeholderTextColor="#9ca3af"
                    keyboardType="numeric"
                    value={form.monthlyPrice}
                    onChangeText={(v) => setForm((p) => ({ ...p, monthlyPrice: v }))}
                  />
                </View>
                <View style={s.halfField}>
                  <Text style={s.label}>Security Deposit ({'\u20B9'})</Text>
                  <TextInput
                    style={s.input}
                    placeholder="e.g. 10000"
                    placeholderTextColor="#9ca3af"
                    keyboardType="numeric"
                    value={form.securityDeposit}
                    onChangeText={(v) => setForm((p) => ({ ...p, securityDeposit: v }))}
                  />
                </View>
              </View>

              {/* Lock-in */}
              <Text style={s.label}>Lock-in Period (months)</Text>
              <TextInput
                style={s.input}
                placeholder="e.g. 3"
                placeholderTextColor="#9ca3af"
                keyboardType="numeric"
                value={form.lockInPeriod}
                onChangeText={(v) => setForm((p) => ({ ...p, lockInPeriod: v }))}
              />

              {/* Amenity toggles */}
              <Text style={s.label}>Included Amenities</Text>
              <View style={s.toggleSection}>
                {AMENITY_KEYS.map((a) => (
                  <View key={a.key} style={s.toggleRow}>
                    <Text style={s.toggleLabel}>{a.label}</Text>
                    <Switch
                      value={form[a.key]}
                      onValueChange={(v) => setForm((p) => ({ ...p, [a.key]: v }))}
                      trackColor={{ false: '#d1d5db', true: '#fdba74' }}
                      thumbColor={form[a.key] ? '#f97316' : '#f4f4f5'}
                    />
                  </View>
                ))}
              </View>

              {/* Description */}
              <Text style={s.label}>Description</Text>
              <TextInput
                style={[s.input, s.textarea]}
                placeholder="Package details..."
                placeholderTextColor="#9ca3af"
                multiline
                numberOfLines={3}
                textAlignVertical="top"
                value={form.description}
                onChangeText={(v) => setForm((p) => ({ ...p, description: v }))}
              />

              {/* Actions */}
              <View style={s.modalActions}>
                <TouchableOpacity style={s.cancelBtn} onPress={() => setShowModal(false)}>
                  <Text style={s.cancelBtnText}>Cancel</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[s.primaryBtn, actionLoading && { opacity: 0.6 }]}
                  onPress={savePackage}
                  disabled={actionLoading}
                >
                  {actionLoading ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={s.primaryBtnText}>
                      {editingPackageId ? 'Save' : 'Add'}
                    </Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
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
  cardDesc: { fontSize: 13, color: '#6b7280', marginBottom: 4 },
  cardPrice: { fontSize: 18, fontWeight: '800', color: '#f97316' },

  /* Meta row */
  metaRow: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', marginTop: 8, gap: 8 },
  badge: {
    backgroundColor: '#f3f4f6',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  badgeText: { fontSize: 12, fontWeight: '600', color: '#374151' },

  /* Amenities */
  amenitiesRow: { flexDirection: 'row', flexWrap: 'wrap', marginTop: 8, gap: 6 },
  amenityTag: {
    backgroundColor: '#fff7ed',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  amenityText: { fontSize: 12, color: '#ea580c' },

  /* Card actions */
  cardActions: { flexDirection: 'row', marginTop: 12, gap: 8 },
  editBtn: {
    backgroundColor: '#f97316',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  editBtnText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  deleteBtn: {
    backgroundColor: '#fee2e2',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  deleteBtnText: { color: '#dc2626', fontSize: 13, fontWeight: '600' },

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
  textarea: {
    minHeight: 80,
    paddingTop: 10,
  },
  row: { flexDirection: 'row', gap: 12 },
  halfField: { flex: 1 },

  /* Toggle section */
  toggleSection: {
    backgroundColor: '#f9fafb',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#d1d5db',
    marginTop: 4,
    overflow: 'hidden',
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  toggleLabel: { fontSize: 14, color: '#374151' },

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
  },
  cancelBtnText: { color: '#6b7280', fontSize: 15, fontWeight: '600' },

  /* Empty */
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#374151', marginBottom: 12 },
  emptyText: { fontSize: 14, color: '#9ca3af', textAlign: 'center', marginTop: 40 },
});
