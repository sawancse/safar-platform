import { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────────── */

type SectionStatus = 'pending' | 'verified' | 'rejected' | 'not_saved';
type KycStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED';

interface KycData {
  status: KycStatus;
  identity?: {
    panNumber: string;
    aadhaarNumber: string;
    status: SectionStatus;
  };
  address?: {
    addressLine1: string;
    addressLine2: string;
    city: string;
    state: string;
    pincode: string;
    status: SectionStatus;
  };
  bank?: {
    accountHolderName: string;
    accountNumber: string;
    ifscCode: string;
    bankName: string;
    status: SectionStatus;
  };
  business?: {
    businessName: string;
    gstin: string;
    businessType: string;
    status: SectionStatus;
  };
}

/* ── Status configs ────────────────────────────────────────────── */

const KYC_STATUS_CONFIG: Record<KycStatus, { label: string; bg: string; text: string }> = {
  NOT_STARTED: { label: 'Not Started', bg: '#f3f4f6', text: '#374151' },
  IN_PROGRESS: { label: 'In Progress', bg: '#fef9c3', text: '#854d0e' },
  SUBMITTED:   { label: 'Submitted',   bg: '#dbeafe', text: '#1e3a8a' },
  VERIFIED:    { label: 'Verified',     bg: '#dcfce7', text: '#14532d' },
  REJECTED:    { label: 'Rejected',     bg: '#fee2e2', text: '#7f1d1d' },
};

const SECTION_STATUS_CONFIG: Record<SectionStatus, { label: string; bg: string; text: string }> = {
  not_saved: { label: 'Not Saved', bg: '#f3f4f6', text: '#6b7280' },
  pending:   { label: 'Pending',   bg: '#fef9c3', text: '#854d0e' },
  verified:  { label: 'Verified',  bg: '#dcfce7', text: '#14532d' },
  rejected:  { label: 'Rejected',  bg: '#fee2e2', text: '#7f1d1d' },
};

/* ── Component ─────────────────────────────────────────────────── */

export default function HostKycScreen() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>('identity');
  const [kycStatus, setKycStatus] = useState<KycStatus>('NOT_STARTED');

  // Identity
  const [panNumber, setPanNumber] = useState('');
  const [aadhaarNumber, setAadhaarNumber] = useState('');
  const [identityStatus, setIdentityStatus] = useState<SectionStatus>('not_saved');

  // Address
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [pincode, setPincode] = useState('');
  const [addressStatus, setAddressStatus] = useState<SectionStatus>('not_saved');

  // Bank
  const [accountHolderName, setAccountHolderName] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [ifscCode, setIfscCode] = useState('');
  const [bankName, setBankName] = useState('');
  const [bankStatus, setBankStatus] = useState<SectionStatus>('not_saved');

  // Business (optional)
  const [businessName, setBusinessName] = useState('');
  const [gstin, setGstin] = useState('');
  const [businessType, setBusinessType] = useState('');
  const [businessStatus, setBusinessStatus] = useState<SectionStatus>('not_saved');

  useEffect(() => {
    loadKyc();
  }, []);

  async function loadKyc() {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    try {
      const data: KycData = await api.getKyc(token);
      setKycStatus(data.status || 'NOT_STARTED');

      if (data.identity) {
        setPanNumber(data.identity.panNumber || '');
        setAadhaarNumber(data.identity.aadhaarNumber || '');
        setIdentityStatus(data.identity.status || 'not_saved');
      }
      if (data.address) {
        setAddressLine1(data.address.addressLine1 || '');
        setAddressLine2(data.address.addressLine2 || '');
        setCity(data.address.city || '');
        setState(data.address.state || '');
        setPincode(data.address.pincode || '');
        setAddressStatus(data.address.status || 'not_saved');
      }
      if (data.bank) {
        setAccountHolderName(data.bank.accountHolderName || '');
        setAccountNumber(data.bank.accountNumber || '');
        setIfscCode(data.bank.ifscCode || '');
        setBankName(data.bank.bankName || '');
        setBankStatus(data.bank.status || 'not_saved');
      }
      if (data.business) {
        setBusinessName(data.business.businessName || '');
        setGstin(data.business.gstin || '');
        setBusinessType(data.business.businessType || '');
        setBusinessStatus(data.business.status || 'not_saved');
      }
    } catch {
      // KYC not started yet — defaults are fine
    } finally {
      setLoading(false);
    }
  }

  async function saveIdentity() {
    if (!panNumber.trim() || !aadhaarNumber.trim()) {
      Alert.alert('Required', 'Please enter both PAN and Aadhaar numbers.');
      return;
    }
    const token = await getAccessToken();
    if (!token) return;
    setSaving('identity');
    try {
      await api.updateKycIdentity({ panNumber: panNumber.trim(), aadhaarNumber: aadhaarNumber.trim() }, token);
      setIdentityStatus('pending');
      Alert.alert('Saved', 'Identity details saved.');
    } catch {
      Alert.alert('Error', 'Failed to save identity details.');
    } finally {
      setSaving(null);
    }
  }

  async function saveAddress() {
    if (!addressLine1.trim() || !city.trim() || !state.trim() || !pincode.trim()) {
      Alert.alert('Required', 'Please fill in all required address fields.');
      return;
    }
    const token = await getAccessToken();
    if (!token) return;
    setSaving('address');
    try {
      await api.updateKycAddress({
        addressLine1: addressLine1.trim(),
        addressLine2: addressLine2.trim(),
        city: city.trim(),
        state: state.trim(),
        pincode: pincode.trim(),
      }, token);
      setAddressStatus('pending');
      Alert.alert('Saved', 'Address details saved.');
    } catch {
      Alert.alert('Error', 'Failed to save address details.');
    } finally {
      setSaving(null);
    }
  }

  async function saveBank() {
    if (!accountHolderName.trim() || !accountNumber.trim() || !ifscCode.trim() || !bankName.trim()) {
      Alert.alert('Required', 'Please fill in all bank details.');
      return;
    }
    const token = await getAccessToken();
    if (!token) return;
    setSaving('bank');
    try {
      await api.updateKycBank({
        accountHolderName: accountHolderName.trim(),
        accountNumber: accountNumber.trim(),
        ifscCode: ifscCode.trim(),
        bankName: bankName.trim(),
      }, token);
      setBankStatus('pending');
      Alert.alert('Saved', 'Bank details saved.');
    } catch {
      Alert.alert('Error', 'Failed to save bank details.');
    } finally {
      setSaving(null);
    }
  }

  async function saveBusiness() {
    const token = await getAccessToken();
    if (!token) return;
    setSaving('business');
    try {
      await api.updateKycBusiness({
        businessName: businessName.trim(),
        gstin: gstin.trim(),
        businessType: businessType.trim(),
      }, token);
      setBusinessStatus('pending');
      Alert.alert('Saved', 'Business details saved.');
    } catch {
      Alert.alert('Error', 'Failed to save business details.');
    } finally {
      setSaving(null);
    }
  }

  async function handleSubmitKyc() {
    const token = await getAccessToken();
    if (!token) return;
    setSubmitting(true);
    try {
      await api.submitKyc(token);
      setKycStatus('SUBMITTED');
      Alert.alert('Submitted', 'Your KYC has been submitted for verification.');
    } catch {
      Alert.alert('Error', 'Failed to submit KYC. Please ensure all sections are saved.');
    } finally {
      setSubmitting(false);
    }
  }

  const allRequiredSaved =
    identityStatus !== 'not_saved' &&
    addressStatus !== 'not_saved' &&
    bankStatus !== 'not_saved';

  const canSubmit = allRequiredSaved && kycStatus !== 'SUBMITTED' && kycStatus !== 'VERIFIED';

  function toggleSection(key: string) {
    setExpandedSection(prev => (prev === key ? null : key));
  }

  /* ── Status badge ────────────────────────────────────────────── */

  function StatusBadge({ status, config }: { status: string; config: Record<string, { label: string; bg: string; text: string }> }) {
    const s = config[status] || config[Object.keys(config)[0]];
    return (
      <View style={[styles.badge, { backgroundColor: s.bg }]}>
        <Text style={[styles.badgeText, { color: s.text }]}>{s.label}</Text>
      </View>
    );
  }

  /* ── Section header ──────────────────────────────────────────── */

  function SectionHeader({ title, sectionKey, status }: { title: string; sectionKey: string; status: SectionStatus }) {
    const expanded = expandedSection === sectionKey;
    return (
      <TouchableOpacity style={styles.sectionHeader} onPress={() => toggleSection(sectionKey)} activeOpacity={0.7}>
        <View style={styles.sectionHeaderLeft}>
          <Text style={styles.sectionHeaderArrow}>{expanded ? '\u25BC' : '\u25B6'}</Text>
          <Text style={styles.sectionHeaderTitle}>{title}</Text>
        </View>
        <StatusBadge status={status} config={SECTION_STATUS_CONFIG} />
      </TouchableOpacity>
    );
  }

  /* ── Loading / Auth guard ────────────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  /* ── Render ──────────────────────────────────────────────────── */

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'\u2190'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>KYC Verification</Text>
        <View style={styles.backBtn} />
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        {/* Overall KYC status */}
        <View style={styles.overallStatus}>
          <Text style={styles.overallLabel}>KYC Status</Text>
          <StatusBadge status={kycStatus} config={KYC_STATUS_CONFIG} />
        </View>

        {kycStatus === 'REJECTED' && (
          <View style={styles.rejectedBanner}>
            <Text style={styles.rejectedText}>
              Your KYC was rejected. Please update the flagged sections and resubmit.
            </Text>
          </View>
        )}

        {/* ── Section 1: Identity ──────────────────────────────── */}
        <View style={styles.section}>
          <SectionHeader title="Identity Verification" sectionKey="identity" status={identityStatus} />
          {expandedSection === 'identity' && (
            <View style={styles.sectionBody}>
              <Text style={styles.label}>PAN Number *</Text>
              <TextInput
                style={styles.input}
                value={panNumber}
                onChangeText={setPanNumber}
                placeholder="ABCDE1234F"
                autoCapitalize="characters"
                maxLength={10}
              />
              <Text style={styles.label}>Aadhaar Number *</Text>
              <TextInput
                style={styles.input}
                value={aadhaarNumber}
                onChangeText={setAadhaarNumber}
                placeholder="1234 5678 9012"
                keyboardType="number-pad"
                maxLength={14}
              />
              <TouchableOpacity
                style={[styles.saveBtn, saving === 'identity' && styles.saveBtnDisabled]}
                onPress={saveIdentity}
                disabled={saving === 'identity'}
              >
                {saving === 'identity' ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.saveBtnText}>Save Identity</Text>
                )}
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* ── Section 2: Address ────────────────────────────────── */}
        <View style={styles.section}>
          <SectionHeader title="Address Verification" sectionKey="address" status={addressStatus} />
          {expandedSection === 'address' && (
            <View style={styles.sectionBody}>
              <Text style={styles.label}>Address Line 1 *</Text>
              <TextInput
                style={styles.input}
                value={addressLine1}
                onChangeText={setAddressLine1}
                placeholder="House/Flat number, Street"
              />
              <Text style={styles.label}>Address Line 2</Text>
              <TextInput
                style={styles.input}
                value={addressLine2}
                onChangeText={setAddressLine2}
                placeholder="Landmark, Area"
              />
              <View style={styles.row}>
                <View style={styles.halfField}>
                  <Text style={styles.label}>City *</Text>
                  <TextInput
                    style={styles.input}
                    value={city}
                    onChangeText={setCity}
                    placeholder="City"
                  />
                </View>
                <View style={styles.halfField}>
                  <Text style={styles.label}>State *</Text>
                  <TextInput
                    style={styles.input}
                    value={state}
                    onChangeText={setState}
                    placeholder="State"
                  />
                </View>
              </View>
              <Text style={styles.label}>Pincode *</Text>
              <TextInput
                style={styles.input}
                value={pincode}
                onChangeText={setPincode}
                placeholder="560001"
                keyboardType="number-pad"
                maxLength={6}
              />
              <TouchableOpacity
                style={[styles.saveBtn, saving === 'address' && styles.saveBtnDisabled]}
                onPress={saveAddress}
                disabled={saving === 'address'}
              >
                {saving === 'address' ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.saveBtnText}>Save Address</Text>
                )}
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* ── Section 3: Bank Details ──────────────────────────── */}
        <View style={styles.section}>
          <SectionHeader title="Bank Details" sectionKey="bank" status={bankStatus} />
          {expandedSection === 'bank' && (
            <View style={styles.sectionBody}>
              <Text style={styles.label}>Account Holder Name *</Text>
              <TextInput
                style={styles.input}
                value={accountHolderName}
                onChangeText={setAccountHolderName}
                placeholder="Full name as per bank"
              />
              <Text style={styles.label}>Account Number *</Text>
              <TextInput
                style={styles.input}
                value={accountNumber}
                onChangeText={setAccountNumber}
                placeholder="Account number"
                keyboardType="number-pad"
              />
              <View style={styles.row}>
                <View style={styles.halfField}>
                  <Text style={styles.label}>IFSC Code *</Text>
                  <TextInput
                    style={styles.input}
                    value={ifscCode}
                    onChangeText={setIfscCode}
                    placeholder="SBIN0001234"
                    autoCapitalize="characters"
                    maxLength={11}
                  />
                </View>
                <View style={styles.halfField}>
                  <Text style={styles.label}>Bank Name *</Text>
                  <TextInput
                    style={styles.input}
                    value={bankName}
                    onChangeText={setBankName}
                    placeholder="Bank name"
                  />
                </View>
              </View>
              <TouchableOpacity
                style={[styles.saveBtn, saving === 'bank' && styles.saveBtnDisabled]}
                onPress={saveBank}
                disabled={saving === 'bank'}
              >
                {saving === 'bank' ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.saveBtnText}>Save Bank Details</Text>
                )}
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* ── Section 4: Business (optional) ───────────────────── */}
        <View style={styles.section}>
          <SectionHeader title="Business Details (Optional)" sectionKey="business" status={businessStatus} />
          {expandedSection === 'business' && (
            <View style={styles.sectionBody}>
              <Text style={styles.label}>Business Name</Text>
              <TextInput
                style={styles.input}
                value={businessName}
                onChangeText={setBusinessName}
                placeholder="Registered business name"
              />
              <Text style={styles.label}>GSTIN</Text>
              <TextInput
                style={styles.input}
                value={gstin}
                onChangeText={setGstin}
                placeholder="22AAAAA0000A1Z5"
                autoCapitalize="characters"
                maxLength={15}
              />
              <Text style={styles.label}>Business Type</Text>
              <TextInput
                style={styles.input}
                value={businessType}
                onChangeText={setBusinessType}
                placeholder="Sole Proprietorship, Pvt Ltd, etc."
              />
              <TouchableOpacity
                style={[styles.saveBtn, saving === 'business' && styles.saveBtnDisabled]}
                onPress={saveBusiness}
                disabled={saving === 'business'}
              >
                {saving === 'business' ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.saveBtnText}>Save Business Details</Text>
                )}
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* ── Submit button ────────────────────────────────────── */}
        <TouchableOpacity
          style={[styles.submitBtn, !canSubmit && styles.submitBtnDisabled]}
          onPress={handleSubmitKyc}
          disabled={!canSubmit || submitting}
        >
          {submitting ? (
            <ActivityIndicator size="small" color="#fff" />
          ) : (
            <Text style={styles.submitBtnText}>
              {kycStatus === 'VERIFIED' ? 'KYC Verified' : kycStatus === 'SUBMITTED' ? 'Awaiting Verification' : 'Submit for Verification'}
            </Text>
          )}
        </TouchableOpacity>

        {!allRequiredSaved && kycStatus !== 'VERIFIED' && kycStatus !== 'SUBMITTED' && (
          <Text style={styles.hintText}>
            Save Identity, Address, and Bank Details before submitting.
          </Text>
        )}
      </ScrollView>
    </View>
  );
}

