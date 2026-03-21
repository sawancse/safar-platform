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
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

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

export default function MessagesScreen() {
  const router = useRouter();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedConvo, setSelectedConvo] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [userId, setUserId] = useState('');
  const flatListRef = useRef<FlatList>(null);

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

  const loadMessages = useCallback(async (convoId: string) => {
    const token = await getAccessToken();
    if (!token) return;
    setMessagesLoading(true);
    try {
      const res = await api.getMessages(convoId, token);
      const msgs = Array.isArray(res) ? res : res?.content ?? [];
      setMessages(msgs);
      api.markAsRead(convoId, token).catch(() => {});
    } catch {
      setMessages([]);
    } finally {
      setMessagesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadConversations();
    // Get userId from stored token data
    (async () => {
      try {
        const AsyncStorage = require('@react-native-async-storage/async-storage').default;
        const uid = await AsyncStorage.getItem('user_id');
        if (uid) setUserId(uid);
      } catch {}
    })();
  }, [loadConversations]);

  // Poll every 10s
  useEffect(() => {
    const interval = setInterval(() => {
      loadConversations();
      if (selectedConvo) loadMessages(selectedConvo.id);
    }, 10000);
    return () => clearInterval(interval);
  }, [selectedConvo, loadConversations, loadMessages]);

  function handleSelectConversation(convo: Conversation) {
    setSelectedConvo(convo);
    loadMessages(convo.id);
    // Clear unread
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
    setSelectedConvo(null);
    setMessages([]);
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  // Chat view
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
            <Text style={styles.chatHeaderName}>{selectedConvo.otherParticipantName || 'User'}</Text>
            <Text style={styles.chatHeaderListing} numberOfLines={1}>{selectedConvo.listingTitle || ''}</Text>
          </View>
        </View>

        {/* Messages */}
        {messagesLoading ? (
          <View style={styles.center}>
            <ActivityIndicator color="#f97316" />
          </View>
        ) : messages.length === 0 ? (
          <View style={styles.center}>
            <Text style={styles.emptyText}>Start the conversation...</Text>
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

        {/* Input */}
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
      </KeyboardAvoidingView>
    );
  }

  // Conversation list
  return (
    <View style={{ flex: 1, backgroundColor: '#f9fafb' }}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Messages</Text>
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
            <Text style={styles.emptyTitle}>No messages yet</Text>
            <Text style={styles.emptySubtitle}>Contact a host from any listing page!</Text>
          </View>
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.convoRow}
            onPress={() => handleSelectConversation(item)}
          >
            <View style={styles.convoAvatar}>
              <Text style={styles.convoAvatarText}>
                {(item.otherParticipantName ?? 'U')[0].toUpperCase()}
              </Text>
            </View>
            <View style={styles.convoInfo}>
              <View style={styles.convoTopRow}>
                <Text style={styles.convoName} numberOfLines={1}>
                  {item.otherParticipantName || 'User'}
                </Text>
                <Text style={styles.convoTime}>{timeAgo(item.lastMessageAt)}</Text>
              </View>
              {item.listingTitle ? (
                <Text style={styles.convoListing} numberOfLines={1}>{item.listingTitle}</Text>
              ) : null}
              <Text style={styles.convoPreview} numberOfLines={1}>
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

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  header: { paddingTop: 56, paddingBottom: 12, paddingHorizontal: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
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
  convoPreview: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  badge: { backgroundColor: '#f97316', borderRadius: 10, minWidth: 20, height: 20, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 5, marginLeft: 8 },
  badgeText: { color: '#fff', fontSize: 10, fontWeight: '700' },

  // Empty
  emptyCenter: { alignItems: 'center', paddingTop: 80 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 4 },
  emptySubtitle: { fontSize: 13, color: '#9ca3af' },
  emptyText: { fontSize: 14, color: '#9ca3af' },

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

  // Input bar
  inputBar: { flexDirection: 'row', alignItems: 'flex-end', padding: 8, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  textInput: { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8, fontSize: 14, maxHeight: 100, color: '#111827', backgroundColor: '#f9fafb' },
  sendBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
  sendBtnText: { color: '#fff', fontSize: 18, fontWeight: '700' },
});
