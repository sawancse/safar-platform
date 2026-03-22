import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import * as Clipboard from 'expo-clipboard';

/* ── Types ─────────────────────────────────────────────────────── */

interface Listing {
  id: string;
  title: string;
}

interface ICalFeed {
  id: string;
  name: string;
  url: string;
  lastSyncedAt?: string;
}

/* ── Component ─────────────────────────────────────────────────── */

export default function HostChannelsScreen() {
  const router = useRouter();

  const [listings, setListings] = useState<Listing[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);

  const [feeds, setFeeds] = useState<ICalFeed[]>([]);
  const [exportUrl, setExportUrl] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [feedsLoading, setFeedsLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [syncingId, setSyncingId] = useState<string | null>(null);
  const [authed, setAuthed] = useState(false);

  const [showAddModal, setShowAddModal] = useState(false);
  const [newFeedName, setNewFeedName] = useState('');
  const [newFeedUrl, setNewFeedUrl] = useState('');
  const [adding, setAdding] = useState(false);

  /* ── Load listings ───────────────────────────────────────────── */

  const loadListings = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
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
  }, []);

  useEffect(() => {
    loadListings();
  }, [loadListings]);

  /* ── Load feeds when listing changes ─────────────────────────── */

  const loadFeeds = useCallback(async () => {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;
    setFeedsLoading(true);
    setExportUrl(null);
    try {
      const data = await api.getICalFeeds(selectedListingId, token);
      setFeeds(Array.isArray(data) ? data : []);
    } catch {
      setFeeds([]);
    } finally {
      setFeedsLoading(false);
    }
  }, [selectedListingId]);

  useEffect(() => {
    loadFeeds();
  }, [loadFeeds]);

  /* ── Export iCal ─────────────────────────────────────────────── */

  async function handleExport() {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;
    setExporting(true);
    try {
      const data = await api.exportICal(selectedListingId, token);
      const url = typeof data === 'string' ? data : data?.url || data?.icalUrl || '';
      setExportUrl(url);
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to generate export link');
    } finally {
      setExporting(false);
    }
  }

  async function copyExportUrl() {
    if (!exportUrl) return;
    try {
      await Clipboard.setStringAsync(exportUrl);
      Alert.alert('Copied', 'iCal URL copied to clipboard');
    } catch {
      Alert.alert('Error', 'Failed to copy URL');
    }
  }

  /* ── Sync feed ───────────────────────────────────────────────── */

  async function handleSync(feedId: string) {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;
    setSyncingId(feedId);
    try {
      await api.syncICalFeed(selectedListingId, feedId, token);
      Alert.alert('Synced', 'Calendar synced successfully');
      loadFeeds();
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to sync calendar');
    } finally {
      setSyncingId(null);
    }
  }

  /* ── Delete feed ─────────────────────────────────────────────── */

  function handleDelete(feedId: string, feedName: string) {
    Alert.alert(
      'Remove Calendar',
      `Are you sure you want to remove "${feedName}"? This cannot be undone.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: async () => {
            if (!selectedListingId) return;
            const token = await getAccessToken();
            if (!token) return;
            try {
              await api.deleteICalFeed(selectedListingId, feedId, token);
              setFeeds((prev) => prev.filter((f) => f.id !== feedId));
            } catch (e: any) {
              Alert.alert('Error', e.message || 'Failed to remove calendar');
            }
          },
        },
      ],
    );
  }

  /* ── Add feed ────────────────────────────────────────────────── */

  async function handleAdd() {
    if (!selectedListingId) return;
    if (!newFeedName.trim()) {
      Alert.alert('Missing Name', 'Please enter a name for this calendar');
      return;
    }
    if (!newFeedUrl.trim()) {
      Alert.alert('Missing URL', 'Please enter the iCal URL');
      return;
    }
    const token = await getAccessToken();
    if (!token) return;
    setAdding(true);
    try {
      await api.addICalFeed(selectedListingId, { name: newFeedName.trim(), url: newFeedUrl.trim() }, token);
      setShowAddModal(false);
      setNewFeedName('');
      setNewFeedUrl('');
      loadFeeds();
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to add calendar');
    } finally {
      setAdding(false);
    }
  }

  /* ── Helpers ─────────────────────────────────────────────────── */

  function truncateUrl(url: string, maxLen = 40): string {
    if (url.length <= maxLen) return url;
    return url.substring(0, maxLen) + '...';
  }

  function formatSyncDate(dateStr?: string): string {
    if (!dateStr) return 'Never synced';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  }

  /* ── Renders ─────────────────────────────────────────────────── */

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
        <Text style={styles.emptyTitle}>Sign in to access Channel Manager</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={styles.primaryBtnText}>Sign in</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (listings.length === 0) {
    return (
      <View style={styles.container}>
        {renderHeader()}
        <View style={styles.center}>
          <Text style={styles.emptyIcon}>🏠</Text>
          <Text style={styles.emptyTitle}>No listings yet</Text>
          <Text style={styles.emptySubtitle}>Create a listing first to manage channel syncing.</Text>
        </View>
      </View>
    );
  }

  const selectedListing = listings.find((l) => l.id === selectedListingId);

  return (
    <View style={styles.container}>
      {renderHeader()}

      <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
        {/* Info banner */}
        <View style={styles.infoBanner}>
          <Text style={styles.infoBannerTitle}>Channel Manager</Text>
          <Text style={styles.infoBannerText}>
            Sync your Safar calendar with Airbnb, Booking.com, and other platforms using iCal feeds.
            This prevents double bookings by keeping availability in sync across all channels.
          </Text>
        </View>

        {/* Listing selector */}
        <View style={styles.selectorContainer}>
          <Text style={styles.selectorLabel}>Listing</Text>
          <TouchableOpacity
            style={styles.selectorBtn}
            onPress={() => setShowDropdown(!showDropdown)}
          >
            <Text style={styles.selectorBtnText} numberOfLines={1}>
              {selectedListing?.title || 'Select listing'}
            </Text>
            <Text style={styles.selectorArrow}>{showDropdown ? '▲' : '▼'}</Text>
          </TouchableOpacity>

          {showDropdown && (
            <View style={styles.dropdown}>
              {listings.map((listing) => (
                <TouchableOpacity
                  key={listing.id}
                  style={[
                    styles.dropdownItem,
                    listing.id === selectedListingId && styles.dropdownItemActive,
                  ]}
                  onPress={() => {
                    setSelectedListingId(listing.id);
                    setShowDropdown(false);
                    setExportUrl(null);
                  }}
                >
                  <Text
                    style={[
                      styles.dropdownItemText,
                      listing.id === selectedListingId && styles.dropdownItemTextActive,
                    ]}
                    numberOfLines={1}
                  >
                    {listing.title || 'Untitled'}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>

        {/* Export section */}
        <View style={styles.sectionCard}>
          <Text style={styles.sectionTitle}>Export Safar Calendar</Text>
          <Text style={styles.sectionSubtitle}>
            Share this URL with Airbnb, Booking.com, etc. to export your Safar availability.
          </Text>

          {exportUrl ? (
            <View style={styles.exportUrlContainer}>
              <Text style={styles.exportUrlText} numberOfLines={2} selectable>
                {exportUrl}
              </Text>
              <TouchableOpacity style={styles.copyBtn} onPress={copyExportUrl}>
                <Text style={styles.copyBtnText}>Copy</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <TouchableOpacity
              style={styles.primaryBtn}
              onPress={handleExport}
              disabled={exporting}
            >
              {exporting ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <Text style={styles.primaryBtnText}>Generate Link</Text>
              )}
            </TouchableOpacity>
          )}
        </View>

        {/* Import section */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Connected Calendars</Text>
        </View>

        {feedsLoading ? (
          <View style={styles.feedsLoading}>
            <ActivityIndicator size="small" color="#f97316" />
          </View>
        ) : feeds.length === 0 ? (
          <View style={styles.emptyFeedsCard}>
            <Text style={styles.emptyFeedsText}>
              No calendars connected yet. Import an iCal feed from another platform to sync availability.
            </Text>
          </View>
        ) : (
          feeds.map((feed) => (
            <View key={feed.id} style={styles.feedCard}>
              <View style={styles.feedHeader}>
                <View style={styles.feedNameBadge}>
                  <Text style={styles.feedNameText}>{feed.name}</Text>
                </View>
              </View>

              <Text style={styles.feedUrl} numberOfLines={1}>
                {truncateUrl(feed.url)}
              </Text>

              <Text style={styles.feedSync}>
                Last synced: {formatSyncDate(feed.lastSyncedAt)}
              </Text>

              <View style={styles.feedActions}>
                <TouchableOpacity
                  style={styles.syncBtn}
                  onPress={() => handleSync(feed.id)}
                  disabled={syncingId === feed.id}
                >
                  {syncingId === feed.id ? (
                    <ActivityIndicator size="small" color="#fff" />
                  ) : (
                    <Text style={styles.syncBtnText}>Sync Now</Text>
                  )}
                </TouchableOpacity>

                <TouchableOpacity
                  style={styles.deleteBtn}
                  onPress={() => handleDelete(feed.id, feed.name)}
                >
                  <Text style={styles.deleteBtnText}>Remove</Text>
                </TouchableOpacity>
              </View>
            </View>
          ))
        )}

        {/* Add calendar button */}
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => setShowAddModal(true)}
        >
          <Text style={styles.addBtnText}>+ Add Calendar Feed</Text>
        </TouchableOpacity>

        <View style={{ height: 40 }} />
      </ScrollView>

      {/* Add feed modal */}
      <Modal
        visible={showAddModal}
        transparent
        animationType="slide"
        onRequestClose={() => setShowAddModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Add Calendar Feed</Text>
            <Text style={styles.modalSubtitle}>
              Import an iCal (.ics) URL from Airbnb, Booking.com, or any other platform.
            </Text>

            <Text style={styles.inputLabel}>Name</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g., Airbnb Calendar"
              placeholderTextColor="#9ca3af"
              value={newFeedName}
              onChangeText={setNewFeedName}
              autoCapitalize="words"
            />

            <Text style={styles.inputLabel}>iCal URL</Text>
            <TextInput
              style={styles.input}
              placeholder="https://..."
              placeholderTextColor="#9ca3af"
              value={newFeedUrl}
              onChangeText={setNewFeedUrl}
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
            />

            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => {
                  setShowAddModal(false);
                  setNewFeedName('');
                  setNewFeedUrl('');
                }}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.modalAddBtn}
                onPress={handleAdd}
                disabled={adding}
              >
                {adding ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.modalAddText}>Add</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );

  /* ── Header ──────────────────────────────────────────────────── */

  function renderHeader() {
    return (
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backBtn}>{'<'} Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Channel Manager</Text>
        <View style={{ width: 60 }} />
      </View>
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

  /* Empty states */
  emptyIcon:      { fontSize: 48, marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 8 },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af', textAlign: 'center' },

  /* Primary button */
  primaryBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 12, alignSelf: 'flex-start' },
  primaryBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },

  /* Info banner */
  infoBanner:      { backgroundColor: '#fff7ed', borderRadius: 16, padding: 16, marginBottom: 16, borderWidth: 1, borderColor: '#fed7aa' },
  infoBannerTitle: { fontSize: 15, fontWeight: '700', color: '#9a3412', marginBottom: 6 },
  infoBannerText:  { fontSize: 13, color: '#c2410c', lineHeight: 20 },

  /* Listing selector */
  selectorContainer: { marginBottom: 16, zIndex: 10 },
  selectorLabel:     { fontSize: 12, fontWeight: '600', color: '#6b7280', marginBottom: 6, textTransform: 'uppercase' },
  selectorBtn:       { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb', paddingHorizontal: 14, paddingVertical: 12 },
  selectorBtnText:   { fontSize: 14, fontWeight: '600', color: '#111827', flex: 1, marginRight: 8 },
  selectorArrow:     { fontSize: 12, color: '#9ca3af' },

  dropdown:              { backgroundColor: '#fff', borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb', marginTop: 4, overflow: 'hidden', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 8 },
  dropdownItem:          { paddingHorizontal: 14, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  dropdownItemActive:    { backgroundColor: '#fff7ed' },
  dropdownItemText:      { fontSize: 14, color: '#374151' },
  dropdownItemTextActive: { color: '#f97316', fontWeight: '600' },

  /* Section card */
  sectionCard:     { backgroundColor: '#fff', borderRadius: 16, padding: 16, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  sectionHeader:   { marginBottom: 12 },
  sectionTitle:    { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 4 },
  sectionSubtitle: { fontSize: 13, color: '#6b7280', lineHeight: 19 },

  /* Export URL */
  exportUrlContainer: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#f3f4f6', borderRadius: 10, padding: 12, marginTop: 12, gap: 8 },
  exportUrlText:      { flex: 1, fontSize: 12, color: '#374151', fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace' },
  copyBtn:            { backgroundColor: '#f97316', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  copyBtnText:        { color: '#fff', fontSize: 13, fontWeight: '600' },

  /* Feed cards */
  feedsLoading:    { height: 100, alignItems: 'center', justifyContent: 'center' },
  emptyFeedsCard:  { backgroundColor: '#fff', borderRadius: 16, padding: 20, marginBottom: 16, alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  emptyFeedsText:  { fontSize: 13, color: '#9ca3af', textAlign: 'center', lineHeight: 20 },

  feedCard:      { backgroundColor: '#fff', borderRadius: 16, padding: 16, marginBottom: 12, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  feedHeader:    { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  feedNameBadge: { backgroundColor: '#fff7ed', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 4 },
  feedNameText:  { fontSize: 14, fontWeight: '700', color: '#f97316' },
  feedUrl:       { fontSize: 12, color: '#9ca3af', marginBottom: 4, fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace' },
  feedSync:      { fontSize: 12, color: '#6b7280', marginBottom: 12 },
  feedActions:   { flexDirection: 'row', gap: 8 },

  syncBtn:      { backgroundColor: '#f97316', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  syncBtnText:  { color: '#fff', fontSize: 13, fontWeight: '600' },
  deleteBtn:    { backgroundColor: '#fee2e2', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  deleteBtnText: { color: '#dc2626', fontSize: 13, fontWeight: '600' },

  /* Add button */
  addBtn:     { backgroundColor: '#fff', borderRadius: 12, borderWidth: 2, borderColor: '#f97316', borderStyle: 'dashed', paddingVertical: 14, alignItems: 'center', marginTop: 4 },
  addBtnText: { fontSize: 14, fontWeight: '600', color: '#f97316' },

  /* Modal */
  modalOverlay:   { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContent:   { backgroundColor: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 24, paddingBottom: Platform.OS === 'ios' ? 40 : 24 },
  modalTitle:     { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 4 },
  modalSubtitle:  { fontSize: 13, color: '#6b7280', marginBottom: 20, lineHeight: 19 },

  inputLabel: { fontSize: 12, fontWeight: '600', color: '#6b7280', marginBottom: 6, textTransform: 'uppercase' },
  input:      { backgroundColor: '#f9fafb', borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb', paddingHorizontal: 14, paddingVertical: 12, fontSize: 14, color: '#111827', marginBottom: 16 },

  modalActions:    { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 8 },
  modalCancelBtn:  { borderRadius: 12, paddingHorizontal: 20, paddingVertical: 12, backgroundColor: '#f3f4f6' },
  modalCancelText: { fontSize: 14, fontWeight: '600', color: '#374151' },
  modalAddBtn:     { borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, backgroundColor: '#f97316', minWidth: 80, alignItems: 'center' },
  modalAddText:    { fontSize: 14, fontWeight: '600', color: '#fff' },
});
