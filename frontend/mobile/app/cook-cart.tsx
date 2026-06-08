import { useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter, useFocusEffect, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import { CartItem, getCart, removeFromCart, clearCart } from '@/lib/cookCart';

export default function CookCartScreen() {
  const router = useRouter();
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkingOut, setCheckingOut] = useState(false);
  const [results, setResults] = useState<{ summary: string; ok: boolean; msg?: string }[] | null>(null);

  const load = useCallback(async () => {
    setItems(await getCart());
    setLoading(false);
  }, []);

  useFocusEffect(useCallback(() => { setLoading(true); setResults(null); load(); }, [load]));

  const total = items.reduce((s, i) => s + (i.estTotalPaise ?? 0), 0);

  async function remove(cartId: string) {
    setItems(await removeFromCart(cartId));
  }

  async function checkout() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setCheckingOut(true);
    const out: { summary: string; ok: boolean; msg?: string }[] = [];
    for (const item of items) {
      try {
        if (item.serviceType === 'DAILY') await api.bookChef(item.payload, token);
        else if (item.serviceType === 'MONTHLY') await api.createChefSubscription(item.payload, token);
        else await api.createEventBooking(item.payload, token);
        out.push({ summary: item.summary, ok: true });
      } catch (e: any) {
        out.push({ summary: item.summary, ok: false, msg: e.message ?? 'Failed' });
      }
    }
    if (out.every((r) => r.ok)) await clearCart();
    setResults(out);
    setItems(await getCart());
    setCheckingOut(false);
  }

  if (loading) return <View style={styles.center}><Stack.Screen options={{ title: 'Cart' }} /><ActivityIndicator color="#f97316" size="large" /></View>;

  if (results) {
    const okCount = results.filter((r) => r.ok).length;
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title: 'Order placed' }} />
        <Text style={styles.successIcon}>{okCount === results.length ? '✅' : '⚠️'}</Text>
        <Text style={styles.successTitle}>{okCount} of {results.length} placed</Text>
        <View style={{ alignSelf: 'stretch', marginTop: 16 }}>
          {results.map((r, i) => (
            <View key={i} style={styles.resultRow}>
              <Text style={styles.resultIcon}>{r.ok ? '✓' : '✕'}</Text>
              <Text style={styles.resultText} numberOfLines={2}>{r.summary}{r.msg ? ` — ${r.msg}` : ''}</Text>
            </View>
          ))}
        </View>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.replace('/cook-bookings')}><Text style={styles.primaryBtnText}>View my bookings</Text></TouchableOpacity>
        <TouchableOpacity onPress={() => router.replace('/cooks')}><Text style={styles.link}>Back to cooks</Text></TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: `Cart (${items.length})` }} />
      <FlatList
        data={items}
        keyExtractor={(i) => i.cartId}
        contentContainerStyle={items.length === 0 ? styles.emptyBox : styles.list}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={{ flex: 1 }}>
              <View style={styles.typeTag}><Text style={styles.typeTagText}>{item.serviceType}</Text></View>
              <Text style={styles.cardChef}>{item.chefName ?? 'Cook'}</Text>
              <Text style={styles.cardSummary}>{item.summary}</Text>
              {item.estTotalPaise ? <Text style={styles.cardPrice}>{formatPaise(item.estTotalPaise)}</Text> : null}
            </View>
            <TouchableOpacity onPress={() => remove(item.cartId)} style={styles.removeBtn}><Text style={styles.removeText}>Remove</Text></TouchableOpacity>
          </View>
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>🛒</Text>
            <Text style={styles.emptyTitle}>Your cart is empty</Text>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/cooks')}><Text style={styles.primaryBtnText}>Browse cooks</Text></TouchableOpacity>
          </View>
        }
      />
      {items.length > 0 && (
        <View style={styles.footer}>
          <View>
            <Text style={styles.footerLabel}>Estimated total</Text>
            <Text style={styles.footerTotal}>{formatPaise(total)}</Text>
          </View>
          <TouchableOpacity style={[styles.checkoutBtn, checkingOut && styles.btnDisabled]} disabled={checkingOut} onPress={checkout}>
            {checkingOut ? <ActivityIndicator color="#fff" /> : <Text style={styles.checkoutText}>Place {items.length} booking{items.length > 1 ? 's' : ''}</Text>}
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb', padding: 24 },
  list:         { padding: 16, gap: 12 },
  emptyBox:     { flexGrow: 1 },

  card:         { flexDirection: 'row', backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6', alignItems: 'center' },
  typeTag:      { alignSelf: 'flex-start', backgroundColor: '#fff7ed', borderRadius: 100, paddingHorizontal: 8, paddingVertical: 2, marginBottom: 6 },
  typeTagText:  { fontSize: 10, fontWeight: '700', color: '#c2410c' },
  cardChef:     { fontSize: 15, fontWeight: '700', color: '#111827' },
  cardSummary:  { fontSize: 13, color: '#6b7280', marginTop: 4 },
  cardPrice:    { fontSize: 15, fontWeight: '800', color: '#f97316', marginTop: 6 },
  removeBtn:    { paddingHorizontal: 10, paddingVertical: 6 },
  removeText:   { fontSize: 13, color: '#dc2626', fontWeight: '600' },

  footer:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#fff', padding: 16, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  footerLabel:  { fontSize: 12, color: '#6b7280' },
  footerTotal:  { fontSize: 20, fontWeight: '800', color: '#111827' },
  checkoutBtn:  { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, paddingHorizontal: 24 },
  btnDisabled:  { opacity: 0.6 },
  checkoutText: { color: '#fff', fontSize: 15, fontWeight: '700' },

  empty:        { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyIcon:    { fontSize: 48, marginBottom: 12 },
  emptyTitle:   { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },

  successIcon:  { fontSize: 56 },
  successTitle: { fontSize: 22, fontWeight: '800', color: '#111827', marginTop: 12 },
  resultRow:    { flexDirection: 'row', gap: 10, paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  resultIcon:   { fontSize: 16, fontWeight: '800', color: '#374151', width: 18 },
  resultText:   { flex: 1, fontSize: 13, color: '#374151' },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, paddingHorizontal: 28, alignItems: 'center', marginTop: 20 },
  primaryBtnText:{ color: '#fff', fontSize: 15, fontWeight: '700' },
  link:         { color: '#f97316', fontSize: 14, fontWeight: '600', marginTop: 14 },
});
