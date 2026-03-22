import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Switch,
  RefreshControl,
  TextInput,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

type Tab = 'host' | 'ngo';

interface Listing {
  id: string;
  title: string;
  city: string;
  state?: string;
  basePricePaise: number;
  aashrayEnabled?: boolean;
  status?: string;
}

interface AashrayProperty {
  id: string;
  title: string;
  city: string;
  basePricePaise: number;
  hostName?: string;
  type?: string;
  amenities?: string[];
}

const HOW_IT_WORKS_HOST = [
  { step: '1', title: 'Enable Aashray', desc: 'Toggle Aashray on any of your listings to offer discounted or free stays.' },
  { step: '2', title: 'NGOs discover you', desc: 'Verified NGOs search for Aashray-enabled properties for their teams.' },
  { step: '3', title: 'Make an impact', desc: 'Host those who serve communities and earn recognition on Safar.' },
];

const BENEFITS = [
  { icon: '0%', label: 'Zero commission on Aashray bookings' },
  { icon: '📋', label: 'Tax benefit documentation provided' },
  { icon: '🏅', label: 'Community recognition badge on profile' },
];

export default function AashrayScreen() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('host');

  /* ── Host state ──────────────────────────────────────── */
  const [listings, setListings] = useState<Listing[]>([]);
  const [loadingListings, setLoadingListings] = useState(true);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [authed, setAuthed] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  /* ── NGO state ───────────────────────────────────────── */
  const [searchCity, setSearchCity] = useState('');
  const [aashrayResults, setAashrayResults] = useState<AashrayProperty[]>([]);
  const [loadingSearch, setLoadingSearch] = useState(false);

  /* ── Host data loader ────────────────────────────────── */
  const loadListings = useCallback(async (silent = false) => {
    const token = await getAccessToken();
    if (!token) {
      setLoadingListings(false);
      return;
    }
    setAuthed(true);
    if (!silent) setLoadingListings(true);
    try {
      const data = await api.getMyListings(token);
      setListings(data ?? []);
    } catch {
      setListings([]);
    } finally {
      setLoadingListings(false);
    }
  }, []);

  useEffect(() => {
    if (tab === 'host') loadListings();
  }, [tab, loadListings]);

  const handleToggleAashray = async (listingId: string, enabled: boolean) => {
    const token = await getAccessToken();
    if (!token) return;
    setTogglingId(listingId);
    try {
      await api.toggleAashray(listingId, enabled, token);
      setListings((prev) =>
        prev.map((l) => (l.id === listingId ? { ...l, aashrayEnabled: enabled } : l)),
      );
    } catch {
      // revert on failure — reload
      await loadListings(true);
    } finally {
      setTogglingId(null);
    }
  };

  const onRefreshHost = async () => {
    setRefreshing(true);
    await loadListings(true);
    setRefreshing(false);
  };

  /* ── NGO search ──────────────────────────────────────── */
  const searchAashray = useCallback(async () => {
    setLoadingSearch(true);
    try {
      const params: Record<string, string> = { aashray: 'true' };
      if (searchCity.trim()) params.city = searchCity.trim();
      const result = await api.search(params);
      setAashrayResults(result?.listings ?? []);
    } catch {
      setAashrayResults([]);
    } finally {
      setLoadingSearch(false);
    }
  }, [searchCity]);

  useEffect(() => {
    if (tab === 'ngo') searchAashray();
  }, [tab, searchAashray]);

  /* ── Render helpers ──────────────────────────────────── */

  function renderHostTab() {
    if (!authed) {
      return (
        <View style={styles.empty}>
          <Text style={styles.emptyIcon}>🔒</Text>
          <Text style={styles.emptyTitle}>Sign in required</Text>
          <Text style={styles.emptySubtitle}>Sign in to manage your Aashray listings</Text>
          <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
            <Text style={styles.primaryBtnText}>Sign In</Text>
          </TouchableOpacity>
        </View>
      );
    }

    return (
      <ScrollView
        style={styles.scrollBody}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefreshHost} tintColor="#f97316" />}
      >
        {/* Enable Aashray section */}
        <Text style={styles.sectionTitle}>Enable Aashray on Your Listings</Text>
        <Text style={styles.sectionSubtitle}>
          Toggle Aashray to offer discounted or free stays for verified NGOs.
        </Text>

        {loadingListings ? (
          <ActivityIndicator color="#f97316" style={{ marginTop: 24 }} size="large" />
        ) : listings.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>🏠</Text>
            <Text style={styles.emptyTitle}>No listings yet</Text>
            <Text style={styles.emptySubtitle}>Create a listing first to enable Aashray</Text>
          </View>
        ) : (
          listings.map((listing) => (
            <View key={listing.id} style={styles.listingCard}>
              <View style={styles.listingInfo}>
                <Text style={styles.listingTitle} numberOfLines={2}>{listing.title}</Text>
                <Text style={styles.listingCity}>{listing.city}{listing.state ? `, ${listing.state}` : ''}</Text>
                {listing.aashrayEnabled && (
                  <View style={styles.aashrayBadge}>
                    <Text style={styles.aashrayBadgeText}>Aashray Active</Text>
                  </View>
                )}
              </View>
              <View style={styles.toggleWrap}>
                {togglingId === listing.id ? (
                  <ActivityIndicator color="#f97316" size="small" />
                ) : (
                  <Switch
                    value={!!listing.aashrayEnabled}
                    onValueChange={(val) => handleToggleAashray(listing.id, val)}
                    trackColor={{ false: '#d1d5db', true: '#fed7aa' }}
                    thumbColor={listing.aashrayEnabled ? '#f97316' : '#9ca3af'}
                  />
                )}
              </View>
            </View>
          ))
        )}

        {/* Benefits card */}
        <Text style={styles.sectionTitle}>Benefits for Hosts</Text>
        <View style={styles.benefitsCard}>
          {BENEFITS.map((b, i) => (
            <View key={i} style={styles.benefitRow}>
              <Text style={styles.benefitIcon}>{b.icon}</Text>
              <Text style={styles.benefitLabel}>{b.label}</Text>
            </View>
          ))}
        </View>

        {/* How it works */}
        <Text style={styles.sectionTitle}>How It Works</Text>
        {HOW_IT_WORKS_HOST.map((item) => (
          <View key={item.step} style={styles.stepCard}>
            <View style={styles.stepCircle}>
              <Text style={styles.stepNumber}>{item.step}</Text>
            </View>
            <View style={styles.stepContent}>
              <Text style={styles.stepTitle}>{item.title}</Text>
              <Text style={styles.stepDesc}>{item.desc}</Text>
            </View>
          </View>
        ))}
      </ScrollView>
    );
  }

  function renderNgoTab() {
    return (
      <ScrollView style={styles.scrollBody} contentContainerStyle={styles.scrollContent}>
        {/* Search */}
        <View style={styles.searchBar}>
          <Text style={styles.searchIcon}>🔍</Text>
          <TextInput
            style={styles.searchInput}
            placeholder="Search by city..."
            placeholderTextColor="#9ca3af"
            value={searchCity}
            onChangeText={setSearchCity}
            onSubmitEditing={searchAashray}
            returnKeyType="search"
          />
        </View>

        {/* Available properties */}
        <Text style={styles.sectionTitle}>Available Aashray Properties</Text>

        {loadingSearch ? (
          <ActivityIndicator color="#f97316" style={{ marginTop: 24 }} size="large" />
        ) : aashrayResults.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>🏡</Text>
            <Text style={styles.emptyTitle}>No Aashray properties found</Text>
            <Text style={styles.emptySubtitle}>Try a different city or check back later</Text>
          </View>
        ) : (
          aashrayResults.map((property) => (
            <View key={property.id} style={styles.propertyCard}>
              <View style={styles.propertyHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.propertyTitle} numberOfLines={2}>{property.title}</Text>
                  <Text style={styles.propertyCity}>{property.city}</Text>
                  {property.hostName && (
                    <Text style={styles.propertyHost}>Hosted by {property.hostName}</Text>
                  )}
                </View>
                <View style={styles.aashrayTag}>
                  <Text style={styles.aashrayTagText}>Aashray</Text>
                </View>
              </View>
              <View style={styles.propertyFooter}>
                <Text style={styles.propertyPrice}>
                  {formatPaise(property.basePricePaise)}/night
                </Text>
                <TouchableOpacity
                  style={styles.requestBtn}
                  onPress={() => router.push(`/book/${property.id}`)}
                  activeOpacity={0.7}
                >
                  <Text style={styles.requestBtnText}>Request Stay</Text>
                </TouchableOpacity>
              </View>
            </View>
          ))
        )}

        {/* NGO Verification Info */}
        <Text style={styles.sectionTitle}>NGO Verification</Text>
        <View style={styles.verificationCard}>
          <Text style={styles.verificationTitle}>Required Documents</Text>
          <View style={styles.docRow}>
            <Text style={styles.docBullet}>1.</Text>
            <Text style={styles.docText}>NGO Registration Certificate (Section 8 / Trust / Society)</Text>
          </View>
          <View style={styles.docRow}>
            <Text style={styles.docBullet}>2.</Text>
            <Text style={styles.docText}>Darpan ID (NGO Darpan unique ID from NITI Aayog)</Text>
          </View>
          <View style={styles.docDivider} />
          <Text style={styles.verificationNote}>
            Upload these documents in your profile to get verified. Verified NGOs can request stays at Aashray properties at discounted or zero rates.
          </Text>
        </View>
      </ScrollView>
    );
  }

  return (
    <View style={styles.container}>
      {/* Hero Section */}
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Aashray</Text>
        <Text style={styles.heroSubtitle}>Shelter for those who serve</Text>
        <Text style={styles.heroDesc}>
          Safar's 0% commission program connecting NGOs with generous hosts who offer discounted or free accommodation for those making a difference.
        </Text>
      </View>

      {/* Tab Switcher */}
      <View style={styles.tabRow}>
        <TouchableOpacity
          style={[styles.tabBtn, tab === 'host' && styles.tabBtnActive]}
          onPress={() => setTab('host')}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabBtnText, tab === 'host' && styles.tabBtnTextActive]}>
            I'm a Host
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tabBtn, tab === 'ngo' && styles.tabBtnActive]}
          onPress={() => setTab('ngo')}
          activeOpacity={0.7}
        >
          <Text style={[styles.tabBtnText, tab === 'ngo' && styles.tabBtnTextActive]}>
            I'm an NGO
          </Text>
        </TouchableOpacity>
      </View>

      {/* Tab Content */}
      {tab === 'host' ? renderHostTab() : renderNgoTab()}
    </View>
  );
}

