import { useEffect, useState, useMemo, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, ScrollView,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import { findAirport, formatDuration, formatTime, durationToMinutes, cabinLabel } from '@/lib/airports';

type Sort = 'best' | 'cheapest' | 'fastest';

export default function FlightResultsScreen() {
  const params = useLocalSearchParams<Record<string, string>>();
  const router = useRouter();
  const [offers, setOffers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [sort, setSort] = useState<Sort>('best');
  const [stopsFilter, setStopsFilter] = useState<number | null>(null);
  const [airlineFilter, setAirlineFilter] = useState<string | null>(null);

  const origin = findAirport(params.origin ?? '');
  const destination = findAirport(params.destination ?? '');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const sp: Record<string, string> = {
        origin: params.origin ?? '', destination: params.destination ?? '',
        departureDate: params.departureDate ?? '', passengers: params.passengers ?? '1',
        cabinClass: params.cabinClass ?? 'economy',
      };
      if (params.returnDate) sp.returnDate = params.returnDate;
      const res = await api.searchFlights(sp);
      setOffers(res?.offers ?? res?.content ?? (Array.isArray(res) ? res : []));
    } catch {
      setOffers([]);
    } finally {
      setLoading(false);
    }
  }, [params.origin, params.destination, params.departureDate, params.passengers, params.cabinClass, params.returnDate]);

  useEffect(() => { load(); }, [load]);

  const airlines = useMemo(() => Array.from(new Set(offers.map((o) => o.airline).filter(Boolean))), [offers]);

  const visible = useMemo(() => {
    let list = offers.slice();
    if (stopsFilter != null) list = list.filter((o) => stopsFilter === 2 ? (o.stops ?? 0) >= 2 : (o.stops ?? 0) === stopsFilter);
    if (airlineFilter) list = list.filter((o) => o.airline === airlineFilter);
    list.sort((a, b) => {
      if (sort === 'cheapest') return (a.pricePaise ?? 0) - (b.pricePaise ?? 0);
      if (sort === 'fastest') return durationToMinutes(a.duration) - durationToMinutes(b.duration);
      const score = (o: any) => (o.pricePaise ?? 0) / 100 + durationToMinutes(o.duration) * 5 + (o.stops ?? 0) * 500;
      return score(a) - score(b);
    });
    return list;
  }, [offers, stopsFilter, airlineFilter, sort]);

  function book(o: any) {
    const sp = new URLSearchParams({
      offerId: o.offerId ?? o.id ?? '',
      origin: o.segments?.[0]?.originCode ?? params.origin ?? '',
      destination: o.segments?.[o.segments.length - 1]?.destinationCode ?? params.destination ?? '',
      departureDate: params.departureDate ?? '',
      passengers: params.passengers ?? '1',
      cabinClass: params.cabinClass ?? 'economy',
      international: params.international ?? 'false',
      totalPaise: String(o.pricePaise ?? 0),
      airline: o.airline ?? '',
      flightNumber: o.flightNumber ?? '',
    });
    router.push(`/flight-book?${sp.toString()}`);
  }

  function renderOffer({ item }: { item: any }) {
    const stops = item.stops ?? 0;
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <Text style={styles.airline}>{item.airline}{item.flightNumber ? ` · ${item.flightNumber}` : ''}</Text>
          <Text style={styles.price}>{formatPaise(item.pricePaise ?? 0)}</Text>
        </View>
        <View style={styles.timeRow}>
          <View style={styles.timeCol}>
            <Text style={styles.time}>{formatTime(item.departureTime)}</Text>
            <Text style={styles.code}>{item.segments?.[0]?.originCode ?? origin?.code}</Text>
          </View>
          <View style={styles.midCol}>
            <Text style={styles.duration}>{formatDuration(item.duration)}</Text>
            <View style={styles.line} />
            <Text style={styles.stops}>{stops === 0 ? 'Non-stop' : `${stops} stop${stops > 1 ? 's' : ''}`}</Text>
          </View>
          <View style={[styles.timeCol, { alignItems: 'flex-end' }]}>
            <Text style={styles.time}>{formatTime(item.arrivalTime)}</Text>
            <Text style={styles.code}>{item.segments?.[item.segments.length - 1]?.destinationCode ?? destination?.code}</Text>
          </View>
        </View>
        <View style={styles.cardFooter}>
          <Text style={styles.cabin}>{cabinLabel(item.cabinClass)}{item.provider ? ` · ${item.provider}` : ''}</Text>
          <TouchableOpacity style={styles.bookBtn} onPress={() => book(item)}><Text style={styles.bookBtnText}>Book</Text></TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: origin && destination ? `${origin.code} → ${destination.code}` : 'Flights' }} />

      {/* Sort + filters */}
      <View style={styles.toolbar}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.toolbarRow}>
          {(['best', 'cheapest', 'fastest'] as Sort[]).map((s) => (
            <TouchableOpacity key={s} style={[styles.chip, sort === s && styles.chipActive]} onPress={() => setSort(s)}>
              <Text style={[styles.chipText, sort === s && styles.chipTextActive]}>{s[0].toUpperCase() + s.slice(1)}</Text>
            </TouchableOpacity>
          ))}
          <View style={styles.sep} />
          {[{ v: 0, l: 'Non-stop' }, { v: 1, l: '1 stop' }, { v: 2, l: '2+ stops' }].map((f) => (
            <TouchableOpacity key={f.v} style={[styles.chip, stopsFilter === f.v && styles.chipActive]} onPress={() => setStopsFilter(stopsFilter === f.v ? null : f.v)}>
              <Text style={[styles.chipText, stopsFilter === f.v && styles.chipTextActive]}>{f.l}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>
      {airlines.length > 1 && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.airlineRow}>
          {airlines.map((a) => (
            <TouchableOpacity key={a} style={[styles.chip, airlineFilter === a && styles.chipActive]} onPress={() => setAirlineFilter(airlineFilter === a ? null : a)}>
              <Text style={[styles.chipText, airlineFilter === a && styles.chipTextActive]}>{a}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      )}

      {loading ? (
        <ActivityIndicator color="#f97316" size="large" style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={visible}
          keyExtractor={(o, i) => o.offerId ?? o.id ?? String(i)}
          renderItem={renderOffer}
          contentContainerStyle={visible.length === 0 ? styles.emptyBox : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>✈️</Text>
              <Text style={styles.emptyTitle}>No flights found</Text>
              <TouchableOpacity style={styles.modifyBtn} onPress={() => router.replace('/flights')}><Text style={styles.modifyText}>Modify search</Text></TouchableOpacity>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  toolbar:       { backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  toolbarRow:    { gap: 8, padding: 12 },
  airlineRow:    { gap: 8, padding: 12, paddingTop: 0, backgroundColor: '#fff' },
  sep:           { width: 1, backgroundColor: '#e5e7eb', marginHorizontal: 4 },
  chip:          { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  chipActive:    { backgroundColor: '#f97316', borderColor: '#f97316' },
  chipText:      { fontSize: 12, fontWeight: '600', color: '#374151' },
  chipTextActive:{ color: '#fff' },

  list:          { padding: 16, gap: 12 },
  emptyBox:      { flexGrow: 1 },

  card:          { backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  cardTop:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  airline:       { fontSize: 14, fontWeight: '700', color: '#111827' },
  price:         { fontSize: 18, fontWeight: '800', color: '#f97316' },
  timeRow:       { flexDirection: 'row', alignItems: 'center', marginTop: 14 },
  timeCol:       { width: 70 },
  time:          { fontSize: 18, fontWeight: '700', color: '#111827' },
  code:          { fontSize: 12, color: '#6b7280', marginTop: 2 },
  midCol:        { flex: 1, alignItems: 'center' },
  duration:      { fontSize: 11, color: '#6b7280' },
  line:          { height: 1, backgroundColor: '#e5e7eb', alignSelf: 'stretch', marginVertical: 4, marginHorizontal: 12 },
  stops:         { fontSize: 11, color: '#9ca3af' },
  cardFooter:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 14, paddingTop: 12, borderTopWidth: 1, borderTopColor: '#f9fafb' },
  cabin:         { fontSize: 12, color: '#6b7280' },
  bookBtn:       { backgroundColor: '#f97316', borderRadius: 10, paddingHorizontal: 24, paddingVertical: 9 },
  bookBtnText:   { color: '#fff', fontWeight: '700', fontSize: 14 },

  empty:         { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyIcon:     { fontSize: 48, marginBottom: 12 },
  emptyTitle:    { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },
  modifyBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 28, paddingVertical: 12 },
  modifyText:    { color: '#fff', fontWeight: '700' },
});
