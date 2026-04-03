import { useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert, Image,
  KeyboardAvoidingView, Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import Constants from 'expo-constants';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const API_URL = Constants.expoConfig?.extra?.apiUrl ?? 'http://localhost:8080';
const ORANGE = '#f97316';
const STEPS = ['Type', 'Location', 'Details', 'Pricing', 'Features', 'Photos', 'Submit'];

const PROPERTY_TYPES = [
  { key: 'APARTMENT', label: 'Apartment', icon: '🏢' },
  { key: 'VILLA', label: 'Villa', icon: '🏡' },
  { key: 'INDEPENDENT_HOUSE', label: 'Independent House', icon: '🏠' },
  { key: 'PLOT', label: 'Plot / Land', icon: '🏗️' },
  { key: 'PENTHOUSE', label: 'Penthouse', icon: '🌇' },
  { key: 'STUDIO', label: 'Studio', icon: '🛏️' },
  { key: 'COMMERCIAL', label: 'Commercial', icon: '🏬' },
];

const FURNISHING_OPTIONS = ['Unfurnished', 'Semi-Furnished', 'Fully Furnished'];
const FACING_OPTIONS = ['North', 'South', 'East', 'West', 'North-East', 'North-West', 'South-East', 'South-West'];
const PARKING_OPTIONS = ['None', 'Covered', 'Open', 'Both'];

const AMENITIES = [
  'Lift', 'Swimming Pool', 'Gym', 'Club House', 'Garden', 'Power Backup',
  'Security', 'CCTV', 'Children Play Area', 'Intercom', 'Rain Water Harvesting',
  'Piped Gas', 'Jogging Track', 'Indoor Games', 'Visitor Parking', 'Fire Safety',
];

interface PhotoItem {
  uri: string;
  category: string;
}

export default function SellPropertyScreen() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  // Step 0: Type
  const [propertyType, setPropertyType] = useState('');

  // Step 1: Location
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [locality, setLocality] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [pincode, setPincode] = useState('');

  // Step 2: Details
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [bhk, setBhk] = useState('');
  const [bathrooms, setBathrooms] = useState('');
  const [areaSqft, setAreaSqft] = useState('');
  const [floor, setFloor] = useState('');
  const [totalFloors, setTotalFloors] = useState('');
  const [propertyAge, setPropertyAge] = useState('');

  // Step 3: Pricing
  const [price, setPrice] = useState('');
  const [priceNegotiable, setPriceNegotiable] = useState(false);

  // Step 4: Features
  const [furnishing, setFurnishing] = useState('');
  const [facing, setFacing] = useState('');
  const [parking, setParking] = useState('');
  const [reraApproved, setReraApproved] = useState(false);
  const [reraNumber, setReraNumber] = useState('');
  const [vastuCompliant, setVastuCompliant] = useState(false);
  const [readyToMove, setReadyToMove] = useState(true);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);

  // Step 5: Photos
  const [photos, setPhotos] = useState<PhotoItem[]>([]);

  const validateStep = useCallback((): boolean => {
    if (step === 0) {
      if (!propertyType) { Alert.alert('Required', 'Please select a property type.'); return false; }
    } else if (step === 1) {
      if (!addressLine1.trim()) { Alert.alert('Required', 'Please enter address line 1.'); return false; }
      if (!city.trim()) { Alert.alert('Required', 'Please enter city.'); return false; }
      if (!state.trim()) { Alert.alert('Required', 'Please enter state.'); return false; }
      if (!pincode.trim() || pincode.length !== 6) { Alert.alert('Required', 'Enter a valid 6-digit pincode.'); return false; }
    } else if (step === 2) {
      if (!title.trim()) { Alert.alert('Required', 'Please enter a title.'); return false; }
      if (!description.trim()) { Alert.alert('Required', 'Please enter a description.'); return false; }
    } else if (step === 3) {
      if (!price.trim() || isNaN(Number(price)) || Number(price) <= 0) {
        Alert.alert('Required', 'Please enter a valid price in rupees.');
        return false;
      }
    }
    return true;
  }, [step, propertyType, addressLine1, city, state, pincode, title, description, price]);

  const goNext = () => {
    if (!validateStep()) return;
    if (step < STEPS.length - 1) setStep(step + 1);
  };
  const goBack = () => {
    if (step > 0) setStep(step - 1);
    else router.back();
  };

  const pickPhotos = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission needed', 'Please grant photo library access.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      const newPhotos: PhotoItem[] = result.assets.map((a) => ({ uri: a.uri, category: 'EXTERIOR' }));
      setPhotos((prev) => [...prev, ...newPhotos]);
    }
  };

  const takePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission needed', 'Please grant camera access.');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      const newPhotos: PhotoItem[] = result.assets.map((a) => ({ uri: a.uri, category: 'EXTERIOR' }));
      setPhotos((prev) => [...prev, ...newPhotos]);
    }
  };

  const showPhotoOptions = () => {
    Alert.alert('Add Photo', 'Choose an option', [
      { text: 'Take Photo', onPress: takePhoto },
      { text: 'Choose from Gallery', onPress: pickPhotos },
      { text: 'Cancel', style: 'cancel' },
    ]);
  };

  const removePhoto = (index: number) => {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  };

  const toggleAmenity = (a: string) => {
    setSelectedAmenities((prev) =>
      prev.includes(a) ? prev.filter((x) => x !== a) : [...prev, a]
    );
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const token = await getAccessToken();
      if (!token) {
        Alert.alert('Auth Error', 'Please log in again.');
        setSubmitting(false);
        return;
      }

      const pricePaise = Math.round(Number(price) * 100);
      const data: Record<string, any> = {
        propertyType,
        title: title.trim(),
        description: description.trim(),
        addressLine1: addressLine1.trim(),
        addressLine2: addressLine2.trim() || undefined,
        locality: locality.trim() || undefined,
        city: city.trim(),
        state: state.trim(),
        pincode: pincode.trim(),
        pricePaise: pricePaise,
        priceNegotiable,
        bhk: bhk ? Number(bhk) : undefined,
        bathrooms: bathrooms ? Number(bathrooms) : undefined,
        areaSqft: areaSqft ? Number(areaSqft) : undefined,
        floor: floor ? Number(floor) : undefined,
        totalFloors: totalFloors ? Number(totalFloors) : undefined,
        propertyAge: propertyAge ? Number(propertyAge) : undefined,
        furnishing: furnishing || undefined,
        facing: facing || undefined,
        parking: parking || undefined,
        reraApproved,
        reraNumber: reraApproved && reraNumber.trim() ? reraNumber.trim() : undefined,
        vastuCompliant,
        readyToMove,
        amenities: selectedAmenities.length > 0 ? selectedAmenities : undefined,
      };

      const created: any = await api.createSaleProperty(data, token);
      const propertyId = created.id;

      // Upload photos
      for (const photo of photos) {
        try {
          const filename = photo.uri.split('/').pop() ?? 'photo.jpg';
          const match = /\.(\w+)$/.exec(filename);
          const type = match ? `image/${match[1]}` : 'image/jpeg';
          const formData = new FormData();
          formData.append('file', { uri: photo.uri, name: filename, type } as any);
          formData.append('salePropertyId', propertyId);
          formData.append('category', photo.category);
          await fetch(`${API_URL}/api/v1/sale-properties/${propertyId}/photos`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'multipart/form-data' },
            body: formData,
          });
        } catch {
          // continue uploading remaining photos
        }
      }

      setSuccess(true);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to create listing');
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.successContainer}>
          <Text style={{ fontSize: 60, marginBottom: 16 }}>🎉</Text>
          <Text style={styles.successTitle}>Property Listed!</Text>
          <Text style={styles.successSub}>
            Your property has been submitted. It will be visible to buyers after review.
          </Text>
          <TouchableOpacity style={styles.successBtn} onPress={() => router.push('/seller-dashboard')}>
            <Text style={styles.successBtnText}>Go to Dashboard</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.successBtnSecondary} onPress={() => router.push('/buy')}>
            <Text style={styles.successBtnSecondaryText}>Browse Properties</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.headerBack}>
            <Text style={{ fontSize: 18, color: '#374151', fontWeight: '700' }}>‹</Text>
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Sell Property</Text>
          <Text style={styles.headerStep}>{step + 1}/{STEPS.length}</Text>
        </View>

        {/* Progress */}
        <View style={styles.progressBar}>
          <View style={[styles.progressFill, { width: `${((step + 1) / STEPS.length) * 100}%` }]} />
        </View>

        <ScrollView style={{ flex: 1 }} contentContainerStyle={styles.formContent} keyboardShouldPersistTaps="handled">
          <Text style={styles.stepLabel}>{STEPS[step]}</Text>

          {/* Step 0: Type */}
          {step === 0 && (
            <View style={styles.typeGrid}>
              {PROPERTY_TYPES.map((t) => (
                <TouchableOpacity
                  key={t.key}
                  style={[styles.typeCard, propertyType === t.key && styles.typeCardActive]}
                  onPress={() => setPropertyType(t.key)}
                >
                  <Text style={styles.typeIcon}>{t.icon}</Text>
                  <Text style={[styles.typeLabel, propertyType === t.key && styles.typeLabelActive]}>{t.label}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          {/* Step 1: Location */}
          {step === 1 && (
            <>
              <Text style={styles.inputLabel}>Address Line 1 *</Text>
              <TextInput style={styles.input} value={addressLine1} onChangeText={setAddressLine1} placeholder="Building, Street" placeholderTextColor="#9ca3af" />
              <Text style={styles.inputLabel}>Address Line 2</Text>
              <TextInput style={styles.input} value={addressLine2} onChangeText={setAddressLine2} placeholder="Area, Landmark" placeholderTextColor="#9ca3af" />
              <Text style={styles.inputLabel}>Locality</Text>
              <TextInput style={styles.input} value={locality} onChangeText={setLocality} placeholder="e.g. Bandra West" placeholderTextColor="#9ca3af" />
              <View style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>City *</Text>
                  <TextInput style={styles.input} value={city} onChangeText={setCity} placeholder="City" placeholderTextColor="#9ca3af" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>State *</Text>
                  <TextInput style={styles.input} value={state} onChangeText={setState} placeholder="State" placeholderTextColor="#9ca3af" />
                </View>
              </View>
              <Text style={styles.inputLabel}>Pincode *</Text>
              <TextInput style={styles.input} value={pincode} onChangeText={setPincode} placeholder="6-digit pincode" placeholderTextColor="#9ca3af" keyboardType="numeric" maxLength={6} />
            </>
          )}

          {/* Step 2: Details */}
          {step === 2 && (
            <>
              <Text style={styles.inputLabel}>Title *</Text>
              <TextInput style={styles.input} value={title} onChangeText={setTitle} placeholder="e.g. 3 BHK Apartment in Bandra" placeholderTextColor="#9ca3af" />
              <Text style={styles.inputLabel}>Description *</Text>
              <TextInput style={[styles.input, { height: 100, textAlignVertical: 'top' }]} value={description} onChangeText={setDescription} placeholder="Describe your property..." placeholderTextColor="#9ca3af" multiline />
              <View style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>BHK</Text>
                  <TextInput style={styles.input} value={bhk} onChangeText={setBhk} placeholder="e.g. 3" placeholderTextColor="#9ca3af" keyboardType="numeric" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>Bathrooms</Text>
                  <TextInput style={styles.input} value={bathrooms} onChangeText={setBathrooms} placeholder="e.g. 2" placeholderTextColor="#9ca3af" keyboardType="numeric" />
                </View>
              </View>
              <Text style={styles.inputLabel}>Area (sq.ft)</Text>
              <TextInput style={styles.input} value={areaSqft} onChangeText={setAreaSqft} placeholder="e.g. 1200" placeholderTextColor="#9ca3af" keyboardType="numeric" />
              <View style={styles.row}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>Floor</Text>
                  <TextInput style={styles.input} value={floor} onChangeText={setFloor} placeholder="e.g. 5" placeholderTextColor="#9ca3af" keyboardType="numeric" />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.inputLabel}>Total Floors</Text>
                  <TextInput style={styles.input} value={totalFloors} onChangeText={setTotalFloors} placeholder="e.g. 12" placeholderTextColor="#9ca3af" keyboardType="numeric" />
                </View>
              </View>
              <Text style={styles.inputLabel}>Property Age (years)</Text>
              <TextInput style={styles.input} value={propertyAge} onChangeText={setPropertyAge} placeholder="e.g. 5" placeholderTextColor="#9ca3af" keyboardType="numeric" />
            </>
          )}

          {/* Step 3: Pricing */}
          {step === 3 && (
            <>
              <Text style={styles.inputLabel}>Expected Price (INR) *</Text>
              <TextInput style={styles.input} value={price} onChangeText={setPrice} placeholder="e.g. 7500000" placeholderTextColor="#9ca3af" keyboardType="numeric" />
              {price && !isNaN(Number(price)) && Number(price) > 0 && (
                <Text style={styles.pricePreview}>
                  {Number(price) >= 10000000
                    ? `₹${(Number(price) / 10000000).toFixed(2)} Cr`
                    : Number(price) >= 100000
                      ? `₹${(Number(price) / 100000).toFixed(2)} Lakh`
                      : `₹${Number(price).toLocaleString('en-IN')}`}
                </Text>
              )}
              <TouchableOpacity
                style={[styles.toggleRow, priceNegotiable && styles.toggleRowActive]}
                onPress={() => setPriceNegotiable(!priceNegotiable)}
              >
                <View style={[styles.toggleDot, priceNegotiable && styles.toggleDotActive]} />
                <Text style={styles.toggleText}>Price is negotiable</Text>
              </TouchableOpacity>
            </>
          )}

          {/* Step 4: Features */}
          {step === 4 && (
            <>
              <Text style={styles.inputLabel}>Furnishing</Text>
              <View style={styles.chipRow}>
                {FURNISHING_OPTIONS.map((f) => (
                  <TouchableOpacity
                    key={f}
                    style={[styles.chip, furnishing === f && styles.chipActive]}
                    onPress={() => setFurnishing(furnishing === f ? '' : f)}
                  >
                    <Text style={[styles.chipText, furnishing === f && styles.chipTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={styles.inputLabel}>Facing</Text>
              <View style={styles.chipRow}>
                {FACING_OPTIONS.map((f) => (
                  <TouchableOpacity
                    key={f}
                    style={[styles.chip, facing === f && styles.chipActive]}
                    onPress={() => setFacing(facing === f ? '' : f)}
                  >
                    <Text style={[styles.chipText, facing === f && styles.chipTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={styles.inputLabel}>Parking</Text>
              <View style={styles.chipRow}>
                {PARKING_OPTIONS.map((p) => (
                  <TouchableOpacity
                    key={p}
                    style={[styles.chip, parking === p && styles.chipActive]}
                    onPress={() => setParking(parking === p ? '' : p)}
                  >
                    <Text style={[styles.chipText, parking === p && styles.chipTextActive]}>{p}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <View style={styles.toggleGroup}>
                <TouchableOpacity
                  style={[styles.toggleRow, reraApproved && styles.toggleRowActive]}
                  onPress={() => setReraApproved(!reraApproved)}
                >
                  <View style={[styles.toggleDot, reraApproved && styles.toggleDotActive]} />
                  <Text style={styles.toggleText}>RERA Approved</Text>
                </TouchableOpacity>
                {reraApproved && (
                  <TextInput
                    style={styles.input}
                    value={reraNumber}
                    onChangeText={setReraNumber}
                    placeholder="RERA Registration Number"
                    placeholderTextColor="#9ca3af"
                  />
                )}
                <TouchableOpacity
                  style={[styles.toggleRow, vastuCompliant && styles.toggleRowActive]}
                  onPress={() => setVastuCompliant(!vastuCompliant)}
                >
                  <View style={[styles.toggleDot, vastuCompliant && styles.toggleDotActive]} />
                  <Text style={styles.toggleText}>Vastu Compliant</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.toggleRow, readyToMove && styles.toggleRowActive]}
                  onPress={() => setReadyToMove(!readyToMove)}
                >
                  <View style={[styles.toggleDot, readyToMove && styles.toggleDotActive]} />
                  <Text style={styles.toggleText}>Ready to Move</Text>
                </TouchableOpacity>
              </View>

              <Text style={[styles.inputLabel, { marginTop: 16 }]}>Amenities</Text>
              <View style={styles.chipRow}>
                {AMENITIES.map((a) => (
                  <TouchableOpacity
                    key={a}
                    style={[styles.chip, selectedAmenities.includes(a) && styles.chipActive]}
                    onPress={() => toggleAmenity(a)}
                  >
                    <Text style={[styles.chipText, selectedAmenities.includes(a) && styles.chipTextActive]}>{a}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </>
          )}

          {/* Step 5: Photos */}
          {step === 5 && (
            <>
              <Text style={styles.inputLabel}>Add photos of your property</Text>
              <TouchableOpacity style={styles.addPhotoBtn} onPress={showPhotoOptions}>
                <Text style={styles.addPhotoIcon}>+</Text>
                <Text style={styles.addPhotoText}>Add Photos</Text>
              </TouchableOpacity>
              <View style={styles.photoGrid}>
                {photos.map((photo, i) => (
                  <View key={i} style={styles.photoThumb}>
                    <Image source={{ uri: photo.uri }} style={styles.photoImage} resizeMode="cover" />
                    <TouchableOpacity style={styles.photoRemove} onPress={() => removePhoto(i)}>
                      <Text style={styles.photoRemoveText}>X</Text>
                    </TouchableOpacity>
                  </View>
                ))}
              </View>
              {photos.length === 0 && (
                <Text style={styles.photoHint}>Photos help your property stand out. Add at least 3-5 photos.</Text>
              )}
            </>
          )}

          {/* Step 6: Review & Submit */}
          {step === 6 && (
            <View style={styles.reviewSection}>
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>Type</Text>
                <Text style={styles.reviewValue}>{propertyType}</Text>
              </View>
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>Title</Text>
                <Text style={styles.reviewValue}>{title}</Text>
              </View>
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>Location</Text>
                <Text style={styles.reviewValue}>{[locality, city, state].filter(Boolean).join(', ')}</Text>
              </View>
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>Price</Text>
                <Text style={styles.reviewValue}>
                  {price && Number(price) >= 10000000
                    ? `₹${(Number(price) / 10000000).toFixed(2)} Cr`
                    : price && Number(price) >= 100000
                      ? `₹${(Number(price) / 100000).toFixed(2)} Lakh`
                      : `₹${Number(price || 0).toLocaleString('en-IN')}`}
                  {priceNegotiable ? ' (Negotiable)' : ''}
                </Text>
              </View>
              {bhk && <View style={styles.reviewRow}><Text style={styles.reviewLabel}>BHK</Text><Text style={styles.reviewValue}>{bhk}</Text></View>}
              {areaSqft && <View style={styles.reviewRow}><Text style={styles.reviewLabel}>Area</Text><Text style={styles.reviewValue}>{areaSqft} sq.ft</Text></View>}
              {furnishing && <View style={styles.reviewRow}><Text style={styles.reviewLabel}>Furnishing</Text><Text style={styles.reviewValue}>{furnishing}</Text></View>}
              {facing && <View style={styles.reviewRow}><Text style={styles.reviewLabel}>Facing</Text><Text style={styles.reviewValue}>{facing}</Text></View>}
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>Photos</Text>
                <Text style={styles.reviewValue}>{photos.length} uploaded</Text>
              </View>
              <View style={styles.reviewRow}>
                <Text style={styles.reviewLabel}>RERA</Text>
                <Text style={styles.reviewValue}>{reraApproved ? `Yes${reraNumber ? ` (${reraNumber})` : ''}` : 'No'}</Text>
              </View>
            </View>
          )}
        </ScrollView>

        {/* Bottom Nav */}
        <View style={styles.bottomNav}>
          {step > 0 && (
            <TouchableOpacity style={styles.backBtn} onPress={goBack}>
              <Text style={styles.backBtnText}>Back</Text>
            </TouchableOpacity>
          )}
          {step < STEPS.length - 1 ? (
            <TouchableOpacity style={[styles.nextBtn, step === 0 && { flex: 1 }]} onPress={goNext} activeOpacity={0.8}>
              <Text style={styles.nextBtnText}>Next</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.submitBtn} onPress={handleSubmit} disabled={submitting} activeOpacity={0.8}>
              {submitting ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <Text style={styles.submitBtnText}>Submit Property</Text>
              )}
            </TouchableOpacity>
          )}
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  header: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  headerBack: {
    width: 32, height: 32, borderRadius: 16, backgroundColor: '#f3f4f6',
    alignItems: 'center', justifyContent: 'center', marginRight: 10,
  },
  headerTitle: { flex: 1, fontSize: 17, fontWeight: '700', color: '#111827' },
  headerStep: { fontSize: 13, fontWeight: '600', color: '#9ca3af' },

  progressBar: { height: 3, backgroundColor: '#f3f4f6' },
  progressFill: { height: 3, backgroundColor: ORANGE },

  formContent: { padding: 20, paddingBottom: 30 },
  stepLabel: { fontSize: 22, fontWeight: '800', color: '#111827', marginBottom: 20 },

  typeGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  typeCard: {
    width: '47%', backgroundColor: '#f9fafb', borderRadius: 14, padding: 16,
    alignItems: 'center', borderWidth: 2, borderColor: 'transparent',
  },
  typeCardActive: { borderColor: ORANGE, backgroundColor: '#fff7ed' },
  typeIcon: { fontSize: 28, marginBottom: 6 },
  typeLabel: { fontSize: 13, fontWeight: '600', color: '#374151' },
  typeLabelActive: { color: ORANGE },

  inputLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6, marginTop: 12 },
  input: {
    backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12,
    fontSize: 14, color: '#111827',
  },
  row: { flexDirection: 'row', gap: 10 },

  pricePreview: { fontSize: 16, fontWeight: '700', color: ORANGE, marginTop: 8 },

  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 4 },
  chip: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
    backgroundColor: '#f3f4f6', borderWidth: 1, borderColor: '#e5e7eb',
  },
  chipActive: { backgroundColor: '#fff7ed', borderColor: ORANGE },
  chipText: { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  chipTextActive: { color: ORANGE },

  toggleGroup: { marginTop: 12, gap: 8 },
  toggleRow: {
    flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 10,
    paddingHorizontal: 14, borderRadius: 10, backgroundColor: '#f9fafb',
  },
  toggleRowActive: { backgroundColor: '#fff7ed' },
  toggleDot: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, borderColor: '#d1d5db' },
  toggleDotActive: { borderColor: ORANGE, backgroundColor: ORANGE },
  toggleText: { fontSize: 14, color: '#374151', fontWeight: '500' },

  addPhotoBtn: {
    borderWidth: 2, borderColor: '#e5e7eb', borderStyle: 'dashed', borderRadius: 14,
    paddingVertical: 24, alignItems: 'center', marginBottom: 16,
  },
  addPhotoIcon: { fontSize: 28, color: ORANGE, fontWeight: '300' },
  addPhotoText: { fontSize: 14, fontWeight: '600', color: '#6b7280', marginTop: 4 },
  photoGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  photoThumb: { width: 100, height: 100, borderRadius: 10, overflow: 'hidden' },
  photoImage: { width: '100%', height: '100%' },
  photoRemove: {
    position: 'absolute', top: 4, right: 4, width: 22, height: 22, borderRadius: 11,
    backgroundColor: 'rgba(0,0,0,0.6)', alignItems: 'center', justifyContent: 'center',
  },
  photoRemoveText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  photoHint: { fontSize: 13, color: '#9ca3af', textAlign: 'center', marginTop: 8 },

  reviewSection: { gap: 2 },
  reviewRow: {
    flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  reviewLabel: { fontSize: 13, color: '#6b7280', fontWeight: '500' },
  reviewValue: { fontSize: 14, fontWeight: '600', color: '#111827', maxWidth: '60%', textAlign: 'right' },

  bottomNav: {
    flexDirection: 'row', padding: 16, gap: 10, borderTopWidth: 1, borderTopColor: '#f3f4f6',
  },
  backBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#f3f4f6', alignItems: 'center' },
  backBtnText: { fontWeight: '600', color: '#6b7280', fontSize: 15 },
  nextBtn: { flex: 2, paddingVertical: 14, borderRadius: 12, backgroundColor: ORANGE, alignItems: 'center' },
  nextBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  submitBtn: { flex: 2, paddingVertical: 14, borderRadius: 12, backgroundColor: '#16a34a', alignItems: 'center' },
  submitBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },

  successContainer: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  successTitle: { fontSize: 24, fontWeight: '800', color: '#111827', marginBottom: 8 },
  successSub: { fontSize: 14, color: '#6b7280', textAlign: 'center', lineHeight: 22, marginBottom: 24 },
  successBtn: { backgroundColor: ORANGE, borderRadius: 12, paddingHorizontal: 32, paddingVertical: 14, marginBottom: 10 },
  successBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  successBtnSecondary: { paddingVertical: 10 },
  successBtnSecondaryText: { color: ORANGE, fontWeight: '600', fontSize: 14 },
});
