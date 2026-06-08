import { useEffect, useState, useCallback, useRef } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator,
  TextInput, Alert, Modal, Linking,
} from 'react-native';
import * as Clipboard from 'expo-clipboard';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken, getUserId } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';

const STAGES = [
  { key: 'confirmed', label: 'Confirmed', icon: '✅' },
  { key: 'enroute',   label: 'En route',  icon: '🛵' },
  { key: 'arrived',   label: 'Arrived',   icon: '📍' },
  { key: 'started',   label: 'Cooking',   icon: '🍳' },
  { key: 'done',      label: 'Done',      icon: '🎉' },
];

// map booking status → tracking stage index
function stageIndex(b: any): number {
  if (b?.status === 'COMPLETED') return 4;
  if (b?.jobStartedAt || b?.status === 'IN_PROGRESS') return 3;
  if (b?.status === 'ADVANCE_PAID' || b?.status === 'CONFIRMED') return 0;
  return -1;
}

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

type Tab = 'details' | 'provider' | 'pay' | 'rate';

export default function CookBookingDetailScreen() {
  const { id, kind: paramKind } = useLocalSearchParams<{ id: string; kind?: string }>();
  const router = useRouter();
  const isEvent = paramKind === 'event';

  const [booking, setBooking] = useState<any>(null);
  const [chef, setChef] = useState<any>(null);
  const [vendor, setVendor] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('details');

  // chat
  const [convId, setConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [draft, setDraft] = useState('');
  const [userId, setUserId] = useState<string | null>(null);
  const [chatOpen, setChatOpen] = useState(false);

  // pay / rate
  const [payOrder, setPayOrder] = useState<any>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    const token = await getAccessToken();
    setUserId(await getUserId());
    try {
      let b: any = null;
      if (isEvent) {
        b = await api.getEventBookingById(id).catch(() => null);
      } else {
        b = await api.getChefBookingById(id).catch(() => null);
        if (!b) b = await api.getEventBookingById(id).catch(() => null);
      }
      setBooking(b);
      if (b?.chefId) api.getChef(b.chefId).then(setChef).catch(() => {});
      if (isEvent && token) api.getEventActiveVendor(id, token).then(setVendor).catch(() => {});
      // chat
      if (token) {
        const convs = await api.getConversations(token).catch(() => []);
        const conv = (convs ?? []).find((c: any) => c.bookingId === id || c.bookingId === b?.id);
        if (conv) {
          setConvId(conv.id);
          const msgs = await api.getMessages(conv.id, token, 0).catch(() => []);
          setMessages(msgs?.content ?? msgs ?? []);
        }
      }
    } finally {
      setLoading(false);
    }
  }, [id, isEvent]);

  useEffect(() => { load(); }, [load]);

  async function sendChat() {
    const token = await getAccessToken();
    if (!token || !draft.trim() || !booking) return;
    const recipientId = chef?.userId ?? vendor?.vendorUserId ?? booking.chefId;
    try {
      await api.sendMessage({ listingId: id!, recipientId, bookingId: id!, content: draft.trim() }, token);
      setDraft('');
      const convs = await api.getConversations(token).catch(() => []);
      const conv = (convs ?? []).find((c: any) => c.bookingId === id);
      if (conv) {
        setConvId(conv.id);
        const msgs = await api.getMessages(conv.id, token, 0).catch(() => []);
        setMessages(msgs?.content ?? msgs ?? []);
      }
    } catch (e: any) {
      Alert.alert('Could not send', e.message ?? 'Try again');
    }
  }

  async function payBalance() {
    const token = await getAccessToken();
    if (!token || !booking) return;
    const bal = booking.balanceAmountPaise ?? 0;
    if (bal <= 0) return;
    setBusy(true);
    try {
      const order = await api.createCookPaymentOrder(booking.id, bal, token);
      setPayOrder(order);
    } catch (e: any) {
      Alert.alert('Could not start payment', e.message ?? 'Try again');
    } finally {
      setBusy(false);
    }
  }

  async function onPaySuccess(res: { paymentId: string; orderId: string }) {
    const token = await getAccessToken();
    try {
      if (token) {
        if (isEvent) await api.payEventBookingBalance(booking.id, res.orderId, res.paymentId, token);
        else await api.payChefBookingBalance(booking.id, res.orderId, res.paymentId, token);
      }
      setPayOrder(null);
      Alert.alert('Paid', 'Balance payment received.');
      load();
    } catch (e: any) {
      setPayOrder(null);
      Alert.alert('Confirmation failed', e.message ?? 'Contact support with your payment id.');
    }
  }

  async function submitRating() {
    const token = await getAccessToken();
    if (!token || !booking) return;
    setBusy(true);
    try {
      if (isEvent) await api.rateEventBooking(booking.id, rating, comment, token);
      else await api.rateChefBooking(booking.id, rating, comment, token);
      Alert.alert('Thanks!', 'Your rating was submitted.');
      load();
    } catch (e: any) {
      Alert.alert('Could not submit', e.message ?? 'Try again');
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator color="#f97316" size="large" /></View>;
  if (!booking) return <View style={styles.center}><Text style={styles.muted}>Booking not found</Text></View>;

  if (payOrder) {
    return (
      <Modal visible animationType="slide">
        <CookPaymentWebView
          order={payOrder}
          prefill={{ name: booking.customerName ?? '', email: '', phone: booking.customerPhone ?? '' }}
          onSuccess={onPaySuccess}
          onFailure={(err) => { Alert.alert('Payment failed', err); setPayOrder(null); }}
          onDismiss={() => setPayOrder(null)}
        />
      </Modal>
    );
  }

  const providerName = chef?.name ?? vendor?.vendorBusinessName ?? booking.chefName ?? booking.vendorBusinessName ?? 'your cook';
  const providerPhone = chef?.phone ?? vendor?.vendorPhone ?? booking.chefPhone;
  const stage = stageIndex(booking);
  const date = isEvent ? booking.eventDate : booking.serviceDate;
  const time = isEvent ? booking.eventTime : booking.serviceTime;
  const balance = booking.balanceAmountPaise ?? 0;
  const balancePaid = booking.paymentStatus === 'FULLY_PAID' || booking.balancePaidAt;
  const canRate = booking.status === 'COMPLETED' && !booking.ratingGiven;
  const isCustomer = !userId || userId === booking.customerId;

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: isEvent ? 'Event booking' : 'Cook booking' }} />
      <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Header */}
        <View style={styles.header}>
          <View style={styles.headerTop}>
            <Text style={styles.headerTitle}>{providerName}</Text>
            <View style={styles.statusBadge}><Text style={styles.statusBadgeText}>{pretty(booking.status)}</Text></View>
          </View>
          {booking.bookingRef ? <Text style={styles.headerRef}>{booking.bookingRef}</Text> : null}
          <Text style={styles.headerLine}>📅 {date}{time ? ` · ${time}` : ''}</Text>
        </View>

        {/* Tracking */}
        {stage >= 0 && (
          <View style={styles.tracking}>
            <Text style={styles.trackingTitle}>Service tracking</Text>
            <View style={styles.stagesRow}>
              {STAGES.map((s, i) => (
                <View key={s.key} style={styles.stageItem}>
                  <View style={[styles.stageDot, i <= stage && styles.stageDotActive]}>
                    <Text style={styles.stageIcon}>{i <= stage ? s.icon : '○'}</Text>
                  </View>
                  <Text style={[styles.stageLabel, i <= stage && styles.stageLabelActive]}>{s.label}</Text>
                  {i < STAGES.length - 1 && <View style={[styles.stageLine, i < stage && styles.stageLineActive]} />}
                </View>
              ))}
            </View>
          </View>
        )}

        {/* Start OTP */}
        {isCustomer && booking.startJobOtp && !booking.jobStartedAt && (
          <View style={styles.otpCard}>
            <Text style={styles.otpTitle}>Share this OTP when {providerName} arrives</Text>
            <Text style={styles.otpBig}>{booking.startJobOtp}</Text>
            <View style={styles.otpBtns}>
              <TouchableOpacity style={styles.otpBtn} onPress={() => { Clipboard.setStringAsync(String(booking.startJobOtp)); Alert.alert('Copied'); }}>
                <Text style={styles.otpBtnText}>Copy</Text>
              </TouchableOpacity>
              {providerPhone ? (
                <TouchableOpacity style={styles.otpBtn} onPress={() => Linking.openURL(`https://wa.me/91${providerPhone}?text=${encodeURIComponent('My start OTP is ' + booking.startJobOtp)}`)}>
                  <Text style={styles.otpBtnText}>WhatsApp</Text>
                </TouchableOpacity>
              ) : null}
            </View>
          </View>
        )}

        {/* Tabs */}
        <View style={styles.tabBar}>
          {(['details', 'provider', 'pay', 'rate'] as Tab[]).map((t) => (
            <TouchableOpacity key={t} style={[styles.tab, tab === t && styles.tabActive]} onPress={() => setTab(t)}>
              <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
                {t === 'details' ? 'Details' : t === 'provider' ? 'Provider' : t === 'pay' ? 'Pay' : 'Rate'}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.tabContent}>
          {tab === 'details' && (
            <View>
              {!isEvent && booking.mealType ? <Row label="Meal" value={`${pretty(booking.mealType)} · ${booking.guestsCount} pax`} /> : null}
              {isEvent && booking.eventType ? <Row label="Occasion" value={pretty(booking.eventType)} /> : null}
              {isEvent && booking.guestCount ? <Row label="Guests" value={String(booking.guestCount)} /> : null}
              {booking.menuName ? <Row label="Menu" value={booking.menuName} /> : null}
              <Row label="Address" value={booking.address || booking.venueAddress || '—'} />
              {booking.specialRequests ? <Row label="Notes" value={booking.specialRequests} /> : null}
              <Row label="Total" value={formatPaise(booking.totalAmountPaise ?? 0)} />
            </View>
          )}

          {tab === 'provider' && (
            <View style={styles.providerCard}>
              <Text style={styles.providerIcon}>👨‍🍳</Text>
              <Text style={styles.providerName}>{providerName}</Text>
              {(chef?.rating ?? vendor?.vendorRatingAvg) ? (
                <Text style={styles.providerMeta}>★ {(chef?.rating ?? vendor?.vendorRatingAvg).toFixed(1)} · {chef?.totalBookings ?? vendor?.vendorJobsCompleted ?? 0} jobs</Text>
              ) : null}
              {chef?.bio ? <Text style={styles.providerBio}>{chef.bio}</Text> : null}
              {vendor?.vendorBusinessName ? <View style={styles.vendorTag}><Text style={styles.vendorTagText}>Partner: {vendor.vendorBusinessName} ({pretty(vendor.status)})</Text></View> : null}

              <View style={styles.contactRow}>
                {providerPhone ? (
                  <TouchableOpacity style={styles.contactBtn} onPress={() => Linking.openURL(`tel:${providerPhone}`)}>
                    <Text style={styles.contactBtnText}>📞 Call {chef ? 'cook' : 'provider'}</Text>
                  </TouchableOpacity>
                ) : null}
                <TouchableOpacity style={[styles.contactBtn, styles.chatBtn]} onPress={() => setChatOpen(true)}>
                  <Text style={[styles.contactBtnText, styles.chatBtnText]}>💬 Chat with {chef ? 'cook' : 'provider'}</Text>
                </TouchableOpacity>
              </View>
              {chef?.id ? (
                <TouchableOpacity onPress={() => router.push(`/cook/${chef.id}`)}><Text style={styles.profileLink}>View full profile →</Text></TouchableOpacity>
              ) : null}
            </View>
          )}

          {tab === 'pay' && (
            <View>
              <Row label="Total" value={formatPaise(booking.totalAmountPaise ?? 0)} />
              <Row label="Advance paid" value={formatPaise(booking.advancePaidPaise ?? booking.advanceAmountPaise ?? 0)} />
              <Row label="Balance due" value={formatPaise(balance)} bold />
              {balancePaid ? (
                <View style={styles.paidBox}><Text style={styles.paidText}>✅ Balance fully paid</Text></View>
              ) : balance > 0 ? (
                <TouchableOpacity style={[styles.primaryBtn, busy && styles.btnDisabled]} disabled={busy} onPress={payBalance}>
                  {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Pay balance {formatPaise(balance)}</Text>}
                </TouchableOpacity>
              ) : (
                <Text style={styles.muted}>No balance due.</Text>
              )}
            </View>
          )}

          {tab === 'rate' && (
            booking.ratingGiven ? (
              <View>
                <Text style={styles.ratedStars}>{'★'.repeat(booking.ratingGiven)}</Text>
                {booking.reviewComment ? <Text style={styles.body}>{booking.reviewComment}</Text> : null}
                <Text style={styles.muted}>You already rated this booking.</Text>
              </View>
            ) : !canRate ? (
              <Text style={styles.muted}>You can rate after the service is completed.</Text>
            ) : (
              <View>
                <Text style={styles.label}>Your rating</Text>
                <View style={styles.starRow}>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <TouchableOpacity key={n} onPress={() => setRating(n)}><Text style={[styles.star, n <= rating && styles.starActive]}>★</Text></TouchableOpacity>
                  ))}
                </View>
                <TextInput style={[styles.input, styles.textarea]} value={comment} onChangeText={setComment} placeholder="Tell us how it went…" placeholderTextColor="#9ca3af" multiline />
                <TouchableOpacity style={[styles.primaryBtn, busy && styles.btnDisabled]} disabled={busy} onPress={submitRating}>
                  {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Submit rating</Text>}
                </TouchableOpacity>
              </View>
            )
          )}
        </View>
      </ScrollView>

      {/* Chat modal */}
      <Modal visible={chatOpen} animationType="slide" onRequestClose={() => setChatOpen(false)}>
        <ChatPanel
          title={`Chat with ${chef ? 'cook' : 'provider'}`}
          messages={messages}
          userId={userId}
          draft={draft}
          setDraft={setDraft}
          onSend={sendChat}
          onClose={() => setChatOpen(false)}
        />
      </Modal>
    </View>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <View style={styles.dataRow}>
      <Text style={styles.dataLabel}>{label}</Text>
      <Text style={[styles.dataValue, bold && styles.dataValueBold]}>{value}</Text>
    </View>
  );
}

