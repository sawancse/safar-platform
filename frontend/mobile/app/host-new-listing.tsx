import { useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Image,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Constants ──────────────────────────────────────────────── */

const ORANGE = '#f97316';
const STEPS = ['Type', 'Info', 'Amenities', 'Photos', 'Review'];

const PROPERTY_TYPES = [
  { key: 'HOME', icon: '🏠', label: 'Home' },
  { key: 'ROOM', icon: '🛏️', label: 'Room' },
  { key: 'HOTEL', icon: '🏨', label: 'Hotel' },
  { key: 'PG', icon: '🏢', label: 'PG' },
  { key: 'HOSTEL', icon: '🛌', label: 'Hostel' },
  { key: 'VILLA', icon: '🏡', label: 'Villa' },
  { key: 'APARTMENT', icon: '🏬', label: 'Apartment' },
  { key: 'FARMHOUSE', icon: '🌾', label: 'Farmhouse' },
  { key: 'COMMERCIAL', icon: '🏗️', label: 'Commercial' },
  { key: 'MEDICAL', icon: '🏥', label: 'Medical' },
];

const PRICING_UNITS = ['NIGHT', 'MONTH', 'HOUR'] as const;

const AMENITIES = [
  'WiFi', 'AC', 'Parking', 'Kitchen', 'TV', 'Washing Machine',
  'Hot Water', 'Power Backup', 'CCTV', 'Elevator', 'Gym',
  'Swimming Pool', 'Pet Friendly', 'Wheelchair Accessible',
];

const PHOTO_CATEGORIES = [
  'EXTERIOR', 'BEDROOM', 'BATHROOM', 'KITCHEN', 'LIVING', 'DINING', 'OTHER',
] as const;

type PhotoCategory = typeof PHOTO_CATEGORIES[number];

interface PhotoItem {
  uri: string;
  category: PhotoCategory;
}

/* ── Component ──────────────────────────────────────────────── */

export default function HostNewListingScreen() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  // Step 1
  const [propertyType, setPropertyType] = useState('');

  // Step 2
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [pincode, setPincode] = useState('');
  const [pricingUnit, setPricingUnit] = useState<typeof PRICING_UNITS[number]>('NIGHT');
  const [basePrice, setBasePrice] = useState('');
  const [maxGuests, setMaxGuests] = useState('');

  // Step 3
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);

  // Step 4
  const [photos, setPhotos] = useState<PhotoItem[]>([]);

  /* ── Validation ─────────────────────────────────────────── */

  const validateStep = useCallback((): boolean => {
    if (step === 0) {
      if (!propertyType) { Alert.alert('Required', 'Please select a property type.'); return false; }
    } else if (step === 1) {
      if (!title.trim()) { Alert.alert('Required', 'Please enter a title.'); return false; }
      if (!description.trim()) { Alert.alert('Required', 'Please enter a description.'); return false; }
      if (!addressLine1.trim()) { Alert.alert('Required', 'Please enter address line 1.'); return false; }
      if (!city.trim()) { Alert.alert('Required', 'Please enter a city.'); return false; }
      if (!state.trim()) { Alert.alert('Required', 'Please enter a state.'); return false; }
      if (!pincode.trim() || pincode.length !== 6) { Alert.alert('Required', 'Please enter a valid 6-digit pincode.'); return false; }
      if (!basePrice.trim() || isNaN(Number(basePrice)) || Number(basePrice) <= 0) {
        Alert.alert('Required', 'Please enter a valid base price.');
        return false;
      }
      if (!maxGuests.trim() || isNaN(Number(maxGuests)) || Number(maxGuests) < 1) {
        Alert.alert('Required', 'Please enter max guests (at least 1).');
        return false;
      }
    }
    // Steps 2 (amenities) and 3 (photos) are optional
    return true;
  }, [step, propertyType, title, description, addressLine1, city, state, pincode, basePrice, maxGuests]);

  /* ── Photo Picker ───────────────────────────────────────── */

  const pickPhotos = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission needed', 'Please grant photo library access to upload images.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      const newPhotos: PhotoItem[] = result.assets.map((a) => ({
        uri: a.uri,
        category: 'EXTERIOR' as PhotoCategory,
      }));
      setPhotos((prev) => [...prev, ...newPhotos]);
    }
  };

  const takePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission needed', 'Please grant camera access to take photos.');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });
    if (!result.canceled && result.assets.length > 0) {
      const newPhotos: PhotoItem[] = result.assets.map((a) => ({
        uri: a.uri,
        category: 'EXTERIOR' as PhotoCategory,
      }));
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

  const setCategoryForPhoto = (index: number, cat: PhotoCategory) => {
    setPhotos((prev) => prev.map((p, i) => (i === index ? { ...p, category: cat } : p)));
  };

  /* ── Submit ─────────────────────────────────────────────── */

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const token = await getAccessToken();
      if (!token) {
        Alert.alert('Auth Error', 'Please log in again.');
        setSubmitting(false);
        return;
      }

      const pricePaise = Math.round(Number(basePrice) * 100);
      const listingData = {
        type: propertyType,
        title: title.trim(),
        description: description.trim(),
        addressLine1: addressLine1.trim(),
        addressLine2: addressLine2.trim() || undefined,
        city: city.trim(),
        state: state.trim(),
        pincode: pincode.trim(),
        pricingUnit,
        basePricePaise: pricePaise,
        maxGuests: Number(maxGuests),
        amenities: selectedAmenities,
      };

      const listing: any = await api.createListing(listingData, token);
      const listingId = listing.id;

      // Upload photos sequentially
      for (const photo of photos) {
        try {
          await api.uploadListingPhoto(listingId, photo.uri, photo.category, token);
        } catch {
          // continue on photo upload failure
        }
      }

      await api.submitForVerification(listingId, token);
      setSuccess(true);
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Failed to create listing.');
    } finally {
      setSubmitting(false);
    }
  };

  /* ── Navigation ─────────────────────────────────────────── */

  const goNext = () => {
    if (!validateStep()) return;
    if (step === STEPS.length - 1) {
      handleSubmit();
    } else {
      setStep((s) => s + 1);
    }
  };

  const goBack = () => {
    if (step === 0) {
      router.back();
    } else {
      setStep((s) => s - 1);
    }
  };

  /* ── Progress Bar ───────────────────────────────────────── */

  const renderProgressBar = () => (
    <View style={styles.progressContainer}>
      {STEPS.map((label, i) => (
        <View key={label} style={styles.progressStep}>
          <View style={[styles.progressDot, i <= step && styles.progressDotActive]}>
            <Text style={[styles.progressDotText, i <= step && styles.progressDotTextActive]}>
              {i + 1}
            </Text>
          </View>
          <Text style={[styles.progressLabel, i <= step && styles.progressLabelActive]}>
            {label}
          </Text>
          {i < STEPS.length - 1 && (
            <View style={[styles.progressLine, i < step && styles.progressLineActive]} />
          )}
        </View>
      ))}
    </View>
  );

  /* ── Step 1: Property Type ──────────────────────────────── */

  const renderStep1 = () => (
    <View>
      <Text style={styles.stepTitle}>What type of property?</Text>
      <Text style={styles.stepSubtitle}>Select the category that best fits your listing.</Text>
      <View style={styles.typeGrid}>
        {PROPERTY_TYPES.map((t) => (
          <TouchableOpacity
            key={t.key}
            style={[styles.typeCard, propertyType === t.key && styles.typeCardSelected]}
            onPress={() => setPropertyType(t.key)}
            activeOpacity={0.7}
          >
            <Text style={styles.typeIcon}>{t.icon}</Text>
            <Text style={[styles.typeLabel, propertyType === t.key && styles.typeLabelSelected]}>
              {t.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  /* ── Step 2: Basic Info ─────────────────────────────────── */

  const renderStep2 = () => (
    <View>
      <Text style={styles.stepTitle}>Basic Information</Text>
      <Text style={styles.stepSubtitle}>Provide details about your listing.</Text>

      <Text style={styles.inputLabel}>Title</Text>
      <TextInput
        style={styles.input}
        value={title}
        onChangeText={setTitle}
        placeholder="e.g. Cozy 2BHK in Koramangala"
        placeholderTextColor="#9ca3af"
      />

      <Text style={styles.inputLabel}>Description</Text>
      <TextInput
        style={[styles.input, styles.textarea]}
        value={description}
        onChangeText={setDescription}
        placeholder="Describe your property, surroundings, and what makes it special..."
        placeholderTextColor="#9ca3af"
        multiline
        numberOfLines={4}
        textAlignVertical="top"
      />

      <Text style={styles.inputLabel}>Address Line 1</Text>
      <TextInput
        style={styles.input}
        value={addressLine1}
        onChangeText={setAddressLine1}
        placeholder="Street address"
        placeholderTextColor="#9ca3af"
      />

      <Text style={styles.inputLabel}>Address Line 2 (optional)</Text>
      <TextInput
        style={styles.input}
        value={addressLine2}
        onChangeText={setAddressLine2}
        placeholder="Apartment, floor, landmark"
        placeholderTextColor="#9ca3af"
      />

      <View style={styles.row}>
        <View style={styles.halfField}>
          <Text style={styles.inputLabel}>City</Text>
          <TextInput
            style={styles.input}
            value={city}
            onChangeText={setCity}
            placeholder="City"
            placeholderTextColor="#9ca3af"
          />
        </View>
        <View style={styles.halfField}>
          <Text style={styles.inputLabel}>State</Text>
          <TextInput
            style={styles.input}
            value={state}
            onChangeText={setState}
            placeholder="State"
            placeholderTextColor="#9ca3af"
          />
        </View>
      </View>

      <Text style={styles.inputLabel}>Pincode</Text>
      <TextInput
        style={styles.input}
        value={pincode}
        onChangeText={setPincode}
        placeholder="6-digit pincode"
        placeholderTextColor="#9ca3af"
        keyboardType="number-pad"
        maxLength={6}
      />

      <Text style={styles.inputLabel}>Pricing Unit</Text>
      <View style={styles.unitRow}>
        {PRICING_UNITS.map((u) => (
          <TouchableOpacity
            key={u}
            style={[styles.unitChip, pricingUnit === u && styles.unitChipSelected]}
            onPress={() => setPricingUnit(u)}
            activeOpacity={0.7}
          >
            <Text style={[styles.unitChipText, pricingUnit === u && styles.unitChipTextSelected]}>
              Per {u.charAt(0) + u.slice(1).toLowerCase()}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.inputLabel}>
        Base Price ({'\u20B9'} per {pricingUnit.toLowerCase()})
      </Text>
      <TextInput
        style={styles.input}
        value={basePrice}
        onChangeText={setBasePrice}
        placeholder="e.g. 1500"
        placeholderTextColor="#9ca3af"
        keyboardType="numeric"
      />

      <Text style={styles.inputLabel}>Max Guests</Text>
      <TextInput
        style={styles.input}
        value={maxGuests}
        onChangeText={setMaxGuests}
        placeholder="e.g. 4"
        placeholderTextColor="#9ca3af"
        keyboardType="number-pad"
      />
    </View>
  );

  /* ── Step 3: Amenities ──────────────────────────────────── */

  const toggleAmenity = (a: string) => {
    setSelectedAmenities((prev) =>
      prev.includes(a) ? prev.filter((x) => x !== a) : [...prev, a]
    );
  };

  const renderStep3 = () => (
    <View>
      <Text style={styles.stepTitle}>Amenities</Text>
      <Text style={styles.stepSubtitle}>Select all amenities your property offers.</Text>
      <View style={styles.amenityGrid}>
        {AMENITIES.map((a) => {
          const selected = selectedAmenities.includes(a);
          return (
            <TouchableOpacity
              key={a}
              style={[styles.amenityChip, selected && styles.amenityChipSelected]}
              onPress={() => toggleAmenity(a)}
              activeOpacity={0.7}
            >
              <Text style={[styles.amenityText, selected && styles.amenityTextSelected]}>
                {a}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );

  /* ── Step 4: Photos ─────────────────────────────────────── */

  const renderStep4 = () => (
    <View>
      <Text style={styles.stepTitle}>Photos</Text>
      <Text style={styles.stepSubtitle}>
        Add photos to showcase your property. Assign a category to each.
      </Text>

      <TouchableOpacity style={styles.addPhotoBtn} onPress={showPhotoOptions} activeOpacity={0.7}>
        <Text style={styles.addPhotoBtnText}>+ Add Photos</Text>
      </TouchableOpacity>

      {photos.length === 0 && (
        <Text style={styles.noPhotosText}>No photos added yet.</Text>
      )}

      <View style={styles.photoGrid}>
        {photos.map((photo, idx) => (
          <View key={idx} style={styles.photoItem}>
            <Image source={{ uri: photo.uri }} style={styles.photoThumb} />
            <TouchableOpacity
              style={styles.photoDeleteBtn}
              onPress={() => removePhoto(idx)}
              activeOpacity={0.7}
            >
              <Text style={styles.photoDeleteText}>X</Text>
            </TouchableOpacity>
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              style={styles.catScroll}
            >
              {PHOTO_CATEGORIES.map((cat) => (
                <TouchableOpacity
                  key={cat}
                  style={[
                    styles.catChip,
                    photo.category === cat && styles.catChipSelected,
                  ]}
                  onPress={() => setCategoryForPhoto(idx, cat)}
                  activeOpacity={0.7}
                >
                  <Text
                    style={[
                      styles.catChipText,
                      photo.category === cat && styles.catChipTextSelected,
                    ]}
                  >
                    {cat}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        ))}
      </View>
    </View>
  );

  /* ── Step 5: Review & Submit ────────────────────────────── */

  const renderStep5 = () => {
    if (success) {
      return (
        <View style={styles.successContainer}>
          <Text style={styles.successIcon}>&#10003;</Text>
          <Text style={styles.successTitle}>Listing Submitted!</Text>
          <Text style={styles.successSubtitle}>
            Your listing has been submitted for verification. You will be notified once it is approved.
          </Text>
          <TouchableOpacity
            style={styles.dashboardBtn}
            onPress={() => router.replace('/host')}
            activeOpacity={0.7}
          >
            <Text style={styles.dashboardBtnText}>Go to Dashboard</Text>
          </TouchableOpacity>
        </View>
      );
    }

    const pricePaise = Math.round(Number(basePrice) * 100);
    const typeObj = PROPERTY_TYPES.find((t) => t.key === propertyType);

    return (
      <View>
        <Text style={styles.stepTitle}>Review Your Listing</Text>
        <Text style={styles.stepSubtitle}>Make sure everything looks good before submitting.</Text>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Property Type</Text>
          <Text style={styles.reviewValue}>{typeObj?.icon} {typeObj?.label}</Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Title</Text>
          <Text style={styles.reviewValue}>{title}</Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Description</Text>
          <Text style={styles.reviewValue}>{description}</Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Address</Text>
          <Text style={styles.reviewValue}>
            {addressLine1}
            {addressLine2 ? `\n${addressLine2}` : ''}
            {'\n'}{city}, {state} - {pincode}
          </Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Pricing</Text>
          <Text style={styles.reviewValue}>
            {'\u20B9'}{basePrice} per {pricingUnit.toLowerCase()} ({pricePaise} paise)
          </Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Max Guests</Text>
          <Text style={styles.reviewValue}>{maxGuests}</Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Amenities</Text>
          <Text style={styles.reviewValue}>
            {selectedAmenities.length > 0 ? selectedAmenities.join(', ') : 'None selected'}
          </Text>
        </View>

        <View style={styles.reviewSection}>
          <Text style={styles.reviewLabel}>Photos</Text>
          <Text style={styles.reviewValue}>
            {photos.length} photo{photos.length !== 1 ? 's' : ''} added
          </Text>
        </View>

        {photos.length > 0 && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.reviewPhotos}>
            {photos.map((p, i) => (
              <View key={i} style={styles.reviewPhotoItem}>
                <Image source={{ uri: p.uri }} style={styles.reviewPhotoThumb} />
                <Text style={styles.reviewPhotoCat}>{p.category}</Text>
              </View>
            ))}
          </ScrollView>
        )}
      </View>
    );
  };

  /* ── Main Render ────────────────────────────────────────── */

  const renderCurrentStep = () => {
    switch (step) {
      case 0: return renderStep1();
      case 1: return renderStep2();
      case 2: return renderStep3();
      case 3: return renderStep4();
      case 4: return renderStep5();
      default: return null;
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} activeOpacity={0.7}>
          <Text style={styles.headerBack}>{'<'} Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Listing</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Progress */}
      {renderProgressBar()}

      {/* Content */}
      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentInner}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {renderCurrentStep()}
      </ScrollView>

      {/* Bottom Nav */}
      {!success && (
        <View style={styles.bottomBar}>
          <TouchableOpacity
            style={styles.backBtn}
            onPress={goBack}
            activeOpacity={0.7}
          >
            <Text style={styles.backBtnText}>
              {step === 0 ? 'Cancel' : 'Back'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.nextBtn, submitting && styles.nextBtnDisabled]}
            onPress={goNext}
            disabled={submitting}
            activeOpacity={0.7}
          >
            {submitting ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.nextBtnText}>
                {step === STEPS.length - 1 ? 'Submit' : 'Next'}
              </Text>
            )}
          </TouchableOpacity>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

/* ── Styles ──────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 52,
    paddingBottom: 12,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerBack: {
    fontSize: 16,
    color: ORANGE,
    fontWeight: '600',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#111827',
  },

  /* Progress */
  progressContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    paddingHorizontal: 12,
    backgroundColor: '#fafafa',
  },
  progressStep: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  progressDot: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#e5e7eb',
    alignItems: 'center',
    justifyContent: 'center',
  },
  progressDotActive: {
    backgroundColor: ORANGE,
  },
  progressDotText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#9ca3af',
  },
  progressDotTextActive: {
    color: '#fff',
  },
  progressLabel: {
    fontSize: 10,
    color: '#9ca3af',
    marginLeft: 4,
    marginRight: 4,
  },
  progressLabelActive: {
    color: ORANGE,
    fontWeight: '600',
  },
  progressLine: {
    width: 16,
    height: 2,
    backgroundColor: '#e5e7eb',
    marginHorizontal: 2,
  },
  progressLineActive: {
    backgroundColor: ORANGE,
  },

  /* Content */
  content: {
    flex: 1,
  },
  contentInner: {
    padding: 20,
    paddingBottom: 40,
  },

  /* Step Titles */
  stepTitle: {
    fontSize: 22,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 4,
  },
  stepSubtitle: {
    fontSize: 14,
    color: '#6b7280',
    marginBottom: 20,
  },

  /* Step 1: Type Grid */
  typeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  typeCard: {
    width: '30%',
    aspectRatio: 1,
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e5e7eb',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 8,
  },
  typeCardSelected: {
    borderColor: ORANGE,
    backgroundColor: '#fff7ed',
  },
  typeIcon: {
    fontSize: 32,
    marginBottom: 6,
  },
  typeLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#374151',
    textAlign: 'center',
  },
  typeLabelSelected: {
    color: ORANGE,
  },

  /* Step 2: Form */
  inputLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 6,
    marginTop: 14,
  },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: '#111827',
    backgroundColor: '#f9fafb',
  },
  textarea: {
    height: 100,
    textAlignVertical: 'top',
  },
  row: {
    flexDirection: 'row',
    gap: 12,
  },
  halfField: {
    flex: 1,
  },
  unitRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 4,
  },
  unitChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1.5,
    borderColor: '#d1d5db',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
  },
  unitChipSelected: {
    borderColor: ORANGE,
    backgroundColor: '#fff7ed',
  },
  unitChipText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#6b7280',
  },
  unitChipTextSelected: {
    color: ORANGE,
  },

  /* Step 3: Amenities */
  amenityGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  amenityChip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    borderWidth: 1.5,
    borderColor: '#d1d5db',
    backgroundColor: '#f9fafb',
  },
  amenityChipSelected: {
    borderColor: ORANGE,
    backgroundColor: ORANGE,
  },
  amenityText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#6b7280',
  },
  amenityTextSelected: {
    color: '#fff',
  },

  /* Step 4: Photos */
  addPhotoBtn: {
    backgroundColor: ORANGE,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 16,
  },
  addPhotoBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  noPhotosText: {
    textAlign: 'center',
    color: '#9ca3af',
    fontSize: 14,
    marginTop: 20,
  },
  photoGrid: {
    gap: 16,
  },
  photoItem: {
    backgroundColor: '#f9fafb',
    borderRadius: 12,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  photoThumb: {
    width: '100%',
    height: 180,
    resizeMode: 'cover',
  },
  photoDeleteBtn: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(0,0,0,0.6)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  photoDeleteText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '700',
  },
  catScroll: {
    paddingVertical: 8,
    paddingHorizontal: 8,
  },
  catChip: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#d1d5db',
    marginRight: 6,
    backgroundColor: '#fff',
  },
  catChipSelected: {
    borderColor: ORANGE,
    backgroundColor: ORANGE,
  },
  catChipText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#6b7280',
  },
  catChipTextSelected: {
    color: '#fff',
  },

  /* Step 5: Review */
  reviewSection: {
    marginBottom: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  reviewLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#9ca3af',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  reviewValue: {
    fontSize: 15,
    color: '#111827',
    lineHeight: 22,
  },
  reviewPhotos: {
    marginTop: 8,
  },
  reviewPhotoItem: {
    marginRight: 10,
    alignItems: 'center',
  },
  reviewPhotoThumb: {
    width: 80,
    height: 60,
    borderRadius: 8,
    resizeMode: 'cover',
  },
  reviewPhotoCat: {
    fontSize: 10,
    color: '#6b7280',
    marginTop: 4,
  },

  /* Success */
  successContainer: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  successIcon: {
    fontSize: 48,
    color: '#22c55e',
    marginBottom: 16,
  },
  successTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 8,
  },
  successSubtitle: {
    fontSize: 15,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 32,
    paddingHorizontal: 20,
  },
  dashboardBtn: {
    backgroundColor: ORANGE,
    borderRadius: 10,
    paddingVertical: 14,
    paddingHorizontal: 40,
  },
  dashboardBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },

  /* Bottom Bar */
  bottomBar: {
    flexDirection: 'row',
    padding: 16,
    paddingBottom: 32,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    backgroundColor: '#fff',
    gap: 12,
  },
  backBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 10,
    borderWidth: 1.5,
    borderColor: '#d1d5db',
    alignItems: 'center',
  },
  backBtnText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
  },
  nextBtn: {
    flex: 2,
    paddingVertical: 14,
    borderRadius: 10,
    backgroundColor: ORANGE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextBtnDisabled: {
    opacity: 0.6,
  },
  nextBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
});
