import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  FlatList,
  Alert,
  Modal,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import * as Linking from 'expo-linking';

/* ── Types ─────────────────────────────────────────────────── */

type AgreementStatus = 'DRAFT' | 'PENDING_HOST_SIGN' | 'PENDING_TENANT_SIGN' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';

interface Agreement {
  agreementNumber: string;
  status: AgreementStatus;
  tenantName: string;
  hostName: string;
  propertyAddress: string;
  moveInDate: string;
  monthlyRentPaise: number;
  securityDepositPaise: number;
  lockInPeriodMonths: number;
  noticePeriodDays: number;
  hostSignedAt: string | null;
  tenantSignedAt: string | null;
  pdfUrl: string | null;
}

/* ── Constants ─────────────────────────────────────────────── */

const STEPS: { key: string; label: string }[] = [
  { key: 'DRAFT', label: 'Draft' },
  { key: 'PENDING_HOST_SIGN', label: 'Host Sign' },
  { key: 'PENDING_TENANT_SIGN', label: 'Tenant Sign' },
  { key: 'ACTIVE', label: 'Active' },
];

const STATUS_INDEX: Record<string, number> = {
  DRAFT: 0,
  PENDING_HOST_SIGN: 1,
  PENDING_TENANT_SIGN: 2,
  ACTIVE: 3,
  EXPIRED: 3,
  TERMINATED: 3,
};

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  DRAFT:               { bg: '#f3f4f6', text: '#374151' },
  PENDING_HOST_SIGN:   { bg: '#fef9c3', text: '#854d0e' },
  PENDING_TENANT_SIGN: { bg: '#fef9c3', text: '#854d0e' },
  ACTIVE:              { bg: '#dcfce7', text: '#14532d' },
  EXPIRED:             { bg: '#fee2e2', text: '#7f1d1d' },
  TERMINATED:          { bg: '#fee2e2', text: '#7f1d1d' },
};

/* ── Helpers ───────────────────────────────────────────────── */

const formatPaise = (p: number) => '₹' + (p / 100).toLocaleString('en-IN');

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/* ── Component ─────────────────────────────────────────────── */