function ChatPanel({ title, messages, userId, draft, setDraft, onSend, onClose }: any) {
  const scrollRef = useRef<ScrollView>(null);
  return (
    <View style={styles.chatContainer}>
      <View style={styles.chatHeader}>
        <Text style={styles.chatTitle}>{title}</Text>
        <TouchableOpacity onPress={onClose}><Text style={styles.chatClose}>✕</Text></TouchableOpacity>
      </View>
      <ScrollView ref={scrollRef} style={styles.chatBody} onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}>
        {messages.length === 0 ? <Text style={styles.chatEmpty}>No messages yet. Say hello!</Text> :
          messages.map((m: any) => {
            const mine = m.senderId === userId;
            return (
              <View key={m.id} style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleTheirs]}>
                <Text style={[styles.bubbleText, mine && styles.bubbleTextMine]}>{m.content}</Text>
              </View>
            );
          })}
      </ScrollView>
      <View style={styles.chatInputRow}>
        <TextInput style={styles.chatInput} value={draft} onChangeText={setDraft} placeholder="Type a message…" placeholderTextColor="#9ca3af" />
        <TouchableOpacity style={styles.chatSend} onPress={onSend}><Text style={styles.chatSendText}>Send</Text></TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  center:         { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  muted:          { color: '#9ca3af', fontSize: 14, marginTop: 8 },
  body:           { fontSize: 14, color: '#374151', marginVertical: 6 },

  header:         { backgroundColor: '#fff', padding: 18 },
  headerTop:      { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle:    { fontSize: 19, fontWeight: '800', color: '#111827', flexShrink: 1 },
  statusBadge:    { backgroundColor: '#fff7ed', borderRadius: 100, paddingHorizontal: 10, paddingVertical: 4 },
  statusBadgeText:{ fontSize: 11, fontWeight: '700', color: '#c2410c' },
  headerRef:      { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  headerLine:     { fontSize: 14, color: '#4b5563', marginTop: 8 },

  tracking:       { backgroundColor: '#fff', marginTop: 12, padding: 16 },
  trackingTitle:  { fontSize: 13, fontWeight: '700', color: '#111827', marginBottom: 14 },
  stagesRow:      { flexDirection: 'row', justifyContent: 'space-between' },
  stageItem:      { flex: 1, alignItems: 'center', position: 'relative' },
  stageDot:       { width: 34, height: 34, borderRadius: 17, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center', zIndex: 1 },
  stageDotActive: { backgroundColor: '#fff7ed', borderWidth: 2, borderColor: '#f97316' },
  stageIcon:      { fontSize: 15 },
  stageLabel:     { fontSize: 10, color: '#9ca3af', marginTop: 4, fontWeight: '600' },
  stageLabelActive:{ color: '#c2410c' },
  stageLine:      { position: 'absolute', top: 17, left: '50%', right: -50, height: 2, backgroundColor: '#f3f4f6' },
  stageLineActive:{ backgroundColor: '#f97316' },

  otpCard:        { backgroundColor: '#f0fdf4', margin: 12, borderRadius: 12, padding: 16, alignItems: 'center', borderWidth: 1, borderColor: '#bbf7d0' },
  otpTitle:       { fontSize: 13, color: '#15803d', fontWeight: '600', textAlign: 'center' },
  otpBig:         { fontSize: 36, fontWeight: '800', color: '#15803d', letterSpacing: 8, marginVertical: 8, fontVariant: ['tabular-nums'] },
  otpBtns:        { flexDirection: 'row', gap: 10 },
  otpBtn:         { backgroundColor: '#16a34a', borderRadius: 8, paddingHorizontal: 18, paddingVertical: 8 },
  otpBtnText:     { color: '#fff', fontWeight: '700', fontSize: 13 },

  tabBar:         { flexDirection: 'row', backgroundColor: '#fff', marginTop: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  tab:            { flex: 1, paddingVertical: 13, alignItems: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive:      { borderBottomColor: '#f97316' },
  tabText:        { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  tabTextActive:  { color: '#f97316' },
  tabContent:     { backgroundColor: '#fff', padding: 16 },

  dataRow:        { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#f9fafb' },
  dataLabel:      { fontSize: 13, color: '#6b7280' },
  dataValue:      { fontSize: 13, color: '#111827', fontWeight: '600', flexShrink: 1, textAlign: 'right', marginLeft: 12 },
  dataValueBold:  { fontSize: 16, fontWeight: '800', color: '#f97316' },

  providerCard:   { alignItems: 'center' },
  providerIcon:   { fontSize: 48 },
  providerName:   { fontSize: 18, fontWeight: '800', color: '#111827', marginTop: 8 },
  providerMeta:   { fontSize: 13, color: '#6b7280', marginTop: 4 },
  providerBio:    { fontSize: 13, color: '#374151', textAlign: 'center', marginTop: 10, lineHeight: 19 },
  vendorTag:      { backgroundColor: '#eef2ff', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6, marginTop: 10 },
  vendorTagText:  { fontSize: 12, color: '#4338ca', fontWeight: '600' },
  contactRow:     { flexDirection: 'row', gap: 10, marginTop: 16, alignSelf: 'stretch' },
  contactBtn:     { flex: 1, borderWidth: 1, borderColor: '#f97316', borderRadius: 10, paddingVertical: 11, alignItems: 'center' },
  contactBtnText: { color: '#f97316', fontWeight: '700', fontSize: 13 },
  chatBtn:        { backgroundColor: '#f97316' },
  chatBtnText:    { color: '#fff' },
  profileLink:    { color: '#f97316', fontWeight: '600', marginTop: 16 },

  paidBox:        { backgroundColor: '#f0fdf4', borderRadius: 10, padding: 14, marginTop: 12, alignItems: 'center' },
  paidText:       { color: '#15803d', fontWeight: '700' },

  label:          { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 8, marginTop: 4 },
  starRow:        { flexDirection: 'row', gap: 6, marginBottom: 14 },
  star:           { fontSize: 38, color: '#e5e7eb' },
  starActive:     { color: '#f59e0b' },
  ratedStars:     { fontSize: 28, color: '#f59e0b' },
  input:          { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:       { height: 90, textAlignVertical: 'top', marginBottom: 14 },

  primaryBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginTop: 8 },
  btnDisabled:    { opacity: 0.6 },
  primaryBtnText: { color: '#fff', fontSize: 15, fontWeight: '700' },

  // chat
  chatContainer:  { flex: 1, backgroundColor: '#f9fafb' },
  chatHeader:     { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, paddingTop: 50, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  chatTitle:      { fontSize: 16, fontWeight: '700', color: '#111827' },
  chatClose:      { fontSize: 20, color: '#6b7280' },
  chatBody:       { flex: 1, padding: 16 },
  chatEmpty:      { color: '#9ca3af', textAlign: 'center', marginTop: 40 },
  bubble:         { maxWidth: '78%', borderRadius: 14, paddingHorizontal: 12, paddingVertical: 8, marginBottom: 8 },
  bubbleMine:     { backgroundColor: '#f97316', alignSelf: 'flex-end' },
  bubbleTheirs:   { backgroundColor: '#fff', alignSelf: 'flex-start', borderWidth: 1, borderColor: '#f3f4f6' },
  bubbleText:     { fontSize: 14, color: '#111827' },
  bubbleTextMine: { color: '#fff' },
  chatInputRow:   { flexDirection: 'row', gap: 8, padding: 12, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  chatInput:      { flex: 1, backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 100, paddingHorizontal: 16, paddingVertical: 10, fontSize: 14 },
  chatSend:       { backgroundColor: '#f97316', borderRadius: 100, paddingHorizontal: 18, justifyContent: 'center' },
  chatSendText:   { color: '#fff', fontWeight: '700' },
});
