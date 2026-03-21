import { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

interface Subscription {
  id: string;
  hostId: string;
  tier: string;
  status: string;
  trialEndsAt: string | null;
  billingCycle: string;
  amountPaise: number;
  nextBillingAt: string | null;
  createdAt: string;
}

const TIERS = [
  {
    key: 'STARTER',
    name: 'Starter',
    price: 999,
    listings: '2 listings',
    features: ['Basic analytics', 'Email support', 'Standard visibility'],
  },
  {
    key: 'PRO',
    name: 'Pro',
    price: 2499,
    listings: '10 listings',
    features: ['Advanced analytics', 'Priority support', 'Boosted visibility', 'Calendar sync'],
    popular: true,
  },
  {
    key: 'COMMERCIAL',
    name: 'Commercial',
    price: 3999,
    listings: 'Unlimited listings',
    features: ['Full analytics suite', 'Dedicated support', 'Maximum visibility', 'API access', 'Multi-user management'],
  },
];

export default function SubscriptionScreen() {
  const router = useRouter();
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  useEffect(() => {
    loadSubscription();
  }, []);

  async function loadSubscription() {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    try {
      const sub = await api.getMySubscription(token);
      setSubscription(sub);
    } catch {
      // No subscription yet — that's fine
    } finally {
      setLoading(false);
    }
  }

  async function handleStartTrial(tier: string) {
    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(tier);
    try {
      const sub = await api.startSubscriptionTrial(tier, token);
      setSubscription(sub);
      Alert.alert('Success', `Started 90-day free trial for ${tier} plan!`);
    } catch (err: any) {
      // 409 means subscription already exists — try upgrade instead
      if (err.message?.includes('409') || err.message?.includes('already')) {
        try {
          const sub = await api.upgradeSubscription(tier, token);
          setSubscription(sub);
          Alert.alert('Success', `Upgraded to ${tier} plan!`);
        } catch (upgradeErr: any) {
          Alert.alert('Error', upgradeErr.message ?? 'Failed to upgrade subscription');
        }
      } else {
        Alert.alert('Error', err.message ?? 'Failed to start trial');
      }
    } finally {
      setActionLoading(null);
    }
  }

  async function handleUpgrade(tier: string) {
    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(tier);
    try {
      const sub = await api.upgradeSubscription(tier, token);
      setSubscription(sub);
      Alert.alert('Success', `Upgraded to ${tier} plan!`);
    } catch (err: any) {
      Alert.alert('Error', err.message ?? 'Failed to upgrade subscription');
    } finally {
      setActionLoading(null);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>🔒</Text>
        <Text style={styles.emptyTitle}>Sign in to manage subscription</Text>
        <TouchableOpacity style={styles.button} onPress={() => router.push('/auth')}>
          <Text style={styles.buttonText}>Sign in</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const currentTier = subscription?.tier ?? null;
  const tierIndex = (t: string) => TIERS.findIndex((ti) => ti.key === t);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContent}>
      {/* Current plan banner */}
      {subscription && (
        <View style={styles.bannerCard}>
          <View style={styles.bannerRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.bannerLabel}>Current Plan</Text>
              <Text style={styles.bannerTier}>{subscription.tier}</Text>
            </View>
            <View style={styles.statusBadge}>
              <Text style={styles.statusText}>{subscription.status}</Text>
            </View>
          </View>
          {subscription.status === 'TRIAL' && subscription.trialEndsAt && (
            <Text style={styles.bannerTrial}>
              Trial ends: {new Date(subscription.trialEndsAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
            </Text>
          )}
          {subscription.nextBillingAt && subscription.status !== 'TRIAL' && (
            <Text style={styles.bannerTrial}>
              Next billing: {new Date(subscription.nextBillingAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
            </Text>
          )}
        </View>
      )}

      {!subscription && (
        <View style={styles.noPlanBanner}>
          <Text style={styles.noPlanTitle}>No active subscription</Text>
          <Text style={styles.noPlanSubtitle}>Start a 90-day free trial to begin hosting</Text>
        </View>
      )}

      {/* Pricing cards */}
      <Text style={styles.sectionTitle}>Choose a Plan</Text>

      {TIERS.map((tier) => {
        const isCurrent = currentTier === tier.key;
        const isLowerTier = currentTier ? tierIndex(tier.key) < tierIndex(currentTier) : false;
        const isHigherTier = currentTier ? tierIndex(tier.key) > tierIndex(currentTier) : false;
        const isPopular = tier.popular;

        return (
          <View
            key={tier.key}
            style={[
              styles.tierCard,
              isCurrent && styles.tierCardCurrent,
              isPopular && !isCurrent && styles.tierCardPopular,
            ]}
          >
            {isPopular && !isCurrent && (
              <View style={styles.popularTag}>
                <Text style={styles.popularTagText}>MOST POPULAR</Text>
              </View>
            )}
            {isCurrent && (
              <View style={styles.currentTag}>
                <Text style={styles.currentTagText}>CURRENT PLAN</Text>
              </View>
            )}

            <Text style={[styles.tierName, isCurrent && styles.tierNameCurrent]}>{tier.name}</Text>
            <View style={styles.priceRow}>
              <Text style={[styles.priceSymbol, isCurrent && styles.priceTextCurrent]}>₹</Text>
              <Text style={[styles.priceValue, isCurrent && styles.priceTextCurrent]}>
                {tier.price.toLocaleString('en-IN')}
              </Text>
              <Text style={[styles.priceMonth, isCurrent && styles.priceTextCurrent]}>/month</Text>
            </View>
            <Text style={[styles.listingLimit, isCurrent && { color: 'rgba(255,255,255,0.8)' }]}>
              {tier.listings}
            </Text>

            <View style={styles.featuresList}>
              {tier.features.map((f) => (
                <View key={f} style={styles.featureRow}>
                  <Text style={[styles.featureCheck, isCurrent && { color: '#fff' }]}>✓</Text>
                  <Text style={[styles.featureText, isCurrent && { color: 'rgba(255,255,255,0.9)' }]}>{f}</Text>
                </View>
              ))}
            </View>

            {/* Button */}
            {isCurrent ? (
              <View style={styles.currentButton}>
                <Text style={styles.currentButtonText}>Current Plan</Text>
              </View>
            ) : isLowerTier ? (
              <View style={styles.disabledButton}>
                <Text style={styles.disabledButtonText}>Lower Tier</Text>
              </View>
            ) : !subscription ? (
              <TouchableOpacity
                style={[styles.actionButton, isPopular && styles.actionButtonPopular]}
                onPress={() => handleStartTrial(tier.key)}
                disabled={actionLoading === tier.key}
              >
                {actionLoading === tier.key ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.actionButtonText}>Start 90-day Free Trial</Text>
                )}
              </TouchableOpacity>
            ) : isHigherTier ? (
              <TouchableOpacity
                style={[styles.actionButton, styles.actionButtonUpgrade]}
                onPress={() => handleUpgrade(tier.key)}
                disabled={actionLoading === tier.key}
              >
                {actionLoading === tier.key ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.actionButtonText}>Upgrade</Text>
                )}
              </TouchableOpacity>
            ) : null}
          </View>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:        { flex: 1, backgroundColor: '#f9fafb' },
  scrollContent:    { padding: 16, paddingBottom: 40 },
  center:           { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },

  /* Banner */
  bannerCard:       { backgroundColor: '#f97316', borderRadius: 20, padding: 20, marginBottom: 16 },
  bannerRow:        { flexDirection: 'row', alignItems: 'center' },
  bannerLabel:      { color: 'rgba(255,255,255,0.8)', fontSize: 13 },
  bannerTier:       { color: '#fff', fontSize: 28, fontWeight: '800', marginTop: 4 },
  bannerTrial:      { color: 'rgba(255,255,255,0.7)', fontSize: 12, marginTop: 8 },
  statusBadge:      { backgroundColor: 'rgba(255,255,255,0.25)', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100 },
  statusText:       { color: '#fff', fontSize: 12, fontWeight: '800' },

  /* No plan */
  noPlanBanner:     { backgroundColor: '#fff', borderRadius: 20, padding: 20, marginBottom: 16, borderWidth: 1, borderColor: '#f3f4f6', alignItems: 'center' },
  noPlanTitle:      { fontSize: 17, fontWeight: '700', color: '#374151', marginBottom: 4 },
  noPlanSubtitle:   { fontSize: 13, color: '#9ca3af' },

  sectionTitle:     { fontSize: 17, fontWeight: '700', color: '#111827', paddingTop: 8, paddingBottom: 12 },

  /* Tier card */
  tierCard:         { backgroundColor: '#fff', borderRadius: 20, padding: 20, marginBottom: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  tierCardCurrent:  { backgroundColor: '#f97316', borderColor: '#f97316' },
  tierCardPopular:  { borderColor: '#f97316', borderWidth: 2 },

  popularTag:       { backgroundColor: '#fff7ed', alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginBottom: 12 },
  popularTagText:   { color: '#c2410c', fontSize: 10, fontWeight: '800' },
  currentTag:       { backgroundColor: 'rgba(255,255,255,0.25)', alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginBottom: 12 },
  currentTagText:   { color: '#fff', fontSize: 10, fontWeight: '800' },

  tierName:         { fontSize: 20, fontWeight: '700', color: '#111827', marginBottom: 8 },
  tierNameCurrent:  { color: '#fff' },

  priceRow:         { flexDirection: 'row', alignItems: 'baseline', marginBottom: 4 },
  priceSymbol:      { fontSize: 18, fontWeight: '700', color: '#111827' },
  priceValue:       { fontSize: 32, fontWeight: '800', color: '#111827' },
  priceMonth:       { fontSize: 14, color: '#6b7280', marginLeft: 4 },
  priceTextCurrent: { color: '#fff' },

  listingLimit:     { fontSize: 13, color: '#6b7280', marginBottom: 16 },

  featuresList:     { marginBottom: 16 },
  featureRow:       { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  featureCheck:     { color: '#f97316', fontSize: 14, fontWeight: '700', marginRight: 8, width: 18 },
  featureText:      { fontSize: 13, color: '#374151', flex: 1 },

  /* Buttons */
  currentButton:      { backgroundColor: 'rgba(255,255,255,0.25)', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  currentButtonText:  { color: '#fff', fontWeight: '700', fontSize: 14 },

  disabledButton:     { backgroundColor: '#f3f4f6', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  disabledButtonText: { color: '#9ca3af', fontWeight: '600', fontSize: 14 },

  actionButton:       { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  actionButtonPopular:{ backgroundColor: '#ea580c' },
  actionButtonUpgrade:{ backgroundColor: '#f97316' },
  actionButtonText:   { color: '#fff', fontWeight: '700', fontSize: 14 },

  /* Empty / auth */
  emptyIcon:        { fontSize: 48, marginBottom: 12 },
  emptyTitle:       { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 8 },
  button:           { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 8 },
  buttonText:       { color: '#fff', fontWeight: '600', fontSize: 14 },
});
