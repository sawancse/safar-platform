import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

const SPECIALTIES = [
  'Cardiology',
  'Orthopedics',
  'Oncology',
  'Neurology',
  'Dental',
  'Ophthalmology',
  'General',
] as const;

const SPECIALTY_ICONS: Record<string, string> = {
  Cardiology:    '❤️',
  Orthopedics:   '🦴',
  Oncology:      '🔬',
  Neurology:     '🧠',
  Dental:        '🦷',
  Ophthalmology: '👁️',
  General:       '🏥',
};

interface Hospital {
  id?: string;
  name: string;
  city: string;
  specialties: string[];
  accreditations: string[];
  rating: number;
  imageUrl?: string;
}

interface StayPackage {
  id?: string;
  hospitalName: string;
  listingTitle: string;
  city: string;
  specialty: string;
  distanceKm: number;
  pricePaise: number;
  amenities: string[];
}

export default function MedicalScreen() {
  const [city, setCity] = useState('');
  const [selectedSpecialty, setSelectedSpecialty] = useState('');
  const [hospitals, setHospitals] = useState<Hospital[]>([]);
  const [packages, setPackages] = useState<StayPackage[]>([]);
  const [loadingHospitals, setLoadingHospitals] = useState(true);
  const [loadingPackages, setLoadingPackages] = useState(false);
  const [selectedHospital, setSelectedHospital] = useState<string | null>(null);

  const fetchHospitals = useCallback(async () => {
    setLoadingHospitals(true);
    try {
      const result = await api.getHospitals();
      let data = result ?? [];
      if (city.trim()) {
        const q = city.trim().toLowerCase();
        data = data.filter((h: Hospital) => h.city?.toLowerCase().includes(q));
      }
      setHospitals(data);
    } catch {
      setHospitals([]);
    } finally {
      setLoadingHospitals(false);
    }
  }, [city]);

  const fetchPackages = useCallback(async () => {
    setLoadingPackages(true);
    try {
      const params: { city?: string; specialty?: string } = {};
      if (city.trim()) params.city = city.trim();
      if (selectedSpecialty) params.specialty = selectedSpecialty;
      const result = await api.getMedicalStaySearch(params);
      setPackages(result ?? []);
    } catch {
      setPackages([]);
    } finally {
      setLoadingPackages(false);
    }
  }, [city, selectedSpecialty]);

  useEffect(() => {
    fetchHospitals();
  }, [fetchHospitals]);

  useEffect(() => {
    fetchPackages();
  }, [fetchPackages]);

  function renderHospitalCard({ item }: { item: Hospital }) {
    const isSelected = selectedHospital === item.name;
    return (
      <TouchableOpacity
        style={[styles.hospitalCard, isSelected && styles.hospitalCardSelected]}
        onPress={() => setSelectedHospital(isSelected ? null : item.name)}
        activeOpacity={0.7}
      >
        <View style={styles.hospitalHeader}>
          <Text style={styles.hospitalIcon}>🏥</Text>
          {item.rating > 0 && (
            <Text style={styles.hospitalRating}>★ {item.rating.toFixed(1)}</Text>
          )}
        </View>
        <Text style={styles.hospitalName} numberOfLines={2}>{item.name}</Text>
        <Text style={styles.hospitalCity}>{item.city}</Text>
        {item.specialties?.length > 0 && (
          <View style={styles.tagRow}>
            {item.specialties.slice(0, 3).map((s) => (
              <View key={s} style={styles.specialtyTag}>
                <Text style={styles.specialtyTagText}>{s}</Text>
              </View>
            ))}
            {item.specialties.length > 3 && (
              <Text style={styles.moreTag}>+{item.specialties.length - 3}</Text>
            )}
          </View>
        )}
        {item.accreditations?.length > 0 && (
          <View style={styles.tagRow}>
            {item.accreditations.map((a) => (
              <View key={a} style={styles.accreditationTag}>
                <Text style={styles.accreditationTagText}>{a}</Text>
              </View>
            ))}
          </View>
        )}
      </TouchableOpacity>
    );
  }

  function renderPackageCard({ item }: { item: StayPackage }) {
    return (
      <View style={styles.packageCard}>
        <View style={styles.packageBody}>
          <View style={styles.packageTopRow}>
            <View style={styles.specialtyBadge}>
              <Text style={styles.specialtyBadgeText}>
                {SPECIALTY_ICONS[item.specialty] ?? '🏥'} {item.specialty}
              </Text>
            </View>
            <Text style={styles.packageDistance}>{item.distanceKm?.toFixed(1)} km</Text>
          </View>
          <Text style={styles.packageTitle} numberOfLines={2}>{item.listingTitle}</Text>
          <Text style={styles.packageHospital}>Near {item.hospitalName}</Text>
          <Text style={styles.packageCity}>{item.city}</Text>
          {item.amenities?.length > 0 && (
            <View style={styles.amenityRow}>
              {item.amenities.slice(0, 4).map((a) => (
                <View key={a} style={styles.amenityTag}>
                  <Text style={styles.amenityTagText}>{a}</Text>
                </View>
              ))}
            </View>
          )}
          <Text style={styles.packagePrice}>{formatPaise(item.pricePaise)}/night</Text>
        </View>
      </View>
    );
  }

  const filteredPackages = selectedHospital
    ? packages.filter((p) => p.hospitalName === selectedHospital)
    : packages;

  return (
    <View style={styles.container}>
      {/* City filter */}
      <View style={styles.searchBar}>
        <Text style={styles.searchIcon}>🔍</Text>
        <TextInput
          style={styles.searchInput}
          placeholder="Filter by city..."
          placeholderTextColor="#9ca3af"
          value={city}
          onChangeText={setCity}
          onSubmitEditing={() => { fetchHospitals(); fetchPackages(); }}
          returnKeyType="search"
        />
      </View>

      {/* Specialty filter chips */}
      <View style={styles.filterRow}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterScroll}>
          <TouchableOpacity
            style={[styles.filterChip, !selectedSpecialty && styles.filterChipActive]}
            onPress={() => setSelectedSpecialty('')}
          >
            <Text style={[styles.filterChipText, !selectedSpecialty && styles.filterChipTextActive]}>
              All
            </Text>
          </TouchableOpacity>
          {SPECIALTIES.map((spec) => (
            <TouchableOpacity
              key={spec}
              style={[styles.filterChip, selectedSpecialty === spec && styles.filterChipActive]}
              onPress={() => setSelectedSpecialty(selectedSpecialty === spec ? '' : spec)}
            >
              <Text style={[styles.filterChipText, selectedSpecialty === spec && styles.filterChipTextActive]}>
                {SPECIALTY_ICONS[spec]} {spec}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      <ScrollView style={styles.scrollBody} contentContainerStyle={styles.scrollContent}>
        {/* Hospitals section */}
        <Text style={styles.sectionTitle}>Hospitals</Text>

        {loadingHospitals ? (
          <ActivityIndicator color="#f97316" style={{ marginTop: 20 }} size="large" />
        ) : hospitals.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>🏥</Text>
            <Text style={styles.emptyTitle}>No hospitals found</Text>
            <Text style={styles.emptySubtitle}>Try a different city</Text>
          </View>
        ) : (
          <FlatList
            data={hospitals}
            keyExtractor={(item, index) => item.id ?? `${item.name}-${index}`}
            renderItem={renderHospitalCard}
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.hospitalList}
          />
        )}

        {/* Stay packages section */}
        <Text style={styles.sectionTitle}>Stay Packages</Text>

        {loadingPackages ? (
          <ActivityIndicator color="#f97316" style={{ marginTop: 20 }} size="large" />
        ) : filteredPackages.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyIcon}>🩺</Text>
            <Text style={styles.emptyTitle}>No stay packages found</Text>
            <Text style={styles.emptySubtitle}>
              {selectedHospital
                ? 'No packages near this hospital'
                : 'Try a different city or specialty'}
            </Text>
          </View>
        ) : (
          filteredPackages.map((pkg, index) => (
            <View key={pkg.id ?? `${pkg.listingTitle}-${index}`}>
              {renderPackageCard({ item: pkg })}
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container:            { flex: 1, backgroundColor: '#f9fafb' },

  searchBar:            { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', margin: 16, marginBottom: 0, borderRadius: 12, borderWidth: 1, borderColor: '#e5e7eb', paddingHorizontal: 12 },
  searchIcon:           { fontSize: 16, marginRight: 8 },
  searchInput:          { flex: 1, fontSize: 14, color: '#111827', paddingVertical: 12 },

  filterRow:            { backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6', paddingVertical: 8 },
  filterScroll:         { paddingHorizontal: 16, gap: 8 },
  filterChip:           { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb' },
  filterChipActive:     { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterChipText:       { fontSize: 12, fontWeight: '600', color: '#374151' },
  filterChipTextActive: { color: '#fff' },

  scrollBody:           { flex: 1 },
  scrollContent:        { paddingBottom: 32 },

  sectionTitle:         { fontSize: 18, fontWeight: '700', color: '#111827', paddingHorizontal: 16, paddingTop: 16, paddingBottom: 8 },

  hospitalList:         { paddingHorizontal: 16, gap: 12 },
  hospitalCard:         { backgroundColor: '#fff', borderRadius: 16, padding: 12, width: 200, borderWidth: 1, borderColor: '#f3f4f6' },
  hospitalCardSelected: { borderColor: '#f97316', borderWidth: 2 },
  hospitalHeader:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  hospitalIcon:         { fontSize: 28 },
  hospitalRating:       { fontSize: 13, fontWeight: '600', color: '#374151' },
  hospitalName:         { fontSize: 14, fontWeight: '700', color: '#111827', marginBottom: 2 },
  hospitalCity:         { fontSize: 12, color: '#6b7280', marginBottom: 8 },

  tagRow:               { flexDirection: 'row', flexWrap: 'wrap', gap: 4, marginTop: 4 },
  specialtyTag:         { backgroundColor: '#fff7ed', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100 },
  specialtyTagText:     { fontSize: 10, fontWeight: '600', color: '#c2410c' },
  accreditationTag:     { backgroundColor: '#ecfdf5', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100 },
  accreditationTagText: { fontSize: 10, fontWeight: '600', color: '#065f46' },
  moreTag:              { fontSize: 10, color: '#9ca3af', alignSelf: 'center' },

  packageCard:          { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginHorizontal: 16, marginBottom: 12 },
  packageBody:          { padding: 12 },
  packageTopRow:        { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  specialtyBadge:       { backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 100 },
  specialtyBadgeText:   { fontSize: 10, fontWeight: '700', color: '#c2410c' },
  packageDistance:      { fontSize: 11, color: '#6b7280' },
  packageTitle:         { fontSize: 15, fontWeight: '700', color: '#111827', marginTop: 6 },
  packageHospital:      { fontSize: 12, color: '#6b7280', marginTop: 2 },
  packageCity:          { fontSize: 12, color: '#6b7280', marginTop: 1 },
  amenityRow:           { flexDirection: 'row', flexWrap: 'wrap', gap: 4, marginTop: 6 },
  amenityTag:           { backgroundColor: '#f3f4f6', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100 },
  amenityTagText:       { fontSize: 10, fontWeight: '600', color: '#374151' },
  packagePrice:         { fontSize: 15, fontWeight: '800', color: '#f97316', marginTop: 6 },

  empty:                { alignItems: 'center', paddingTop: 40, paddingBottom: 20 },
  emptyIcon:            { fontSize: 48, marginBottom: 12 },
  emptyTitle:           { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:        { fontSize: 14, color: '#9ca3af', marginTop: 4 },
});
