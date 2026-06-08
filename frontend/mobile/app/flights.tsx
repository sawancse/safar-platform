import { useState } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, TextInput, Modal, FlatList, Alert,
} from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { Airport, ALL_AIRPORTS, searchAirports, isDomesticRoute, CABIN_CLASSES, cabinLabel } from '@/lib/airports';

export default function FlightsScreen() {
  const router = useRouter();
  const [origin, setOrigin] = useState<Airport | null>(ALL_AIRPORTS[0]);
  const [destination, setDestination] = useState<Airport | null>(ALL_AIRPORTS[1]);
  const [departureDate, setDepartureDate] = useState('');
  const [returnDate, setReturnDate] = useState('');
  const [roundTrip, setRoundTrip] = useState(false);
  const [adults, setAdults] = useState(1);
  const [children, setChildren] = useState(0);
  const [infants, setInfants] = useState(0);
  const [cabinClass, setCabinClass] = useState('economy');

  const [picking, setPicking] = useState<null | 'origin' | 'destination'>(null);
  const [query, setQuery] = useState('');

  const results = searchAirports(query);

  function swap() { setOrigin(destination); setDestination(origin); }

  function pick(a: Airport) {
    if (picking === 'origin') setOrigin(a); else setDestination(a);
    setPicking(null);
    setQuery('');
  }

  function onSearch() {
    if (!origin || !destination) { Alert.alert('Select airports', 'Choose origin and destination.'); return; }
    if (origin.code === destination.code) { Alert.alert('Same airport', 'Origin and destination must differ.'); return; }
    if (!departureDate) { Alert.alert('Pick a date', 'Choose a departure date.'); return; }
    const passengers = adults + children + infants;
    const intl = !isDomesticRoute(origin.code, destination.code);
    const params = new URLSearchParams({
      origin: origin.code, destination: destination.code, departureDate,
      passengers: String(passengers), adults: String(adults), children: String(children),
      infants: String(infants), cabinClass, international: String(intl),
    });
    if (roundTrip && returnDate) params.set('returnDate', returnDate);
    router.push(`/flight-results?${params.toString()}`);
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: 'Flights' }} />

      {/* Trip type */}
      <View style={styles.tripRow}>
        <TouchableOpacity style={[styles.tripBtn, !roundTrip && styles.tripBtnActive]} onPress={() => setRoundTrip(false)}>
          <Text style={[styles.tripText, !roundTrip && styles.tripTextActive]}>One way</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.tripBtn, roundTrip && styles.tripBtnActive]} onPress={() => setRoundTrip(true)}>
          <Text style={[styles.tripText, roundTrip && styles.tripTextActive]}>Round trip</Text>
        </TouchableOpacity>
      </View>

      {/* Airports */}
      <View style={styles.card}>
        <TouchableOpacity style={styles.airportRow} onPress={() => { setPicking('origin'); setQuery(''); }}>
          <Text style={styles.airportLabel}>From</Text>
          <Text style={styles.airportValue}>{origin ? `${origin.city} (${origin.code})` : 'Select'}</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.swapBtn} onPress={swap}><Text style={styles.swapIcon}>⇅</Text></TouchableOpacity>
        <View style={styles.divider} />
        <TouchableOpacity style={styles.airportRow} onPress={() => { setPicking('destination'); setQuery(''); }}>
          <Text style={styles.airportLabel}>To</Text>
          <Text style={styles.airportValue}>{destination ? `${destination.city} (${destination.code})` : 'Select'}</Text>
        </TouchableOpacity>
      </View>

      {/* Dates */}
      <View style={styles.row}>
        <View style={{ flex: 1 }}>
          <Text style={styles.label}>Departure</Text>
          <TextInput style={styles.input} value={departureDate} onChangeText={setDepartureDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" />
        </View>
        {roundTrip && (
          <View style={{ flex: 1 }}>
            <Text style={styles.label}>Return</Text>
            <TextInput style={styles.input} value={returnDate} onChangeText={setReturnDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" />
          </View>
        )}
      </View>

      {/* Passengers */}
      <Text style={styles.label}>Passengers</Text>
      <Stepper label="Adults" sub="12+ yrs" value={adults} setValue={(v) => setAdults(Math.max(1, v))} min={1} />
      <Stepper label="Children" sub="2-11 yrs" value={children} setValue={(v) => setChildren(Math.max(0, v))} />
      <Stepper label="Infants" sub="under 2" value={infants} setValue={(v) => setInfants(Math.max(0, Math.min(adults, v)))} />

      {/* Cabin */}
      <Text style={styles.label}>Cabin class</Text>
      <View style={styles.cabinRow}>
        {CABIN_CLASSES.map((c) => (
          <TouchableOpacity key={c.value} style={[styles.cabinChip, cabinClass === c.value && styles.cabinChipActive]} onPress={() => setCabinClass(c.value)}>
            <Text style={[styles.cabinText, cabinClass === c.value && styles.cabinTextActive]}>{c.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity style={styles.searchBtn} onPress={onSearch}>
        <Text style={styles.searchBtnText}>Search flights</Text>
      </TouchableOpacity>

      {/* My flights link */}
      <TouchableOpacity style={styles.myFlights} onPress={() => router.push('/my-flights')}>
        <Text style={styles.myFlightsText}>✈  My flight bookings</Text>
      </TouchableOpacity>

      {/* Airport picker modal */}
      <Modal visible={picking !== null} animationType="slide" onRequestClose={() => setPicking(null)}>
        <View style={styles.modal}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{picking === 'origin' ? 'Select origin' : 'Select destination'}</Text>
            <TouchableOpacity onPress={() => setPicking(null)}><Text style={styles.modalClose}>✕</Text></TouchableOpacity>
          </View>
          <TextInput style={styles.searchInput} value={query} onChangeText={setQuery} placeholder="City, airport or code" placeholderTextColor="#9ca3af" autoFocus />
          <FlatList
            data={results}
            keyExtractor={(a) => a.code}
            keyboardShouldPersistTaps="handled"
            renderItem={({ item }) => (
              <TouchableOpacity style={styles.airportItem} onPress={() => pick(item)}>
                <Text style={styles.airportItemCode}>{item.code}</Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.airportItemCity}>{item.city}</Text>
                  <Text style={styles.airportItemName}>{item.name}</Text>
                </View>
              </TouchableOpacity>
            )}
          />
        </View>
      </Modal>
    </ScrollView>
  );
}

function Stepper({ label, sub, value, setValue, min }: { label: string; sub: string; value: number; setValue: (v: number) => void; min?: number }) {
  return (
    <View style={styles.stepper}>
      <View>
        <Text style={styles.stepperLabel}>{label}</Text>
        <Text style={styles.stepperSub}>{sub}</Text>
      </View>
      <View style={styles.stepperCtrls}>
        <TouchableOpacity style={[styles.stepBtn, value <= (min ?? 0) && styles.stepBtnDisabled]} onPress={() => setValue(value - 1)}>
          <Text style={styles.stepBtnText}>−</Text>
        </TouchableOpacity>
        <Text style={styles.stepValue}>{value}</Text>
        <TouchableOpacity style={styles.stepBtn} onPress={() => setValue(value + 1)}><Text style={styles.stepBtnText}>+</Text></TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: '#f9fafb' },

  tripRow:         { flexDirection: 'row', gap: 10, marginBottom: 16 },
  tripBtn:         { flex: 1, paddingVertical: 11, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', alignItems: 'center', backgroundColor: '#fff' },
  tripBtnActive:   { backgroundColor: '#f97316', borderColor: '#f97316' },
  tripText:        { fontSize: 14, fontWeight: '600', color: '#374151' },
  tripTextActive:  { color: '#fff' },

  card:            { backgroundColor: '#fff', borderRadius: 14, padding: 6, borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 16, position: 'relative' },
  airportRow:      { padding: 14 },
  airportLabel:    { fontSize: 12, color: '#6b7280' },
  airportValue:    { fontSize: 18, fontWeight: '700', color: '#111827', marginTop: 2 },
  divider:         { height: 1, backgroundColor: '#f3f4f6' },
  swapBtn:         { position: 'absolute', right: 18, top: '50%', marginTop: -16, width: 32, height: 32, borderRadius: 16, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#fed7aa', zIndex: 2 },
  swapIcon:        { fontSize: 16, color: '#f97316', fontWeight: '700' },

  row:             { flexDirection: 'row', gap: 12 },
  label:           { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6, marginTop: 8 },
  input:           { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', marginBottom: 8 },

  stepper:         { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  stepperLabel:    { fontSize: 14, fontWeight: '600', color: '#111827' },
  stepperSub:      { fontSize: 11, color: '#9ca3af', marginTop: 1 },
  stepperCtrls:    { flexDirection: 'row', alignItems: 'center', gap: 16 },
  stepBtn:         { width: 32, height: 32, borderRadius: 16, borderWidth: 1, borderColor: '#f97316', alignItems: 'center', justifyContent: 'center' },
  stepBtnDisabled: { borderColor: '#e5e7eb' },
  stepBtnText:     { fontSize: 18, color: '#f97316', fontWeight: '700' },
  stepValue:       { fontSize: 16, fontWeight: '700', color: '#111827', minWidth: 18, textAlign: 'center' },

  cabinRow:        { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  cabinChip:       { paddingHorizontal: 14, paddingVertical: 9, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  cabinChipActive: { backgroundColor: '#f97316', borderColor: '#f97316' },
  cabinText:       { fontSize: 13, fontWeight: '600', color: '#374151' },
  cabinTextActive: { color: '#fff' },

  searchBtn:       { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 20 },
  searchBtnText:   { color: '#fff', fontSize: 16, fontWeight: '700' },
  myFlights:       { marginTop: 16, alignItems: 'center' },
  myFlightsText:   { color: '#f97316', fontSize: 14, fontWeight: '600' },

  modal:           { flex: 1, backgroundColor: '#fff', paddingTop: 50 },
  modalHeader:     { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingBottom: 12 },
  modalTitle:      { fontSize: 17, fontWeight: '700', color: '#111827' },
  modalClose:      { fontSize: 20, color: '#6b7280' },
  searchInput:     { marginHorizontal: 16, backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 11, fontSize: 15, marginBottom: 8 },
  airportItem:     { flexDirection: 'row', alignItems: 'center', gap: 14, paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f9fafb' },
  airportItemCode: { fontSize: 15, fontWeight: '800', color: '#f97316', width: 44 },
  airportItemCity: { fontSize: 15, fontWeight: '600', color: '#111827' },
  airportItemName: { fontSize: 12, color: '#9ca3af', marginTop: 1 },
});
