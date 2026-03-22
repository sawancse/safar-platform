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

/* ── Constants ─────────────────────────────────────────────────── */

const ROOM_TYPES = ['AC', 'NON_AC'] as const;
const SHARING_TYPES = ['PRIVATE', 'DOUBLE', 'TRIPLE', 'DORMITORY'] as const;
const INCLUSION_TYPES = ['MEAL', 'ADDON', 'DISCOUNT', 'WELLNESS', 'TRANSPORT'] as const;

const INCLUSION_COLORS: Record<string, { bg: string; text: string }> = {
  MEAL:      { bg: '#fef9c3', text: '#854d0e' },
  ADDON:     { bg: '#dbeafe', text: '#1e3a8a' },
  DISCOUNT:  { bg: '#dcfce7', text: '#14532d' },
  WELLNESS:  { bg: '#fce7f3', text: '#831843' },
  TRANSPORT: { bg: '#e0e7ff', text: '#3730a3' },
};

function formatPrice(paise: number) {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

/* ── Types ─────────────────────────────────────────────────────── */

interface Listing {
  id: string;
  title: string;
}

interface RoomType {
  id: string;
  name: string;
  description?: string;
  maxOccupancy: number;
  numberOfRooms: number;
  basePricePaise: number;
  roomType: string;
  sharingType: string;
  amenities?: string[];
}

interface Inclusion {
  id: string;
  name: string;
  type: string;
  description?: string;
  pricePaise: number;
}

interface RoomFormData {
  name: string;
  description: string;
  maxOccupancy: string;
  numberOfRooms: string;
  basePrice: string;
  roomType: string;
  sharingType: string;
}

interface InclusionFormData {
  name: string;
  type: string;
  description: string;
  price: string;
}

const EMPTY_ROOM_FORM: RoomFormData = {
  name: '',
  description: '',
  maxOccupancy: '',
  numberOfRooms: '',
  basePrice: '',
  roomType: 'AC',
  sharingType: 'PRIVATE',
};

const EMPTY_INCLUSION_FORM: InclusionFormData = {
  name: '',
  type: 'MEAL',
  description: '',
  price: '',
};

/* ── Component ─────────────────────────────────────────────────── */

export default function HostRoomsScreen() {
  const router = useRouter();

  /* ── State ─────────────────────────────────────────────── */
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [listings, setListings] = useState<Listing[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showListingPicker, setShowListingPicker] = useState(false);

  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [inclusions, setInclusions] = useState<Record<string, Inclusion[]>>({});

  // Room modal
  const [showRoomModal, setShowRoomModal] = useState(false);
  const [editingRoomId, setEditingRoomId] = useState<string | null>(null);
  const [roomForm, setRoomForm] = useState<RoomFormData>(EMPTY_ROOM_FORM);

  // Inclusion modal
  const [showInclusionModal, setShowInclusionModal] = useState(false);
  const [inclusionRoomId, setInclusionRoomId] = useState<string | null>(null);
  const [inclusionForm, setInclusionForm] = useState<InclusionFormData>(EMPTY_INCLUSION_FORM);

  // Expanded cards (to show inclusions)
  const [expandedRooms, setExpandedRooms] = useState<Set<string>>(new Set());

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

  const loadRoomTypes = useCallback(async () => {
    if (!selectedListingId) { setRoomTypes([]); return; }
    const token = await getAccessToken();
    if (!token) return;
    try {
      const data = await api.getRoomTypes(selectedListingId, token);
      setRoomTypes(data);
      // Load inclusions for each room type
      const inc: Record<string, Inclusion[]> = {};
      await Promise.all(
        data.map(async (rt: RoomType) => {
          try {
            inc[rt.id] = await api.getRoomTypeInclusions(selectedListingId, rt.id, token);
          } catch {
            inc[rt.id] = [];
          }
        }),
      );
      setInclusions(inc);
    } catch {
      setRoomTypes([]);
      setInclusions({});
    }
  }, [selectedListingId]);

  useEffect(() => { loadListings(); }, [loadListings]);
  useEffect(() => { if (selectedListingId) loadRoomTypes(); }, [selectedListingId, loadRoomTypes]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadRoomTypes();
    setRefreshing(false);
  }, [loadRoomTypes]);

  /* ── Room CRUD ─────────────────────────────────────────── */

  const openAddRoom = () => {
    setEditingRoomId(null);
    setRoomForm(EMPTY_ROOM_FORM);
    setShowRoomModal(true);
  };

  const openEditRoom = (rt: RoomType) => {
    setEditingRoomId(rt.id);
    setRoomForm({
      name: rt.name,
      description: rt.description || '',
      maxOccupancy: String(rt.maxOccupancy),
      numberOfRooms: String(rt.numberOfRooms),
      basePrice: String(rt.basePricePaise / 100),
      roomType: rt.roomType,
      sharingType: rt.sharingType,
    });
    setShowRoomModal(true);
  };

  const saveRoom = async () => {
    if (!selectedListingId) return;
    if (!roomForm.name.trim()) { Alert.alert('Error', 'Name is required'); return; }
    if (!roomForm.maxOccupancy || Number(roomForm.maxOccupancy) < 1) { Alert.alert('Error', 'Max occupancy must be at least 1'); return; }
    if (!roomForm.numberOfRooms || Number(roomForm.numberOfRooms) < 1) { Alert.alert('Error', 'Number of rooms must be at least 1'); return; }
    if (!roomForm.basePrice || Number(roomForm.basePrice) <= 0) { Alert.alert('Error', 'Price must be greater than 0'); return; }

    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(true);

    const payload = {
      name: roomForm.name.trim(),
      description: roomForm.description.trim() || null,
      maxOccupancy: Number(roomForm.maxOccupancy),
      numberOfRooms: Number(roomForm.numberOfRooms),
      basePricePaise: Math.round(Number(roomForm.basePrice) * 100),
      roomType: roomForm.roomType,
      sharingType: roomForm.sharingType,
    };

    try {
      if (editingRoomId) {
        await api.updateRoomType(selectedListingId, editingRoomId, payload, token);
      } else {
        await api.createRoomType(selectedListingId, payload, token);
      }
      setShowRoomModal(false);
      await loadRoomTypes();
    } catch (err: any) {
      Alert.alert('Error', err?.response?.data?.detail || 'Failed to save room type');
    } finally {
      setActionLoading(false);
    }
  };

  const deleteRoom = (rt: RoomType) => {
    Alert.alert('Delete Room Type', `Delete "${rt.name}"? This cannot be undone.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          if (!selectedListingId) return;
          const token = await getAccessToken();
          if (!token) return;
          try {
            await api.deleteRoomType(selectedListingId, rt.id, token);
            await loadRoomTypes();
          } catch (err: any) {
            Alert.alert('Error', err?.response?.data?.detail || 'Failed to delete');
          }
        },
      },
    ]);
  };

  /* ── Inclusion CRUD ────────────────────────────────────── */

  const openAddInclusion = (roomTypeId: string) => {
    setInclusionRoomId(roomTypeId);
    setInclusionForm(EMPTY_INCLUSION_FORM);
    setShowInclusionModal(true);
  };

  const saveInclusion = async () => {
    if (!selectedListingId || !inclusionRoomId) return;
    if (!inclusionForm.name.trim()) { Alert.alert('Error', 'Name is required'); return; }

    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(true);

    const payload = {
      name: inclusionForm.name.trim(),
      type: inclusionForm.type,
      description: inclusionForm.description.trim() || null,
      pricePaise: inclusionForm.price ? Math.round(Number(inclusionForm.price) * 100) : 0,
    };

    try {
      await api.createRoomTypeInclusion(selectedListingId, inclusionRoomId, payload, token);
      setShowInclusionModal(false);
      // Reload inclusions for this room
      const updated = await api.getRoomTypeInclusions(selectedListingId, inclusionRoomId, token);
      setInclusions((prev) => ({ ...prev, [inclusionRoomId!]: updated }));
    } catch (err: any) {
      Alert.alert('Error', err?.response?.data?.detail || 'Failed to add inclusion');
    } finally {
      setActionLoading(false);
    }
  };

  const deleteInclusion = (roomTypeId: string, inc: Inclusion) => {
    Alert.alert('Delete Inclusion', `Delete "${inc.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          if (!selectedListingId) return;
          const token = await getAccessToken();
          if (!token) return;
          try {
            await api.deleteRoomTypeInclusion(selectedListingId, roomTypeId, inc.id, token);
            const updated = await api.getRoomTypeInclusions(selectedListingId, roomTypeId, token);
            setInclusions((prev) => ({ ...prev, [roomTypeId]: updated }));
          } catch (err: any) {
            Alert.alert('Error', err?.response?.data?.detail || 'Failed to delete inclusion');
          }
        },
      },
    ]);
  };

  const toggleExpand = (id: string) => {
    setExpandedRooms((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  /* ── Render helpers ────────────────────────────────────── */

  const selectedListing = listings.find((l) => l.id === selectedListingId);

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

  /* ── Main render ───────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Room Types</Text>
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

      {/* Add Room Button */}
      {selectedListingId && (
        <TouchableOpacity style={s.addBar} onPress={openAddRoom}>
          <Text style={s.addBarText}>+ Add Room Type</Text>
        </TouchableOpacity>
      )}

      {/* Room Type List */}
      <ScrollView
        style={s.list}
        contentContainerStyle={s.listContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
      >
        {!selectedListingId && (
          <Text style={s.emptyText}>Select a listing to manage room types</Text>
        )}
        {selectedListingId && roomTypes.length === 0 && (
          <Text style={s.emptyText}>No room types yet. Add one above.</Text>
        )}
        {roomTypes.map((rt) => {
          const expanded = expandedRooms.has(rt.id);
          const roomInclusions = inclusions[rt.id] || [];
          return (
            <View key={rt.id} style={s.card}>
              {/* Card header */}
              <View style={s.cardHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={s.cardName}>{rt.name}</Text>
                  {rt.description ? <Text style={s.cardDesc}>{rt.description}</Text> : null}
                </View>
                <Text style={s.cardPrice}>{formatPrice(rt.basePricePaise)}</Text>
              </View>

              {/* Meta row */}
              <View style={s.metaRow}>
                <View style={s.badge}>
                  <Text style={s.badgeText}>{rt.roomType}</Text>
                </View>
                <View style={s.badge}>
                  <Text style={s.badgeText}>{rt.sharingType}</Text>
                </View>
                <Text style={s.metaText}>Max {rt.maxOccupancy} guests</Text>
                <Text style={s.metaText}>{rt.numberOfRooms} rooms</Text>
              </View>

              {/* Amenities */}
              {rt.amenities && rt.amenities.length > 0 && (
                <View style={s.amenitiesRow}>
                  {rt.amenities.map((a, i) => (
                    <View key={i} style={s.amenityTag}>
                      <Text style={s.amenityText}>{a}</Text>
                    </View>
                  ))}
                </View>
              )}

              {/* Actions */}
              <View style={s.cardActions}>
                <TouchableOpacity style={s.editBtn} onPress={() => openEditRoom(rt)}>
                  <Text style={s.editBtnText}>Edit</Text>
                </TouchableOpacity>
                <TouchableOpacity style={s.deleteBtn} onPress={() => deleteRoom(rt)}>
                  <Text style={s.deleteBtnText}>Delete</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={s.expandBtn}
                  onPress={() => toggleExpand(rt.id)}
                >
                  <Text style={s.expandBtnText}>
                    {expanded ? 'Hide Inclusions' : `Inclusions (${roomInclusions.length})`}
                  </Text>
                </TouchableOpacity>
              </View>

              {/* Inclusions section */}
              {expanded && (
                <View style={s.inclusionsSection}>
                  <View style={s.inclusionsHeader}>
                    <Text style={s.inclusionsTitle}>Inclusions</Text>
                    <TouchableOpacity onPress={() => openAddInclusion(rt.id)}>
                      <Text style={s.addInclusionText}>+ Add</Text>
                    </TouchableOpacity>
                  </View>
                  {roomInclusions.length === 0 && (
                    <Text style={s.emptyIncText}>No inclusions yet</Text>
                  )}
                  {roomInclusions.map((inc) => {
                    const colors = INCLUSION_COLORS[inc.type] || { bg: '#f3f4f6', text: '#374151' };
                    return (
                      <View key={inc.id} style={s.incCard}>
                        <View style={{ flex: 1 }}>
                          <View style={s.incTopRow}>
                            <Text style={s.incName}>{inc.name}</Text>
                            <View style={[s.incTypeBadge, { backgroundColor: colors.bg }]}>
                              <Text style={[s.incTypeText, { color: colors.text }]}>{inc.type}</Text>
                            </View>
                          </View>
                          {inc.description ? <Text style={s.incDesc}>{inc.description}</Text> : null}
                          {inc.pricePaise > 0 && (
                            <Text style={s.incPrice}>{formatPrice(inc.pricePaise)}</Text>
                          )}
                        </View>
                        <TouchableOpacity onPress={() => deleteInclusion(rt.id, inc)} style={s.incDelete}>
                          <Text style={s.incDeleteText}>✕</Text>
                        </TouchableOpacity>
                      </View>
                    );
                  })}
                </View>
              )}
            </View>
          );
        })}
        <View style={{ height: 100 }} />
      </ScrollView>

      {/* ── Add/Edit Room Modal ─────────────────────────── */}
      <Modal visible={showRoomModal} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.formModal}>
            <ScrollView showsVerticalScrollIndicator={false}>
              <Text style={s.modalTitle}>
                {editingRoomId ? 'Edit Room Type' : 'Add Room Type'}
              </Text>

              <Text style={s.label}>Name *</Text>
              <TextInput
                style={s.input}
                placeholder="e.g. Deluxe AC Room"
                placeholderTextColor="#9ca3af"
                value={roomForm.name}
                onChangeText={(v) => setRoomForm((p) => ({ ...p, name: v }))}
              />

              <Text style={s.label}>Description</Text>
              <TextInput
                style={[s.input, { height: 80, textAlignVertical: 'top' }]}
                placeholder="Room description"
                placeholderTextColor="#9ca3af"
                multiline
                value={roomForm.description}
                onChangeText={(v) => setRoomForm((p) => ({ ...p, description: v }))}
              />

              <View style={s.row}>
                <View style={s.halfField}>
                  <Text style={s.label}>Max Occupancy *</Text>
                  <TextInput
                    style={s.input}
                    placeholder="2"
                    placeholderTextColor="#9ca3af"
                    keyboardType="number-pad"
                    value={roomForm.maxOccupancy}
                    onChangeText={(v) => setRoomForm((p) => ({ ...p, maxOccupancy: v }))}
                  />
                </View>
                <View style={s.halfField}>
                  <Text style={s.label}>No. of Rooms *</Text>
                  <TextInput
                    style={s.input}
                    placeholder="5"
                    placeholderTextColor="#9ca3af"
                    keyboardType="number-pad"
                    value={roomForm.numberOfRooms}
                    onChangeText={(v) => setRoomForm((p) => ({ ...p, numberOfRooms: v }))}
                  />
                </View>
              </View>

              <Text style={s.label}>Base Price (₹) *</Text>
              <TextInput
                style={s.input}
                placeholder="1500"
                placeholderTextColor="#9ca3af"
                keyboardType="decimal-pad"
                value={roomForm.basePrice}
                onChangeText={(v) => setRoomForm((p) => ({ ...p, basePrice: v }))}
              />

              <Text style={s.label}>Room Type</Text>
              <View style={s.chipRow}>
                {ROOM_TYPES.map((t) => (
                  <TouchableOpacity
                    key={t}
                    style={[s.chip, roomForm.roomType === t && s.chipActive]}
                    onPress={() => setRoomForm((p) => ({ ...p, roomType: t }))}
                  >
                    <Text style={[s.chipText, roomForm.roomType === t && s.chipTextActive]}>
                      {t.replace('_', ' ')}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={s.label}>Sharing Type</Text>
              <View style={s.chipRow}>
                {SHARING_TYPES.map((t) => (
                  <TouchableOpacity
                    key={t}
                    style={[s.chip, roomForm.sharingType === t && s.chipActive]}
                    onPress={() => setRoomForm((p) => ({ ...p, sharingType: t }))}
                  >
                    <Text style={[s.chipText, roomForm.sharingType === t && s.chipTextActive]}>
                      {t}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <View style={s.modalActions}>
                <TouchableOpacity
                  style={s.cancelBtn}
                  onPress={() => setShowRoomModal(false)}
                >
                  <Text style={s.cancelBtnText}>Cancel</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[s.primaryBtn, actionLoading && { opacity: 0.6 }]}
                  onPress={saveRoom}
                  disabled={actionLoading}
                >
                  {actionLoading ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={s.primaryBtnText}>
                      {editingRoomId ? 'Update' : 'Create'}
                    </Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* ── Add Inclusion Modal ─────────────────────────── */}
      <Modal visible={showInclusionModal} transparent animationType="slide">
        <View style={s.modalOverlay}>
          <View style={s.formModal}>
            <ScrollView showsVerticalScrollIndicator={false}>
              <Text style={s.modalTitle}>Add Inclusion</Text>

              <Text style={s.label}>Name *</Text>
              <TextInput
                style={s.input}
                placeholder="e.g. Breakfast"
                placeholderTextColor="#9ca3af"
                value={inclusionForm.name}
                onChangeText={(v) => setInclusionForm((p) => ({ ...p, name: v }))}
              />

              <Text style={s.label}>Type</Text>
              <View style={s.chipRow}>
                {INCLUSION_TYPES.map((t) => {
                  const colors = INCLUSION_COLORS[t] || { bg: '#f3f4f6', text: '#374151' };
                  return (
                    <TouchableOpacity
                      key={t}
                      style={[
                        s.chip,
                        inclusionForm.type === t && { backgroundColor: colors.bg, borderColor: colors.text },
                      ]}
                      onPress={() => setInclusionForm((p) => ({ ...p, type: t }))}
                    >
                      <Text
                        style={[
                          s.chipText,
                          inclusionForm.type === t && { color: colors.text },
                        ]}
                      >
                        {t}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>

              <Text style={s.label}>Description</Text>
              <TextInput
                style={[s.input, { height: 80, textAlignVertical: 'top' }]}
                placeholder="Optional description"
                placeholderTextColor="#9ca3af"
                multiline
                value={inclusionForm.description}
                onChangeText={(v) => setInclusionForm((p) => ({ ...p, description: v }))}
              />

              <Text style={s.label}>Price (₹)</Text>
              <TextInput
                style={s.input}
                placeholder="0 for free"
                placeholderTextColor="#9ca3af"
                keyboardType="decimal-pad"
                value={inclusionForm.price}
                onChangeText={(v) => setInclusionForm((p) => ({ ...p, price: v }))}
              />

              <View style={s.modalActions}>
                <TouchableOpacity
                  style={s.cancelBtn}
                  onPress={() => setShowInclusionModal(false)}
                >
                  <Text style={s.cancelBtnText}>Cancel</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[s.primaryBtn, actionLoading && { opacity: 0.6 }]}
                  onPress={saveInclusion}
                  disabled={actionLoading}
                >
                  {actionLoading ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={s.primaryBtnText}>Add</Text>
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
  metaText: { fontSize: 13, color: '#6b7280' },

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
  expandBtn: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginLeft: 'auto',
  },
  expandBtnText: { fontSize: 12, color: '#6b7280', fontWeight: '500' },

  /* Inclusions */
  inclusionsSection: {
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
  },
  inclusionsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  inclusionsTitle: { fontSize: 14, fontWeight: '700', color: '#111827' },
  addInclusionText: { fontSize: 13, fontWeight: '700', color: '#f97316' },
  emptyIncText: { fontSize: 13, color: '#9ca3af', fontStyle: 'italic' },
  incCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: '#f9fafb',
    borderRadius: 10,
    padding: 10,
    marginBottom: 8,
  },
  incTopRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 2 },
  incName: { fontSize: 14, fontWeight: '600', color: '#111827' },
  incTypeBadge: { borderRadius: 4, paddingHorizontal: 6, paddingVertical: 1 },
  incTypeText: { fontSize: 11, fontWeight: '600' },
  incDesc: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  incPrice: { fontSize: 13, fontWeight: '600', color: '#f97316', marginTop: 2 },
  incDelete: { padding: 6, marginLeft: 8 },
  incDeleteText: { fontSize: 16, color: '#9ca3af' },

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
  row: { flexDirection: 'row', gap: 12 },
  halfField: { flex: 1 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
  chip: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 8,
    backgroundColor: '#fff',
  },
  chipActive: { backgroundColor: '#fff7ed', borderColor: '#f97316' },
  chipText: { fontSize: 13, color: '#6b7280' },
  chipTextActive: { color: '#f97316', fontWeight: '600' },

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