const styles = StyleSheet.create({
  container:            { flex: 1, backgroundColor: '#f9fafb' },

  /* Hero */
  hero:                 { backgroundColor: '#fef3c7', paddingHorizontal: 20, paddingTop: 20, paddingBottom: 24 },
  heroTitle:            { fontSize: 28, fontWeight: '800', color: '#92400e' },
  heroSubtitle:         { fontSize: 15, fontWeight: '600', color: '#b45309', marginTop: 2 },
  heroDesc:             { fontSize: 13, color: '#78350f', marginTop: 10, lineHeight: 20 },

  /* Tabs */
  tabRow:               { flexDirection: 'row', backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  tabBtn:               { flex: 1, paddingVertical: 14, alignItems: 'center' },
  tabBtnActive:         { borderBottomWidth: 3, borderBottomColor: '#f97316' },
  tabBtnText:           { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  tabBtnTextActive:     { color: '#f97316' },

  /* Scroll */
  scrollBody:           { flex: 1 },
  scrollContent:        { paddingBottom: 40 },

  /* Sections */
  sectionTitle:         { fontSize: 17, fontWeight: '700', color: '#111827', paddingHorizontal: 16, paddingTop: 20, paddingBottom: 4 },
  sectionSubtitle:      { fontSize: 13, color: '#6b7280', paddingHorizontal: 16, marginBottom: 12 },

  /* Listing toggle cards */
  listingCard:          { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', marginHorizontal: 16, marginBottom: 10, borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  listingInfo:          { flex: 1 },
  listingTitle:         { fontSize: 14, fontWeight: '700', color: '#111827' },
  listingCity:          { fontSize: 12, color: '#6b7280', marginTop: 2 },
  toggleWrap:           { marginLeft: 12 },
  aashrayBadge:         { backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 100, alignSelf: 'flex-start', marginTop: 6 },
  aashrayBadgeText:     { fontSize: 10, fontWeight: '700', color: '#15803d' },

  /* Benefits */
  benefitsCard:         { backgroundColor: '#fff', marginHorizontal: 16, borderRadius: 12, padding: 16, borderWidth: 1, borderColor: '#f3f4f6', marginTop: 8 },
  benefitRow:           { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  benefitIcon:          { fontSize: 20, width: 36, textAlign: 'center' },
  benefitLabel:         { fontSize: 13, fontWeight: '600', color: '#374151', flex: 1 },

  /* How it works */
  stepCard:             { flexDirection: 'row', alignItems: 'flex-start', marginHorizontal: 16, marginTop: 12 },
  stepCircle:           { width: 32, height: 32, borderRadius: 16, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  stepNumber:           { fontSize: 14, fontWeight: '800', color: '#fff' },
  stepContent:          { flex: 1 },
  stepTitle:            { fontSize: 14, fontWeight: '700', color: '#111827' },
  stepDesc:             { fontSize: 12, color: '#6b7280', marginTop: 2, lineHeight: 18 },

  /* Search */
  searchBar:            { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', margin: 16, marginBottom: 0, borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb', paddingHorizontal: 12 },
  searchIcon:           { fontSize: 16, marginRight: 8 },
  searchInput:          { flex: 1, fontSize: 14, color: '#111827', paddingVertical: 12 },

  /* Property cards (NGO) */
  propertyCard:         { backgroundColor: '#fff', marginHorizontal: 16, marginBottom: 12, borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  propertyHeader:       { flexDirection: 'row', alignItems: 'flex-start' },
  propertyTitle:        { fontSize: 15, fontWeight: '700', color: '#111827' },
  propertyCity:         { fontSize: 12, color: '#6b7280', marginTop: 2 },
  propertyHost:         { fontSize: 12, color: '#9ca3af', marginTop: 1 },
  propertyFooter:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 },
  propertyPrice:        { fontSize: 16, fontWeight: '800', color: '#f97316' },
  aashrayTag:           { backgroundColor: '#fef3c7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, marginLeft: 8 },
  aashrayTagText:       { fontSize: 10, fontWeight: '700', color: '#92400e' },

  requestBtn:           { backgroundColor: '#f97316', paddingHorizontal: 16, paddingVertical: 10, borderRadius: 10 },
  requestBtnText:       { fontSize: 13, fontWeight: '700', color: '#fff' },

  /* Verification card */
  verificationCard:     { backgroundColor: '#fff', marginHorizontal: 16, borderRadius: 12, padding: 16, borderWidth: 1, borderColor: '#f3f4f6', marginTop: 8 },
  verificationTitle:    { fontSize: 14, fontWeight: '700', color: '#111827', marginBottom: 10 },
  docRow:               { flexDirection: 'row', marginBottom: 8 },
  docBullet:            { fontSize: 13, fontWeight: '700', color: '#f97316', width: 20 },
  docText:              { fontSize: 13, color: '#374151', flex: 1, lineHeight: 19 },
  docDivider:           { height: 1, backgroundColor: '#f3f4f6', marginVertical: 12 },
  verificationNote:     { fontSize: 12, color: '#6b7280', lineHeight: 18 },

  /* Empty / auth */
  empty:                { alignItems: 'center', paddingTop: 40, paddingBottom: 20 },
  emptyIcon:            { fontSize: 48, marginBottom: 12 },
  emptyTitle:           { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:        { fontSize: 14, color: '#9ca3af', marginTop: 4, textAlign: 'center', paddingHorizontal: 32 },

  primaryBtn:           { backgroundColor: '#f97316', paddingHorizontal: 24, paddingVertical: 12, borderRadius: 10, marginTop: 16 },
  primaryBtnText:       { fontSize: 14, fontWeight: '700', color: '#fff' },
});
