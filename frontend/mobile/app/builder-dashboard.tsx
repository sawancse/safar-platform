import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, FlatList,
  StyleSheet, ActivityIndicator, RefreshControl, Alert, Modal, TextInput,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  DRAFT: { bg: '#f3f4f6', text: '#6b7280' },
  PENDING: { bg: '#fef3c7', text: '#92400e' },
  PUBLISHED: { bg: '#dcfce7', text: '#15803d' },
  UPCOMING: { bg: '#dbeafe', text: '#1d4ed8' },
  UNDER_CONSTRUCTION: { bg: '#fef3c7', text: '#92400e' },
  READY_TO_MOVE: { bg: '#dcfce7', text: '#15803d' },
  SOLD_OUT: { bg: '#fee2e2', text: '#dc2626' },
  PAUSED: { bg: '#fde68a', text: '#92400e' },
};

export default function BuilderDashboardScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [projects, setProjects] = useState<any[]>([]);
  const [stats, setStats] = useState({ total: 0, active: 0, totalUnits: 0, inquiries: 0 });

  // Post update modal
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [updateProjectId, setUpdateProjectId] = useState('');
  const [updateTitle, setUpdateTitle] = useState('');
  const [updateDesc, setUpdateDesc] = useState('');
  const [updateProgress, setUpdateProgress] = useState('');
  const [posting, setPosting] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const token = await getAccessToken();
      if (!token) { router.push('/auth'); return; }

      const data = await api.getMyBuilderProjects(token);
      const list = Array.isArray(data) ? data : (data?.content || data?.projects || []);
      setProjects(list);

      const activeCount = list.filter((p: any) =>
        ['PUBLISHED', 'UPCOMING', 'UNDER_CONSTRUCTION', 'READY_TO_MOVE'].includes(p.status)
      ).length;
      const totalUnits = list.reduce((sum: number, p: any) => sum + (p.totalUnits || 0), 0);
      const totalInquiries = list.reduce((sum: number, p: any) => sum + (p.inquiryCount || 0), 0);

      setStats({
        total: list.length,
        active: activeCount,
        totalUnits,
        inquiries: totalInquiries,
      });
    } catch {
      setProjects([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [router]);

  useEffect(() => { loadData(); }, [loadData]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadData();
  }, [loadData]);

  const handlePublish = useCallback(async (projectId: string) => {
    try {
      const token = await getAccessToken();
      if (!token) return;
      await api.publishBuilderProject(projectId, token);
      Alert.alert('Published', 'Project is now live.');
      loadData();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to publish');
    }
  }, [loadData]);

  const openUpdateModal = useCallback((projectId: string) => {
    setUpdateProjectId(projectId);
    setUpdateTitle('');
    setUpdateDesc('');
    setUpdateProgress('');
    setShowUpdateModal(true);
  }, []);

  const submitUpdate = useCallback(async () => {
    if (!updateTitle.trim()) { Alert.alert('Error', 'Title is required'); return; }
    setPosting(true);
    try {
      const token = await getAccessToken();
      if (!token) return;
      await api.addConstructionUpdate(updateProjectId, {
        title: updateTitle.trim(),
        description: updateDesc.trim() || undefined,
        progressPercent: updateProgress ? parseInt(updateProgress, 10) : undefined,
      }, token);
      setShowUpdateModal(false);
      Alert.alert('Success', 'Construction update posted.');
      loadData();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to post update');
    } finally {
      setPosting(false);
    }
  }, [updateProjectId, updateTitle, updateDesc, updateProgress, loadData]);

  const renderProject = ({ item }: { item: any }) => {
    const statusColor = STATUS_COLORS[item.status] || STATUS_COLORS.DRAFT;
    const progressPct = item.constructionProgress ?? 0;

    return (
      <TouchableOpacity
        style={styles.projectCard}
        onPress={() => router.push(`/project-detail/${item.id}`)}
        activeOpacity={0.85}
      >
        <View style={styles.projectCardHeader}>
          <View style={{ flex: 1 }}>
            <Text style={styles.projectName} numberOfLines={1}>{item.projectName || item.name}</Text>
            <Text style={styles.projectLocation} numberOfLines={1}>
              {[item.locality, item.city].filter(Boolean).join(', ')}
            </Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: statusColor.bg }]}>
            <Text style={[styles.statusBadgeText, { color: statusColor.text }]}>
              {(item.status || 'DRAFT').replace(/_/g, ' ')}
            </Text>
          </View>
        </View>

        {/* Progress bar */}
        <View style={styles.progressRow}>
          <View style={styles.progressBarBg}>
            <View style={[styles.progressBarFill, { width: `${Math.min(progressPct, 100)}%` }]} />
          </View>
          <Text style={styles.progressText}>{progressPct}%</Text>
        </View>

        {/* Metrics */}
        <View style={styles.metricsRow}>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.totalUnits ?? 0}</Text>
            <Text style={styles.metricLabel}>Units</Text>
          </View>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.totalTowers ?? 0}</Text>
            <Text style={styles.metricLabel}>Towers</Text>
          </View>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.unitTypeCount ?? 0}</Text>
            <Text style={styles.metricLabel}>Types</Text>
          </View>
          <View style={styles.metric}>
            <Text style={styles.metricValue}>{item.inquiryCount ?? 0}</Text>
            <Text style={styles.metricLabel}>Inquiries</Text>
          </View>
        </View>

        {/* Actions */}
        <View style={styles.actionsRow}>
          {item.status === 'DRAFT' && (
            <TouchableOpacity
              style={styles.actionBtn}
              onPress={() => handlePublish(item.id)}
            >
              <Text style={styles.actionBtnText}>Publish</Text>
            </TouchableOpacity>
          )}
          <TouchableOpacity
            style={[styles.actionBtn, styles.actionBtnOutline]}
            onPress={() => openUpdateModal(item.id)}
          >
            <Text style={[styles.actionBtnText, styles.actionBtnOutlineText]}>Post Update</Text>
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    );
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.container} edges={['top']}>
        <View style={styles.center}>
          <ActivityIndicator size="large" color={ORANGE} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Builder Dashboard</Text>
        <View style={{ width: 40 }} />
      </View>

      <FlatList
        data={projects}
        keyExtractor={item => item.id}
        renderItem={renderProject}
        contentContainerStyle={{ padding: 16, paddingBottom: 100 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
        ListHeaderComponent={
          <View>
            {/* Stats cards */}
            <View style={styles.statsGrid}>
              <View style={styles.statCard}>
                <Text style={styles.statValue}>{stats.total}</Text>
                <Text style={styles.statLabel}>Projects</Text>
              </View>
              <View style={styles.statCard}>
                <Text style={[styles.statValue, { color: '#15803d' }]}>{stats.active}</Text>
                <Text style={styles.statLabel}>Active</Text>
              </View>
              <View style={styles.statCard}>
                <Text style={styles.statValue}>{stats.totalUnits}</Text>
                <Text style={styles.statLabel}>Units</Text>
              </View>
              <View style={styles.statCard}>
                <Text style={[styles.statValue, { color: ORANGE }]}>{stats.inquiries}</Text>
                <Text style={styles.statLabel}>Inquiries</Text>
              </View>
            </View>

            {/* Add project button */}
            <TouchableOpacity
              style={styles.addProjectBtn}
              onPress={() => router.push('/sell')}
            >
              <Text style={styles.addProjectBtnText}>+ Add Project</Text>
            </TouchableOpacity>

            <Text style={styles.sectionTitle}>Your Projects</Text>
          </View>
        }
        ListEmptyComponent={
          <View style={styles.emptyBox}>
            <Text style={styles.emptyTitle}>No projects yet</Text>
            <Text style={styles.emptySubtitle}>Create your first builder project to get started</Text>
          </View>
        }
      />

      {/* Post construction update modal */}
      <Modal visible={showUpdateModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Post Construction Update</Text>

            <Text style={styles.inputLabel}>Title *</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="e.g. Foundation complete"
              placeholderTextColor="#9ca3af"
              value={updateTitle}
              onChangeText={setUpdateTitle}
            />

            <Text style={styles.inputLabel}>Description</Text>
            <TextInput
              style={[styles.modalInput, { height: 80, textAlignVertical: 'top' }]}
              placeholder="Details about the progress..."
              placeholderTextColor="#9ca3af"
              value={updateDesc}
              onChangeText={setUpdateDesc}
              multiline
            />

            <Text style={styles.inputLabel}>Progress (%)</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="e.g. 45"
              placeholderTextColor="#9ca3af"
              value={updateProgress}
              onChangeText={setUpdateProgress}
              keyboardType="numeric"
            />

            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setShowUpdateModal(false)}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalSubmitBtn}
                onPress={submitUpdate}
                disabled={posting}
              >
                {posting ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.modalSubmitText}>Post Update</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 22, color: ORANGE, fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  statsGrid: { flexDirection: 'row', gap: 10, marginBottom: 16 },
  statCard: {
    flex: 1, backgroundColor: '#f9fafb', borderRadius: 12, padding: 14, alignItems: 'center',
    borderWidth: 1, borderColor: '#e5e7eb',
  },
  statValue: { fontSize: 22, fontWeight: '800', color: '#111827' },
  statLabel: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  addProjectBtn: {
    backgroundColor: ORANGE, borderRadius: 12, paddingVertical: 14, alignItems: 'center',
    marginBottom: 20,
  },
  addProjectBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  sectionTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 12 },
  projectCard: {
    backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 14,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.06, shadowRadius: 8,
    elevation: 2, borderWidth: 1, borderColor: '#f3f4f6',
  },
  projectCardHeader: { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 10 },
  projectName: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 2 },
  projectLocation: { fontSize: 13, color: '#6b7280' },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12, marginLeft: 8 },
  statusBadgeText: { fontSize: 11, fontWeight: '600' },
  progressRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  progressBarBg: { flex: 1, height: 6, borderRadius: 3, backgroundColor: '#e5e7eb' },
  progressBarFill: { height: 6, borderRadius: 3, backgroundColor: ORANGE },
  progressText: { fontSize: 12, color: '#6b7280', marginLeft: 8, fontWeight: '600', width: 36 },
  metricsRow: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  metric: { flex: 1, alignItems: 'center' },
  metricValue: { fontSize: 16, fontWeight: '700', color: '#111827' },
  metricLabel: { fontSize: 11, color: '#9ca3af' },
  actionsRow: { flexDirection: 'row', gap: 10 },
  actionBtn: {
    flex: 1, backgroundColor: ORANGE, borderRadius: 8, paddingVertical: 10, alignItems: 'center',
  },
  actionBtnText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  actionBtnOutline: {
    backgroundColor: 'transparent', borderWidth: 1.5, borderColor: ORANGE,
  },
  actionBtnOutlineText: { color: ORANGE },
  emptyBox: { alignItems: 'center', paddingVertical: 48 },
  emptyTitle: { fontSize: 17, fontWeight: '600', color: '#374151', marginBottom: 6 },
  emptySubtitle: { fontSize: 14, color: '#9ca3af', textAlign: 'center' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalContent: {
    backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, paddingBottom: 32,
  },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  inputLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  modalInput: {
    height: 44, borderRadius: 10, backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#e5e7eb',
    paddingHorizontal: 14, fontSize: 14, color: '#111827', marginBottom: 14,
  },
  modalActions: { flexDirection: 'row', gap: 12, marginTop: 8 },
  modalCancelBtn: {
    flex: 1, borderWidth: 1.5, borderColor: '#d1d5db', borderRadius: 10,
    paddingVertical: 12, alignItems: 'center',
  },
  modalCancelText: { fontSize: 15, color: '#6b7280', fontWeight: '600' },
  modalSubmitBtn: {
    flex: 1, backgroundColor: ORANGE, borderRadius: 10, paddingVertical: 12, alignItems: 'center',
  },
  modalSubmitText: { color: '#fff', fontSize: 15, fontWeight: '700' },
});
