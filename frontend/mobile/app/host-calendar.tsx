import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Helpers ─────────────────────────────────────────────────── */

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const DAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfWeek(year: number, month: number): number {
  return new Date(year, month, 1).getDay();
}

function formatDate(year: number, month: number, day: number): string {
  const m = String(month + 1).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${year}-${m}-${d}`;
}

type DayStatus = 'AVAILABLE' | 'BLOCKED' | 'BOOKED';

const STATUS_COLORS: Record<DayStatus, { bg: string; text: string }> = {
  AVAILABLE: { bg: '#dcfce7', text: '#14532d' },
  BLOCKED:   { bg: '#fee2e2', text: '#7f1d1d' },
  BOOKED:    { bg: '#dbeafe', text: '#1e3a8a' },
};

/* ── Component ───────────────────────────────────────────────── */

export default function HostCalendarScreen() {
  const router = useRouter();

  const today = new Date();
  const [currentYear, setCurrentYear] = useState(today.getFullYear());
  const [currentMonth, setCurrentMonth] = useState(today.getMonth());

  const [listings, setListings] = useState<any[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);

  const [availability, setAvailability] = useState<Record<string, DayStatus>>({});
  const [selectionStart, setSelectionStart] = useState<number | null>(null);
  const [selectionEnd, setSelectionEnd] = useState<number | null>(null);

  const [loading, setLoading] = useState(true);
  const [calendarLoading, setCalendarLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [authed, setAuthed] = useState(false);

  /* ── Load listings ─────────────────────────────────────────── */

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

  /* ── Load availability when listing or month changes ───────── */

  const loadAvailability = useCallback(async () => {
    if (!selectedListingId) return;
    const token = await getAccessToken();
    if (!token) return;
    setCalendarLoading(true);
    try {
      const data = await api.getAvailabilityMonth(
        selectedListingId,
        currentYear,
        currentMonth + 1,
        token,
      );
      const map: Record<string, DayStatus> = {};
      if (Array.isArray(data)) {
        data.forEach((entry: any) => {
          map[entry.date] = entry.status || 'AVAILABLE';
        });
      } else if (data && typeof data === 'object') {
        Object.keys(data).forEach((date) => {
          map[date] = data[date] || 'AVAILABLE';
        });
      }
      setAvailability(map);
    } catch {
      setAvailability({});
    } finally {
      setCalendarLoading(false);
    }
  }, [selectedListingId, currentYear, currentMonth]);

  useEffect(() => {
    loadAvailability();
  }, [loadAvailability]);

  /* ── Month navigation ──────────────────────────────────────── */

  function prevMonth() {
    clearSelection();
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear(currentYear - 1);
    } else {
      setCurrentMonth(currentMonth - 1);
    }
  }

  function nextMonth() {
    clearSelection();
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear(currentYear + 1);
    } else {
      setCurrentMonth(currentMonth + 1);
    }
  }

  /* ── Date selection ────────────────────────────────────────── */

  function clearSelection() {
    setSelectionStart(null);
    setSelectionEnd(null);
  }

  function handleDayPress(day: number) {
    const dateStr = formatDate(currentYear, currentMonth, day);
    const status = availability[dateStr];
    if (status === 'BOOKED') return;

    if (selectionStart === null) {
      setSelectionStart(day);
      setSelectionEnd(day);
    } else if (selectionEnd === selectionStart && day !== selectionStart) {
      const start = Math.min(selectionStart, day);
      const end = Math.max(selectionStart, day);
      setSelectionStart(start);
      setSelectionEnd(end);
    } else {
      setSelectionStart(day);
      setSelectionEnd(day);
    }
  }

  function isDaySelected(day: number): boolean {
    if (selectionStart === null || selectionEnd === null) return false;
    return day >= selectionStart && day <= selectionEnd;
  }

  /* ── Bulk update ───────────────────────────────────────────── */

  async function bulkUpdate(newStatus: 'AVAILABLE' | 'BLOCKED') {
    if (!selectedListingId || selectionStart === null || selectionEnd === null) return;
    const token = await getAccessToken();
    if (!token) return;

    const dates: string[] = [];
    for (let d = selectionStart; d <= selectionEnd; d++) {
      const dateStr = formatDate(currentYear, currentMonth, d);
      if (availability[dateStr] !== 'BOOKED') {
        dates.push(dateStr);
      }
    }

    if (dates.length === 0) return;

    setUpdating(true);
    try {
      await api.bulkUpdateAvailability(
        selectedListingId,
        { dates, status: newStatus },
        token,
      );
      const updated = { ...availability };
      dates.forEach((date) => {
        updated[date] = newStatus;
      });
      setAvailability(updated);
      clearSelection();
      Alert.alert('Updated', `${dates.length} day(s) marked as ${newStatus.toLowerCase()}.`);
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to update availability');
    } finally {
      setUpdating(false);
    }
  }

  /* ── Renders ───────────────────────────────────────────────── */

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
        <Text style={styles.emptyTitle}>Sign in to access calendar</Text>
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
          <Text style={styles.emptySubtitle}>Create a listing first to manage availability.</Text>
        </View>
      </View>
    );
  }

  const selectedListing = listings.find((l) => l.id === selectedListingId);
  const daysInMonth = getDaysInMonth(currentYear, currentMonth);
  const firstDay = getFirstDayOfWeek(currentYear, currentMonth);
  const hasSelection = selectionStart !== null && selectionEnd !== null;

  return (
    <View style={styles.container}>
      {renderHeader()}

      <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
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
                    clearSelection();
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

        {/* Month navigation */}
        <View style={styles.monthNav}>
          <TouchableOpacity onPress={prevMonth} style={styles.monthArrow}>
            <Text style={styles.monthArrowText}>{'<'}</Text>
          </TouchableOpacity>
          <Text style={styles.monthTitle}>
            {MONTH_NAMES[currentMonth]} {currentYear}
          </Text>
          <TouchableOpacity onPress={nextMonth} style={styles.monthArrow}>
            <Text style={styles.monthArrowText}>{'>'}</Text>
          </TouchableOpacity>
        </View>

        {/* Calendar grid */}
        {calendarLoading ? (
          <View style={styles.calendarLoading}>
            <ActivityIndicator size="small" color="#f97316" />
          </View>
        ) : (
          <View style={styles.calendarContainer}>
            {/* Day headers */}
            <View style={styles.calendarRow}>
              {DAY_LABELS.map((label) => (
                <View key={label} style={styles.dayHeaderCell}>
                  <Text style={styles.dayHeaderText}>{label}</Text>
                </View>
              ))}
            </View>

            {/* Day cells */}
            {renderCalendarRows(daysInMonth, firstDay)}
          </View>
        )}

        {/* Legend */}
        <View style={styles.legendContainer}>
          <View style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: '#dcfce7' }]} />
            <Text style={styles.legendText}>Available</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: '#fee2e2' }]} />
            <Text style={styles.legendText}>Blocked</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: '#dbeafe' }]} />
            <Text style={styles.legendText}>Booked</Text>
          </View>
          <View style={styles.legendItem}>
            <View style={[styles.legendDot, { backgroundColor: '#fff', borderWidth: 2, borderColor: '#f97316' }]} />
            <Text style={styles.legendText}>Selected</Text>
          </View>
        </View>

        {/* Bulk action buttons */}
        {hasSelection && (
          <View style={styles.actionContainer}>
            <Text style={styles.actionLabel}>
              {selectionStart === selectionEnd
                ? `Selected: ${formatDate(currentYear, currentMonth, selectionStart!)}`
                : `Selected: ${formatDate(currentYear, currentMonth, selectionStart!)} to ${formatDate(currentYear, currentMonth, selectionEnd!)}`}
            </Text>
            <View style={styles.actionRow}>
              <TouchableOpacity
                style={styles.greenBtn}
                onPress={() => bulkUpdate('AVAILABLE')}
                disabled={updating}
              >
                {updating ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.greenBtnText}>Mark Available</Text>
                )}
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.redBtn}
                onPress={() => bulkUpdate('BLOCKED')}
                disabled={updating}
              >
                {updating ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.redBtnText}>Mark Blocked</Text>
                )}
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.grayBtn}
                onPress={clearSelection}
                disabled={updating}
              >
                <Text style={styles.grayBtnText}>Clear</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </ScrollView>
    </View>
  );

  /* ── Header ────────────────────────────────────────────────── */

  function renderHeader() {
    return (
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backBtn}>{'<'} Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Calendar</Text>
        <View style={{ width: 60 }} />
      </View>
    );
  }

  /* ── Calendar rows ─────────────────────────────────────────── */

  function renderCalendarRows(daysInMonth: number, firstDay: number) {
    const rows: JSX.Element[] = [];
    let cells: JSX.Element[] = [];

    // Empty cells before the first day
    for (let i = 0; i < firstDay; i++) {
      cells.push(<View key={`empty-${i}`} style={styles.dayCell} />);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = formatDate(currentYear, currentMonth, day);
      const status = availability[dateStr] as DayStatus | undefined;
      const colors = status ? STATUS_COLORS[status] : { bg: '#f9fafb', text: '#6b7280' };
      const selected = isDaySelected(day);
      const isBooked = status === 'BOOKED';

      cells.push(
        <TouchableOpacity
          key={day}
          style={[
            styles.dayCell,
            { backgroundColor: colors.bg },
            selected && styles.dayCellSelected,
          ]}
          onPress={() => handleDayPress(day)}
          activeOpacity={isBooked ? 1 : 0.6}
        >
          <Text
            style={[
              styles.dayText,
              { color: colors.text },
              selected && styles.dayTextSelected,
            ]}
          >
            {day}
          </Text>
        </TouchableOpacity>,
      );

      if ((firstDay + day) % 7 === 0 || day === daysInMonth) {
        // Pad remaining cells in the last row
        if (day === daysInMonth) {
          const remaining = 7 - cells.length;
          for (let i = 0; i < remaining; i++) {
            cells.push(<View key={`pad-${i}`} style={styles.dayCell} />);
          }
        }
        rows.push(
          <View key={`row-${rows.length}`} style={styles.calendarRow}>
            {cells}
          </View>,
        );
        cells = [];
      }
    }

    return rows;
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
  primaryBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 16 },
  primaryBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },

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

  /* Month navigation */
  monthNav:       { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 },
  monthArrow:     { width: 40, height: 40, borderRadius: 20, backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', alignItems: 'center', justifyContent: 'center' },
  monthArrowText: { fontSize: 18, fontWeight: '600', color: '#f97316' },
  monthTitle:     { fontSize: 17, fontWeight: '700', color: '#111827' },

  /* Calendar */
  calendarContainer: { backgroundColor: '#fff', borderRadius: 16, padding: 8, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  calendarLoading:   { height: 260, alignItems: 'center', justifyContent: 'center' },
  calendarRow:       { flexDirection: 'row' },

  dayHeaderCell: { flex: 1, alignItems: 'center', paddingVertical: 8 },
  dayHeaderText: { fontSize: 12, fontWeight: '600', color: '#9ca3af' },

  dayCell:         { flex: 1, aspectRatio: 1, alignItems: 'center', justifyContent: 'center', margin: 2, borderRadius: 8 },
  dayCellSelected: { borderWidth: 2, borderColor: '#f97316' },
  dayText:         { fontSize: 14, fontWeight: '500' },
  dayTextSelected: { fontWeight: '700' },

  /* Legend */
  legendContainer: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'center', marginBottom: 16, gap: 16 },
  legendItem:      { flexDirection: 'row', alignItems: 'center' },
  legendDot:       { width: 14, height: 14, borderRadius: 4, marginRight: 6 },
  legendText:      { fontSize: 12, color: '#6b7280' },

  /* Actions */
  actionContainer: { backgroundColor: '#fff', borderRadius: 16, padding: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  actionLabel:     { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 12 },
  actionRow:       { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },

  /* Action buttons */
  greenBtn:     { backgroundColor: '#16a34a', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 10 },
  greenBtnText: { fontSize: 13, fontWeight: '600', color: '#fff' },
  redBtn:       { backgroundColor: '#dc2626', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 10 },
  redBtnText:   { fontSize: 13, fontWeight: '600', color: '#fff' },
  grayBtn:      { backgroundColor: '#e5e7eb', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 10 },
  grayBtnText:  { fontSize: 13, fontWeight: '600', color: '#374151' },
});
