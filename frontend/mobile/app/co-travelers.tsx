import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, TextInput, Modal,
  StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

interface CoTraveler {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  gender?: string;
}

const GENDERS = ['Male', 'Female', 'Other'];

function getInitials(first: string, last: string) {
  return `${(first?.[0] ?? '').toUpperCase()}${(last?.[0] ?? '').toUpperCase()}`;
}

function calculateAge(dob?: string): number | null {
  if (!dob) return null;
  const birth = new Date(dob);
  if (isNaN(birth.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}

export default function CoTravelersScreen() {
  const router = useRouter();
  const [items, setItems] = useState<CoTraveler[]>([]);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState<string | null>(null);

  // Add modal state
  const [modalVisible, setModalVisible] = useState(false);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState('');
  const [adding, setAdding] = useState(false);

  const loadCoTravelers = useCallback(async (t: string) => {
    try {
      const data = await api.getCoTravelers(t);
      setItems(data ?? []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    (async () => {
      const t = await getAccessToken();
      if (!t) {
        router.replace('/auth');
        return;
      }
      setToken(t);
      loadCoTravelers(t);
    })();
  }, []);

  function resetForm() {
    setFirstName('');
    setLastName('');
    setDateOfBirth('');
    setGender('');
  }

  async function handleAdd() {
    if (!token) return;
    if (!firstName.trim() || !lastName.trim()) {
      Alert.alert('Required', 'First name and last name are required');
      return;
    }
    setAdding(true);
    try {
      const body: any = { firstName: firstName.trim(), lastName: lastName.trim() };
      if (dateOfBirth.trim()) body.dateOfBirth = dateOfBirth.trim();
      if (gender) body.gender = gender;
      const created = await api.addCoTraveler(body, token);
      setItems((prev) => [...prev, created]);
      setModalVisible(false);
      resetForm();
    } catch (err: any) {
      Alert.alert('Error', err.message ?? 'Failed to add co-traveler');
    } finally {
      setAdding(false);
    }
  }

  async function handleRemove(id: string, name: string) {
    if (!token) return;
    Alert.alert('Remove', `Remove ${name} from co-travelers?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove',
        style: 'destructive',
        onPress: async () => {
          try {
            await api.removeCoTraveler(id, token);
            setItems((prev) => prev.filter((i) => i.id !== id));
          } catch (err: any) {
            Alert.alert('Error', err.message ?? 'Failed to remove');
          }
        },
      },
    ]);
  }

  function renderItem({ item }: { item: CoTraveler }) {
    const fullName = `${item.firstName} ${item.lastName}`;
    const age = calculateAge(item.dateOfBirth);

    return (
      <View style={styles.card}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{getInitials(item.firstName, item.lastName)}</Text>
        </View>
        <View style={styles.cardBody}>
          <Text style={styles.cardName}>{fullName}</Text>
          <View style={styles.cardMeta}>
            {age != null && <Text style={styles.metaText}>Age {age}</Text>}
            {item.gender ? (
              <View style={styles.genderBadge}>
                <Text style={styles.genderBadgeText}>{item.gender}</Text>
              </View>
            ) : null}
          </View>
        </View>
        <TouchableOpacity style={styles.removeBtn} onPress={() => handleRemove(item.id, fullName)}>
          <Text style={styles.removeText}>Remove</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>‹ Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Co-travelers</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Subtitle + Add button */}
      <View style={styles.topBar}>
        <Text style={styles.subtitle}>People you frequently travel with</Text>
        <TouchableOpacity style={styles.addBtn} onPress={() => setModalVisible(true)}>
          <Text style={styles.addBtnText}>+ Add Co-traveler</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#f97316" style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={items.length === 0 ? styles.emptyContainer : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>👥</Text>
              <Text style={styles.emptyTitle}>Add your first co-traveler</Text>
              <Text style={styles.emptySubtitle}>
                Save details of people you frequently travel with for faster bookings
              </Text>
              <TouchableOpacity style={styles.emptyAddBtn} onPress={() => setModalVisible(true)}>
                <Text style={styles.emptyAddBtnText}>Add Co-traveler</Text>
              </TouchableOpacity>
            </View>
          }
        />
      )}

      {/* Add Modal */}
      <Modal visible={modalVisible} animationType="slide" transparent>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Add Co-traveler</Text>

            <Text style={styles.label}>First Name *</Text>
            <TextInput
              style={styles.input}
              value={firstName}
              onChangeText={setFirstName}
              placeholder="First name"
              placeholderTextColor="#9ca3af"
              autoCapitalize="words"
            />

            <Text style={styles.label}>Last Name *</Text>
            <TextInput
              style={styles.input}
              value={lastName}
              onChangeText={setLastName}
              placeholder="Last name"
              placeholderTextColor="#9ca3af"
              autoCapitalize="words"
            />

            <Text style={styles.label}>Date of Birth</Text>
            <TextInput
              style={styles.input}
              value={dateOfBirth}
              onChangeText={setDateOfBirth}
              placeholder="YYYY-MM-DD"
              placeholderTextColor="#9ca3af"
              keyboardType="numbers-and-punctuation"
            />

            <Text style={styles.label}>Gender</Text>
            <View style={styles.genderRow}>
              {GENDERS.map((g) => (
                <TouchableOpacity
                  key={g}
                  style={[styles.genderOption, gender === g && styles.genderOptionActive]}
                  onPress={() => setGender(gender === g ? '' : g)}
                >
                  <Text style={[styles.genderOptionText, gender === g && styles.genderOptionTextActive]}>
                    {g}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={styles.cancelBtn}
                onPress={() => { setModalVisible(false); resetForm(); }}
              >
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.saveBtn} onPress={handleAdd} disabled={adding}>
                {adding ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text style={styles.saveBtnText}>Add</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  header:         { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn:        { width: 60 },
  backText:       { fontSize: 16, color: '#f97316', fontWeight: '600' },
  headerTitle:    { fontSize: 17, fontWeight: '700', color: '#111827' },

  topBar:         { paddingHorizontal: 16, paddingTop: 16, paddingBottom: 8 },
  subtitle:       { fontSize: 13, color: '#6b7280', marginBottom: 12 },
  addBtn:         { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 12, alignItems: 'center' },
  addBtnText:     { color: '#fff', fontWeight: '700', fontSize: 15 },

  list:           { padding: 16 },
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },

  card:           { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 12, flexDirection: 'row', alignItems: 'center', padding: 12 },
  avatar:         { width: 48, height: 48, borderRadius: 24, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#fed7aa' },
  avatarText:     { fontSize: 16, fontWeight: '700', color: '#f97316' },
  cardBody:       { flex: 1, marginLeft: 12 },
  cardName:       { fontSize: 15, fontWeight: '600', color: '#111827' },
  cardMeta:       { flexDirection: 'row', alignItems: 'center', marginTop: 4, gap: 8 },
  metaText:       { fontSize: 12, color: '#6b7280' },
  genderBadge:    { backgroundColor: '#f3f4f6', borderRadius: 8, paddingHorizontal: 8, paddingVertical: 2 },
  genderBadgeText:{ fontSize: 11, fontWeight: '500', color: '#374151' },
  removeBtn:      { paddingHorizontal: 14, paddingVertical: 8 },
  removeText:     { fontSize: 12, fontWeight: '600', color: '#ef4444' },

  empty:          { alignItems: 'center', paddingTop: 80, paddingHorizontal: 32 },
  emptyIcon:      { fontSize: 48, marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af', marginTop: 4, textAlign: 'center' },
  emptyAddBtn:    { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 20 },
  emptyAddBtnText:{ color: '#fff', fontWeight: '600', fontSize: 14 },

  // Modal
  modalOverlay:   { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContent:   { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 40 },
  modalTitle:     { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  label:          { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 4, marginTop: 14 },
  input:          { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#111827' },
  genderRow:      { flexDirection: 'row', gap: 8, marginTop: 4 },
  genderOption:   { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingVertical: 10, alignItems: 'center', backgroundColor: '#fff' },
  genderOptionActive:     { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  genderOptionText:       { fontSize: 13, fontWeight: '500', color: '#6b7280' },
  genderOptionTextActive: { color: '#f97316', fontWeight: '600' },
  modalButtons:   { flexDirection: 'row', gap: 12, marginTop: 24 },
  cancelBtn:      { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  cancelBtnText:  { color: '#374151', fontWeight: '600', fontSize: 15 },
  saveBtn:        { flex: 1, backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  saveBtnText:    { color: '#fff', fontWeight: '700', fontSize: 15 },
});