export default function PGAgreementScreen() {
  const router = useRouter();

  const [agreement, setAgreement] = useState<Agreement | null>(null);
  const [agreementText, setAgreementText] = useState<string>('');
  const [tenancyId, setTenancyId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [signing, setSigning] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAgreement = useCallback(async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);

      const token = await getAccessToken();
      if (!token) { router.replace('/auth'); return; }

      const dashboard = await api.getTenantDashboard(token);
      const tId = dashboard.data?.id || dashboard.data?.tenancyId;
      if (!tId) { setError('No active tenancy found'); return; }
      setTenancyId(tId);

      const [agRes, textRes] = await Promise.all([
        api.getAgreement(tId, token),
        api.getAgreementText(tId, token),
      ]);

      setAgreement(agRes.data);
      setAgreementText(typeof textRes.data === 'string' ? textRes.data : textRes.data?.text || '');
    } catch (e: any) {
      setError(e?.response?.data?.detail || e.message || 'Failed to load agreement');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [router]);

  useEffect(() => { fetchAgreement(); }, [fetchAgreement]);

  const handleSign = () => {
    Alert.alert(
      'Sign Agreement',
      'By signing, you agree to all terms and conditions in this rental agreement. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Sign Now',
          style: 'default',
          onPress: async () => {
            try {
              setSigning(true);
              const token = await getAccessToken();
              if (!token || !tenancyId) return;
              await api.tenantSignAgreement(tenancyId, token);
              Alert.alert('Success', 'Agreement signed successfully!');
              fetchAgreement(true);
            } catch (e: any) {
              Alert.alert('Error', e?.response?.data?.detail || 'Failed to sign agreement');
            } finally {
              setSigning(false);
            }
          },
        },
      ],
    );
  };

  const handleDownloadPdf = () => {
    if (agreement?.pdfUrl) {
      Linking.openURL(agreement.pdfUrl);
    } else if (tenancyId) {
      // Use the server-generated PDF endpoint
      Linking.openURL(`/api/v1/pg-tenancies/${tenancyId}/agreement/pdf`);
    }
  };

  const handleViewAgreement = () => {
    if (tenancyId) {
      Linking.openURL(`/api/v1/pg-tenancies/${tenancyId}/agreement/view`);
    }
  };

  /* ── Renders ─────────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#F97316" />
        <Text style={styles.loadingText}>Loading agreement...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryBtn} onPress={() => fetchAgreement()}>
          <Text style={styles.retryText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (!agreement) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>📋</Text>
        <Text style={styles.emptyTitle}>No Agreement</Text>
        <Text style={styles.emptyDesc}>
          A rental agreement has not been created for your tenancy yet.
        </Text>
      </View>
    );
  }

  const currentStep = STATUS_INDEX[agreement.status] ?? 0;
  const sc = STATUS_COLORS[agreement.status] || STATUS_COLORS.DRAFT;

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'<'} Back</Text>
        </TouchableOpacity>
        <View style={styles.headerRow}>
          <Text style={styles.title}>Rental Agreement</Text>
          <View style={[styles.badge, { backgroundColor: sc.bg }]}>
            <Text style={[styles.badgeText, { color: sc.text }]}>
              {agreement.status.replace(/_/g, ' ')}
            </Text>
          </View>
        </View>
        <Text style={styles.agreementNum}>{agreement.agreementNumber}</Text>
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => fetchAgreement(true)}
            colors={['#F97316']}
          />
        }
      >
        {/* Progress Steps */}
        <View style={styles.stepsContainer}>
          {STEPS.map((step, i) => {
            const completed = i <= currentStep;
            const isLast = i === STEPS.length - 1;
            return (
              <View key={step.key} style={styles.stepWrapper}>
                <View style={styles.stepRow}>
                  <View
                    style={[
                      styles.stepCircle,
                      completed && styles.stepCircleActive,
                    ]}
                  >
                    <Text
                      style={[
                        styles.stepNum,
                        completed && styles.stepNumActive,
                      ]}
                    >
                      {completed ? '✓' : i + 1}
                    </Text>
                  </View>
                  <Text
                    style={[
                      styles.stepLabel,
                      completed && styles.stepLabelActive,
                    ]}
                  >
                    {step.label}
                  </Text>
                </View>
                {!isLast && (
                  <View
                    style={[
                      styles.stepLine,
                      i < currentStep && styles.stepLineActive,
                    ]}
                  />
                )}
              </View>
            );
          })}
        </View>

        {/* Details Card */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Agreement Details</Text>
          <DetailRow label="Tenant" value={agreement.tenantName} />
          <DetailRow label="Host" value={agreement.hostName} />
          <DetailRow label="Property" value={agreement.propertyAddress} />
          <DetailRow label="Move-in Date" value={formatDate(agreement.moveInDate)} />
          <DetailRow label="Monthly Rent" value={formatPaise(agreement.monthlyRentPaise)} highlight />
          <DetailRow label="Security Deposit" value={formatPaise(agreement.securityDepositPaise)} />
          <DetailRow label="Lock-in Period" value={`${agreement.lockInPeriodMonths} months`} />
          <DetailRow label="Notice Period" value={`${agreement.noticePeriodDays} days`} />
        </View>

        {/* Signature Status */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Signatures</Text>
          <View style={styles.signRow}>
            <View style={styles.signBlock}>
              <Text style={styles.signLabel}>Host</Text>
              {agreement.hostSignedAt ? (
                <>
                  <Text style={styles.signedBadge}>Signed</Text>
                  <Text style={styles.signDate}>{formatDateTime(agreement.hostSignedAt)}</Text>
                </>
              ) : (
                <Text style={styles.pendingBadge}>Pending</Text>
              )}
            </View>
            <View style={styles.signDivider} />
            <View style={styles.signBlock}>
              <Text style={styles.signLabel}>Tenant</Text>
              {agreement.tenantSignedAt ? (
                <>
                  <Text style={styles.signedBadge}>Signed</Text>
                  <Text style={styles.signDate}>{formatDateTime(agreement.tenantSignedAt)}</Text>
                </>
              ) : (
                <Text style={styles.pendingBadge}>Pending</Text>
              )}
            </View>
          </View>
        </View>

        {/* Agreement Text */}
        {agreementText ? (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Agreement Terms</Text>
            <ScrollView
              style={styles.textScroll}
              nestedScrollEnabled
            >
              <Text style={styles.agreementTextContent}>{agreementText}</Text>
            </ScrollView>
          </View>
        ) : null}

        {/* Actions */}
        {agreement.status === 'PENDING_TENANT_SIGN' && (
          <TouchableOpacity
            style={[styles.signBtn, signing && styles.signBtnDisabled]}
            onPress={handleSign}
            disabled={signing}
          >
            {signing ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.signBtnText}>Sign Agreement</Text>
            )}
          </TouchableOpacity>
        )}

        {/* View & Download buttons — show for any non-DRAFT status */}
        {agreement.status !== 'DRAFT' && agreement.status !== 'NOT_CREATED' && (
          <View style={{ flexDirection: 'row', gap: 10, marginTop: 12 }}>
            <TouchableOpacity
              style={[styles.downloadBtn, { flex: 1, backgroundColor: '#f3f4f6' }]}
              onPress={handleViewAgreement}
            >
              <Text style={[styles.downloadBtnText, { color: '#374151' }]}>View Agreement</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.downloadBtn, { flex: 1 }]} onPress={handleDownloadPdf}>
              <Text style={styles.downloadBtnText}>Download PDF</Text>
            </TouchableOpacity>
          </View>
        )}

        <View style={{ height: 32 }} />
      </ScrollView>
    </View>
  );
}