/* ── Styles ──────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  scroll: { flex: 1 },
  scrollContent: { padding: 16, paddingBottom: 40 },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 52,
    paddingBottom: 14,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 22, color: '#111827' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },

  /* Overall status */
  overallStatus: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#f9fafb',
    padding: 16,
    borderRadius: 12,
    marginBottom: 16,
  },
  overallLabel: { fontSize: 16, fontWeight: '600', color: '#374151' },

  /* Rejected banner */
  rejectedBanner: {
    backgroundColor: '#fef2f2',
    borderWidth: 1,
    borderColor: '#fecaca',
    borderRadius: 10,
    padding: 12,
    marginBottom: 16,
  },
  rejectedText: { fontSize: 13, color: '#991b1b', lineHeight: 18 },

  /* Badge */
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  badgeText: { fontSize: 12, fontWeight: '600' },

  /* Section */
  section: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    marginBottom: 12,
    overflow: 'hidden',
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#f9fafb',
  },
  sectionHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  sectionHeaderArrow: { fontSize: 12, color: '#6b7280', marginRight: 10, width: 14 },
  sectionHeaderTitle: { fontSize: 15, fontWeight: '600', color: '#111827' },
  sectionBody: { padding: 16, paddingTop: 8 },

  /* Form */
  label: { fontSize: 13, fontWeight: '500', color: '#374151', marginTop: 12, marginBottom: 6 },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: '#111827',
    backgroundColor: '#fff',
  },
  row: { flexDirection: 'row', gap: 12 },
  halfField: { flex: 1 },

  /* Save button */
  saveBtn: {
    backgroundColor: '#f97316',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 20,
  },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: { color: '#fff', fontSize: 15, fontWeight: '600' },

  /* Submit button */
  submitBtn: {
    backgroundColor: '#111827',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  submitBtnDisabled: { backgroundColor: '#9ca3af' },
  submitBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },

  /* Hint */
  hintText: { textAlign: 'center', fontSize: 13, color: '#6b7280', marginTop: 10 },
});
