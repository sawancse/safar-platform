'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { api } from '@/lib/api';
import type { Conversation, ChatMessage, QuickReplyTemplate } from '@/types';

interface Props {
  token: string;
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

export default function HostMessagesTab({ token }: Props) {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [newMessage, setNewMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [quickReplies, setQuickReplies] = useState<QuickReplyTemplate[]>([]);
  const [newTemplate, setNewTemplate] = useState('');
  const [showTemplates, setShowTemplates] = useState(false);
  const userId = typeof window !== 'undefined' ? localStorage.getItem('user_id') ?? '' : '';
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const selectedConversation = conversations.find(c => c.id === selectedId);

  const loadConversations = useCallback(async () => {
    try {
      const convos = await api.getConversations(token);
      setConversations(convos);
    } catch { setConversations([]); }
  }, [token]);

  const loadMessages = useCallback(async (id: string) => {
    try {
      const res = await api.getMessages(id, token);
      setMessages(Array.isArray(res) ? res : res?.content ?? []);
      api.markAsRead(id, token).catch(() => {});
    } catch { setMessages([]); }
  }, [token]);

  useEffect(() => {
    loadConversations().finally(() => setLoading(false));
    api.getQuickReplies(token).then(setQuickReplies).catch(() => setQuickReplies([]));
  }, [token, loadConversations]);

  // Poll every 10s
  useEffect(() => {
    const interval = setInterval(() => {
      loadConversations();
      if (selectedId) loadMessages(selectedId);
    }, 10000);
    return () => clearInterval(interval);
  }, [token, selectedId, loadConversations, loadMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function handleSelect(id: string) {
    setSelectedId(id);
    loadMessages(id);
    setConversations(prev => prev.map(c => c.id === id ? { ...c, unreadCount: 0 } : c));
  }

  async function handleSend(text?: string) {
    const content = (text || newMessage).trim();
    if (!content || !selectedConversation || sending) return;
    setSending(true);
    try {
      const recipientId = selectedConversation.participant1Id === userId
        ? selectedConversation.participant2Id
        : selectedConversation.participant1Id;
      await api.sendMessage({
        listingId: selectedConversation.listingId,
        recipientId,
        bookingId: selectedConversation.bookingId,
        content,
      }, token);
      setNewMessage('');
      await loadMessages(selectedConversation.id);
      await loadConversations();
    } catch (e: any) {
      alert(e.message || 'Failed to send');
    } finally {
      setSending(false);
    }
  }

  async function handleAddTemplate() {
    if (!newTemplate.trim()) return;
    try {
      const t = await api.createQuickReply(newTemplate.trim(), token);
      setQuickReplies(prev => [...prev, t]);
      setNewTemplate('');
    } catch (e: any) { alert(e.message || 'Failed to create template'); }
  }

  async function handleDeleteTemplate(id: string) {
    try {
      await api.deleteQuickReply(id, token);
      setQuickReplies(prev => prev.filter(t => t.id !== id));
    } catch {}
  }

  if (loading) {
    return <div className="text-center py-16 text-gray-400 animate-pulse">Loading messages...</div>;
  }

  return (
    <div className="border rounded-xl overflow-hidden bg-white" style={{ height: '600px' }}>
      <div className="flex h-full">
        {/* Conversation list */}
        <div className="w-80 border-r flex flex-col">
          <div className="p-3 border-b flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-700">Guest Messages</h3>
            <button
              onClick={() => setShowTemplates(!showTemplates)}
              className="text-xs text-orange-500 hover:text-orange-600 font-medium"
            >
              {showTemplates ? 'Hide Templates' : 'Quick Replies'}
            </button>
          </div>

          {showTemplates ? (
            <div className="flex-1 overflow-y-auto p-3 space-y-2">
              <p className="text-xs text-gray-500 mb-2">Quick reply templates - click to use in chat</p>
              {quickReplies.map(qr => (
                <div key={qr.id} className="flex items-center gap-2 bg-gray-50 rounded-lg p-2">
                  <button
                    onClick={() => { setNewMessage(qr.content); setShowTemplates(false); }}
                    className="flex-1 text-left text-xs text-gray-700 hover:text-orange-600"
                  >
                    {qr.content}
                  </button>
                  <button
                    onClick={() => handleDeleteTemplate(qr.id)}
                    className="text-red-400 hover:text-red-600 text-xs shrink-0"
                  >
                    &#10005;
                  </button>
                </div>
              ))}
              <div className="flex gap-1 mt-3">
                <input
                  value={newTemplate}
                  onChange={(e) => setNewTemplate(e.target.value)}
                  placeholder="Add new template..."
                  className="flex-1 border rounded-lg px-2 py-1 text-xs outline-none focus:border-orange-400"
                  onKeyDown={(e) => e.key === 'Enter' && handleAddTemplate()}
                />
                <button
                  onClick={handleAddTemplate}
                  disabled={!newTemplate.trim()}
                  className="bg-orange-500 text-white text-xs px-3 py-1 rounded-lg disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto">
              {conversations.length === 0 ? (
                <div className="text-center py-12 text-gray-400 text-sm">No guest messages yet</div>
              ) : (
                conversations.map(c => (
                  <button
                    key={c.id}
                    onClick={() => handleSelect(c.id)}
                    className={`w-full text-left px-3 py-3 border-b hover:bg-gray-50 transition flex gap-2 ${
                      c.id === selectedId ? 'bg-orange-50' : ''
                    }`}
                  >
                    <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold shrink-0">
                      {(c.otherParticipantName ?? 'G')[0].toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold truncate">{c.otherParticipantName || 'Guest'}</span>
                        <span className="text-[10px] text-gray-400 shrink-0">{timeAgo(c.lastMessageAt)}</span>
                      </div>
                      <p className="text-[10px] text-orange-500 truncate">{c.listingTitle}</p>
                      <p className="text-[10px] text-gray-400 truncate">{c.lastMessageText}</p>
                    </div>
                    {c.unreadCount > 0 && (
                      <span className="bg-orange-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center shrink-0 self-center">
                        {c.unreadCount}
                      </span>
                    )}
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Chat panel */}
        <div className="flex-1 flex flex-col">
          {!selectedId ? (
            <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
              Select a conversation to reply
            </div>
          ) : (
            <>
              <div className="p-3 border-b">
                <p className="text-sm font-semibold">{selectedConversation?.otherParticipantName || 'Guest'}</p>
                <p className="text-xs text-gray-400">{selectedConversation?.listingTitle}{selectedConversation?.bookingRef ? ` - #${selectedConversation.bookingRef}` : ''}</p>
              </div>
              <div className="flex-1 overflow-y-auto px-3 py-3 space-y-1.5 bg-gray-50">
                {messages.map(msg => {
                  const isMine = msg.senderId === userId;
                  const isSystem = msg.messageType === 'SYSTEM' || msg.messageType === 'BOOKING_UPDATE';
                  if (isSystem) {
                    return (
                      <div key={msg.id} className="text-center">
                        <span className="text-[10px] text-gray-400 bg-gray-100 rounded-full px-2 py-0.5">{msg.content}</span>
                      </div>
                    );
                  }
                  return (
                    <div key={msg.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-[70%] rounded-xl px-3 py-1.5 ${
                        isMine ? 'bg-orange-500 text-white' : 'bg-white border text-gray-800'
                      }`}>
                        <p className="text-xs whitespace-pre-wrap">{msg.content}</p>
                        <p className={`text-[9px] text-right mt-0.5 ${isMine ? 'text-orange-100' : 'text-gray-400'}`}>
                          {formatTime(msg.createdAt)}
                          {isMine && (msg.readAt ? ' \u2713\u2713' : ' \u2713')}
                        </p>
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>

              {/* Quick reply chips */}
              {quickReplies.length > 0 && (
                <div className="px-3 py-1.5 border-t flex gap-1.5 overflow-x-auto">
                  {quickReplies.slice(0, 4).map(qr => (
                    <button
                      key={qr.id}
                      onClick={() => handleSend(qr.content)}
                      className="text-[10px] bg-orange-50 text-orange-600 border border-orange-200 rounded-full px-2.5 py-1 whitespace-nowrap hover:bg-orange-100 transition"
                    >
                      {qr.content.length > 30 ? qr.content.slice(0, 30) + '...' : qr.content}
                    </button>
                  ))}
                </div>
              )}

              <div className="p-2 border-t bg-white flex gap-2">
                <input
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
                  placeholder="Reply to guest..."
                  className="flex-1 border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400"
                />
                <button
                  onClick={() => handleSend()}
                  disabled={!newMessage.trim() || sending}
                  className="bg-orange-500 text-white rounded-lg px-4 py-1.5 text-sm font-medium hover:bg-orange-600 disabled:opacity-50"
                >
                  {sending ? '...' : 'Send'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