/* ── Detail Row Sub-component ──────────────────────────────── */

function DetailRow({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <View style={styles.detailRow}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text
        style={[styles.detailValue, highlight && { color: '#F97316', fontWeight: '700' }]}
        numberOfLines={2}
      >
        {value}
      </Text>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  loadingText: { marginTop: 12, color: '#6b7280', fontSize: 14 },
  errorText: { color: '#dc2626', fontSize: 15, textAlign: 'center', marginBottom: 16 },
  retryBtn: {
    backgroundColor: '#F97316',
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 8,
  },
  retryText: { color: '#fff', fontWeight: '600', fontSize: 15 },

  /* Empty */
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 8 },
  emptyDesc: { fontSize: 14, color: '#6b7280', textAlign: 'center', lineHeight: 20 },

  /* Header */
  header: {
    backgroundColor: '#fff',
    paddingTop: 56,
    paddingBottom: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backBtn: { marginBottom: 8 },
  backText: { color: '#F97316', fontSize: 15, fontWeight: '500' },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: { fontSize: 22, fontWeight: '700', color: '#111827' },
  agreementNum: { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase' },

  /* Content */
  content: { padding: 16 },

  /* Progress Steps */
  stepsContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  stepWrapper: {
    flex: 1,
    alignItems: 'center',
  },
  stepRow: { alignItems: 'center' },
  stepCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#e5e7eb',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 6,
  },
  stepCircleActive: { backgroundColor: '#F97316' },
  stepNum: { fontSize: 13, fontWeight: '700', color: '#9ca3af' },
  stepNumActive: { color: '#fff' },
  stepLabel: { fontSize: 11, color: '#9ca3af', textAlign: 'center' },
  stepLabelActive: { color: '#F97316', fontWeight: '600' },
  stepLine: {
    position: 'absolute',
    top: 16,
    right: -20,
    width: 40,
    height: 2,
    backgroundColor: '#e5e7eb',
    zIndex: -1,
  },
  stepLineActive: { backgroundColor: '#F97316' },

  /* Card */
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 12,
  },

  /* Detail Rows */
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  detailLabel: { fontSize: 13, color: '#6b7280', flex: 1 },
  detailValue: { fontSize: 13, color: '#111827', fontWeight: '500', flex: 1, textAlign: 'right' },

  /* Signatures */
  signRow: { flexDirection: 'row', alignItems: 'center' },
  signBlock: { flex: 1, alignItems: 'center', paddingVertical: 8 },
  signDivider: { width: 1, height: 60, backgroundColor: '#e5e7eb' },
  signLabel: { fontSize: 13, color: '#6b7280', marginBottom: 6 },
  signedBadge: {
    fontSize: 13,
    fontWeight: '700',
    color: '#16a34a',
    marginBottom: 2,
  },
  pendingBadge: {
    fontSize: 13,
    fontWeight: '600',
    color: '#9ca3af',
  },
  signDate: { fontSize: 11, color: '#9ca3af' },

  /* Agreement Text */
  textScroll: { maxHeight: 300 },
  agreementTextContent: {
    fontSize: 12,
    color: '#374151',
    lineHeight: 18,
    fontFamily: 'monospace',
  },

  /* Buttons */
  signBtn: {
    backgroundColor: '#F97316',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 4,
  },
  signBtnDisabled: { opacity: 0.6 },
  signBtnText: { color: '#fff', fontSize: 17, fontWeight: '700' },

  downloadBtn: {
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#F97316',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 4,
  },
  downloadBtnText: { color: '#F97316', fontSize: 16, fontWeight: '700' },
});
