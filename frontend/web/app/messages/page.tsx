'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { api } from '@/lib/api';
import type { Conversation, ChatMessage } from '@/types';

function timeAgo(iso?: string): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'now';
  if (mins < 60) return `${mins}m`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d`;
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

function formatMessageTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}

function formatDateDivider(iso: string): string {
  const d = new Date(iso);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}

export default function MessagesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [token, setToken] = useState('');
  const [userId, setUserId] = useState('');
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [showMobileChat, setShowMobileChat] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const selectedConversation = conversations.find(c => c.id === selectedId);

  const loadConversations = useCallback(async (t: string) => {
    try {
      const convos = await api.getConversations(t);
      setConversations(convos);
    } catch {
      setConversations([]);
    }
  }, []);

  const loadMessages = useCallback(async (conversationId: string, t: string) => {
    setMessagesLoading(true);
    try {
      const res = await api.getMessages(conversationId, t);
      const msgs = Array.isArray(res) ? res : res?.content ?? [];
      setMessages(msgs);
      // Mark as read
      api.markAsRead(conversationId, t).catch(() => {});
    } catch {
      setMessages([]);
    } finally {
      setMessagesLoading(false);
    }
  }, []);

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    const uid = localStorage.getItem('user_id') ?? '';
    if (!t) {
      router.push('/auth?redirect=/messages');
      return;
    }
    setToken(t);
    setUserId(uid);
    loadConversations(t).finally(() => setLoading(false));
  }, [router, loadConversations]);

  // Auto-select conversation from query params
  useEffect(() => {
    const convId = searchParams.get('conversation');
    if (convId && token) {
      setSelectedId(convId);
      loadMessages(convId, token);
      setShowMobileChat(true);
    }
  }, [searchParams, token, loadMessages]);

  // Poll for new messages every 10 seconds
  useEffect(() => {
    if (!token) return;
    const interval = setInterval(() => {
      loadConversations(token);
      if (selectedId) {
        loadMessages(selectedId, token);
      }
    }, 10000);
    return () => clearInterval(interval);
  }, [token, selectedId, loadConversations, loadMessages]);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function handleSelectConversation(id: string) {
    setSelectedId(id);
    loadMessages(id, token);
    setShowMobileChat(true);
    // Update unread count in the list
    setConversations(prev =>
      prev.map(c => c.id === id ? { ...c, unreadCount: 0 } : c)
    );
  }

  async function handleSend() {
    if (!newMessage.trim() || !selectedConversation || sending) return;
    setSending(true);
    try {
      const recipientId = selectedConversation.participant1Id === userId
        ? selectedConversation.participant2Id
        : selectedConversation.participant1Id;
      await api.sendMessage({
        listingId: selectedConversation.listingId,
        recipientId,
        bookingId: selectedConversation.bookingId,
        content: newMessage.trim(),
      }, token);
      setNewMessage('');
      // Refresh messages
      await loadMessages(selectedConversation.id, token);
      await loadConversations(token);
      inputRef.current?.focus();
    } catch (e: any) {
      alert(e.message || 'Failed to send message');
    } finally {
      setSending(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="text-4xl animate-spin">&#x23F3;</div>
      </div>
    );
  }

  // Group messages by date for dividers
  function getMessageGroups(msgs: ChatMessage[]) {
    const groups: { date: string; messages: ChatMessage[] }[] = [];
    let lastDate = '';
    for (const msg of msgs) {
      const d = new Date(msg.createdAt).toDateString();
      if (d !== lastDate) {
        groups.push({ date: msg.createdAt, messages: [msg] });
        lastDate = d;
      } else {
        groups[groups.length - 1].messages.push(msg);
      }
    }
    return groups;
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-4">Messages</h1>

      <div className="border rounded-2xl overflow-hidden bg-white shadow-sm" style={{ height: 'calc(100vh - 180px)', minHeight: '500px' }}>
        <div className="flex h-full">
          {/* Left sidebar - conversation list */}
          <div className={`w-full md:w-96 border-r flex flex-col ${showMobileChat ? 'hidden md:flex' : 'flex'}`}>
            <div className="p-4 border-b">
              <h2 className="font-semibold text-gray-700">Conversations</h2>
            </div>
            <div className="flex-1 overflow-y-auto">
              {conversations.length === 0 ? (
                <div className="text-center py-16 px-6">
                  <p className="text-3xl mb-3">&#x1F4AC;</p>
                  <p className="text-sm font-medium text-gray-500">No messages yet</p>
                  <p className="text-xs text-gray-400 mt-1">Contact a host from any listing page!</p>
                </div>
              ) : (
                conversations.map((c) => {
                  const isSelected = c.id === selectedId;
                  return (
                    <button
                      key={c.id}
                      onClick={() => handleSelectConversation(c.id)}
                      className={`w-full text-left px-4 py-3 border-b hover:bg-gray-50 transition flex gap-3 ${
                        isSelected ? 'bg-orange-50 border-l-2 border-l-orange-500' : ''
                      }`}
                    >
                      {/* Avatar */}
                      <div className="w-10 h-10 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-sm font-bold shrink-0">
                        {(c.otherParticipantName ?? 'U')[0].toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold text-gray-800 truncate">
                            {c.otherParticipantName || 'User'}
                          </span>
                          <span className="text-xs text-gray-400 shrink-0 ml-2">
                            {timeAgo(c.lastMessageAt)}
                          </span>
                        </div>
                        {c.listingTitle && (
                          <p className="text-xs text-orange-500 truncate">{c.listingTitle}</p>
                        )}
                        <p className="text-xs text-gray-400 truncate mt-0.5">
                          {c.lastMessageText || 'No messages yet'}
                        </p>
                      </div>
                      {c.unreadCount > 0 && (
                        <span className="bg-orange-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center shrink-0 self-center">
                          {c.unreadCount > 9 ? '9+' : c.unreadCount}
                        </span>
                      )}
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Right panel - chat view */}
          <div className={`flex-1 flex flex-col ${!showMobileChat ? 'hidden md:flex' : 'flex'}`}>
            {!selectedId ? (
              <div className="flex-1 flex items-center justify-center text-center p-8">
                <div>
                  <p className="text-5xl mb-4">&#x1F4E8;</p>
                  <p className="text-gray-500 font-medium">Select a conversation</p>
                  <p className="text-sm text-gray-400 mt-1">Choose from your messages on the left</p>
                </div>
              </div>
            ) : (
              <>
                {/* Chat header */}
                <div className="p-4 border-b flex items-center gap-3">
                  <button
                    onClick={() => { setShowMobileChat(false); setSelectedId(null); }}
                    className="md:hidden text-gray-400 hover:text-gray-600 mr-1"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                  <div className="w-9 h-9 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-sm font-bold">
                    {(selectedConversation?.otherParticipantName ?? 'U')[0].toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-800 truncate">
                      {selectedConversation?.otherParticipantName || 'User'}
                    </p>
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      {selectedConversation?.listingTitle && (
                        <span className="truncate">{selectedConversation.listingTitle}</span>
                      )}
                      {selectedConversation?.bookingRef && (
                        <span className="font-mono">#{selectedConversation.bookingRef}</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Messages area */}
                <div className="flex-1 overflow-y-auto px-4 py-4 space-y-1 bg-gray-50">
                  {messagesLoading ? (
                    <div className="flex items-center justify-center py-16">
                      <div className="text-2xl animate-spin">&#x23F3;</div>
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="text-center py-16">
                      <p className="text-gray-400 text-sm">Start the conversation...</p>
                    </div>
                  ) : (
                    getMessageGroups(messages).map((group, gi) => (
                      <div key={gi}>
                        {/* Date divider */}
                        <div className="flex items-center gap-3 my-4">
                          <div className="flex-1 h-px bg-gray-200" />
                          <span className="text-xs text-gray-400 font-medium">
                            {formatDateDivider(group.date)}
                          </span>
                          <div className="flex-1 h-px bg-gray-200" />
                        </div>
                        {group.messages.map((msg) => {
                          const isMine = msg.senderId === userId;
                          const isSystem = msg.messageType === 'SYSTEM' || msg.messageType === 'BOOKING_UPDATE';

                          if (isSystem) {
                            return (
                              <div key={msg.id} className="flex justify-center my-2">
                                <span className="text-xs text-gray-400 bg-gray-100 rounded-full px-3 py-1">
                                  {msg.content}
                                </span>
                              </div>
                            );
                          }

                          return (
                            <div key={msg.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'} mb-1.5`}>
                              <div
                                className={`max-w-[75%] rounded-2xl px-3.5 py-2 ${
                                  isMine
                                    ? 'bg-orange-500 text-white rounded-br-md'
                                    : 'bg-white text-gray-800 border rounded-bl-md'
                                }`}
                              >
                                <p className="text-sm whitespace-pre-wrap break-words">{msg.content}</p>
                                <div className={`flex items-center gap-1 mt-0.5 ${isMine ? 'justify-end' : ''}`}>
                                  <span className={`text-[10px] ${isMine ? 'text-orange-100' : 'text-gray-400'}`}>
                                    {formatMessageTime(msg.createdAt)}
                                  </span>
                                  {isMine && (
                                    <span className={`text-[10px] ${msg.readAt ? 'text-orange-100' : 'text-orange-200'}`}>
                                      {msg.readAt ? '\u2713\u2713' : '\u2713'}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    ))
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Input bar */}
                <div className="p-3 border-t bg-white">
                  <div className="flex items-end gap-2">
                    <textarea
                      ref={inputRef}
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="Type a message..."
                      rows={1}
                      className="flex-1 border rounded-xl px-3 py-2 text-sm resize-none outline-none focus:border-orange-400 max-h-24"
                      style={{ minHeight: '38px' }}
                    />
                    <button
                      onClick={handleSend}
                      disabled={!newMessage.trim() || sending}
                      className="bg-orange-500 text-white rounded-xl p-2.5 hover:bg-orange-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {sending ? (
                        <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                      ) : (
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                        </svg>
                      )}
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
