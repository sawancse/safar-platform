import { useEffect, useState } from 'react';
import {
  View, Text, TextInput, ScrollView, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert, Image,
} from 'react-native';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import Constants from 'expo-constants';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const BASE_URL: string =
  (Constants.expoConfig?.extra?.apiUrl as string) ?? 'http://localhost:8080';

const AVATAR_COLORS = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ef4444', '#ec4899'];

function getInitials(name: string): string {
  return (name || '?')
    .split(' ')
    .map((w) => w[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();
}

function getAvatarColor(name: string): string {
  const sum = (name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return AVATAR_COLORS[sum % AVATAR_COLORS.length];
}

function resolveAvatarUrl(avatarUrl: string): string {
  if (avatarUrl.startsWith('http')) return avatarUrl;
  return `${BASE_URL}${avatarUrl}`;
}

export default function ProfileEditScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [token, setToken] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState('');
  const [nationality, setNationality] = useState('');
  const [address, setAddress] = useState('');
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const t = await getAccessToken();
      if (!t) {
        router.replace('/auth');
        return;
      }
      setToken(t);
      try {
        const profile = await api.getMyProfile(t);
        setName(profile.name ?? '');
        setDisplayName(profile.displayName ?? '');
        setEmail(profile.email ?? '');
        setPhone(profile.phone ?? '');
        setDateOfBirth(profile.dateOfBirth ?? '');
        setGender(profile.gender ?? '');
        setNationality(profile.nationality ?? '');
        setAddress(profile.address ?? '');
        setAvatarUrl(profile.avatarUrl ?? null);
      } catch (err: any) {
        Alert.alert('Error', err.message ?? 'Failed to load profile');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  async function pickImage(source: 'camera' | 'gallery') {
    if (!token) return;

    // Request permissions
    if (source === 'camera') {
      const { status } = await ImagePicker.requestCameraPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission needed', 'Camera access is required to take a photo.');
        return;
      }
    } else {
      const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission needed', 'Photo library access is required to select a photo.');
        return;
      }
    }

    const result = source === 'camera'
      ? await ImagePicker.launchCameraAsync({
          mediaTypes: ImagePicker.MediaTypeOptions.Images,
          allowsEditing: true,
          aspect: [1, 1],
          quality: 0.8,
        })
      : await ImagePicker.launchImageLibraryAsync({
          mediaTypes: ImagePicker.MediaTypeOptions.Images,
          allowsEditing: true,
          aspect: [1, 1],
          quality: 0.8,
        });

    if (result.canceled || !result.assets?.[0]) return;

    const asset = result.assets[0];

    // Check file size (5MB limit)
    if (asset.fileSize && asset.fileSize > 5 * 1024 * 1024) {
      Alert.alert('Too large', 'Image must be under 5MB.');
      return;
    }

    setUploading(true);
    try {
      const res = await api.uploadAvatar(asset.uri, token);
      setAvatarUrl(res.avatarUrl);
      Alert.alert('Success', 'Profile photo updated!');
    } catch (err: any) {
      Alert.alert('Upload failed', err.message ?? 'Failed to upload photo.');
    } finally {
      setUploading(false);
    }
  }

  function handleAvatarPress() {
    Alert.alert('Profile Photo', 'Choose an option', [
      { text: 'Take Photo', onPress: () => pickImage('camera') },
      { text: 'Choose from Gallery', onPress: () => pickImage('gallery') },
      ...(avatarUrl
        ? [{
            text: 'Remove Photo',
            style: 'destructive' as const,
            onPress: async () => {
              if (!token) return;
              try {
                await api.updateMyProfile({ avatarUrl: '' }, token);
                setAvatarUrl(null);
              } catch {
                Alert.alert('Error', 'Failed to remove photo.');
              }
            },
          }]
        : []),
      { text: 'Cancel', style: 'cancel' as const },
    ]);
  }

  async function handleSave() {
    if (!token) return;
    setSaving(true);
    try {
      await api.updateMyProfile(
        { name, displayName, email, dateOfBirth, gender, nationality, address },
        token,
      );
      Alert.alert('Success', 'Profile updated successfully', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err: any) {
      Alert.alert('Error', err.message ?? 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>&#8249; Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Edit Profile</Text>
        <View style={{ width: 60 }} />
      </View>

      <ScrollView contentContainerStyle={styles.form} keyboardShouldPersistTaps="handled">
        {/* Avatar Section */}
        <View style={styles.avatarSection}>
          <TouchableOpacity onPress={handleAvatarPress} disabled={uploading} activeOpacity={0.7}>
            <View style={styles.avatarWrapper}>
              {uploading ? (
                <View style={[styles.avatarFallback, { backgroundColor: '#e5e7eb' }]}>
                  <ActivityIndicator color="#f97316" size="large" />
                </View>
              ) : avatarUrl ? (
                <Image
                  source={{ uri: resolveAvatarUrl(avatarUrl) }}
                  style={styles.avatarImage}
                />
              ) : (
                <View style={[styles.avatarFallback, { backgroundColor: getAvatarColor(name) }]}>
                  <Text style={styles.avatarInitials}>{getInitials(name)}</Text>
                </View>
              )}
            </View>
          </TouchableOpacity>
          <Text style={styles.avatarHint}>
            {uploading ? 'Uploading...' : avatarUrl ? 'Tap photo to change' : 'Tap to add photo'}
          </Text>
        </View>

        <Text style={styles.label}>Name</Text>
        <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="Full name" placeholderTextColor="#9ca3af" />

        <Text style={styles.label}>Display Name</Text>
        <TextInput style={styles.input} value={displayName} onChangeText={setDisplayName} placeholder="Display name" placeholderTextColor="#9ca3af" />

        <Text style={styles.label}>Email</Text>
        <TextInput style={styles.input} value={email} onChangeText={setEmail} placeholder="Email address" placeholderTextColor="#9ca3af" keyboardType="email-address" autoCapitalize="none" />

        <Text style={styles.label}>Phone</Text>
        <TextInput style={[styles.input, styles.readOnly]} value={phone} editable={false} />

        <Text style={styles.label}>Date of Birth</Text>
        <TextInput style={styles.input} value={dateOfBirth} onChangeText={setDateOfBirth} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" />

        <Text style={styles.label}>Gender</Text>
        <TextInput style={styles.input} value={gender} onChangeText={setGender} placeholder="Male / Female / Other" placeholderTextColor="#9ca3af" />

        <Text style={styles.label}>Nationality</Text>
        <TextInput style={styles.input} value={nationality} onChangeText={setNationality} placeholder="e.g. Indian" placeholderTextColor="#9ca3af" />

        <Text style={styles.label}>Address</Text>
        <TextInput style={[styles.input, { minHeight: 60, textAlignVertical: 'top' }]} value={address} onChangeText={setAddress} placeholder="Your address" placeholderTextColor="#9ca3af" multiline />

        <TouchableOpacity style={styles.saveBtn} onPress={handleSave} disabled={saving}>
          {saving ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.saveBtnText}>Save</Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  center:         { flex: 1, alignItems: 'center', justifyContent: 'center' },
  header:         { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn:        { width: 60 },
  backText:       { fontSize: 16, color: '#f97316', fontWeight: '600' },
  headerTitle:    { fontSize: 17, fontWeight: '700', color: '#111827' },
  form:           { padding: 16, paddingBottom: 40 },
  label:          { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 4, marginTop: 14 },
  input:          { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#111827' },
  readOnly:       { backgroundColor: '#f3f4f6', color: '#9ca3af' },
  saveBtn:        { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginTop: 24 },
  saveBtnText:    { color: '#fff', fontWeight: '700', fontSize: 16 },
  avatarSection:  { alignItems: 'center', paddingVertical: 8, marginBottom: 8 },
  avatarWrapper:  { width: 96, height: 96, borderRadius: 48, overflow: 'hidden', borderWidth: 2, borderColor: '#e5e7eb', position: 'relative' },
  avatarImage:    { width: '100%', height: '100%' },
  avatarFallback: { width: '100%', height: '100%', alignItems: 'center', justifyContent: 'center' },
  avatarInitials: { fontSize: 28, fontWeight: '700', color: '#fff' },
  avatarHint:     { fontSize: 13, color: '#6b7280', marginTop: 8 },
});
