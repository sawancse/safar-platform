'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

interface SmartReplyBarProps {
  messages: { role: string; content: string }[];
  listingContext: { listing_name?: string; city?: string; property_type?: string };
  language?: string;
  onSelectReply: (reply: string) => void;
}

export default function SmartReplyBar({ messages, listingContext, language = 'en', onSelectReply }: SmartReplyBarProps) {
  const [replies, setReplies] = useState<string[]>([]);
  const [intent, setIntent] = useState('');
  const [autoResponse, setAutoResponse] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (messages.length > 0) {
      loadSuggestions();
    }
  }, [messages.length]);

  async function loadSuggestions() {
    setLoading(true);
    try {
      const result = await api.aiReplySuggest(messages, listingContext);
      setReplies(result.replies || []);
      setIntent(result.detected_intent || '');
    } catch (e) {
      console.error('Failed to load suggestions:', e);
    }
    setLoading(false);
  }

  if (loading) {
    return (
      <div className="flex gap-2 px-4 py-2 bg-gray-50 border-t">
        <div className="animate-pulse flex gap-2">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-8 w-32 bg-gray-200 rounded-full" />
          ))}
        </div>
      </div>
    );
  }

  if (replies.length === 0) return null;

  return (
    <div className="px-4 py-2 bg-gray-50 border-t">
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs text-gray-400">
          AI suggests ({intent})
        </span>
        <label className="flex items-center gap-1 text-xs text-gray-400">
          <input type="checkbox" checked={autoResponse} onChange={e => setAutoResponse(e.target.checked)}
            className="rounded border-gray-300 w-3 h-3" />
          Auto-reply
        </label>
      </div>
      <div className="flex gap-2 overflow-x-auto">
        {replies.map((reply, i) => (
          <button key={i} onClick={() => onSelectReply(reply)}
            className="shrink-0 text-sm bg-white border border-orange-200 text-orange-700 rounded-full px-3 py-1.5 hover:bg-orange-50 transition-colors">
            {reply.length > 60 ? reply.slice(0, 60) + '...' : reply}
          </button>
        ))}
      </div>
    </div>
  );
}
