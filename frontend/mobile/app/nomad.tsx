import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const CATEGORIES = ['TIP', 'MEETUP', 'SKILL_SWAP', 'RECOMMENDATION', 'QUESTION'] as const;
type Category = (typeof CATEGORIES)[number];

const CATEGORY_META: Record<string, { label: string; icon: string }> = {
  TIP:            { label: 'Tips',            icon: '\uD83D\uDCA1' },
  MEETUP:         { label: 'Meetups',         icon: '\uD83E\uDD1D' },
  SKILL_SWAP:     { label: 'Skill Swap',      icon: '\uD83D\uDD04' },
  RECOMMENDATION: { label: 'Recommendations', icon: '\u2B50' },
  QUESTION:       { label: 'Questions',       icon: '\u2753' },
};

interface NomadPost {
  id: string;
  authorName: string;
  title: string;
  body: string;
  category: string;
  city: string;
  upvotes: number;
  commentCount: number;
  createdAt: string;
  comments?: { id: string; authorName: string; body: string; createdAt: string }[];
}

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 30) return `${days}d ago`;
  return `${Math.floor(days / 30)}mo ago`;
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

export default function NomadScreen() {
  const router = useRouter();
  const [posts, setPosts] = useState<NomadPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [expandedPostId, setExpandedPostId] = useState<string | null>(null);
  const [commentText, setCommentText] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);

  // New post modal
  const [newPostModal, setNewPostModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newBody, setNewBody] = useState('');
  const [newCategory, setNewCategory] = useState<string>('TIP');
  const [newCity, setNewCity] = useState('');
  const [submittingPost, setSubmittingPost] = useState(false);

  const fetchPosts = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.getNomadFeed('', selectedCategory || undefined);
      setPosts(result ?? []);
    } catch {
      setPosts([]);
    } finally {
      setLoading(false);
    }
  }, [selectedCategory]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  function handleToggleExpand(postId: string) {
    setExpandedPostId(expandedPostId === postId ? null : postId);
    setCommentText('');
  }

  async function handleAddComment(postId: string) {
    if (!commentText.trim()) return;
    const token = await getAccessToken();
    if (!token) {
      Alert.alert('Sign in required', 'Please sign in to comment.');
      router.push('/auth');
      return;
    }
    setSubmittingComment(true);
    try {
      await api.addComment(postId, commentText.trim(), token);
      setCommentText('');
      // Refresh feed to get updated comment count
      fetchPosts();
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to add comment');
    } finally {
      setSubmittingComment(false);
    }
  }

  function openNewPostModal() {
    setNewTitle('');
    setNewBody('');
    setNewCategory('TIP');
    setNewCity('');
    setNewPostModal(true);
  }

  async function handleSubmitPost() {
    if (!newTitle.trim() || !newBody.trim() || !newCity.trim()) {
      Alert.alert('Missing fields', 'Please fill in title, body, and city.');
      return;
    }
    const token = await getAccessToken();
    if (!token) {
      Alert.alert('Sign in required', 'Please sign in to create a post.');
      setNewPostModal(false);
      router.push('/auth');
      return;
    }
    setSubmittingPost(true);
    try {
      await api.createNomadPost(
        { title: newTitle.trim(), body: newBody.trim(), category: newCategory, city: newCity.trim() },
        token,
      );
      setNewPostModal(false);
      Alert.alert('Posted!', 'Your post has been published.');
      fetchPosts();
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to create post');
    } finally {
      setSubmittingPost(false);
    }
  }

  function renderPost({ item }: { item: NomadPost }) {
    const meta = CATEGORY_META[item.category] ?? { label: item.category, icon: '\uD83D\uDCCC' };
    const isExpanded = expandedPostId === item.id;

    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.85}
        onPress={() => handleToggleExpand(item.id)}
      >
        {/* Author row */}
        <View style={styles.authorRow}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{getInitials(item.authorName)}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.authorName}>{item.authorName}</Text>
            <Text style={styles.timeAgo}>{timeAgo(item.createdAt)}</Text>
          </View>
          <View style={styles.categoryBadge}>
            <Text style={styles.categoryText}>{meta.icon} {meta.label}</Text>
          </View>
        </View>

        {/* Title & body */}
        <Text style={styles.cardTitle}>{item.title}</Text>
        <Text style={styles.cardBody} numberOfLines={isExpanded ? undefined : 3}>
          {item.body}
        </Text>

        {/* City tag + stats */}
        <View style={styles.cardFooter}>
          <View style={styles.cityTag}>
            <Text style={styles.cityText}>{'\uD83D\uDCCD'} {item.city}</Text>
          </View>
          <View style={styles.statsRow}>
            <Text style={styles.statText}>{'\u25B2'} {item.upvotes}</Text>
            <Text style={styles.statText}>{'\uD83D\uDCAC'} {item.commentCount}</Text>
          </View>
        </View>

        {/* Expanded: comments section */}
        {isExpanded && (
          <View style={styles.commentsSection}>
            <View style={styles.commentDivider} />
            {item.comments && item.comments.length > 0 ? (
              item.comments.map((c) => (
                <View key={c.id} style={styles.commentItem}>
                  <View style={styles.commentAvatarSmall}>
                    <Text style={styles.commentAvatarText}>{getInitials(c.authorName)}</Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.commentAuthor}>{c.authorName}</Text>
                    <Text style={styles.commentBody}>{c.body}</Text>
                  </View>
                </View>
              ))
            ) : (
              <Text style={styles.noComments}>No comments yet. Be the first!</Text>
            )}

            {/* Comment input */}
            <View style={styles.commentInputRow}>
              <TextInput
                style={styles.commentInput}
                placeholder="Write a comment..."
                placeholderTextColor="#9ca3af"
                value={commentText}
                onChangeText={setCommentText}
              />
              <TouchableOpacity
                style={[styles.commentSendBtn, submittingComment && { opacity: 0.5 }]}
                onPress={() => handleAddComment(item.id)}
                disabled={submittingComment}
              >
                {submittingComment ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.commentSendText}>{'\u2191'}</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        )}
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      {/* Category filter chips */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.filterRow}
      >
        <TouchableOpacity
          style={[styles.filterChip, !selectedCategory && styles.filterChipActive]}
          onPress={() => setSelectedCategory('')}
        >
          <Text style={[styles.filterChipText, !selectedCategory && styles.filterChipTextActive]}>
            All
          </Text>
        </TouchableOpacity>
        {CATEGORIES.map((cat) => {
          const meta = CATEGORY_META[cat];
          return (
            <TouchableOpacity
              key={cat}
              style={[styles.filterChip, selectedCategory === cat && styles.filterChipActive]}
              onPress={() => setSelectedCategory(selectedCategory === cat ? '' : cat)}
            >
              <Text style={[styles.filterChipText, selectedCategory === cat && styles.filterChipTextActive]}>
                {meta.icon} {meta.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>

      {/* Post list */}
      {loading ? (
        <ActivityIndicator color="#f97316" style={{ marginTop: 40 }} size="large" />
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(item) => item.id}
          renderItem={renderPost}
          contentContainerStyle={posts.length === 0 ? styles.emptyContainer : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>{'\uD83C\uDF0D'}</Text>
              <Text style={styles.emptyTitle}>No posts yet</Text>
              <Text style={styles.emptySubtitle}>Be the first to share with the community</Text>
            </View>
          }
        />
      )}

      {/* FAB - New Post */}
      <TouchableOpacity style={styles.fab} onPress={openNewPostModal}>
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      {/* New Post Modal */}
      <Modal
        visible={newPostModal}
        transparent
        animationType="slide"
        onRequestClose={() => setNewPostModal(false)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          style={styles.modalOverlay}
        >
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>New Post</Text>

            {/* Title input */}
            <TextInput
              style={styles.modalInput}
              placeholder="Title"
              placeholderTextColor="#9ca3af"
              value={newTitle}
              onChangeText={setNewTitle}
            />

            {/* Body input */}
            <TextInput
              style={[styles.modalInput, styles.modalTextarea]}
              placeholder="What's on your mind?"
              placeholderTextColor="#9ca3af"
              multiline
              numberOfLines={5}
              textAlignVertical="top"
              value={newBody}
              onChangeText={setNewBody}
            />

            {/* Category picker */}
            <Text style={styles.modalLabel}>Category</Text>
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.categoryPickerRow}
            >
              {CATEGORIES.map((cat) => {
                const meta = CATEGORY_META[cat];
                return (
                  <TouchableOpacity
                    key={cat}
                    style={[styles.filterChip, newCategory === cat && styles.filterChipActive]}
                    onPress={() => setNewCategory(cat)}
                  >
                    <Text style={[styles.filterChipText, newCategory === cat && styles.filterChipTextActive]}>
                      {meta.icon} {meta.label}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>

            {/* City input */}
            <TextInput
              style={styles.modalInput}
              placeholder="City (e.g. Mumbai, Goa)"
              placeholderTextColor="#9ca3af"
              value={newCity}
              onChangeText={setNewCity}
            />

            {/* Actions */}
            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setNewPostModal(false)}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalSubmitBtn, submittingPost && { opacity: 0.5 }]}
                onPress={handleSubmitPost}
                disabled={submittingPost}
              >
                {submittingPost ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.modalSubmitText}>Submit</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container:            { flex: 1, backgroundColor: '#f9fafb' },
  filterRow:            { flexDirection: 'row', gap: 8, padding: 16, paddingBottom: 8, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  filterChip:           { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb' },
  filterChipActive:     { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterChipText:       { fontSize: 12, fontWeight: '600', color: '#374151' },
  filterChipTextActive: { color: '#fff' },

  list:                 { padding: 16, paddingBottom: 80 },
  emptyContainer:       { flex: 1 },

  // Post card
  card:                 { backgroundColor: '#fff', borderRadius: 16, padding: 14, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  authorRow:            { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  avatar:               { width: 36, height: 36, borderRadius: 18, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center', marginRight: 10 },
  avatarText:           { fontSize: 13, fontWeight: '700', color: '#c2410c' },
  authorName:           { fontSize: 13, fontWeight: '600', color: '#111827' },
  timeAgo:              { fontSize: 11, color: '#9ca3af' },
  categoryBadge:        { backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  categoryText:         { fontSize: 10, fontWeight: '700', color: '#c2410c' },
  cardTitle:            { fontSize: 15, fontWeight: '700', color: '#111827', marginBottom: 4 },
  cardBody:             { fontSize: 13, color: '#374151', lineHeight: 19 },
  cardFooter:           { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 },
  cityTag:              { backgroundColor: '#f3f4f6', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  cityText:             { fontSize: 11, color: '#6b7280' },
  statsRow:             { flexDirection: 'row', gap: 12 },
  statText:             { fontSize: 12, color: '#6b7280', fontWeight: '500' },

  // Expanded comments
  commentsSection:      { marginTop: 10 },
  commentDivider:       { height: 1, backgroundColor: '#f3f4f6', marginBottom: 10 },
  commentItem:          { flexDirection: 'row', alignItems: 'flex-start', marginBottom: 10, gap: 8 },
  commentAvatarSmall:   { width: 26, height: 26, borderRadius: 13, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  commentAvatarText:    { fontSize: 10, fontWeight: '700', color: '#6b7280' },
  commentAuthor:        { fontSize: 12, fontWeight: '600', color: '#111827' },
  commentBody:          { fontSize: 12, color: '#374151', marginTop: 1 },
  noComments:           { fontSize: 12, color: '#9ca3af', fontStyle: 'italic', marginBottom: 10 },
  commentInputRow:      { flexDirection: 'row', gap: 8, marginTop: 4 },
  commentInput:         { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 8, fontSize: 13, color: '#111827', backgroundColor: '#f9fafb' },
  commentSendBtn:       { backgroundColor: '#f97316', borderRadius: 10, width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
  commentSendText:      { fontSize: 18, fontWeight: '700', color: '#fff' },

  // FAB
  fab:                  { position: 'absolute', bottom: 24, right: 20, width: 56, height: 56, borderRadius: 28, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center', elevation: 6, shadowColor: '#000', shadowOffset: { width: 0, height: 3 }, shadowOpacity: 0.25, shadowRadius: 4 },
  fabText:              { fontSize: 28, fontWeight: '600', color: '#fff', marginTop: -2 },

  // Empty state
  empty:                { alignItems: 'center', paddingTop: 80 },
  emptyIcon:            { fontSize: 48, marginBottom: 12 },
  emptyTitle:           { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:        { fontSize: 14, color: '#9ca3af', marginTop: 4 },

  // New post modal
  modalOverlay:         { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContent:         { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32 },
  modalTitle:           { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  modalLabel:           { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 8 },
  modalInput:           { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb', marginBottom: 12 },
  modalTextarea:        { minHeight: 100 },
  categoryPickerRow:    { flexDirection: 'row', gap: 8, marginBottom: 12 },
  modalActions:         { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 8 },
  modalCancelBtn:       { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 20, paddingVertical: 10 },
  modalCancelText:      { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  modalSubmitBtn:       { backgroundColor: '#f97316', borderRadius: 10, paddingHorizontal: 20, paddingVertical: 10 },
  modalSubmitText:      { fontSize: 14, fontWeight: '700', color: '#fff' },
});
