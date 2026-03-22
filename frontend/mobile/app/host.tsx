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
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Status configs ──────────────────────────────────────────── */

const LISTING_STATUS: Record<string, { label: string; bg: string; text: string }> = {
  DRAFT:                { label: 'Draft',               bg: '#f3f4f6', text: '#374151' },
  PENDING_VERIFICATION: { label: 'Pending Verification', bg: '#fef9c3', text: '#854d0e' },
  VERIFIED:             { label: 'Verified',            bg: '#dcfce7', text: '#14532d' },
  REJECTED:             { label: 'Rejected',            bg: '#fee2e2', text: '#7f1d1d' },
};

const BOOKING_STATUS: Record<string, { label: string; bg: string; text: string }> = {
  PENDING_PAYMENT: { label: 'Pending Payment', bg: '#fef9c3', text: '#854d0e' },
  CONFIRMED:       { label: 'Confirmed',       bg: '#dbeafe', text: '#1e3a8a' },
  CHECKED_IN:      { label: 'Checked In',      bg: '#dcfce7', text: '#14532d' },
  COMPLETED:       { label: 'Completed',       bg: '#f3f4f6', text: '#374151' },
  CANCELLED:       { label: 'Cancelled',       bg: '#fee2e2', text: '#7f1d1d' },
  NO_SHOW:         { label: 'No Show',         bg: '#fce7f3', text: '#831843' },
};

const FILTER_OPTIONS = ['All', 'Pending', 'Confirmed', 'Checked In', 'Completed', 'Cancelled'];
const FILTER_MAP: Record<string, string | null> = {
  All: null,
  Pending: 'PENDING_PAYMENT',
  Confirmed: 'CONFIRMED',
  'Checked In': 'CHECKED_IN',
  Completed: 'COMPLETED',
  Cancelled: 'CANCELLED',
};

