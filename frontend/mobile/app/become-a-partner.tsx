import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet,
} from 'react-native';
import { useRouter } from 'expo-router';

interface Vertical {
  emoji: string;
  title: string;
  tagline: string;
  earnings: string;
  route: string;
  cta: string;
  badge?: string;
}

const VERTICALS: Vertical[] = [
  { emoji: '👨‍🍳', title: 'Cook / Chef', tagline: 'Cook daily meals, run subscriptions, cater events', earnings: '₹15,000 – ₹80,000 / mo', route: '/cooks', cta: 'Register as a cook', badge: 'Most popular' },
  { emoji: '🪔', title: 'Pandit / Acharya', tagline: 'Offer pujas — Griha Pravesh, Satyanarayan, weddings, online video pujas', earnings: '₹2,500 – ₹25,000 / puja', route: '/cooks', cta: 'Register as a pandit' },
  { emoji: '🎂', title: 'Cake Designer', tagline: 'Sell bespoke cakes — fondant, photo-print, sculpted, theme cakes', earnings: '₹500 – ₹15,000 / cake', route: '/cooks', cta: 'Register as a baker' },
  { emoji: '🌸', title: 'Decorator', tagline: 'Decorate weddings, sangeet, birthdays, corporate events', earnings: '₹5,000 – ₹2,00,000 / event', route: '/cooks', cta: 'Register as a decorator' },
  { emoji: '🎤', title: 'Singer / Performer', tagline: 'Get booked for weddings, sangeet nights, corporate events', earnings: '₹8,000 – ₹1,50,000 / event', route: '/cooks', cta: 'Register as a performer' },
  { emoji: '🧑‍🍳', title: 'Staff Agency', tagline: 'Supply waiters, bartenders, cooks, hostesses for events', earnings: '₹500 – ₹2,500 / staff / day', route: '/cooks', cta: 'Register your agency' },
];

const HOW_IT_WORKS = [
  { step: 1, title: 'Register & verify', body: 'Fill the wizard, upload Aadhaar + PAN. We verify in 24-48 hours.' },
  { step: 2, title: 'Set your prices & calendar', body: 'You decide rates and which dates you accept work.' },
  { step: 3, title: 'Receive bookings', body: 'Customers book directly. Confirm, deliver, get paid.' },
  { step: 4, title: 'Get paid weekly', body: 'Bank settlement every Tuesday. Commission only on completed jobs.' },
];

export default function BecomePartnerScreen() {
  const router = useRouter();
  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Earn on BhramanKaro. Pick your craft.</Text>
        <Text style={styles.heroSub}>Six ways to grow your business — cooking, pujas, cakes, décor, music and event staff.</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.grid}>
          {VERTICALS.map((v) => (
            <TouchableOpacity key={v.title} style={styles.card} activeOpacity={0.85} onPress={() => router.push(v.route as any)}>
              {v.badge ? <View style={styles.badge}><Text style={styles.badgeText}>{v.badge}</Text></View> : null}
              <Text style={styles.cardEmoji}>{v.emoji}</Text>
              <Text style={styles.cardTitle}>{v.title}</Text>
              <Text style={styles.cardTagline}>{v.tagline}</Text>
              <Text style={styles.cardEarnings}>{v.earnings}</Text>
              <View style={styles.cardCta}><Text style={styles.cardCtaText}>{v.cta} →</Text></View>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.sectionTitle}>How it works</Text>
        {HOW_IT_WORKS.map((s) => (
          <View key={s.step} style={styles.stepRow}>
            <View style={styles.stepCircle}><Text style={styles.stepNum}>{s.step}</Text></View>
            <View style={{ flex: 1 }}>
              <Text style={styles.stepTitle}>{s.title}</Text>
              <Text style={styles.stepBody}>{s.body}</Text>
            </View>
          </View>
        ))}

        <TouchableOpacity style={styles.altCard} onPress={() => router.push('/host-new-listing')}>
          <Text style={styles.altText}>Have a property to rent out instead?</Text>
          <Text style={styles.altLink}>List your property →</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.altCard} onPress={() => router.push('/broker')}>
          <Text style={styles.altText}>Are you a real-estate broker?</Text>
          <Text style={styles.altLink}>Become a BhramanKaro Broker →</Text>
        </TouchableOpacity>

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  hero:          { backgroundColor: '#fff7ed', paddingHorizontal: 20, paddingTop: 18, paddingBottom: 22 },
  heroTitle:     { fontSize: 24, fontWeight: '800', color: '#9a3412' },
  heroSub:       { fontSize: 13, color: '#9a3412', marginTop: 8, lineHeight: 19 },
  scroll:        { padding: 16 },
  grid:          { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  card:          { width: '47.5%' as any, backgroundColor: '#fff', borderRadius: 16, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  badge:         { position: 'absolute', top: 10, right: 10, backgroundColor: '#fef3c7', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 100 },
  badgeText:     { fontSize: 9, fontWeight: '800', color: '#92400e' },
  cardEmoji:     { fontSize: 30, marginBottom: 8 },
  cardTitle:     { fontSize: 15, fontWeight: '700', color: '#111827' },
  cardTagline:   { fontSize: 11, color: '#6b7280', marginTop: 4, lineHeight: 16, minHeight: 48 },
  cardEarnings:  { fontSize: 12, fontWeight: '700', color: '#f97316', marginTop: 6 },
  cardCta:       { marginTop: 10 },
  cardCtaText:   { fontSize: 12, fontWeight: '700', color: '#1d4ed8' },
  sectionTitle:  { fontSize: 17, fontWeight: '700', color: '#111827', marginTop: 28, marginBottom: 12 },
  stepRow:       { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 14, gap: 12 },
  stepCircle:    { width: 30, height: 30, borderRadius: 15, backgroundColor: '#111827', alignItems: 'center', justifyContent: 'center' },
  stepNum:       { color: '#fff', fontWeight: '800', fontSize: 14 },
  stepTitle:     { fontSize: 14, fontWeight: '700', color: '#111827' },
  stepBody:      { fontSize: 12, color: '#6b7280', marginTop: 2, lineHeight: 18 },
  altCard:       { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginTop: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  altText:       { fontSize: 13, color: '#374151', fontWeight: '600' },
  altLink:       { fontSize: 13, color: '#f97316', fontWeight: '700', marginTop: 4 },
});
