import { useEffect, useState, useCallback, useRef } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Modal,
  ScrollView,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────── */

interface Conversation {
  id: string;
  participant1Id: string;
  participant2Id: string;
  listingId: string;
  bookingId?: string;
  lastMessageText?: string;
  lastMessageAt?: string;
  unreadCount: number;
  otherParticipantName?: string;
  listingTitle?: string;
  bookingRef?: string;
}

interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  messageType: 'TEXT' | 'SYSTEM' | 'BOOKING_UPDATE';
  readAt?: string;
  createdAt: string;
}

interface QuickReply {
  id: string;
  label: string;
  text: string;
}

/* ── Helpers ───────────────────────────────────────────────── */

function timeAgo(iso?: string): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'now';
  if (mins < 60) return `${mins}m`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h`;
  const days = Math.floor(hrs / 24);
  return `${days}d`;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}

/* ── Default Quick Reply Templates ─────────────────────────── */

const DEFAULT_TEMPLATES: { label: string; text: string }[] = [
  { label: 'Check-in instructions', text: 'Hi! Check-in is between 2 PM and 8 PM. Please share your expected arrival time so I can arrange the keys.' },
  { label: 'WiFi details', text: 'WiFi Network: SafarStay_Guest\nPassword: welcome2024\nPlease let me know if you face any connectivity issues.' },
  { label: 'Directions', text: 'From the main road, take the first left after the petrol pump. Our property is the third building on the right with a blue gate. I\'ll share my location pin as well.' },
  { label: 'House rules', text: 'A few house rules:\n- No smoking indoors\n- Quiet hours: 10 PM - 7 AM\n- No outside guests without prior notice\n- Please keep the property clean\nThank you for understanding!' },
];

/* ── Component ─────────────────────────────────────────────── */

export default function HostMessagesScreen() {
  const router = useRouter();

  // Core state
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedConvo, setSelectedConvo] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [userId, setUserId] = useState('');
  const flatListRef = useRef<FlatList>(null);

  // Quick replies
  const [quickReplies, setQuickReplies] = useState<QuickReply[]>([]);
  const [quickRepliesLoading, setQuickRepliesLoading] = useState(false);

  // Quick reply management modal
  const [manageModalVisible, setManageModalVisible] = useState(false);
  const [newTemplateLabel, setNewTemplateLabel] = useState('');
  const [newTemplateText, setNewTemplateText] = useState('');
  const [addingTemplate, setAddingTemplate] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  /* ── Data Loading ──────────────────────────────────────── */

  const loadConversations = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    try {
      const convos = await api.getConversations(token);
      setConversations(convos);
    } catch {
      setConversations([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadMessages = useCallback(async (convoId: string, page = 0) => {
    const token = await getAccessToken();
    if (!token) return;
    setMessagesLoading(true);
    try {
      const res = await api.getMessages(convoId, token, page);
      const msgs = Array.isArray(res) ? res : res?.content ?? [];
      setMessages(msgs);
      api.markAsRead(convoId, token).catch(() => {});
    } catch {
      setMessages([]);
    } finally {
      setMessagesLoading(false);
    }
  }, []);

  const loadQuickReplies = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) return;
    setQuickRepliesLoading(true);
    try {
      const replies = await api.getQuickReplies(token);
      setQuickReplies(Array.isArray(replies) ? replies : []);
    } catch {
      setQuickReplies([]);
    } finally {
      setQuickRepliesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadConversations();
    loadQuickReplies();
    (async () => {
      try {
        const AsyncStorage = require('@react-native-async-storage/async-storage').default;
        const uid = await AsyncStorage.getItem('user_id');
        if (uid) setUserId(uid);
      } catch {}
    })();
  }, [loadConversations, loadQuickReplies]);

  // Poll every 10s
  useEffect(() => {
    const interval = setInterval(() => {
      loadConversations();
      if (selectedConvo) loadMessages(selectedConvo.id);
    }, 10000);
    return () => clearInterval(interval);
  }, [selectedConvo, loadConversations, loadMessages]);

  /* ── Handlers ──────────────────────────────────────────── */

  function handleSelectConversation(convo: Conversation) {
    setSelectedConvo(convo);
    loadMessages(convo.id);
    setConversations(prev => prev.map(c => c.id === convo.id ? { ...c, unreadCount: 0 } : c));
  }

  async function handleSend() {
    if (!newMessage.trim() || !selectedConvo || sending) return;
    const token = await getAccessToken();
    if (!token) return;
    setSending(true);
    try {
      const recipientId = selectedConvo.participant1Id === userId
        ? selectedConvo.participant2Id
        : selectedConvo.participant1Id;
      await api.sendMessage({
        listingId: selectedConvo.listingId,
        recipientId,
        bookingId: selectedConvo.bookingId,
        content: newMessage.trim(),
      }, token);
      setNewMessage('');
      await loadMessages(selectedConvo.id);
      await loadConversations();
    } catch {}
    finally { setSending(false); }
  }

  function handleBack() {
    if (selectedConvo) {
      setSelectedConvo(null);
      setMessages([]);
    } else {
      router.back();
    }
  }

  function handleQuickReplyTap(text: string) {
    setNewMessage(prev => prev ? `${prev}\n${text}` : text);
  }

  async function handleAddTemplate() {
    if (!newTemplateLabel.trim() || !newTemplateText.trim()) {
      Alert.alert('Missing fields', 'Please enter both a label and template text.');
      return;
    }
    const token = await getAccessToken();
    if (!token) return;
    setAddingTemplate(true);
    try {
      await api.createQuickReply({ label: newTemplateLabel.trim(), text: newTemplateText.trim() }, token);
      setNewTemplateLabel('');
      setNewTemplateText('');
      await loadQuickReplies();
    } catch {
      Alert.alert('Error', 'Failed to create quick reply.');
    } finally {
      setAddingTemplate(false);
    }
  }

  async function handleDeleteTemplate(id: string) {
    Alert.alert('Delete template', 'Are you sure you want to delete this quick reply?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          const token = await getAccessToken();
          if (!token) return;
          setDeletingId(id);
          try {
            await api.deleteQuickReply(id, token);
            await loadQuickReplies();
          } catch {
            Alert.alert('Error', 'Failed to delete quick reply.');
          } finally {
            setDeletingId(null);
          }
        },
      },
    ]);
  }

  /* ── Loading State ─────────────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  /* ── Chat View ─────────────────────────────────────────── */

  if (selectedConvo) {
    return (
      <KeyboardAvoidingView
        style={{ flex: 1, backgroundColor: '#fff' }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={90}
      >
        {/* Header */}
        <View style={styles.chatHeader}>
          <TouchableOpacity onPress={handleBack} style={styles.backBtn}>
            <Text style={styles.backText}>{'\u2039'}</Text>
          </TouchableOpacity>
          <View style={styles.chatHeaderInfo}>
            <Text style={styles.chatHeaderName}>{selectedConvo.otherParticipantName || 'Guest'}</Text>
            <Text style={styles.chatHeaderListing} numberOfLines={1}>
              {selectedConvo.listingTitle || ''}
              {selectedConvo.bookingRef ? ` \u00B7 ${selectedConvo.bookingRef}` : ''}
            </Text>
          </View>
        </View>

        {/* Messages */}
        {messagesLoading ? (
          <View style={styles.center}>
            <ActivityIndicator color="#f97316" />
          </View>
        ) : messages.length === 0 ? (
          <View style={styles.center}>
            <Text style={styles.emptyText}>No messages yet. Use a quick reply to start!</Text>
          </View>
        ) : (
          <FlatList
            ref={flatListRef}
            data={messages}
            keyExtractor={(item) => item.id}
            contentContainerStyle={styles.messagesList}
            onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
            renderItem={({ item }) => {
              const isMine = item.senderId === userId;
              const isSystem = item.messageType === 'SYSTEM' || item.messageType === 'BOOKING_UPDATE';

              if (isSystem) {
                return (
                  <View style={styles.systemMsg}>
                    <Text style={styles.systemMsgText}>{item.content}</Text>
                  </View>
                );
              }

              return (
                <View style={[styles.bubble, isMine ? styles.bubbleMine : styles.bubbleTheirs]}>
                  <Text style={[styles.bubbleText, isMine && { color: '#fff' }]}>{item.content}</Text>
                  <Text style={[styles.bubbleTime, isMine && { color: 'rgba(255,255,255,0.7)' }]}>
                    {formatTime(item.createdAt)}
                    {isMine && (item.readAt ? ' \u2713\u2713' : ' \u2713')}
                  </Text>
                </View>
              );
            }}
          />
        )}

        {/* Quick Reply Bar */}
        <View style={styles.quickReplyBar}>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.quickReplyScroll}
          >
            {quickReplies.map((qr) => (
              <TouchableOpacity
                key={qr.id}
                style={styles.quickReplyChip}
                onPress={() => handleQuickReplyTap(qr.text)}
              >
                <Text style={styles.quickReplyChipText}>{qr.label}</Text>
              </TouchableOpacity>
            ))}
            {quickReplies.length === 0 && !quickRepliesLoading && (
              <Text style={styles.noQuickReplies}>No quick replies yet</Text>
            )}
            <TouchableOpacity
              style={styles.manageChip}
              onPress={() => setManageModalVisible(true)}
            >
              <Text style={styles.manageChipText}>Manage</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>

        {/* Input Bar */}
        <View style={styles.inputBar}>
          <TextInput
            style={styles.textInput}
            value={newMessage}
            onChangeText={setNewMessage}
            placeholder="Type a message..."
            placeholderTextColor="#9ca3af"
            multiline
            returnKeyType="send"
            onSubmitEditing={handleSend}
          />
          <TouchableOpacity
            style={[styles.sendBtn, (!newMessage.trim() || sending) && { opacity: 0.5 }]}
            onPress={handleSend}
            disabled={!newMessage.trim() || sending}
          >
            {sending ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.sendBtnText}>{'\u2191'}</Text>
            )}
          </TouchableOpacity>
        </View>

        {/* Quick Reply Management Modal */}
        <Modal
          visible={manageModalVisible}
          animationType="slide"
          presentationStyle="pageSheet"
          onRequestClose={() => setManageModalVisible(false)}
        >
          <View style={styles.modalContainer}>
            {/* Modal Header */}
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Quick Replies</Text>
              <TouchableOpacity onPress={() => setManageModalVisible(false)}>
                <Text style={styles.modalClose}>{'\u2715'}</Text>
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalBody} contentContainerStyle={{ paddingBottom: 32 }}>
              {/* Existing Templates */}
              {quickReplies.length === 0 && !quickRepliesLoading && (
                <View style={styles.emptyTemplates}>
                  <Text style={styles.emptyTemplatesText}>
                    No quick replies yet. Add templates below for faster responses.
                  </Text>
                </View>
              )}

              {quickReplies.map((qr) => (
                <View key={qr.id} style={styles.templateCard}>
                  <View style={styles.templateContent}>
                    <Text style={styles.templateLabel}>{qr.label}</Text>
                    <Text style={styles.templateText} numberOfLines={3}>{qr.text}</Text>
                  </View>
                  <TouchableOpacity
                    style={styles.deleteBtn}
                    onPress={() => handleDeleteTemplate(qr.id)}
                    disabled={deletingId === qr.id}
                  >
                    {deletingId === qr.id ? (
                      <ActivityIndicator size="small" color="#ef4444" />
                    ) : (
                      <Text style={styles.deleteBtnText}>{'\u2715'}</Text>
                    )}
                  </TouchableOpacity>
                </View>
              ))}

              {/* Add New Template */}
              <View style={styles.addSection}>
                <Text style={styles.addSectionTitle}>Add New Template</Text>
                <TextInput
                  style={styles.modalInput}
                  value={newTemplateLabel}
                  onChangeText={setNewTemplateLabel}
                  placeholder="Label (e.g., Check-in instructions)"
                  placeholderTextColor="#9ca3af"
                  maxLength={50}
                />
                <TextInput
                  style={[styles.modalInput, styles.modalTextArea]}
                  value={newTemplateText}
                  onChangeText={setNewTemplateText}
                  placeholder="Template text..."
                  placeholderTextColor="#9ca3af"
                  multiline
                  numberOfLines={4}
                  textAlignVertical="top"
                  maxLength={500}
                />
                <TouchableOpacity
                  style={[styles.addBtn, (!newTemplateLabel.trim() || !newTemplateText.trim() || addingTemplate) && { opacity: 0.5 }]}
                  onPress={handleAddTemplate}
                  disabled={!newTemplateLabel.trim() || !newTemplateText.trim() || addingTemplate}
                >
                  {addingTemplate ? (
                    <ActivityIndicator color="#fff" size="small" />
                  ) : (
                    <Text style={styles.addBtnText}>Add Template</Text>
                  )}
                </TouchableOpacity>
              </View>

              {/* Suggested Templates */}
              <View style={styles.suggestedSection}>
                <Text style={styles.suggestedTitle}>Suggested Templates</Text>
                <Text style={styles.suggestedSubtitle}>Tap to add</Text>
                {DEFAULT_TEMPLATES.map((tmpl, idx) => {
                  const alreadyAdded = quickReplies.some(qr => qr.label === tmpl.label);
                  return (
                    <TouchableOpacity
                      key={idx}
                      style={[styles.suggestedCard, alreadyAdded && { opacity: 0.4 }]}
                      disabled={alreadyAdded}
                      onPress={async () => {
                        const token = await getAccessToken();
                        if (!token) return;
                        try {
                          await api.createQuickReply({ label: tmpl.label, text: tmpl.text }, token);
                          await loadQuickReplies();
                        } catch {
                          Alert.alert('Error', 'Failed to add template.');
                        }
                      }}
                    >
                      <Text style={styles.suggestedLabel}>
                        {tmpl.label}
                        {alreadyAdded ? ' (added)' : ''}
                      </Text>
                      <Text style={styles.suggestedText} numberOfLines={2}>{tmpl.text}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </ScrollView>
          </View>
        </Modal>
      </KeyboardAvoidingView>
    );
  }

  /* ── Conversation List ─────────────────────────────────── */

  return (
    <View style={{ flex: 1, backgroundColor: '#f9fafb' }}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.headerBackBtn}>
          <Text style={styles.backText}>{'\u2039'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Guest Messages</Text>
      </View>

      <FlatList
        data={conversations}
        keyExtractor={(item) => item.id}
        contentContainerStyle={conversations.length === 0 ? styles.center : undefined}
        onRefresh={loadConversations}
        refreshing={loading}
        ListEmptyComponent={
          <View style={styles.emptyCenter}>
            <Text style={styles.emptyIcon}>{'\uD83D\uDCAC'}</Text>
            <Text style={styles.emptyTitle}>No guest messages yet</Text>
            <Text style={styles.emptySubtitle}>Messages from guests will appear here</Text>
          </View>
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.convoRow}
            onPress={() => handleSelectConversation(item)}
          >
            <View style={styles.convoAvatar}>
              <Text style={styles.convoAvatarText}>
                {(item.otherParticipantName ?? 'G')[0].toUpperCase()}
              </Text>
            </View>
            <View style={styles.convoInfo}>
              <View style={styles.convoTopRow}>
                <Text style={[styles.convoName, item.unreadCount > 0 && { fontWeight: '700' }]} numberOfLines={1}>
                  {item.otherParticipantName || 'Guest'}
                </Text>
                <Text style={styles.convoTime}>{timeAgo(item.lastMessageAt)}</Text>
              </View>
              {item.listingTitle ? (
                <Text style={styles.convoListing} numberOfLines={1}>{item.listingTitle}</Text>
              ) : null}
              {item.bookingRef ? (
                <Text style={styles.convoBookingRef}>Booking: {item.bookingRef}</Text>
              ) : null}
              <Text
                style={[styles.convoPreview, item.unreadCount > 0 && { color: '#111827', fontWeight: '500' }]}
                numberOfLines={1}
              >
                {item.lastMessageText || 'No messages yet'}
              </Text>
            </View>
            {item.unreadCount > 0 && (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{item.unreadCount > 9 ? '9+' : item.unreadCount}</Text>
              </View>
            )}
          </TouchableOpacity>
        )}
      />
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 56,
    paddingBottom: 12,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  headerBackBtn: { padding: 4, marginRight: 8 },
  headerTitle: { fontSize: 22, fontWeight: '700', color: '#111827' },

  // Conversation list
  convoRow: { flexDirection: 'row', alignItems: 'center', padding: 14, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  convoAvatar: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#fed7aa', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  convoAvatarText: { fontSize: 16, fontWeight: '700', color: '#ea580c' },
  convoInfo: { flex: 1 },
  convoTopRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  convoName: { fontSize: 14, fontWeight: '600', color: '#111827', flex: 1, marginRight: 8 },
  convoTime: { fontSize: 11, color: '#9ca3af' },
  convoListing: { fontSize: 11, color: '#f97316', marginTop: 1 },
  convoBookingRef: { fontSize: 10, color: '#6b7280', marginTop: 1 },
  convoPreview: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  badge: { backgroundColor: '#f97316', borderRadius: 10, minWidth: 20, height: 20, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 5, marginLeft: 8 },
  badgeText: { color: '#fff', fontSize: 10, fontWeight: '700' },

  // Empty
  emptyCenter: { alignItems: 'center', paddingTop: 80 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 4 },
  emptySubtitle: { fontSize: 13, color: '#9ca3af' },
  emptyText: { fontSize: 14, color: '#9ca3af', textAlign: 'center' },

  // Chat header
  chatHeader: { flexDirection: 'row', alignItems: 'center', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 12, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn: { padding: 8, marginRight: 4 },
  backText: { fontSize: 28, color: '#f97316', fontWeight: '300' },
  chatHeaderInfo: { flex: 1 },
  chatHeaderName: { fontSize: 15, fontWeight: '600', color: '#111827' },
  chatHeaderListing: { fontSize: 11, color: '#f97316', marginTop: 1 },

  // Messages
  messagesList: { padding: 12, paddingBottom: 4 },
  bubble: { maxWidth: '78%', borderRadius: 16, paddingHorizontal: 12, paddingVertical: 8, marginBottom: 6 },
  bubbleMine: { alignSelf: 'flex-end', backgroundColor: '#f97316', borderBottomRightRadius: 4 },
  bubbleTheirs: { alignSelf: 'flex-start', backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderBottomLeftRadius: 4 },
  bubbleText: { fontSize: 14, color: '#111827', lineHeight: 20 },
  bubbleTime: { fontSize: 10, color: '#9ca3af', textAlign: 'right', marginTop: 2 },
  systemMsg: { alignItems: 'center', marginVertical: 8 },
  systemMsgText: { fontSize: 11, color: '#9ca3af', backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 10, paddingVertical: 3, overflow: 'hidden' },

  // Quick Reply Bar
  quickReplyBar: {
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
    backgroundColor: '#fefce8',
    paddingVertical: 6,
  },
  quickReplyScroll: { paddingHorizontal: 10, gap: 8, flexDirection: 'row', alignItems: 'center' },
  quickReplyChip: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#f97316',
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  quickReplyChipText: { fontSize: 12, color: '#f97316', fontWeight: '500' },
  noQuickReplies: { fontSize: 12, color: '#9ca3af', marginRight: 8 },
  manageChip: {
    backgroundColor: '#f97316',
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  manageChipText: { fontSize: 12, color: '#fff', fontWeight: '600' },

  // Input bar
  inputBar: { flexDirection: 'row', alignItems: 'flex-end', padding: 8, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  textInput: { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8, fontSize: 14, maxHeight: 100, color: '#111827', backgroundColor: '#f9fafb' },
  sendBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
  sendBtnText: { color: '#fff', fontSize: 18, fontWeight: '700' },

  // Modal
  modalContainer: { flex: 1, backgroundColor: '#fff' },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 56,
    paddingBottom: 14,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
    backgroundColor: '#fff',
  },
  modalTitle: { fontSize: 20, fontWeight: '700', color: '#111827' },
  modalClose: { fontSize: 20, color: '#6b7280', padding: 4 },
  modalBody: { flex: 1, paddingHorizontal: 16, paddingTop: 16 },

  // Template cards
  templateCard: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    padding: 12,
    marginBottom: 10,
    alignItems: 'flex-start',
  },
  templateContent: { flex: 1 },
  templateLabel: { fontSize: 14, fontWeight: '600', color: '#111827', marginBottom: 4 },
  templateText: { fontSize: 12, color: '#6b7280', lineHeight: 18 },
  deleteBtn: { padding: 8, marginLeft: 8 },
  deleteBtnText: { fontSize: 14, color: '#ef4444', fontWeight: '600' },

  emptyTemplates: { padding: 20, alignItems: 'center' },
  emptyTemplatesText: { fontSize: 13, color: '#9ca3af', textAlign: 'center' },

  // Add section
  addSection: {
    marginTop: 20,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
  },
  addSectionTitle: { fontSize: 16, fontWeight: '600', color: '#111827', marginBottom: 12 },
  modalInput: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: '#111827',
    backgroundColor: '#f9fafb',
    marginBottom: 10,
  },
  modalTextArea: { minHeight: 80 },
  addBtn: {
    backgroundColor: '#f97316',
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    marginTop: 4,
  },
  addBtnText: { color: '#fff', fontSize: 14, fontWeight: '600' },

  // Suggested templates
  suggestedSection: {
    marginTop: 24,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
  },
  suggestedTitle: { fontSize: 16, fontWeight: '600', color: '#111827' },
  suggestedSubtitle: { fontSize: 12, color: '#9ca3af', marginBottom: 12 },
  suggestedCard: {
    backgroundColor: '#fffbeb',
    borderWidth: 1,
    borderColor: '#fde68a',
    borderRadius: 10,
    padding: 12,
    marginBottom: 8,
  },
  suggestedLabel: { fontSize: 13, fontWeight: '600', color: '#92400e', marginBottom: 2 },
  suggestedText: { fontSize: 12, color: '#78716c', lineHeight: 17 },
});
