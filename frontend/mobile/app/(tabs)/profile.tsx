import { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, Image } from 'react-native';
import { useRouter } from 'expo-router';
import Constants from 'expo-constants';
import { getAccessToken, getUserRole, clearTokens } from '@/lib/auth';
import { useTranslation } from '@/lib/i18n';
import { api } from '@/lib/api';
import LanguageSelector from '@/components/LanguageSelector';

const BASE_URL: string =
  (Constants.expoConfig?.extra?.apiUrl as string) ?? 'http://localhost:8080';

const AVATAR_COLORS = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ef4444', '#ec4899'];

function resolveAvatarUrl(url: string): string {
  if (url.startsWith('http')) return url;
  return `${BASE_URL}${url}`;
}

function getAvatarColor(name: string): string {
  const sum = (name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return AVATAR_COLORS[sum % AVATAR_COLORS.length];
}

function getInitials(name: string): string {
  return (name || '?').split(' ').map((w) => w[0]).join('').substring(0, 2).toUpperCase();
}

export default function ProfileScreen() {
  const router = useRouter();
  const { t } = useTranslation();
  const [role, setRole] = useState<string | null>(null);
  const [userName, setUserName] = useState<string | null>(null);
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const r = await getUserRole();
      setRole(r);
      if (r) {
        const token = await getAccessToken();
        if (token) {
          try {
            const profile = await api.getMyProfile(token);
            setUserName(profile.name ?? null);
            setUserEmail(profile.email ?? null);
            setAvatarUrl(profile.avatarUrl ?? null);
          } catch {}
        }
      }
    })();
  }, []);

  async function handleLogout() {
    Alert.alert(t('profile.logout'), t('profile.logoutConfirm'), [
      { text: t('common.cancel'), style: 'cancel' },
      {
        text: t('profile.logout'),
        style: 'destructive',
        onPress: async () => {
          await clearTokens();
          setRole(null);
          router.replace('/');
        },
      },
    ]);
  }

  if (!role) {
    return (
      <View style={styles.center}>
        <Text style={styles.icon}>👤</Text>
        <Text style={styles.title}>{t('auth.notSignedIn')}</Text>
        <TouchableOpacity style={styles.btn} onPress={() => router.push('/auth')}>
          <Text style={styles.btnText}>{t('auth.signIn')}</Text>
        </TouchableOpacity>
        <View style={styles.langSection}>
          <Text style={styles.langLabel}>{t('profile.language')}</Text>
          <LanguageSelector />
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.avatarBox}>
        {avatarUrl ? (
          <Image
            source={{ uri: resolveAvatarUrl(avatarUrl) }}
            style={styles.avatarImage}
          />
        ) : userName ? (
          <View style={[styles.avatarFallback, { backgroundColor: getAvatarColor(userName) }]}>
            <Text style={styles.avatarInitials}>{getInitials(userName)}</Text>
          </View>
        ) : (
          <Text style={styles.avatarIcon}>&#128100;</Text>
        )}
        {userName ? <Text style={styles.userName}>{userName}</Text> : null}
        {userEmail ? <Text style={styles.userEmail}>{userEmail}</Text> : null}
        <Text style={styles.roleBadge}>{role}</Text>
      </View>

      <View style={styles.menu}>
        {/* Language Selector */}
        <View style={styles.menuItem}>
          <Text style={styles.menuLabel}>{t('profile.language')}</Text>
          <LanguageSelector />
        </View>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/profile-edit')}>
          <Text style={styles.menuLabel}>✏️ Edit Profile</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/messages')}>
          <Text style={styles.menuLabel}>{'\uD83D\uDCAC'} Messages</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/notifications')}>
          <Text style={styles.menuLabel}>{'\uD83D\uDD14'} Notifications</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/saved')}>
          <Text style={styles.menuLabel}>♥ Saved Stays</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        {(role === 'HOST' || role === 'ADMIN') && (
          <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/host')}>
            <Text style={styles.menuLabel}>🏠 {t('profile.myListings')}</Text>
            <Text style={styles.menuArrow}>›</Text>
          </TouchableOpacity>
        )}
        {(role === 'HOST' || role === 'ADMIN') && (
          <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/subscription')}>
            <Text style={styles.menuLabel}>💎 Host Subscription</Text>
            <Text style={styles.menuArrow}>›</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/trips')}>
          <Text style={styles.menuLabel}>🗺️ {t('profile.myTrips')}</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/co-travelers')}>
          <Text style={styles.menuLabel}>👥 Co-travelers</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/payment-methods')}>
          <Text style={styles.menuLabel}>💳 Payment Methods</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/miles')}>
          <Text style={styles.menuLabel}>🏅 Property Miles</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/medical')}>
          <Text style={styles.menuLabel}>🏥 Medical Tourism</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/nomad')}>
          <Text style={styles.menuLabel}>🌍 Nomad Network</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/experiences')}>
          <Text style={styles.menuLabel}>🎯 Experiences</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.menuItem, styles.menuItemLast]} onPress={handleLogout}>
          <Text style={[styles.menuLabel, { color: '#ef4444' }]}>{t('profile.logout')}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  center:        { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  icon:          { fontSize: 48, marginBottom: 12 },
  title:         { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },
  btn:           { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12 },
  btnText:       { color: '#fff', fontWeight: '600', fontSize: 14 },
  avatarBox:     { alignItems: 'center', paddingVertical: 32, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  avatarImage:   { width: 80, height: 80, borderRadius: 40, marginBottom: 8, borderWidth: 2, borderColor: '#e5e7eb' },
  avatarFallback: { width: 80, height: 80, borderRadius: 40, marginBottom: 8, alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: '#e5e7eb' },
  avatarInitials: { fontSize: 28, fontWeight: '700', color: '#fff' },
  avatarIcon:    { fontSize: 56, marginBottom: 8 },
  userName:      { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 2 },
  userEmail:     { fontSize: 13, color: '#6b7280', marginBottom: 6 },
  roleBadge:     { backgroundColor: '#fff7ed', color: '#c2410c', fontWeight: '700', fontSize: 12, paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  menu:          { marginTop: 16, backgroundColor: '#fff', borderRadius: 16, marginHorizontal: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6' },
  menuItem:      { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  menuItemLast:  { borderBottomWidth: 0 },
  menuLabel:     { fontSize: 15, color: '#374151' },
  menuArrow:     { fontSize: 20, color: '#9ca3af' },
  langSection:   { marginTop: 32, alignItems: 'center' },
  langLabel:     { fontSize: 14, color: '#6b7280', marginBottom: 8 },
});
