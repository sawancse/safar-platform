import { useState, useEffect, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, TextInput, Modal,
  StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { getAccessToken } from '@/lib/auth';

const API_URL = 'http://localhost:8080';

interface PaymentMethod {
  id: string;
  type: 'UPI' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'NET_BANKING';
  label?: string;
  isDefault?: boolean;
  upiId?: string;
  cardLast4?: string;
  cardNetwork?: string;
  cardHolder?: string;
  cardExpiry?: string;
  bankName?: string;
  createdAt: string;
}

const TYPE_ICONS: Record<string, string> = {
  UPI: '📱', CREDIT_CARD: '💳', DEBIT_CARD: '💳', NET_BANKING: '🏦',
};

const TYPE_LABELS: Record<string, string> = {
  UPI: 'UPI', CREDIT_CARD: 'Credit Card', DEBIT_CARD: 'Debit Card', NET_BANKING: 'Net Banking',
};

export default function PaymentMethodsScreen() {
  const router = useRouter();
  const [methods, setMethods] = useState<PaymentMethod[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [addType, setAddType] = useState<'UPI' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'NET_BANKING'>('UPI');
  const [saving, setSaving] = useState(false);

  // UPI form
  const [upiId, setUpiId] = useState('');
  const [upiLabel, setUpiLabel] = useState('');

  // Card form
  const [cardHolder, setCardHolder] = useState('');
  const [cardLast4, setCardLast4] = useState('');
  const [cardNetwork, setCardNetwork] = useState('Visa');
  const [cardExpiry, setCardExpiry] = useState('');

  // Bank form
  const [bankName, setBankName] = useState('');

  const fetchMethods = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) return;
    try {
      const res = await fetch(`${API_URL}/api/v1/users/me/payment-methods`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) setMethods(await res.json());
    } catch {} finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchMethods(); }, [fetchMethods]);

  async function handleAdd() {
    const token = await getAccessToken();
    if (!token) return;
    setSaving(true);
    try {
      const body: any = { type: addType };
      if (addType === 'UPI') {
        if (!upiId.match(/^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$/)) {
          Alert.alert('Invalid UPI ID', 'Please enter a valid UPI ID');
          setSaving(false);
          return;
        }
        body.upiId = upiId;
        body.label = upiLabel || upiId;
      } else if (addType === 'CREDIT_CARD' || addType === 'DEBIT_CARD') {
        body.cardHolder = cardHolder;
        body.cardLast4 = cardLast4;
        body.cardNetwork = cardNetwork;
        body.cardExpiry = cardExpiry;
        body.label = `${cardNetwork} •••• ${cardLast4}`;
      } else {
        body.bankName = bankName;
        body.label = bankName;
      }
      const res = await fetch(`${API_URL}/api/v1/users/me/payment-methods`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
      });
      if (res.ok) {
        setShowAdd(false);
        resetForm();
        fetchMethods();
      } else {
        Alert.alert('Error', 'Failed to add payment method');
      }
    } catch { Alert.alert('Error', 'Something went wrong'); }
    finally { setSaving(false); }
  }

  async function handleDelete(id: string) {
    const token = await getAccessToken();
    if (!token) return;
    Alert.alert('Remove', 'Remove this payment method?', [
      { text: 'Cancel' },
      { text: 'Remove', style: 'destructive', onPress: async () => {
        try {
          await fetch(`${API_URL}/api/v1/users/me/payment-methods/${id}`, {
            method: 'DELETE', headers: { Authorization: `Bearer ${token}` },
          });
          setMethods(prev => prev.filter(m => m.id !== id));
        } catch { Alert.alert('Error', 'Failed to remove'); }
      }},
    ]);
  }

  async function handleSetDefault(id: string) {
    const token = await getAccessToken();
    if (!token) return;
    try {
      await fetch(`${API_URL}/api/v1/users/me/payment-methods/${id}/default`, {
        method: 'PUT', headers: { Authorization: `Bearer ${token}` },
      });
      setMethods(prev => prev.map(m => ({ ...m, isDefault: m.id === id })));
    } catch {}
  }

  function resetForm() {
    setUpiId(''); setUpiLabel('');
    setCardHolder(''); setCardLast4(''); setCardNetwork('Visa'); setCardExpiry('');
    setBankName('');
    setAddType('UPI');
  }

  if (loading) {
    return <View style={styles.center}><ActivityIndicator color="#f97316" size="large" /></View>;
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={methods}
        keyExtractor={item => item.id}
        contentContainerStyle={methods.length === 0 ? styles.center : styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>💳</Text>
            <Text style={styles.emptyTitle}>No payment methods</Text>
            <Text style={styles.emptySubtitle}>Add UPI, cards, or net banking</Text>
          </View>
        }
        renderItem={({ item }) => (
          <View style={[styles.card, item.isDefault && styles.cardDefault]}>
            <View style={styles.cardRow}>
              <Text style={styles.cardIcon}>{TYPE_ICONS[item.type]}</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.cardLabel}>{item.label || TYPE_LABELS[item.type]}</Text>
                <Text style={styles.cardType}>{TYPE_LABELS[item.type]}</Text>
                {item.upiId && <Text style={styles.cardDetail}>{item.upiId}</Text>}
                {item.cardLast4 && <Text style={styles.cardDetail}>{item.cardNetwork} •••• {item.cardLast4}</Text>}
                {item.bankName && <Text style={styles.cardDetail}>{item.bankName}</Text>}
              </View>
              {item.isDefault && <Text style={styles.defaultBadge}>Default</Text>}
            </View>
            <View style={styles.cardActions}>
              {!item.isDefault && (
                <TouchableOpacity onPress={() => handleSetDefault(item.id)}>
                  <Text style={styles.actionText}>Set default</Text>
                </TouchableOpacity>
              )}
              <TouchableOpacity onPress={() => handleDelete(item.id)}>
                <Text style={[styles.actionText, { color: '#dc2626' }]}>Remove</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      />

      {/* Add button */}
      <TouchableOpacity style={styles.addBtn} onPress={() => setShowAdd(true)}>
        <Text style={styles.addBtnText}>+ Add Payment Method</Text>
      </TouchableOpacity>

      {/* Add Modal */}
      <Modal visible={showAdd} transparent animationType="slide" onRequestClose={() => setShowAdd(false)}>
        <TouchableOpacity style={styles.sheetOverlay} activeOpacity={1} onPress={() => setShowAdd(false)}>
          <View style={styles.sheetContent} onStartShouldSetResponder={() => true}>
            <View style={styles.sheetHandle} />
            <Text style={styles.sheetTitle}>Add Payment Method</Text>

            {/* Type selector */}
            <View style={styles.typeRow}>
              {(['UPI', 'CREDIT_CARD', 'DEBIT_CARD', 'NET_BANKING'] as const).map(t => (
                <TouchableOpacity key={t}
                  style={[styles.typeChip, addType === t && styles.typeChipActive]}
                  onPress={() => setAddType(t)}>
                  <Text style={styles.typeChipIcon}>{TYPE_ICONS[t]}</Text>
                  <Text style={[styles.typeChipText, addType === t && { color: '#f97316' }]}>
                    {t === 'CREDIT_CARD' ? 'Credit' : t === 'DEBIT_CARD' ? 'Debit' : t === 'NET_BANKING' ? 'Bank' : 'UPI'}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* UPI form */}
            {addType === 'UPI' && (
              <View style={styles.form}>
                <Text style={styles.formLabel}>UPI ID</Text>
                <TextInput style={styles.formInput} placeholder="yourname@upi"
                  value={upiId} onChangeText={setUpiId} autoCapitalize="none" />
                <Text style={styles.formLabel}>Label (optional)</Text>
                <TextInput style={styles.formInput} placeholder="Personal UPI"
                  value={upiLabel} onChangeText={setUpiLabel} />
              </View>
            )}

            {/* Card form */}
            {(addType === 'CREDIT_CARD' || addType === 'DEBIT_CARD') && (
              <View style={styles.form}>
                <Text style={styles.formLabel}>Cardholder Name</Text>
                <TextInput style={styles.formInput} placeholder="Name on card"
                  value={cardHolder} onChangeText={setCardHolder} />
                <Text style={styles.formLabel}>Last 4 Digits</Text>
                <TextInput style={styles.formInput} placeholder="1234" maxLength={4}
                  keyboardType="numeric" value={cardLast4} onChangeText={setCardLast4} />
                <Text style={styles.formLabel}>Network</Text>
                <View style={styles.typeRow}>
                  {['Visa', 'Mastercard', 'RuPay', 'Amex'].map(n => (
                    <TouchableOpacity key={n}
                      style={[styles.typeChip, cardNetwork === n && styles.typeChipActive]}
                      onPress={() => setCardNetwork(n)}>
                      <Text style={[styles.typeChipText, cardNetwork === n && { color: '#f97316' }]}>{n}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
                <Text style={styles.formLabel}>Expiry (MM/YY)</Text>
                <TextInput style={styles.formInput} placeholder="12/28" maxLength={5}
                  value={cardExpiry} onChangeText={setCardExpiry} />
              </View>
            )}

            {/* Bank form */}
            {addType === 'NET_BANKING' && (
              <View style={styles.form}>
                <Text style={styles.formLabel}>Bank Name</Text>
                <TextInput style={styles.formInput} placeholder="State Bank of India"
                  value={bankName} onChangeText={setBankName} />
              </View>
            )}

            <TouchableOpacity style={styles.saveBtn} onPress={handleAdd} disabled={saving}>
              <Text style={styles.saveBtnText}>{saving ? 'Saving...' : 'Add'}</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  center:         { flex: 1, alignItems: 'center', justifyContent: 'center' },
  list:           { padding: 16, gap: 12 },
  empty:          { alignItems: 'center' },
  emptyIcon:      { fontSize: 48, marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af', marginTop: 4 },

  card:           { backgroundColor: '#fff', borderRadius: 16, padding: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  cardDefault:    { borderColor: '#f97316', borderWidth: 2 },
  cardRow:        { flexDirection: 'row', alignItems: 'center', gap: 12 },
  cardIcon:       { fontSize: 28 },
  cardLabel:      { fontSize: 15, fontWeight: '600', color: '#111827' },
  cardType:       { fontSize: 12, color: '#6b7280', marginTop: 1 },
  cardDetail:     { fontSize: 12, color: '#9ca3af', marginTop: 1, fontFamily: 'monospace' },
  defaultBadge:   { fontSize: 11, fontWeight: '700', color: '#f97316', backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, overflow: 'hidden' },
  cardActions:    { flexDirection: 'row', gap: 16, marginTop: 12, paddingTop: 12, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  actionText:     { fontSize: 13, fontWeight: '600', color: '#f97316' },

  addBtn:         { margin: 16, backgroundColor: '#f97316', borderRadius: 14, paddingVertical: 14, alignItems: 'center' },
  addBtnText:     { color: '#fff', fontWeight: '700', fontSize: 15 },

  sheetOverlay:   { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheetContent:   { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32 },
  sheetHandle:    { width: 36, height: 4, borderRadius: 2, backgroundColor: '#e5e7eb', alignSelf: 'center', marginBottom: 16 },
  sheetTitle:     { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 16 },

  typeRow:        { flexDirection: 'row', gap: 8, marginBottom: 16, flexWrap: 'wrap' },
  typeChip:       { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 8, alignItems: 'center' },
  typeChipActive: { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  typeChipIcon:   { fontSize: 20, marginBottom: 2 },
  typeChipText:   { fontSize: 12, fontWeight: '600', color: '#6b7280' },

  form:           { gap: 8, marginBottom: 16 },
  formLabel:      { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  formInput:      { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb' },

  saveBtn:        { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  saveBtnText:    { color: '#fff', fontWeight: '700', fontSize: 15 },
});