function formatPayout(paise: number) {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

/* ── Component ───────────────────────────────────────────────── */

export default function HostScreen() {
  const router = useRouter();

  const [tab, setTab] = useState<'bookings' | 'listings' | 'manage'>('bookings');
  const [filter, setFilter] = useState('All');
  const [bookings, setBookings] = useState<any[]>([]);
  const [listings, setListings] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const loadData = useCallback(async (silent = false) => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    if (!silent) setLoading(true);
    try {
      const [b, l] = await Promise.all([
        api.getHostBookings(token),
        api.getMyListings(token),
      ]);
      setBookings(b);
      setListings(l);
    } catch {
      setBookings([]);
      setListings([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadData(true);
  }, [loadData]);

  /* ── Booking actions ─────────────────────────────────────── */

  async function handleAction(
    bookingId: string,
    action: 'confirm' | 'checkIn' | 'complete' | 'noShow',
  ) {
    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(bookingId);
    try {
      let updated: any;
      switch (action) {
        case 'confirm':
          updated = await api.confirmBooking(bookingId, token);
          break;
        case 'checkIn':
          updated = await api.checkInBooking(bookingId, token);
          break;
        case 'complete':
          updated = await api.completeBooking(bookingId, token);
          break;
        case 'noShow':
          updated = await api.markNoShow(bookingId, token);
          break;
      }
      setBookings((prev) => prev.map((b) => (b.id === bookingId ? updated : b)));
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Action failed');
    } finally {
      setActionLoading(null);
    }
  }

  function handleCancel(bookingId: string) {
    Alert.alert('Cancel Booking', 'Enter a reason for cancellation:', [
      {
        text: 'Change of plans',
        onPress: () => doCancel(bookingId, 'Change of plans'),
      },
      {
        text: 'Guest requested',
        onPress: () => doCancel(bookingId, 'Guest requested'),
      },
      {
        text: 'Property unavailable',
        onPress: () => doCancel(bookingId, 'Property unavailable'),
      },
      {
        text: 'Other reason',
        onPress: () => doCancel(bookingId, 'Other reason'),
      },
      { text: 'Back', style: 'cancel' },
    ]);
  }

  async function doCancel(bookingId: string, reason: string) {
    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(bookingId);
    try {
      const updated = await api.cancelBooking(bookingId, reason, token);
      setBookings((prev) => prev.map((b) => (b.id === bookingId ? updated : b)));
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to cancel booking');
    } finally {
      setActionLoading(null);
    }
  }

  /* ── Renders ─────────────────────────────────────────────── */

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
        <Text style={styles.emptyTitle}>Sign in to access host dashboard</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={styles.primaryBtnText}>Sign in</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const filteredBookings =
    FILTER_MAP[filter] === null
      ? bookings
      : bookings.filter((b) => b.status === FILTER_MAP[filter]);

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backBtn}>{'<'} Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Host Dashboard</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Tab Switcher */}
      <View style={styles.tabRow}>
        <TouchableOpacity
          style={[styles.tab, tab === 'bookings' && styles.tabActive]}
          onPress={() => setTab('bookings')}
        >
          <Text style={[styles.tabText, tab === 'bookings' && styles.tabTextActive]}>
            Bookings
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, tab === 'listings' && styles.tabActive]}
          onPress={() => setTab('listings')}
        >
          <Text style={[styles.tabText, tab === 'listings' && styles.tabTextActive]}>
            Listings
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, tab === 'manage' && styles.tabActive]}
          onPress={() => setTab('manage')}
        >
          <Text style={[styles.tabText, tab === 'manage' && styles.tabTextActive]}>
            Manage
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#f97316']} />}
      >
        {tab === 'bookings' ? renderBookings() : tab === 'listings' ? renderListings() : renderManage()}
      </ScrollView>
    </View>
  );

  /* ── Bookings tab ────────────────────────────────────────── */

  function renderBookings() {
    return (
      <>
        {/* Filter pills */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterRow}>
          {FILTER_OPTIONS.map((f) => (
            <TouchableOpacity
              key={f}
              style={[styles.filterPill, filter === f && styles.filterPillActive]}
              onPress={() => setFilter(f)}
            >
              <Text style={[styles.filterText, filter === f && styles.filterTextActive]}>{f}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {filteredBookings.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyIcon}>📋</Text>
            <Text style={styles.emptyTitle}>No bookings found</Text>
            <Text style={styles.emptySubtitle}>
              {filter === 'All' ? 'You have no bookings yet.' : `No ${filter.toLowerCase()} bookings.`}
            </Text>
          </View>
        ) : (
          filteredBookings.map((booking) => renderBookingCard(booking))
        )}
      </>
    );
  }

  function renderBookingCard(booking: any) {
    const s = BOOKING_STATUS[booking.status] ?? BOOKING_STATUS.COMPLETED;
    const isActioning = actionLoading === booking.id;
    const nights =
      booking.checkInDate && booking.checkOutDate
        ? Math.max(
            1,
            Math.round(
              (new Date(booking.checkOutDate).getTime() - new Date(booking.checkInDate).getTime()) /
                (1000 * 60 * 60 * 24),
            ),
          )
        : 1;

    return (
      <View key={booking.id} style={styles.card}>
        {/* Ref + Status */}
        <View style={styles.cardTopRow}>
          <Text style={styles.bookingRef}>#{booking.bookingRef || booking.id?.slice(0, 8)}</Text>
          <Text style={[styles.statusBadge, { backgroundColor: s.bg, color: s.text }]}>
            {s.label}
          </Text>
        </View>

        {/* Guest info */}
        <View style={styles.infoSection}>
          <Text style={styles.infoLabel}>Guest</Text>
          <Text style={styles.infoValue}>{booking.guestName || 'N/A'}</Text>
          {booking.guestPhone && (
            <Text style={styles.infoSubValue}>{booking.guestPhone}</Text>
          )}
        </View>

        {/* Stay details */}
        <View style={styles.infoSection}>
          <Text style={styles.infoLabel}>Stay</Text>
          <Text style={styles.infoValue}>
            {booking.checkInDate} → {booking.checkOutDate}
          </Text>
          <Text style={styles.infoSubValue}>
            {nights} night{nights > 1 ? 's' : ''} · {booking.guests || booking.numberOfGuests || 1} guest
            {(booking.guests || booking.numberOfGuests || 1) > 1 ? 's' : ''}
          </Text>
        </View>

        {/* Payout */}
        <View style={styles.infoSection}>
          <Text style={styles.infoLabel}>Host Payout</Text>
          <Text style={styles.payoutAmount}>
            {formatPayout(booking.hostPayoutPaise ?? booking.totalAmountPaise ?? 0)}
          </Text>
        </View>

        {/* Special requests */}
        {booking.specialRequests ? (
          <View style={styles.infoSection}>
            <Text style={styles.infoLabel}>Special Requests</Text>
            <Text style={styles.infoSubValue}>{booking.specialRequests}</Text>
          </View>
        ) : null}

        {/* Action buttons */}
        {renderActions(booking, isActioning)}
      </View>
    );
  }

  function renderActions(booking: any, isActioning: boolean) {
    if (isActioning) {
      return (
        <View style={styles.actionRow}>
          <ActivityIndicator size="small" color="#f97316" />
        </View>
      );
    }

    switch (booking.status) {
      case 'PENDING_PAYMENT':
        return (
          <View style={styles.actionRow}>
            <TouchableOpacity
              style={styles.blueBtn}
              onPress={() => handleAction(booking.id, 'confirm')}
            >
              <Text style={styles.blueBtnText}>Confirm (Cash)</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.redBtn}
              onPress={() => handleCancel(booking.id)}
            >
              <Text style={styles.redBtnText}>Reject</Text>
            </TouchableOpacity>
          </View>
        );

      case 'CONFIRMED':
        return (
          <View style={styles.actionRow}>
            <TouchableOpacity
              style={styles.greenBtn}
              onPress={() => handleAction(booking.id, 'checkIn')}
            >
              <Text style={styles.greenBtnText}>Check In</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.grayBtn}
              onPress={() => handleAction(booking.id, 'noShow')}
            >
              <Text style={styles.grayBtnText}>No Show</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.redOutlineBtn}
              onPress={() => handleCancel(booking.id)}
            >
              <Text style={styles.redOutlineBtnText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        );

      case 'CHECKED_IN':
        return (
          <View style={styles.actionRow}>
            <TouchableOpacity
              style={styles.greenBtn}
              onPress={() => handleAction(booking.id, 'complete')}
            >
              <Text style={styles.greenBtnText}>Mark Complete</Text>
            </TouchableOpacity>
          </View>
        );

      default:
        return null;
    }
  }

  /* ── Listings tab ────────────────────────────────────────── */

  function renderListings() {
    return (
      <>
        <TouchableOpacity
          style={styles.createBtn}
          onPress={() => router.push('/host-new-listing')}
        >
          <Text style={styles.createBtnText}>+ Create New Listing</Text>
        </TouchableOpacity>

        {listings.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyIcon}>🏠</Text>
            <Text style={styles.emptyTitle}>No listings yet</Text>
            <Text style={styles.emptySubtitle}>Create your first listing to start hosting.</Text>
          </View>
        ) : (
          listings.map((listing) => {
            const s = LISTING_STATUS[listing.status] ?? LISTING_STATUS.DRAFT;
            const pricePaise = listing.basePricePaise ?? listing.pricePerNightPaise ?? listing.pricePerNight ?? 0;
            const unit = listing.pricingUnit === 'MONTH' ? '/ month' : listing.pricingUnit === 'HOUR' ? '/ hour' : '/ night';

            return (
              <TouchableOpacity
                key={listing.id}
                style={styles.card}
                onPress={() => router.push(`/listing/${listing.id}`)}
              >
                <View style={styles.cardTopRow}>
                  <Text style={styles.listingTitle} numberOfLines={1}>
                    {listing.title || 'Untitled Listing'}
                  </Text>
                  <Text style={[styles.statusBadge, { backgroundColor: s.bg, color: s.text }]}>
                    {s.label}
                  </Text>
                </View>
                <Text style={styles.listingCity}>{listing.city || listing.address?.city || '—'}</Text>
                <Text style={styles.listingPrice}>
                  {formatPayout(pricePaise)} <Text style={styles.listingPriceUnit}>{unit}</Text>
                </Text>
              </TouchableOpacity>
            );
          })
        )}
      </>
    );
  }

  /* ── Manage tab ─────────────────────────────────────────── */

  function renderManage() {
    const items = [
      { icon: '📅', label: 'Calendar', desc: 'Manage availability', route: '/host-calendar' },
      { icon: '💰', label: 'Earnings', desc: 'Revenue & invoices', route: '/host-earnings' },
      { icon: '📊', label: 'Analytics', desc: 'Occupancy & metrics', route: '/host-analytics' },
      { icon: '💳', label: 'Transactions', desc: 'Payment history', route: '/host-transactions' },
      { icon: '⭐', label: 'Reviews', desc: 'Guest reviews & replies', route: '/host-reviews' },
      { icon: '💬', label: 'Messages', desc: 'Guest conversations', route: '/host-messages' },
      { icon: '🛏️', label: 'Room Types', desc: 'Rooms & inclusions', route: '/host-rooms' },
      { icon: '💲', label: 'Pricing Rules', desc: 'Seasonal & dynamic', route: '/host-pricing' },
      { icon: '📦', label: 'PG Packages', desc: 'Monthly packages', route: '/host-packages' },
      { icon: '🔗', label: 'Channel Manager', desc: 'iCal sync', route: '/host-channels' },
      { icon: '👥', label: 'Tenants', desc: 'PG tenant management', route: '/host-tenants' },
      { icon: '📋', label: 'KYC', desc: 'Identity verification', route: '/host-kyc' },
      { icon: '💎', label: 'Subscription', desc: 'Plan & billing', route: '/subscription' },
    ];

    return (
      <>
        {items.map((item) => (
          <TouchableOpacity
            key={item.label}
            style={styles.manageItem}
            onPress={() => router.push(item.route as any)}
          >
            <Text style={styles.manageIcon}>{item.icon}</Text>
            <View style={styles.manageContent}>
              <Text style={styles.manageLabel}>{item.label}</Text>
              <Text style={styles.manageDesc}>{item.desc}</Text>
            </View>
            <Text style={styles.manageArrow}>›</Text>
          </TouchableOpacity>
        ))}
      </>
    );
  }
}

