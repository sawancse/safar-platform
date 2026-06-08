import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const STATS = [
  { value: '10,000+', label: 'Registered brokers' },
  { value: '50,000+', label: 'Properties listed' },
  { value: '25,000+', label: 'Deals closed' },
  { value: '₹100Cr+', label: 'Total value' },
];

const BENEFITS = [
  { icon: '📋', title: 'Bulk Listing Tools', desc: 'CSV import & templates to list dozens of properties fast.' },
  { icon: '👥', title: 'Lead Management', desc: 'Buyer inquiries & site visits in one inbox.' },
  { icon: '💰', title: 'Commission Tracking', desc: 'Automatic calculation, transparent payouts.' },
  { icon: '🗂️', title: 'Free CRM', desc: 'Contact history, follow-ups & deal tracking.' },
];

const SPECIALIZATIONS = [
  { value: 'RESIDENTIAL', label: 'Residential' },
  { value: 'COMMERCIAL', label: 'Commercial' },
  { value: 'BOTH', label: 'Both' },
];

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Chennai', 'Kolkata', 'Pune', 'Ahmedabad', 'Jaipur', 'Lucknow', 'Noida', 'Gurgaon', 'Kochi', 'Goa', 'Indore', 'Chandigarh'];

export default function BrokerScreen() {
  const router = useRouter();
  const [checking, setChecking] = useState(true);
  const [authed, setAuthed] = useState(false);
  const [registered, setRegistered] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const [companyName, setCompanyName] = useState('');
  const [reraAgentId, setReraAgentId] = useState('');
  const [operatingCities, setOperatingCities] = useState<string[]>([]);
  const [specialization, setSpecialization] = useState('RESIDENTIAL');
  const [experienceYears, setExperienceYears] = useState('');
  const [bio, setBio] = useState('');
  const [website, setWebsite] = useState('');
  const [officeAddress, setOfficeAddress] = useState('');
  const [officeCity, setOfficeCity] = useState('');
  const [officeState, setOfficeState] = useState('');
  const [officePincode, setOfficePincode] = useState('');

  useEffect(() => {
    (async () => {
      const token = await getAccessToken();
      if (!token) { setChecking(false); return; }
      setAuthed(true);
      try {
        await api.getBrokerProfile(token);
        setRegistered(true);
      } catch {
        setRegistered(false);
      } finally {
        setChecking(false);
      }
    })();
  }, []);

  function toggleCity(c: string) {
    setOperatingCities((prev) => prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c]);
  }

  async function submit() {
    if (!companyName.trim()) { Alert.alert('Missing', 'Enter your company name.'); return; }
    if (operatingCities.length === 0) { Alert.alert('Missing', 'Pick at least one operating city.'); return; }
    if (!officeAddress.trim() || !officeCity.trim() || !officeState.trim() || !officePincode.trim()) {
      Alert.alert('Missing', 'Fill the full office address.'); return;
    }
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setSubmitting(true);
    try {
      await api.registerBroker({
        companyName, reraAgentId: reraAgentId || null, operatingCities, specialization,
        experienceYears: Number(experienceYears) || 0, bio, website: website || null,
        officeAddress, officeCity, officeState, officePincode,
      }, token);
      setDone(true);
    } catch (e: any) {
      Alert.alert('Registration failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  if (checking) {
    return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  }

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Become a Safar Broker</Text>
        <Text style={styles.heroSub}>List properties, manage clients, and earn commissions with zero platform fee.</Text>
        <View style={styles.trustRow}>
          <Text style={styles.trustChip}>✓ RERA Verified</Text>
          <Text style={styles.trustChip}>₹0 Platform Fee</Text>
          <Text style={styles.trustChip}>Dedicated Support</Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.statsRow}>
          {STATS.map((s) => (
            <View key={s.label} style={styles.statCard}>
              <Text style={styles.statValue}>{s.value}</Text>
              <Text style={styles.statLabel}>{s.label}</Text>
            </View>
          ))}
        </View>

        <Text style={styles.sectionTitle}>Why brokers choose Safar</Text>
        {BENEFITS.map((b) => (
          <View key={b.title} style={styles.benefitCard}>
            <Text style={styles.benefitIcon}>{b.icon}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.benefitTitle}>{b.title}</Text>
              <Text style={styles.benefitDesc}>{b.desc}</Text>
            </View>
          </View>
        ))}

        {done ? (
          <View style={styles.successCard}>
            <Text style={styles.successIcon}>🎉</Text>
            <Text style={styles.successTitle}>You're registered!</Text>
            <Text style={styles.successSub}>Manage your sale listings from the host dashboard.</Text>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/seller-dashboard')}>
              <Text style={styles.primaryBtnText}>Go to Dashboard</Text>
            </TouchableOpacity>
          </View>
        ) : !authed ? (
          <View style={styles.gateCard}>
            <Text style={styles.gateText}>Sign in to register as a broker.</Text>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
              <Text style={styles.primaryBtnText}>Sign In</Text>
            </TouchableOpacity>
          </View>
        ) : registered ? (
          <View style={styles.gateCard}>
            <Text style={styles.gateText}>You're already a registered broker.</Text>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/seller-dashboard')}>
              <Text style={styles.primaryBtnText}>Go to Dashboard</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <Text style={styles.sectionTitle}>Register</Text>
            <Field label="Company name *"><TextInput style={styles.input} value={companyName} onChangeText={setCompanyName} placeholder="ABC Realtors" placeholderTextColor="#9ca3af" /></Field>
            <Field label="RERA Agent ID"><TextInput style={styles.input} value={reraAgentId} onChangeText={setReraAgentId} placeholder="A52000012345" placeholderTextColor="#9ca3af" /></Field>

            <Text style={styles.label}>Operating cities *</Text>
            <View style={styles.pillRow}>
              {CITIES.map((c) => {
                const on = operatingCities.includes(c);
                return (
                  <TouchableOpacity key={c} style={[styles.pill, on && styles.pillActive]} onPress={() => toggleCity(c)}>
                    <Text style={[styles.pillText, on && styles.pillTextActive]}>{c}</Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            <Text style={styles.label}>Specialization *</Text>
            <View style={styles.pillRow}>
              {SPECIALIZATIONS.map((s) => (
                <TouchableOpacity key={s.value} style={[styles.pill, specialization === s.value && styles.pillActive]} onPress={() => setSpecialization(s.value)}>
                  <Text style={[styles.pillText, specialization === s.value && styles.pillTextActive]}>{s.label}</Text>
                </TouchableOpacity>
              ))}
            </View>

            <Field label="Experience (years)"><TextInput style={styles.input} value={experienceYears} onChangeText={setExperienceYears} keyboardType="numeric" placeholder="0" placeholderTextColor="#9ca3af" /></Field>
            <Field label="Bio"><TextInput style={[styles.input, styles.textarea]} value={bio} onChangeText={setBio} placeholder="Tell buyers about your firm…" placeholderTextColor="#9ca3af" multiline /></Field>
            <Field label="Website"><TextInput style={styles.input} value={website} onChangeText={setWebsite} placeholder="https://" placeholderTextColor="#9ca3af" autoCapitalize="none" keyboardType="url" /></Field>
            <Field label="Office address *"><TextInput style={[styles.input, styles.textarea]} value={officeAddress} onChangeText={setOfficeAddress} placeholder="Building, street, area" placeholderTextColor="#9ca3af" multiline /></Field>
            <View style={styles.row}>
              <Field label="Office city *" flex><TextInput style={styles.input} value={officeCity} onChangeText={setOfficeCity} placeholderTextColor="#9ca3af" /></Field>
              <Field label="State *" flex><TextInput style={styles.input} value={officeState} onChangeText={setOfficeState} placeholderTextColor="#9ca3af" /></Field>
            </View>
            <Field label="Pincode *"><TextInput style={styles.input} value={officePincode} onChangeText={setOfficePincode} keyboardType="numeric" maxLength={6} placeholderTextColor="#9ca3af" /></Field>

            <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={submit}>
              {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Register as Broker</Text>}
            </TouchableOpacity>
          </>
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

function Field({ label, children, flex }: { label: string; children: React.ReactNode; flex?: boolean }) {
  return (
    <View style={[styles.field, flex && { flex: 1 }]}>
      <Text style={styles.label}>{label}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  center:        { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  hero:          { backgroundColor: '#003B95', paddingHorizontal: 20, paddingTop: 18, paddingBottom: 22 },
  heroTitle:     { fontSize: 24, fontWeight: '800', color: '#fff' },
  heroSub:       { fontSize: 13, color: '#dbeafe', marginTop: 8, lineHeight: 19 },
  trustRow:      { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14 },
  trustChip:     { fontSize: 11, fontWeight: '700', color: '#fff', backgroundColor: 'rgba(255,255,255,0.15)', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 100, overflow: 'hidden' },
  scroll:        { padding: 16 },
  statsRow:      { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  statCard:      { width: '47.5%' as any, backgroundColor: '#fff', borderRadius: 14, padding: 14, alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6' },
  statValue:     { fontSize: 20, fontWeight: '800', color: '#003B95' },
  statLabel:     { fontSize: 11, color: '#6b7280', marginTop: 4, textAlign: 'center' },
  sectionTitle:  { fontSize: 17, fontWeight: '700', color: '#111827', marginTop: 24, marginBottom: 12 },
  benefitCard:   { flexDirection: 'row', gap: 12, backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  benefitIcon:   { fontSize: 24 },
  benefitTitle:  { fontSize: 14, fontWeight: '700', color: '#111827' },
  benefitDesc:   { fontSize: 12, color: '#6b7280', marginTop: 2, lineHeight: 17 },
  field:         { marginBottom: 14 },
  label:         { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  input:         { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:      { height: 84, textAlignVertical: 'top' },
  row:           { flexDirection: 'row', gap: 12 },
  pillRow:       { flexDirection: 'row', gap: 8, marginBottom: 14, flexWrap: 'wrap' },
  pill:          { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:    { backgroundColor: '#f97316', borderColor: '#f97316' },
  pillText:      { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },
  primaryBtn:    { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 6 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  gateCard:      { backgroundColor: '#fff', borderRadius: 14, padding: 20, alignItems: 'center', marginTop: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  gateText:      { fontSize: 14, color: '#374151', marginBottom: 12, textAlign: 'center' },
  successCard:   { backgroundColor: '#fff', borderRadius: 14, padding: 24, alignItems: 'center', marginTop: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  successIcon:   { fontSize: 44 },
  successTitle:  { fontSize: 18, fontWeight: '800', color: '#111827', marginTop: 10 },
  successSub:    { fontSize: 13, color: '#6b7280', marginTop: 4, textAlign: 'center' },
});
