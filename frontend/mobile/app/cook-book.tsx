import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, Modal,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';
import { addToCart } from '@/lib/cookCart';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'ALL_DAY'];
const EVENT_CUISINES = ['North Indian', 'South Indian', 'Chinese', 'Continental', 'Mughlai', 'Multi-cuisine', 'Rajasthani', 'Bengali', 'Street Food', 'Jain'];

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

type ServiceType = 'DAILY' | 'MONTHLY' | 'EVENT';

export default function CookBookScreen() {
  const { chefId, type: paramType, eventType: paramEventType } =
    useLocalSearchParams<{ chefId?: string; type?: string; eventType?: string }>();
  const router = useRouter();
  const initialType: ServiceType = paramType === 'EVENT' ? 'EVENT' : paramType === 'MONTHLY' ? 'MONTHLY' : 'DAILY';
  const [type, setType] = useState<ServiceType>(initialType);

  const [chef, setChef] = useState<any>(null);

  // common
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [locality, setLocality] = useState('');
  const [pincode, setPincode] = useState('');
  const [specialRequests, setSpecialRequests] = useState('');

  // daily
  const [mealType, setMealType] = useState('LUNCH');
  const [serviceDate, setServiceDate] = useState('');
  const [serviceTime, setServiceTime] = useState('12:00');
  const [guestsCount, setGuestsCount] = useState('2');
  const [numberOfMeals, setNumberOfMeals] = useState('1');

  // monthly
  const [plan, setPlan] = useState('Standard');
  const [mealsPerDay, setMealsPerDay] = useState('2');
  const [schedule, setSchedule] = useState('Mon-Sat');
  const [startDate, setStartDate] = useState('');
  const [dietaryPreferences, setDietaryPreferences] = useState('');

  // event
  const [eventType, setEventType] = useState(paramEventType ?? 'BIRTHDAY');
  const [eventDate, setEventDate] = useState('');
  const [eventTime, setEventTime] = useState('19:00');
  const [guestCount, setGuestCount] = useState('30');
  const [durationHours, setDurationHours] = useState('4');
  const [menuDescription, setMenuDescription] = useState('');
  const [cuisinePref, setCuisinePref] = useState('');
  const [eventPricing, setEventPricing] = useState<any[]>([]);
  const [selectedAddons, setSelectedAddons] = useState<Record<string, boolean>>({});

  // dish catalog (per-dish selection with filters)
  const [dishCatalog, setDishCatalog] = useState<any[]>([]);
  const [dishModal, setDishModal] = useState(false);
  const [dishFilter, setDishFilter] = useState<'all' | 'veg' | 'fried' | 'recommended' | 'nog'>('all');
  const [selectedDishes, setSelectedDishes] = useState<Record<string, any>>({});

  const [submitting, setSubmitting] = useState(false);
  const [payOrder, setPayOrder] = useState<any>(null);   // { order, bookingId }
  const [done, setDone] = useState<any>(null);

  useEffect(() => {
    if (chefId) api.getChef(chefId).then(setChef).catch(() => {});
  }, [chefId]);

  useEffect(() => {
    if (type === 'EVENT') api.getEventPricing(chefId).then((p) => setEventPricing(p ?? [])).catch(() => {});
  }, [type, chefId]);

  useEffect(() => {
    if (type === 'EVENT' && dishCatalog.length === 0) {
      api.getDishCatalog().then((d) => setDishCatalog(Array.isArray(d) ? d : (d?.items ?? d?.dishes ?? []))).catch(() => {});
    }
  }, [type, dishCatalog.length]);

  const filteredDishes = dishCatalog.filter((d) => {
    if (dishFilter === 'veg') return d.isVeg;
    if (dishFilter === 'fried') return d.isFried;
    if (dishFilter === 'recommended') return d.isRecommended;
    if (dishFilter === 'nog') return d.noOnionGarlic;
    return true;
  });
  const selectedDishList = Object.values(selectedDishes);
  function toggleDish(d: any) {
    setSelectedDishes((prev) => {
      const next = { ...prev };
      if (next[d.id]) delete next[d.id]; else next[d.id] = { id: d.id, name: d.name, pricePaise: d.pricePaise ?? 0 };
      return next;
    });
  }

  const addons = eventPricing.filter((p) => p.category === 'ADDON' || p.category === 'LIVE_COUNTER');
  const addonPaise = (id: string) => {
    const a = addons.find((x) => (x.id ?? x.code) === id);
    return a ? (a.pricePaise ?? a.paise ?? 0) : 0;
  };
  const eventEstimatePaise = (() => {
    const perPlate = chef?.eventMinPlatePaise ?? 0;
    const food = perPlate * Number(guestCount || 0);
    const addonTotal = Object.entries(selectedAddons)
      .filter(([, on]) => on)
      .reduce((s, [id]) => s + addonPaise(id), 0);
    return food + addonTotal;
  })();

  async function submitDaily() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    if (!customerName || !customerPhone || !serviceDate || !address) {
      Alert.alert('Missing details', 'Please fill name, phone, service date and address.');
      return;
    }
    setSubmitting(true);
    try {
      const booking = await api.bookChef({
        chefId, serviceType: 'DAILY', mealType, serviceDate, serviceTime,
        guestsCount: Number(guestsCount), numberOfMeals: Number(numberOfMeals),
        specialRequests, address, city, locality, pincode, customerName, customerPhone,
      }, token);
      const advance = booking.advanceAmountPaise ?? booking.totalAmountPaise ?? 0;
      if (advance > 0) {
        const order = await api.createCookPaymentOrder(booking.id, advance, token);
        setPayOrder({ order, booking });
      } else {
        setDone(booking);
      }
    } catch (e: any) {
      Alert.alert('Booking failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  async function submitMonthly() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    if (!customerName || !customerPhone || !startDate || !address) {
      Alert.alert('Missing details', 'Please fill name, phone, start date and address.');
      return;
    }
    setSubmitting(true);
    try {
      const sub = await api.createChefSubscription({
        chefId, serviceType: 'MONTHLY', plan, mealsPerDay: Number(mealsPerDay), schedule, startDate,
        dietaryPreferences, mealType, address, city, locality, pincode, customerName, customerPhone, specialRequests,
      }, token);
      setDone({ ...sub, isSubscription: true });
    } catch (e: any) {
      Alert.alert('Subscription failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  async function submitEvent() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    if (!customerName || !customerPhone || !eventDate || !address) {
      Alert.alert('Missing details', 'Please fill name, phone, event date and venue.');
      return;
    }
    setSubmitting(true);
    try {
      const dishNote = selectedDishList.length ? `Selected dishes: ${selectedDishList.map((d: any) => d.name).join(', ')}` : '';
      const event = await api.createEventBooking({
        chefId, eventType, eventDate, eventTime,
        durationHours: Number(durationHours), guestCount: Number(guestCount),
        venueAddress: address, city, pincode, customerName, customerPhone,
        menuDescription: [menuDescription, dishNote].filter(Boolean).join('\n'),
        specialRequests, cuisinePreferences: cuisinePref,
        servicesJson: JSON.stringify(Object.keys(selectedAddons).filter((k) => selectedAddons[k])),
        selectedDishesJson: JSON.stringify(selectedDishList),
      }, token);
      setDone({ ...event, isEvent: true });
    } catch (e: any) {
      Alert.alert('Request failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  async function addCurrentToCart() {
    if (!customerName || !customerPhone || !address) { Alert.alert('Missing details', 'Fill name, phone and address first.'); return; }
    const chefName = chef?.name;
    if (type === 'DAILY') {
      if (!serviceDate) { Alert.alert('Pick a date', 'Choose a service date.'); return; }
      await addToCart({
        serviceType: 'DAILY', chefId, chefName,
        summary: `${pretty(mealType)} · ${serviceDate} · ${guestsCount} pax`,
        estTotalPaise: (chef?.dailyRatePaise ?? 0) * Number(numberOfMeals || 1),
        payload: { chefId, serviceType: 'DAILY', mealType, serviceDate, serviceTime, guestsCount: Number(guestsCount), numberOfMeals: Number(numberOfMeals), specialRequests, address, city, locality, pincode, customerName, customerPhone },
      });
    } else if (type === 'MONTHLY') {
      if (!startDate) { Alert.alert('Pick a date', 'Choose a start date.'); return; }
      await addToCart({
        serviceType: 'MONTHLY', chefId, chefName,
        summary: `${plan} · ${mealsPerDay} meal(s)/day · from ${startDate}`,
        estTotalPaise: chef?.monthlyRatePaise ?? 0,
        payload: { chefId, serviceType: 'MONTHLY', plan, mealsPerDay: Number(mealsPerDay), schedule, startDate, dietaryPreferences, mealType, address, city, locality, pincode, customerName, customerPhone, specialRequests },
      });
    } else {
      if (!eventDate) { Alert.alert('Pick a date', 'Choose an event date.'); return; }
      await addToCart({
        serviceType: 'EVENT', chefId, chefName,
        summary: `${pretty(eventType)} · ${eventDate} · ${guestCount} guests`,
        estTotalPaise: eventEstimatePaise,
        payload: { chefId, eventType, eventDate, eventTime, durationHours: Number(durationHours), guestCount: Number(guestCount), venueAddress: address, city, pincode, customerName, customerPhone, menuDescription, specialRequests, cuisinePreferences: cuisinePref, servicesJson: JSON.stringify(Object.keys(selectedAddons).filter((k) => selectedAddons[k])), selectedDishesJson: JSON.stringify(selectedDishList) },
      });
    }
    Alert.alert('Added to cart', 'Continue browsing or go to your cart.', [
      { text: 'Keep browsing', onPress: () => router.replace('/cooks') },
      { text: 'Go to cart', onPress: () => router.replace('/cook-cart') },
    ]);
  }

  async function onPaymentSuccess(res: { paymentId: string; orderId: string }) {
    const token = await getAccessToken();
    const booking = payOrder.booking;
    try {
      if (token) await api.confirmChefBookingPayment(booking.id, res.orderId, res.paymentId, token);
      setPayOrder(null);
      setDone(booking);
    } catch (e: any) {
      Alert.alert('Payment confirmation failed', e.message ?? 'Contact support with your payment id.');
      setPayOrder(null);
    }
  }

  // ── Payment WebView ──
  if (payOrder) {
    return (
      <Modal visible animationType="slide">
        <CookPaymentWebView
          order={payOrder.order}
          prefill={{ name: customerName, email: '', phone: customerPhone }}
          onSuccess={onPaymentSuccess}
          onFailure={(err) => { Alert.alert('Payment failed', err); setPayOrder(null); }}
          onDismiss={() => setPayOrder(null)}
        />
      </Modal>
    );
  }

  // ── Success ──
  if (done) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title: 'Booked' }} />
        <Text style={styles.successIcon}>✅</Text>
        <Text style={styles.successTitle}>{done.isEvent ? 'Event request sent!' : done.isSubscription ? 'Subscription created!' : 'Booking confirmed!'}</Text>
        {done.bookingRef ? <Text style={styles.successRef}>Ref: {done.bookingRef}</Text> : null}
        <Text style={styles.successMsg}>
          {done.isEvent
            ? 'The cook/caterer will review your event and send a quote. Track it in My Bookings.'
            : done.isSubscription
            ? 'Your monthly cook subscription is set up. Manage it under the Subscriptions tab.'
            : 'Your cook is booked. The balance is payable after the service.'}
        </Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.replace('/cook-bookings')}>
          <Text style={styles.primaryBtnText}>View my bookings</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => router.replace('/cooks')}><Text style={styles.linkText}>Back to cooks</Text></TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: type === 'EVENT' ? 'Event request' : 'Book a cook' }} />
      {chef ? (
        <View style={styles.chefBar}>
          <Text style={styles.chefBarIcon}>👨‍🍳</Text>
          <View style={{ flex: 1 }}>
            <Text style={styles.chefBarName}>{chef.name}</Text>
            <Text style={styles.chefBarSub}>{chef.city}{chef.cuisines ? ` · ${pretty((chef.cuisines as string).split(',')[0])}` : ''}</Text>
          </View>
        </View>
      ) : null}

      {/* Service type switch */}
      <View style={styles.typeRow}>
        {(['DAILY', 'MONTHLY', 'EVENT'] as ServiceType[]).map((t) => (
          <TouchableOpacity key={t} style={[styles.typeTab, type === t && styles.typeTabActive]} onPress={() => setType(t)}>
            <Text style={[styles.typeTabText, type === t && styles.typeTabTextActive]}>
              {t === 'DAILY' ? 'One day' : t === 'MONTHLY' ? 'Monthly' : 'Event'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Field label="Your name *"><TextInput style={styles.input} value={customerName} onChangeText={setCustomerName} placeholder="Full name" placeholderTextColor="#9ca3af" /></Field>
      <Field label="Phone *"><TextInput style={styles.input} value={customerPhone} onChangeText={setCustomerPhone} placeholder="10-digit mobile" placeholderTextColor="#9ca3af" keyboardType="phone-pad" maxLength={10} /></Field>

      {type === 'DAILY' && (
        <>
          <Text style={styles.label}>Meal</Text>
          <View style={styles.pillRow}>
            {MEAL_TYPES.map((m) => (
              <TouchableOpacity key={m} style={[styles.pill, mealType === m && styles.pillActive]} onPress={() => setMealType(m)}>
                <Text style={[styles.pillText, mealType === m && styles.pillTextActive]}>{pretty(m)}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <View style={styles.row}>
            <Field label="Service date *" flex><TextInput style={styles.input} value={serviceDate} onChangeText={setServiceDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" /></Field>
            <Field label="Time" flex><TextInput style={styles.input} value={serviceTime} onChangeText={setServiceTime} placeholder="HH:MM" placeholderTextColor="#9ca3af" /></Field>
          </View>
          <View style={styles.row}>
            <Field label="Household size" flex><TextInput style={styles.input} value={guestsCount} onChangeText={setGuestsCount} keyboardType="numeric" /></Field>
            <Field label="No. of meals" flex><TextInput style={styles.input} value={numberOfMeals} onChangeText={setNumberOfMeals} keyboardType="numeric" /></Field>
          </View>
        </>
      )}

      {type === 'MONTHLY' && (
        <>
          <Text style={styles.label}>Meal</Text>
          <View style={styles.pillRow}>
            {MEAL_TYPES.map((m) => (
              <TouchableOpacity key={m} style={[styles.pill, mealType === m && styles.pillActive]} onPress={() => setMealType(m)}>
                <Text style={[styles.pillText, mealType === m && styles.pillTextActive]}>{pretty(m)}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <View style={styles.row}>
            <Field label="Plan" flex><TextInput style={styles.input} value={plan} onChangeText={setPlan} placeholder="Standard / Premium" placeholderTextColor="#9ca3af" /></Field>
            <Field label="Meals / day" flex><TextInput style={styles.input} value={mealsPerDay} onChangeText={setMealsPerDay} keyboardType="numeric" /></Field>
          </View>
          <View style={styles.row}>
            <Field label="Start date *" flex><TextInput style={styles.input} value={startDate} onChangeText={setStartDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" /></Field>
            <Field label="Schedule" flex><TextInput style={styles.input} value={schedule} onChangeText={setSchedule} placeholder="Mon-Sat" placeholderTextColor="#9ca3af" /></Field>
          </View>
          <Field label="Dietary preferences"><TextInput style={styles.input} value={dietaryPreferences} onChangeText={setDietaryPreferences} placeholder="e.g. Veg, no onion/garlic" placeholderTextColor="#9ca3af" /></Field>
        </>
      )}

      {type === 'EVENT' && (
        <>
          <Field label="Occasion"><TextInput style={styles.input} value={eventType} onChangeText={setEventType} placeholder="BIRTHDAY / WEDDING / …" placeholderTextColor="#9ca3af" autoCapitalize="characters" /></Field>
          <View style={styles.row}>
            <Field label="Event date *" flex><TextInput style={styles.input} value={eventDate} onChangeText={setEventDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" /></Field>
            <Field label="Time" flex><TextInput style={styles.input} value={eventTime} onChangeText={setEventTime} placeholder="HH:MM" placeholderTextColor="#9ca3af" /></Field>
          </View>
          <View style={styles.row}>
            <Field label="Guests" flex><TextInput style={styles.input} value={guestCount} onChangeText={setGuestCount} keyboardType="numeric" /></Field>
            <Field label="Duration (hrs)" flex><TextInput style={styles.input} value={durationHours} onChangeText={setDurationHours} keyboardType="numeric" /></Field>
          </View>
          <Text style={styles.label}>Cuisine style</Text>
          <View style={styles.pillRow}>
            {EVENT_CUISINES.map((c) => (
              <TouchableOpacity key={c} style={[styles.pill, cuisinePref === c && styles.pillActive]} onPress={() => setCuisinePref(cuisinePref === c ? '' : c)}>
                <Text style={[styles.pillText, cuisinePref === c && styles.pillTextActive]}>{c}</Text>
              </TouchableOpacity>
            ))}
          </View>

          {addons.length > 0 && (
            <>
              <Text style={styles.label}>Add-ons & live counters</Text>
              <View style={styles.pillRow}>
                {addons.map((a) => {
                  const aid = a.id ?? a.code;
                  const on = !!selectedAddons[aid];
                  const price = a.pricePaise ?? a.paise ?? 0;
                  return (
                    <TouchableOpacity key={aid} style={[styles.pill, on && styles.pillActive]} onPress={() => setSelectedAddons((s) => ({ ...s, [aid]: !on }))}>
                      <Text style={[styles.pillText, on && styles.pillTextActive]}>{a.label ?? a.name}{price ? ` +${formatPaise(price)}` : ''}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </>
          )}

          {dishCatalog.length > 0 && (
            <>
              <Text style={styles.label}>Choose dishes</Text>
              <TouchableOpacity style={styles.dishPickBtn} onPress={() => setDishModal(true)}>
                <Text style={styles.dishPickText}>
                  {selectedDishList.length ? `${selectedDishList.length} dish${selectedDishList.length > 1 ? 'es' : ''} selected` : 'Browse dish catalog'}
                </Text>
                <Text style={styles.dishPickChevron}>›</Text>
              </TouchableOpacity>
              {selectedDishList.length > 0 && (
                <View style={styles.selDishWrap}>
                  {selectedDishList.map((d: any) => (
                    <TouchableOpacity key={d.id} style={styles.selDishChip} onPress={() => toggleDish(d)}>
                      <Text style={styles.selDishText}>{d.name}  ✕</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              )}
            </>
          )}

          <Field label="Other menu notes"><TextInput style={[styles.input, styles.textarea]} value={menuDescription} onChangeText={setMenuDescription} placeholder="e.g. 5 starters, 3 mains, no onion/garlic…" placeholderTextColor="#9ca3af" multiline /></Field>

          {eventEstimatePaise > 0 && (
            <View style={styles.priceBox}>
              <Text style={styles.priceRow}>Estimated total <Text style={styles.priceVal}>{formatPaise(eventEstimatePaise)}</Text></Text>
              <Text style={styles.priceHint}>Indicative; the cook/caterer confirms a final quote.</Text>
            </View>
          )}
        </>
      )}

      <Field label={type === 'EVENT' ? 'Venue address *' : 'Delivery address *'}><TextInput style={[styles.input, styles.textarea]} value={address} onChangeText={setAddress} placeholder="House / flat, street, landmark" placeholderTextColor="#9ca3af" multiline /></Field>
      <View style={styles.row}>
        <Field label="City" flex><TextInput style={styles.input} value={city} onChangeText={setCity} placeholder="City" placeholderTextColor="#9ca3af" /></Field>
        <Field label="Pincode" flex><TextInput style={styles.input} value={pincode} onChangeText={setPincode} placeholder="6-digit" placeholderTextColor="#9ca3af" keyboardType="numeric" maxLength={6} /></Field>
      </View>
      {type === 'DAILY' ? <Field label="Locality"><TextInput style={styles.input} value={locality} onChangeText={setLocality} placeholder="e.g. Gachibowli" placeholderTextColor="#9ca3af" /></Field> : null}
      <Field label="Special requests"><TextInput style={[styles.input, styles.textarea]} value={specialRequests} onChangeText={setSpecialRequests} placeholder="Allergies, preferences…" placeholderTextColor="#9ca3af" multiline /></Field>

      {type === 'DAILY' && chef?.dailyRatePaise ? (
        <View style={styles.priceBox}>
          <Text style={styles.priceRow}>Estimated total <Text style={styles.priceVal}>{formatPaise(chef.dailyRatePaise * Number(numberOfMeals || 1))}</Text></Text>
          <Text style={styles.priceHint}>Pay 10% advance now, balance after service.</Text>
        </View>
      ) : null}

      <TouchableOpacity
        style={[styles.primaryBtn, submitting && styles.btnDisabled]}
        disabled={submitting}
        onPress={type === 'EVENT' ? submitEvent : type === 'MONTHLY' ? submitMonthly : submitDaily}
      >
        {submitting ? <ActivityIndicator color="#fff" /> : (
          <Text style={styles.primaryBtnText}>
            {type === 'EVENT' ? 'Send event request' : type === 'MONTHLY' ? 'Start subscription' : 'Continue to payment'}
          </Text>
        )}
      </TouchableOpacity>

      <TouchableOpacity style={styles.cartBtn} disabled={submitting} onPress={addCurrentToCart}>
        <Text style={styles.cartBtnText}>🛒  Add to cart</Text>
      </TouchableOpacity>

      {/* Dish catalog picker */}
      <Modal visible={dishModal} animationType="slide" onRequestClose={() => setDishModal(false)}>
        <View style={styles.dishModal}>
          <View style={styles.dishModalHeader}>
            <Text style={styles.dishModalTitle}>Choose dishes</Text>
            <TouchableOpacity onPress={() => setDishModal(false)}><Text style={styles.dishModalClose}>Done</Text></TouchableOpacity>
          </View>
          <View style={styles.filterRow}>
            {([['all', 'All'], ['veg', 'Veg'], ['fried', 'Fried'], ['recommended', 'Recommended'], ['nog', 'No onion/garlic']] as const).map(([k, lbl]) => (
              <TouchableOpacity key={k} style={[styles.filterChip, dishFilter === k && styles.filterChipActive]} onPress={() => setDishFilter(k)}>
                <Text style={[styles.filterChipText, dishFilter === k && styles.filterChipTextActive]}>{lbl}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
            {filteredDishes.length === 0 ? (
              <Text style={styles.dishEmpty}>No dishes match this filter.</Text>
            ) : filteredDishes.map((d) => {
              const on = !!selectedDishes[d.id];
              return (
                <TouchableOpacity key={d.id} style={[styles.dishRow, on && styles.dishRowOn]} onPress={() => toggleDish(d)}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.dishName}>{d.isVeg ? '🟢 ' : '🔴 '}{d.name}{d.isRecommended ? '  ⭐' : ''}</Text>
                    <View style={styles.dishTagRow}>
                      {d.category ? <Text style={styles.dishTag}>{pretty(String(d.category))}</Text> : null}
                      {d.isFried ? <Text style={styles.dishTag}>Fried</Text> : null}
                      {d.noOnionGarlic ? <Text style={styles.dishTag}>No onion/garlic</Text> : null}
                    </View>
                  </View>
                  {d.pricePaise ? <Text style={styles.dishPrice}>{formatPaise(d.pricePaise)}</Text> : null}
                  <Text style={[styles.dishCheck, on && styles.dishCheckOn]}>{on ? '✓' : '+'}</Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>
        </View>
      </Modal>
    </ScrollView>
  );
}

function Field({ label, children, flex }: { label: string; children: React.ReactNode; flex?: boolean }) {
  return (
    <View style={[styles.field, flex && { flex: 1 }]}>
      <Text style={styles.label}>{label}</Text>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  center:        { flex: 1, backgroundColor: '#f9fafb', alignItems: 'center', justifyContent: 'center', padding: 24 },

  typeRow:       { flexDirection: 'row', gap: 8, marginBottom: 16, backgroundColor: '#f3f4f6', borderRadius: 100, padding: 4 },
  typeTab:       { flex: 1, paddingVertical: 9, borderRadius: 100, alignItems: 'center' },
  typeTabActive: { backgroundColor: '#fff', shadowColor: '#000', shadowOpacity: 0.05, shadowRadius: 2, shadowOffset: { width: 0, height: 1 }, elevation: 1 },
  typeTabText:   { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  typeTabTextActive: { color: '#f97316' },

  chefBar:       { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: '#fff7ed', borderRadius: 12, padding: 12, marginBottom: 16, borderWidth: 1, borderColor: '#fed7aa' },
  chefBarIcon:   { fontSize: 28 },
  chefBarName:   { fontSize: 15, fontWeight: '700', color: '#111827' },
  chefBarSub:    { fontSize: 12, color: '#6b7280', marginTop: 2 },

  field:         { marginBottom: 14 },
  label:         { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  input:         { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:      { height: 84, textAlignVertical: 'top' },
  row:           { flexDirection: 'row', gap: 12 },

  pillRow:       { flexDirection: 'row', gap: 8, marginBottom: 14, flexWrap: 'wrap' },
  pill:          { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:    { backgroundColor: '#f97316', borderColor: '#f97316' },
  pillText:      { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },

  priceBox:      { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  priceRow:      { fontSize: 14, color: '#374151', fontWeight: '600' },
  priceVal:      { color: '#f97316', fontWeight: '800' },
  priceHint:     { fontSize: 12, color: '#6b7280', marginTop: 6 },

  primaryBtn:    { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 4 },
  btnDisabled:   { opacity: 0.6 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  cartBtn:       { borderRadius: 12, paddingVertical: 13, alignItems: 'center', marginTop: 10, borderWidth: 1, borderColor: '#f97316' },
  cartBtnText:   { color: '#f97316', fontSize: 15, fontWeight: '700' },
  linkText:      { color: '#f97316', fontSize: 14, fontWeight: '600', marginTop: 16 },

  dishPickBtn:   { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', borderWidth: 1, borderColor: '#f97316', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12, marginBottom: 10 },
  dishPickText:  { fontSize: 14, fontWeight: '600', color: '#f97316' },
  dishPickChevron: { fontSize: 20, color: '#f97316', fontWeight: '700' },
  selDishWrap:   { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 14 },
  selDishChip:   { backgroundColor: '#fff7ed', borderWidth: 1, borderColor: '#fed7aa', borderRadius: 100, paddingHorizontal: 12, paddingVertical: 6 },
  selDishText:   { fontSize: 12, fontWeight: '600', color: '#9a3412' },
  dishModal:     { flex: 1, backgroundColor: '#f9fafb' },
  dishModalHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  dishModalTitle:{ fontSize: 18, fontWeight: '800', color: '#111827' },
  dishModalClose:{ fontSize: 15, fontWeight: '700', color: '#f97316' },
  filterRow:     { flexDirection: 'row', flexWrap: 'wrap', gap: 8, padding: 16, paddingBottom: 4 },
  filterChip:    { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  filterChipActive: { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterChipText: { fontSize: 12, fontWeight: '600', color: '#374151' },
  filterChipTextActive: { color: '#fff' },
  dishEmpty:     { fontSize: 13, color: '#9ca3af', textAlign: 'center', marginTop: 24 },
  dishRow:       { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  dishRowOn:     { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  dishName:      { fontSize: 14, fontWeight: '600', color: '#111827' },
  dishTagRow:    { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 4 },
  dishTag:       { fontSize: 10, color: '#6b7280', backgroundColor: '#f3f4f6', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 100, overflow: 'hidden' },
  dishPrice:     { fontSize: 13, fontWeight: '700', color: '#f97316' },
  dishCheck:     { fontSize: 18, fontWeight: '800', color: '#9ca3af', width: 24, textAlign: 'center' },
  dishCheckOn:   { color: '#f97316' },
  successIcon:   { fontSize: 56 },
  successTitle:  { fontSize: 22, fontWeight: '800', color: '#111827', marginTop: 12 },
  successRef:    { fontSize: 14, color: '#6b7280', marginTop: 4, fontWeight: '600' },
  successMsg:    { fontSize: 14, color: '#6b7280', textAlign: 'center', marginTop: 12, lineHeight: 20 },
});