/* ── Styles ──────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  center:        { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  scrollView:    { flex: 1 },
  scrollContent: { padding: 16, paddingBottom: 40 },

  /* Header */
  header:      { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingTop: 56, paddingBottom: 12, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn:     { fontSize: 14, color: '#f97316', fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },

  /* Tabs */
  tabRow:        { flexDirection: 'row', backgroundColor: '#fff', paddingHorizontal: 16, paddingBottom: 12 },
  tab:           { flex: 1, alignItems: 'center', paddingVertical: 10, borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive:     { borderBottomColor: '#f97316' },
  tabText:       { fontSize: 14, fontWeight: '600', color: '#9ca3af' },
  tabTextActive: { color: '#f97316' },

  /* Filter pills */
  filterRow:        { flexGrow: 0, marginBottom: 12 },
  filterPill:       { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20, backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', marginRight: 8 },
  filterPillActive: { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterText:       { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  filterTextActive: { color: '#fff' },

  /* Cards */
  card:       { backgroundColor: '#fff', borderRadius: 16, padding: 16, marginBottom: 12, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  cardTopRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },

  /* Status badge */
  statusBadge: { fontSize: 11, fontWeight: '600', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 12, overflow: 'hidden' },

  /* Booking card */
  bookingRef:   { fontSize: 14, fontWeight: '700', color: '#111827' },
  infoSection:  { marginBottom: 8 },
  infoLabel:    { fontSize: 11, fontWeight: '600', color: '#9ca3af', textTransform: 'uppercase', marginBottom: 2 },
  infoValue:    { fontSize: 14, fontWeight: '600', color: '#111827' },
  infoSubValue: { fontSize: 12, color: '#6b7280', marginTop: 1 },
  payoutAmount: { fontSize: 16, fontWeight: '700', color: '#059669' },

  /* Action buttons */
  actionRow:         { flexDirection: 'row', flexWrap: 'wrap', marginTop: 10, gap: 8 },
  blueBtn:           { backgroundColor: '#3b82f6', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  blueBtnText:       { fontSize: 12, fontWeight: '600', color: '#fff' },
  greenBtn:          { backgroundColor: '#16a34a', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  greenBtnText:      { fontSize: 12, fontWeight: '600', color: '#fff' },
  grayBtn:           { backgroundColor: '#e5e7eb', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  grayBtnText:       { fontSize: 12, fontWeight: '600', color: '#374151' },
  redBtn:            { backgroundColor: '#dc2626', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  redBtnText:        { fontSize: 12, fontWeight: '600', color: '#fff' },
  redOutlineBtn:     { borderWidth: 1, borderColor: '#fca5a5', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8, backgroundColor: '#fef2f2' },
  redOutlineBtnText: { fontSize: 12, fontWeight: '600', color: '#dc2626' },

  /* Listing card */
  listingTitle:     { fontSize: 15, fontWeight: '600', color: '#111827', flex: 1, marginRight: 8 },
  listingCity:      { fontSize: 13, color: '#6b7280', marginBottom: 4 },
  listingPrice:     { fontSize: 16, fontWeight: '700', color: '#111827' },
  listingPriceUnit: { fontSize: 12, fontWeight: '400', color: '#9ca3af' },

  /* Empty states */
  emptyContainer: { alignItems: 'center', paddingTop: 60 },
  emptyIcon:      { fontSize: 48, marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 8 },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af', textAlign: 'center' },

  /* Create listing */
  createBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginBottom: 16 },
  createBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },

  /* Manage tab */
  manageItem:    { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.04, shadowRadius: 3, elevation: 1 },
  manageIcon:    { fontSize: 24, marginRight: 14, width: 32, textAlign: 'center' },
  manageContent: { flex: 1 },
  manageLabel:   { fontSize: 15, fontWeight: '600', color: '#111827' },
  manageDesc:    { fontSize: 12, color: '#9ca3af', marginTop: 2 },
  manageArrow:   { fontSize: 22, color: '#d1d5db', marginLeft: 8 },

  /* Shared buttons */
  primaryBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 16 },
  primaryBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },
});
