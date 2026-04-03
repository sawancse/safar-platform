import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, Image, TextInput,
  StyleSheet, ActivityIndicator, Dimensions, FlatList, Switch, Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';
const SCREEN_WIDTH = Dimensions.get('window').width;

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) return `${(rupees / 10000000).toFixed(2)} Cr`;
  if (rupees >= 100000) return `${(rupees / 100000).toFixed(2)} L`;
  return rupees.toLocaleString('en-IN');
}

function formatEMI(totalPaise: number, years = 20, rate = 8.5): string {
  const principal = totalPaise / 100;
  const monthlyRate = rate / 12 / 100;
  const n = years * 12;
  if (principal <= 0) return '--';
  const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, n)) / (Math.pow(1 + monthlyRate, n) - 1);
  if (emi >= 100000) return `${(emi / 100000).toFixed(1)}L/mo`;
  return `${Math.round(emi).toLocaleString('en-IN')}/mo`;
}

export default function ProjectDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();

  const [project, setProject] = useState<any>(null);
  const [unitTypes, setUnitTypes] = useState<any[]>([]);
  const [updates, setUpdates] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Price calculator state
  const [selectedUnitType, setSelectedUnitType] = useState<string>('');
  const [calcFloor, setCalcFloor] = useState('5');
  const [calcPreferredFacing, setCalcPreferredFacing] = useState(false);
  const [calculatedPrice, setCalculatedPrice] = useState<any>(null);
  const [calculating, setCalculating] = useState(false);

  const loadData = useCallback(async () => {
    if (!id) return;
    try {
      const [proj, units, upd] = await Promise.all([
        api.getBuilderProject(id),
        api.getUnitTypes(id).catch(() => []),
        api.getConstructionUpdates(id).catch(() => []),
      ]);
      setProject(proj);
      setUnitTypes(Array.isArray(units) ? units : []);
      setUpdates(Array.isArray(upd) ? upd : []);
      if (units?.length > 0 && !selectedUnitType) {
        setSelectedUnitType(units[0].id);
      }
    } catch {
      setProject(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadData(); }, [loadData]);

  const calculatePrice = useCallback(async () => {
    if (!selectedUnitType) return;
    setCalculating(true);
    try {
      const result = await api.calculateUnitPrice(
        selectedUnitType,
        parseInt(calcFloor, 10) || 1,
        calcPreferredFacing,
      );
      setCalculatedPrice(result);
    } catch {
      setCalculatedPrice(null);
    } finally {
      setCalculating(false);
    }
  }, [selectedUnitType, calcFloor, calcPreferredFacing]);

  const handleInquiry = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    Alert.alert('Inquiry Sent', 'The builder will contact you shortly.');
  }, [router]);

  const handleScheduleVisit = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    Alert.alert('Visit Requested', 'The builder will confirm your site visit schedule.');
  }, [router]);

  if (loading) {
    return (
      <SafeAreaView style={styles.container} edges={['top']}>
        <View style={styles.center}>
          <ActivityIndicator size="large" color={ORANGE} />
        </View>
      </SafeAreaView>
    );
  }

  if (!project) {
    return (
      <SafeAreaView style={styles.container} edges={['top']}>
        <View style={styles.center}>
          <Text style={styles.emptyTitle}>Project not found</Text>
          <TouchableOpacity onPress={() => router.back()}>
            <Text style={[styles.linkText, { marginTop: 12 }]}>Go back</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const photos: string[] = project.photos || (project.coverPhoto ? [project.coverPhoto] : []);
  const progressPct = project.constructionProgress ?? 0;

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle} numberOfLines={1}>{project.projectName || project.name}</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
        {/* Photo gallery horizontal scroll */}
        {photos.length > 0 ? (
          <FlatList
            horizontal
            pagingEnabled
            data={photos}
            keyExtractor={(_, i) => String(i)}
            renderItem={({ item }) => (
              <Image source={{ uri: item }} style={styles.galleryImage} resizeMode="cover" />
            )}
            showsHorizontalScrollIndicator={false}
            style={{ height: 260 }}
          />
        ) : (
          <View style={[styles.galleryImage, styles.galleryPlaceholder]}>
            <Text style={styles.placeholderText}>No Photos</Text>
          </View>
        )}

        <View style={styles.content}>
          {/* Builder name + verified badge */}
          <View style={styles.builderRow}>
            <Text style={styles.builderName}>{project.builderName || 'Builder'}</Text>
            {project.verified && (
              <View style={styles.verifiedBadge}>
                <Text style={styles.verifiedBadgeText}>Verified</Text>
              </View>
            )}
          </View>

          <Text style={styles.projectName}>{project.projectName || project.name}</Text>
          <Text style={styles.locationText}>
            {[project.locality, project.city, project.state].filter(Boolean).join(', ')}
          </Text>

          {project.reraNumber && (
            <View style={styles.reraRow}>
              <View style={styles.reraBadge}>
                <Text style={styles.reraBadgeText}>RERA</Text>
              </View>
              <Text style={styles.reraNumber}>{project.reraNumber}</Text>
            </View>
          )}

          {/* Key stats */}
          <View style={styles.statsGrid}>
            <StatBox label="Towers" value={project.totalTowers ?? '--'} />
            <StatBox label="Units" value={project.totalUnits ?? '--'} />
            <StatBox label="Floors" value={project.maxFloors ?? '--'} />
            <StatBox label="Possession" value={project.possessionDate ?? '--'} />
          </View>

          {/* Progress bar */}
          <View style={styles.progressSection}>
            <Text style={styles.sectionLabel}>Construction Progress</Text>
            <View style={styles.progressRow}>
              <View style={styles.progressBarBg}>
                <View style={[styles.progressBarFill, { width: `${Math.min(progressPct, 100)}%` }]} />
              </View>
              <Text style={styles.progressText}>{progressPct}%</Text>
            </View>
          </View>

          {/* Description */}
          {project.description && (
            <View style={styles.section}>
              <Text style={styles.sectionLabel}>About</Text>
              <Text style={styles.descText}>{project.description}</Text>
            </View>
          )}

          {/* Unit type cards */}
          {unitTypes.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionLabel}>Unit Types</Text>
              {unitTypes.map(ut => (
                <View key={ut.id} style={styles.unitCard}>
                  <View style={styles.unitCardHeader}>
                    <Text style={styles.unitBhk}>{ut.bhk || ut.name}</Text>
                    <Text style={styles.unitPrice}>
                      {ut.basePricePaise ? formatPrice(ut.basePricePaise) : 'Price on request'}
                    </Text>
                  </View>
                  {ut.carpetAreaSqft && (
                    <Text style={styles.unitDetail}>Carpet: {ut.carpetAreaSqft} sq.ft</Text>
                  )}
                  {ut.superBuiltUpAreaSqft && (
                    <Text style={styles.unitDetail}>Super built-up: {ut.superBuiltUpAreaSqft} sq.ft</Text>
                  )}
                  {ut.floorPlanUrl && (
                    <TouchableOpacity>
                      <Text style={styles.linkText}>View Floor Plan</Text>
                    </TouchableOpacity>
                  )}
                </View>
              ))}
            </View>
          )}

          {/* Price calculator */}
          {unitTypes.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionLabel}>Price Calculator</Text>
              <View style={styles.calcBox}>
                {/* Unit type picker */}
                <Text style={styles.calcLabel}>Unit Type</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 12 }}>
                  {unitTypes.map(ut => (
                    <TouchableOpacity
                      key={ut.id}
                      style={[styles.calcChip, selectedUnitType === ut.id ? styles.calcChipActive : null]}
                      onPress={() => { setSelectedUnitType(ut.id); setCalculatedPrice(null); }}
                    >
                      <Text style={[styles.calcChipText, selectedUnitType === ut.id ? styles.calcChipTextActive : null]}>
                        {ut.bhk || ut.name}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>

                {/* Floor input */}
                <Text style={styles.calcLabel}>Floor</Text>
                <TextInput
                  style={styles.calcInput}
                  value={calcFloor}
                  onChangeText={t => { setCalcFloor(t); setCalculatedPrice(null); }}
                  keyboardType="numeric"
                  placeholder="e.g. 5"
                  placeholderTextColor="#9ca3af"
                />

                {/* Preferred facing toggle */}
                <View style={styles.calcToggleRow}>
                  <Text style={styles.calcLabel}>Preferred Facing</Text>
                  <Switch
                    value={calcPreferredFacing}
                    onValueChange={v => { setCalcPreferredFacing(v); setCalculatedPrice(null); }}
                    trackColor={{ false: '#e5e7eb', true: ORANGE }}
                    thumbColor="#fff"
                  />
                </View>

                <TouchableOpacity style={styles.calcBtn} onPress={calculatePrice} disabled={calculating}>
                  {calculating ? (
                    <ActivityIndicator size="small" color="#fff" />
                  ) : (
                    <Text style={styles.calcBtnText}>Calculate Price</Text>
                  )}
                </TouchableOpacity>

                {calculatedPrice && (
                  <View style={styles.calcResult}>
                    <Text style={styles.calcResultPrice}>
                      {calculatedPrice.totalPricePaise
                        ? formatPrice(calculatedPrice.totalPricePaise)
                        : 'Price on request'}
                    </Text>
                    {calculatedPrice.totalPricePaise && (
                      <Text style={styles.calcResultEmi}>
                        EMI ~{formatEMI(calculatedPrice.totalPricePaise)}
                      </Text>
                    )}
                    {calculatedPrice.breakup && (
                      <View style={styles.breakupBox}>
                        {Object.entries(calculatedPrice.breakup).map(([key, val]: [string, any]) => (
                          <View key={key} style={styles.breakupRow}>
                            <Text style={styles.breakupLabel}>{key.replace(/([A-Z])/g, ' $1').trim()}</Text>
                            <Text style={styles.breakupValue}>{typeof val === 'number' ? formatPrice(val) : String(val)}</Text>
                          </View>
                        ))}
                      </View>
                    )}
                  </View>
                )}
              </View>
            </View>
          )}

          {/* Amenities */}
          {project.amenities?.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionLabel}>Amenities</Text>
              <View style={styles.amenitiesGrid}>
                {project.amenities.map((a: string, i: number) => (
                  <View key={i} style={styles.amenityChip}>
                    <Text style={styles.amenityChipText}>{a}</Text>
                  </View>
                ))}
              </View>
            </View>
          )}

          {/* Construction updates timeline */}
          {updates.length > 0 && (
            <View style={styles.section}>
              <Text style={styles.sectionLabel}>Construction Updates</Text>
              {updates.map((upd, i) => (
                <View key={upd.id || i} style={styles.timelineItem}>
                  <View style={styles.timelineDot} />
                  {i < updates.length - 1 && <View style={styles.timelineLine} />}
                  <View style={styles.timelineContent}>
                    <Text style={styles.timelineDate}>{upd.date || upd.createdAt?.slice(0, 10) || ''}</Text>
                    <Text style={styles.timelineTitle}>{upd.title}</Text>
                    {upd.description && <Text style={styles.timelineDesc}>{upd.description}</Text>}
                    {upd.progressPercent != null && (
                      <View style={styles.timelineProgressRow}>
                        <View style={styles.miniProgressBg}>
                          <View style={[styles.miniProgressFill, { width: `${Math.min(upd.progressPercent, 100)}%` }]} />
                        </View>
                        <Text style={styles.miniProgressText}>{upd.progressPercent}%</Text>
                      </View>
                    )}
                    {upd.photoUrl && (
                      <Image source={{ uri: upd.photoUrl }} style={styles.timelinePhoto} resizeMode="cover" />
                    )}
                  </View>
                </View>
              ))}
            </View>
          )}

          <View style={{ height: 100 }} />
        </View>
      </ScrollView>

      {/* Bottom action bar */}
      <View style={styles.bottomBar}>
        <TouchableOpacity style={styles.bottomBtnOutline} onPress={handleInquiry}>
          <Text style={styles.bottomBtnOutlineText}>Send Inquiry</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.bottomBtnFilled} onPress={handleScheduleVisit}>
          <Text style={styles.bottomBtnFilledText}>Schedule Visit</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

