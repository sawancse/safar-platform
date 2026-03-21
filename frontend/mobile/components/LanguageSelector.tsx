import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { useTranslation, type Locale } from '@/lib/i18n';

const LOCALES: { value: Locale; label: string }[] = [
  { value: 'en', label: 'English' },
  { value: 'hi', label: 'हिन्दी' },
];

export default function LanguageSelector() {
  const { locale, setLocale } = useTranslation();

  return (
    <View style={styles.container}>
      {LOCALES.map(({ value, label }) => (
        <TouchableOpacity
          key={value}
          style={[styles.btn, locale === value && styles.active]}
          onPress={() => setLocale(value)}
          activeOpacity={0.7}
        >
          <Text style={[styles.text, locale === value && styles.activeText]}>
            {label}
          </Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    gap: 8,
    padding: 4,
  },
  btn: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  active: {
    backgroundColor: '#f97316',
    borderColor: '#f97316',
  },
  text: {
    fontSize: 14,
    color: '#333',
  },
  activeText: {
    color: '#fff',
    fontWeight: '600',
  },
});
