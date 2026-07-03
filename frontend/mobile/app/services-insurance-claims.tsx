import { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TouchableOpacity, TextInput, Alert, ActivityIndicator,
} from 'react-native';
import { Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const STEPS = [
  { n: '1', t: 'Report your claim', d: 'Share your policy reference and what happened.' },
  { n: '2', t: 'Submit documents', d: 'Our advisor tells you exactly what the insurer needs.' },
  { n: '3', t: 'Track to settlement', d: 'We follow up with the insurer until you are paid.' },
];

const FAQS = [
  { q: 'How soon should I report a claim?', a: 'As early as possible — most insurers require intimation within 24–48 hours for motor and hospitalisation.' },
  { q: 'What documents will I need?', a: 'It varies by policy — typically the policy copy, ID proof, and event-specific proofs (bills, FIR, discharge summary).' },
  { q: 'Is there a fee for claim assistance?', a: 'No. Claim assistance is a free service for policies bought on BhramanKaro.' },
];

export default function InsuranceClaimsScreen() {
  const [form, setForm] = useState({ name: '', phone: '', notes: '' });
  const [submitting, setSubmitting] = useState(false);

  async function submit() {
    if (!form.name || !form.phone) {
      Alert.alert('Missing details', 'Name and phone are required.');
      return;
    }
    setSubmitting(true);
    try {
      const token = await getAccessToken();
      await api.insuranceAdvisorCallback({
        name: form.name,
        phone: form.phone,
        product: 'claims',
        preferredTime: 'Claim assistance',
        notes: form.notes,
      }, token || undefined);
      setForm({ name: '', phone: '', notes: '' });
      Alert.alert('Request received', 'Our claims advisor will call you shortly.');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 48 }}>
      <Stack.Screen options={{ title: 'Claims Assistance' }} />
      <Text style={styles.title}>Claims Assistance</Text>
      <Text style={styles.subtitle}>We help you file and follow up on your insurance claim — free for policies bought on BhramanKaro.</Text>

      <Text style={styles.sectionTitle}>How it works</Text>
      {STEPS.map((s) => (
        <View key={s.n} style={styles.stepRow}>
          <View style={styles.stepNum}><Text style={styles.stepNumText}>{s.n}</Text></View>
          <View style={{ flex: 1 }}>
            <Text style={styles.stepTitle}>{s.t}</Text>
            <Text style={styles.stepDesc}>{s.d}</Text>
          </View>
        </View>
      ))}

      <Text style={styles.sectionTitle}>Get help now</Text>
      <View style={styles.card}>
        <TextInput style={styles.input} placeholder="Your name" placeholderTextColor="#9ca3af"
          value={form.name} onChangeText={(v) => setForm((f) => ({ ...f, name: v }))} />
        <TextInput style={styles.input} placeholder="Phone" placeholderTextColor="#9ca3af" keyboardType="phone-pad"
          value={form.phone} onChangeText={(v) => setForm((f) => ({ ...f, phone: v }))} />
        <TextInput style={[styles.input, { height: 90, textAlignVertical: 'top' }]} placeholder="Policy reference + what happened" placeholderTextColor="#9ca3af"
          multiline value={form.notes} onChangeText={(v) => setForm((f) => ({ ...f, notes: v }))} />
        <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={submit}>
          {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Request claim help</Text>}
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>FAQs</Text>
      {FAQS.map((f, i) => (
        <View key={i} style={styles.card}>
          <Text style={styles.faqQ}>{f.q}</Text>
          <Text style={styles.faqA}>{f.a}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  title:        { fontSize: 22, fontWeight: '800', color: '#111827' },
  subtitle:     { fontSize: 13, color: '#6b7280', marginTop: 6 },
  sectionTitle: { fontSize: 17, fontWeight: '800', color: '#111827', marginTop: 22, marginBottom: 10 },
  stepRow:      { flexDirection: 'row', gap: 12, marginBottom: 14, alignItems: 'flex-start' },
  stepNum:      { width: 30, height: 30, borderRadius: 15, backgroundColor: '#0ea5e9', alignItems: 'center', justifyContent: 'center' },
  stepNumText:  { color: '#fff', fontWeight: '800' },
  stepTitle:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  stepDesc:     { fontSize: 12, color: '#6b7280', marginTop: 2 },
  card:         { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  input:        { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb', marginBottom: 10 },
  primaryBtn:   { backgroundColor: '#0ea5e9', borderRadius: 12, paddingVertical: 13, alignItems: 'center', marginTop: 2 },
  primaryBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  faqQ:         { fontSize: 14, fontWeight: '700', color: '#111827' },
  faqA:         { fontSize: 13, color: '#6b7280', marginTop: 4, lineHeight: 19 },
});