function StatBox({ label, value }: { label: string; value: string | number }) {
  return (
    <View style={styles.statBox}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
  emptyTitle: { fontSize: 17, fontWeight: '600', color: '#374151' },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 22, color: ORANGE, fontWeight: '600' },
  headerTitle: { fontSize: 17, fontWeight: '700', color: '#111827', flex: 1, textAlign: 'center' },
  galleryImage: { width: SCREEN_WIDTH, height: 260, backgroundColor: '#f3f4f6' },
  galleryPlaceholder: { justifyContent: 'center', alignItems: 'center' },
  placeholderText: { color: '#9ca3af', fontSize: 14 },
  content: { padding: 16 },
  builderRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 4 },
  builderName: { fontSize: 13, color: '#6b7280', fontWeight: '500' },
  verifiedBadge: {
    marginLeft: 8, backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 4,
  },
  verifiedBadgeText: { fontSize: 11, color: '#15803d', fontWeight: '600' },
  projectName: { fontSize: 22, fontWeight: '800', color: '#111827', marginBottom: 4 },
  locationText: { fontSize: 14, color: '#6b7280', marginBottom: 8 },
  reraRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  reraBadge: { backgroundColor: '#15803d', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 3, marginRight: 8 },
  reraBadgeText: { color: '#fff', fontSize: 10, fontWeight: '700' },
  reraNumber: { fontSize: 12, color: '#6b7280' },
  statsGrid: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 16, gap: 8 },
  statBox: {
    flex: 1, minWidth: 80, backgroundColor: '#f9fafb', borderRadius: 10, padding: 12, alignItems: 'center',
  },
  statValue: { fontSize: 18, fontWeight: '700', color: '#111827' },
  statLabel: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  progressSection: { marginBottom: 16 },
  sectionLabel: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 10 },
  progressRow: { flexDirection: 'row', alignItems: 'center' },
  progressBarBg: { flex: 1, height: 8, borderRadius: 4, backgroundColor: '#e5e7eb' },
  progressBarFill: { height: 8, borderRadius: 4, backgroundColor: ORANGE },
  progressText: { fontSize: 13, color: '#6b7280', marginLeft: 10, fontWeight: '600', width: 40 },
  section: { marginBottom: 20 },
  descText: { fontSize: 14, color: '#374151', lineHeight: 22 },
  unitCard: {
    backgroundColor: '#f9fafb', borderRadius: 12, padding: 14, marginBottom: 10,
    borderWidth: 1, borderColor: '#e5e7eb',
  },
  unitCardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 },
  unitBhk: { fontSize: 16, fontWeight: '700', color: '#111827' },
  unitPrice: { fontSize: 15, fontWeight: '700', color: ORANGE },
  unitDetail: { fontSize: 13, color: '#6b7280', marginBottom: 2 },
  linkText: { fontSize: 13, color: ORANGE, fontWeight: '600', marginTop: 4 },
  calcBox: {
    backgroundColor: '#fffbeb', borderRadius: 14, padding: 16, borderWidth: 1, borderColor: '#fde68a',
  },
  calcLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  calcChip: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20, backgroundColor: '#fff',
    borderWidth: 1, borderColor: '#d1d5db', marginRight: 8,
  },
  calcChipActive: { backgroundColor: ORANGE, borderColor: ORANGE },
  calcChipText: { fontSize: 13, color: '#374151', fontWeight: '500' },
  calcChipTextActive: { color: '#fff' },
  calcInput: {
    height: 42, borderRadius: 8, backgroundColor: '#fff', borderWidth: 1, borderColor: '#d1d5db',
    paddingHorizontal: 12, fontSize: 14, color: '#111827', marginBottom: 12,
  },
  calcToggleRow: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14,
  },
  calcBtn: {
    backgroundColor: ORANGE, borderRadius: 10, paddingVertical: 12, alignItems: 'center',
  },
  calcBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },
  calcResult: { marginTop: 14, alignItems: 'center' },
  calcResultPrice: { fontSize: 24, fontWeight: '800', color: '#111827' },
  calcResultEmi: { fontSize: 14, color: '#6b7280', marginTop: 4 },
  breakupBox: { marginTop: 12, width: '100%' },
  breakupRow: {
    flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4,
    borderBottomWidth: 1, borderBottomColor: '#fde68a',
  },
  breakupLabel: { fontSize: 13, color: '#6b7280' },
  breakupValue: { fontSize: 13, fontWeight: '600', color: '#374151' },
  amenitiesGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  amenityChip: {
    paddingHorizontal: 12, paddingVertical: 7, borderRadius: 20,
    backgroundColor: '#f3f4f6',
  },
  amenityChipText: { fontSize: 13, color: '#374151' },
  timelineItem: { flexDirection: 'row', marginBottom: 16, paddingLeft: 8 },
  timelineDot: {
    width: 12, height: 12, borderRadius: 6, backgroundColor: ORANGE,
    marginTop: 4, marginRight: 12, zIndex: 1,
  },
  timelineLine: {
    position: 'absolute', left: 13, top: 16, width: 2, height: '100%',
    backgroundColor: '#e5e7eb',
  },
  timelineContent: { flex: 1 },
  timelineDate: { fontSize: 12, color: '#9ca3af', marginBottom: 2 },
  timelineTitle: { fontSize: 15, fontWeight: '600', color: '#111827', marginBottom: 4 },
  timelineDesc: { fontSize: 13, color: '#6b7280', lineHeight: 20, marginBottom: 6 },
  timelineProgressRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 6 },
  miniProgressBg: { flex: 1, height: 4, borderRadius: 2, backgroundColor: '#e5e7eb' },
  miniProgressFill: { height: 4, borderRadius: 2, backgroundColor: ORANGE },
  miniProgressText: { fontSize: 11, color: '#6b7280', marginLeft: 6, width: 32 },
  timelinePhoto: { width: '100%', height: 140, borderRadius: 8, marginTop: 4 },
  bottomBar: {
    flexDirection: 'row', padding: 16, gap: 12, borderTopWidth: 1, borderTopColor: '#f3f4f6',
    backgroundColor: '#fff',
  },
  bottomBtnOutline: {
    flex: 1, borderWidth: 2, borderColor: ORANGE, borderRadius: 12,
    paddingVertical: 14, alignItems: 'center',
  },
  bottomBtnOutlineText: { color: ORANGE, fontSize: 15, fontWeight: '700' },
  bottomBtnFilled: {
    flex: 1, backgroundColor: ORANGE, borderRadius: 12,
    paddingVertical: 14, alignItems: 'center',
  },
  bottomBtnFilledText: { color: '#fff', fontSize: 15, fontWeight: '700' },
});
