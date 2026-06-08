import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Image,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

type Tab = 'about' | 'menus' | 'reviews';

export default function CookDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [chef, setChef] = useState<any>(null);
  const [menus, setMenus] = useState<any[]>([]);
  const [reviews, setReviews] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('about');

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [c, m, r] = await Promise.all([
        api.getChef(id),
        api.getChefMenus(id).catch(() => []),
        api.getChefReviews(id).catch(() => []),
      ]);
      setChef(c);
      setMenus(m ?? []);
      setReviews(r ?? []);
    } catch {
      setChef(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return <View style={styles.center}><ActivityIndicator color="#f97316" size="large" /></View>;
  }
  if (!chef) {
    return <View style={styles.center}><Text style={styles.muted}>Cook not found</Text></View>;
  }

  const cuisines = (chef.cuisines ?? '').split(',').map((c: string) => c.trim()).filter(Boolean);

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: chef.name ?? 'Cook' }} />
      <ScrollView contentContainerStyle={{ paddingBottom: 120 }}>
        {/* Header */}
        <View style={styles.header}>
          {chef.profilePhotoUrl ? (
            <Image source={{ uri: chef.profilePhotoUrl }} style={styles.avatar} />
          ) : (
            <View style={[styles.avatar, styles.avatarFallback]}><Text style={styles.avatarIcon}>👨‍🍳</Text></View>
          )}
          <View style={styles.nameRow}>
            <Text style={styles.name}>{chef.name}</Text>
            {chef.verified && <View style={styles.badge}><Text style={styles.badgeText}>✓ Verified</Text></View>}
            {chef.badge ? <View style={styles.topBadge}><Text style={styles.topBadgeText}>{pretty(chef.badge)}</Text></View> : null}
          </View>
          <Text style={styles.city}>{chef.city}{chef.state ? `, ${chef.state}` : ''}</Text>
          {chef.available === false && <Text style={styles.unavail}>Currently unavailable</Text>}

          {/* Stats */}
          <View style={styles.statsRow}>
            <Stat label="Rating" value={chef.rating ? `★ ${chef.rating.toFixed(1)}` : '—'} sub={chef.reviewCount ? `${chef.reviewCount}` : ''} />
            <Stat label="Experience" value={chef.experienceYears ? `${chef.experienceYears}y` : '—'} />
            <Stat label="Bookings" value={chef.totalBookings ?? '—'} />
            <Stat label="Completion" value={chef.completionRate != null ? `${chef.completionRate}%` : '—'} />
          </View>
        </View>

        {/* Pricing CTAs */}
        <View style={styles.priceCards}>
          {chef.dailyRatePaise ? (
            <PriceCard
              title="Book for a Day" price={formatPaise(chef.dailyRatePaise)}
              onPress={() => router.push(`/cook-book?chefId=${id}&type=DAILY`)}
            />
          ) : null}
          {chef.eventMinPlatePaise ? (
            <PriceCard
              title="Event Quote" price={`${formatPaise(chef.eventMinPlatePaise)}/plate`}
              onPress={() => router.push(`/cook-book?chefId=${id}&type=EVENT`)}
            />
          ) : null}
        </View>

        {/* Tabs */}
        <View style={styles.tabBar}>
          {(['about', 'menus', 'reviews'] as Tab[]).map((t) => (
            <TouchableOpacity key={t} style={[styles.tab, tab === t && styles.tabActive]} onPress={() => setTab(t)}>
              <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
                {t === 'about' ? 'About' : t === 'menus' ? `Menus (${menus.length})` : `Reviews (${reviews.length})`}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.tabContent}>
          {tab === 'about' && (
            <View>
              {chef.bio ? <Text style={styles.bio}>{chef.bio}</Text> : null}
              {cuisines.length > 0 && (
                <>
                  <Text style={styles.subhead}>Cuisines</Text>
                  <View style={styles.chipWrap}>
                    {cuisines.map((c: string) => <View key={c} style={styles.chip}><Text style={styles.chipText}>{pretty(c)}</Text></View>)}
                  </View>
                </>
              )}
              {chef.specialties ? <><Text style={styles.subhead}>Specialties</Text><Text style={styles.body}>{chef.specialties}</Text></> : null}
              {chef.localities ? <><Text style={styles.subhead}>Serving areas</Text><Text style={styles.body}>{chef.localities}</Text></> : null}
              {(chef.eventMinPax || chef.eventMaxPax) ? (
                <><Text style={styles.subhead}>Event capacity</Text><Text style={styles.body}>{chef.eventMinPax ?? '?'}–{chef.eventMaxPax ?? '?'} guests</Text></>
              ) : null}
              {chef.foodSafetyCertificate ? <Text style={styles.safeBadge}>🛡️ Food safety certified</Text> : null}
            </View>
          )}

          {tab === 'menus' && (
            menus.length === 0 ? <Text style={styles.muted}>No menus listed yet.</Text> :
            menus.map((m) => (
              <View key={m.id} style={styles.menuCard}>
                <View style={styles.menuTop}>
                  <Text style={styles.menuName}>{m.name}</Text>
                  {m.pricePerPlatePaise ? <Text style={styles.menuPrice}>{formatPaise(m.pricePerPlatePaise)}/plate</Text> : null}
                </View>
                {m.description ? <Text style={styles.menuDesc}>{m.description}</Text> : null}
                <View style={styles.chipWrap}>
                  {m.cuisineType ? <View style={styles.tagChip}><Text style={styles.tagText}>{pretty(m.cuisineType)}</Text></View> : null}
                  {m.mealType ? <View style={styles.tagChip}><Text style={styles.tagText}>{pretty(m.mealType)}</Text></View> : null}
                  {m.isVeg ? <View style={[styles.tagChip, styles.vegChip]}><Text style={styles.vegText}>Veg</Text></View> : null}
                  {m.isJain ? <View style={[styles.tagChip, styles.vegChip]}><Text style={styles.vegText}>Jain</Text></View> : null}
                </View>
                {(m.minGuests || m.maxGuests) ? <Text style={styles.menuMeta}>{m.minGuests ?? 1}–{m.maxGuests ?? '∞'} guests</Text> : null}
              </View>
            ))
          )}

          {tab === 'reviews' && (
            reviews.length === 0 ? <Text style={styles.muted}>No reviews yet.</Text> :
            reviews.map((r) => (
              <View key={r.id} style={styles.reviewCard}>
                <View style={styles.reviewTop}>
                  <Text style={styles.reviewName}>{r.customerName ?? 'Guest'}</Text>
                  <Text style={styles.reviewStars}>{'★'.repeat(Math.round(r.rating ?? 0))}</Text>
                </View>
                {r.comment ? <Text style={styles.reviewComment}>{r.comment}</Text> : null}
                {r.serviceDate ? <Text style={styles.reviewDate}>{r.serviceDate}</Text> : null}
              </View>
            ))
          )}
        </View>
      </ScrollView>

      {/* Sticky book bar */}
      <View style={styles.bookBar}>
        <TouchableOpacity style={styles.bookBtn} onPress={() => router.push(`/cook-book?chefId=${id}&type=DAILY`)}>
          <Text style={styles.bookBtnText}>Book this cook</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

function Stat({ label, value, sub }: { label: string; value: any; sub?: string }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statValue}>{value}{sub ? <Text style={styles.statSub}> ({sub})</Text> : null}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

function PriceCard({ title, price, onPress }: { title: string; price: string; onPress: () => void }) {
  return (
    <TouchableOpacity style={styles.priceCard} onPress={onPress}>
      <Text style={styles.priceCardTitle}>{title}</Text>
      <Text style={styles.priceCardPrice}>{price}</Text>
      <Text style={styles.priceCardCta}>Select →</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container:        { flex: 1, backgroundColor: '#f9fafb' },
  center:           { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  muted:            { color: '#9ca3af', fontSize: 14 },

  header:           { backgroundColor: '#fff', padding: 20, alignItems: 'center' },
  avatar:           { width: 88, height: 88, borderRadius: 44 },
  avatarFallback:   { backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  avatarIcon:       { fontSize: 40 },
  nameRow:          { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 12, flexWrap: 'wrap', justifyContent: 'center' },
  name:             { fontSize: 22, fontWeight: '800', color: '#111827' },
  badge:            { backgroundColor: '#dcfce7', borderRadius: 100, paddingHorizontal: 8, paddingVertical: 2 },
  badgeText:        { fontSize: 11, fontWeight: '700', color: '#15803d' },
  topBadge:         { backgroundColor: '#fef3c7', borderRadius: 100, paddingHorizontal: 8, paddingVertical: 2 },
  topBadgeText:     { fontSize: 11, fontWeight: '700', color: '#b45309' },
  city:             { fontSize: 14, color: '#6b7280', marginTop: 4 },
  unavail:          { fontSize: 12, color: '#dc2626', marginTop: 4, fontWeight: '600' },

  statsRow:         { flexDirection: 'row', justifyContent: 'space-around', alignSelf: 'stretch', marginTop: 16, paddingTop: 16, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  stat:             { alignItems: 'center' },
  statValue:        { fontSize: 16, fontWeight: '800', color: '#111827' },
  statSub:          { fontSize: 11, color: '#9ca3af', fontWeight: '600' },
  statLabel:        { fontSize: 11, color: '#6b7280', marginTop: 2 },

  priceCards:       { flexDirection: 'row', gap: 10, padding: 16 },
  priceCard:        { flex: 1, backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#fed7aa' },
  priceCardTitle:   { fontSize: 13, fontWeight: '700', color: '#111827' },
  priceCardPrice:   { fontSize: 16, fontWeight: '800', color: '#f97316', marginTop: 6 },
  priceCardCta:     { fontSize: 12, color: '#c2410c', marginTop: 6, fontWeight: '600' },

  tabBar:           { flexDirection: 'row', backgroundColor: '#fff', paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  tab:              { paddingVertical: 12, paddingHorizontal: 12, borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive:        { borderBottomColor: '#f97316' },
  tabText:          { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  tabTextActive:    { color: '#f97316' },
  tabContent:       { padding: 16 },

  bio:              { fontSize: 14, color: '#374151', lineHeight: 21 },
  subhead:          { fontSize: 13, fontWeight: '700', color: '#111827', marginTop: 16, marginBottom: 6 },
  body:             { fontSize: 14, color: '#374151' },
  chipWrap:         { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  chip:             { backgroundColor: '#fff7ed', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  chipText:         { fontSize: 12, fontWeight: '600', color: '#c2410c' },
  safeBadge:        { fontSize: 13, color: '#15803d', fontWeight: '600', marginTop: 12 },

  menuCard:         { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  menuTop:          { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  menuName:         { fontSize: 15, fontWeight: '700', color: '#111827', flexShrink: 1 },
  menuPrice:        { fontSize: 14, fontWeight: '800', color: '#f97316' },
  menuDesc:         { fontSize: 13, color: '#6b7280', marginTop: 4, marginBottom: 8 },
  menuMeta:         { fontSize: 12, color: '#6b7280', marginTop: 8 },
  tagChip:          { backgroundColor: '#f3f4f6', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  tagText:          { fontSize: 11, fontWeight: '600', color: '#374151' },
  vegChip:          { backgroundColor: '#dcfce7' },
  vegText:          { fontSize: 11, fontWeight: '700', color: '#15803d' },

  reviewCard:       { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  reviewTop:        { flexDirection: 'row', justifyContent: 'space-between' },
  reviewName:       { fontSize: 14, fontWeight: '700', color: '#111827' },
  reviewStars:      { fontSize: 13, color: '#f59e0b' },
  reviewComment:    { fontSize: 13, color: '#374151', marginTop: 6, lineHeight: 19 },
  reviewDate:       { fontSize: 11, color: '#9ca3af', marginTop: 6 },

  bookBar:          { position: 'absolute', bottom: 0, left: 0, right: 0, backgroundColor: '#fff', padding: 16, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  bookBtn:          { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center' },
  bookBtnText:      { color: '#fff', fontSize: 16, fontWeight: '700' },
});
