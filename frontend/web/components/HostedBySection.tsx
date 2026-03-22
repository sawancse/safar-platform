'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import UserAvatar from './UserAvatar';
import TrustBadge from './TrustBadge';

interface HostProfile {
  id: string;
  name: string;
  avatarUrl: string;
  bio: string;
  languages: string;
  verificationLevel: string;
  trustScore: number;
  trustBadge: string;
  responseRate: number;
  avgResponseMinutes: number;
  totalHostReviews: number;
  hostType: string;
  selfieVerified: boolean;
  createdAt: string;
  lastActiveAt: string;
}

interface Props {
  hostId: string;
  listingId?: string;
  listingTitle?: string;
}

const QUICK_QUESTIONS = [
  'Is parking available?',
  'Can I check in early?',
  'Is the kitchen equipped?',
  'Are pets allowed?',
  'Is WiFi included?',
  'Is there a washing machine?',
];

function formatJoinDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
}

function formatResponseTime(minutes?: number): string {
  if (!minutes) return 'Not available';
  if (minutes < 60) return `within ${minutes} minutes`;
  if (minutes < 1440) return `within ${Math.round(minutes / 60)} hours`;
  return `within ${Math.round(minutes / 1440)} days`;
}

export default function HostedBySection({ hostId, listingId, listingTitle }: Props) {
  const router = useRouter();
  const [host, setHost] = useState<HostProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    api.getHostProfile(hostId)
      .then(setHost)
      .catch(() => setHost(null))
      .finally(() => setLoading(false));
  }, [hostId]);

  async function handleSendMessage() {
    if (!message.trim()) return;
    const token = localStorage.getItem('access_token');
    if (!token) {
      router.push(`/auth?redirect=/listings/${listingId || ''}`);
      return;
    }
    setSending(true);
    try {
      await api.sendMessage({
        listingId: listingId || '',
        recipientId: hostId,
        content: message.trim(),
      }, token);
      setSent(true);
      setTimeout(() => {
        setShowModal(false);
        setSent(false);
        setMessage('');
      }, 1500);
    } catch (e: any) {
      alert(e.message || 'Failed to send message');
    } finally {
      setSending(false);
    }
  }

  function handleQuickQuestion(q: string) {
    setMessage(q);
  }

  function openContactModal() {
    const token = localStorage.getItem('access_token');
    if (!token) {
      router.push(`/auth?redirect=/listings/${listingId || ''}`);
      return;
    }
    setShowModal(true);
  }

  if (loading) return <div className="animate-pulse h-40 bg-gray-100 rounded-xl" />;
  if (!host) return null;

  const languages = host.languages ? host.languages.split(',').map(l => l.trim()) : [];

  return (
    <>
      <div className="border rounded-xl p-6">
        <div className="flex items-start gap-4 mb-4">
          <UserAvatar avatarUrl={host.avatarUrl} name={host.name} size={64} />
          <div className="flex-1">
            <h3 className="text-lg font-semibold">Hosted by {host.name}</h3>
            <div className="flex items-center gap-2 mt-1 flex-wrap">
              <TrustBadge score={host.trustScore || 0} size="sm" />
              {host.verificationLevel === 'VERIFIED' && (
                <span className="text-xs text-green-600 font-medium flex items-center gap-0.5">
                  &#10003; Identity verified
                </span>
              )}
              <span className="text-xs text-gray-400">Joined {formatJoinDate(host.createdAt)}</span>
            </div>
          </div>
        </div>

        {/* Response metrics */}
        {(host.responseRate > 0 || host.totalHostReviews > 0) && (
          <div className="flex gap-6 text-sm text-gray-600 mb-4">
            {host.responseRate > 0 && (
              <span>Response rate: <strong>{host.responseRate}%</strong></span>
            )}
            {host.avgResponseMinutes && host.avgResponseMinutes > 0 && (
              <span>Responds {formatResponseTime(host.avgResponseMinutes)}</span>
            )}
            {host.totalHostReviews > 0 && (
              <span><strong>{host.totalHostReviews}</strong> reviews</span>
            )}
          </div>
        )}

        {/* Bio */}
        {host.bio && (
          <p className="text-gray-700 text-sm leading-relaxed mb-4">{host.bio}</p>
        )}

        {/* Languages */}
        {languages.length > 0 && (
          <div className="flex items-center gap-2 mb-4">
            <span className="text-sm text-gray-500">Languages:</span>
            <div className="flex gap-1 flex-wrap">
              {languages.map(lang => (
                <span key={lang} className="bg-gray-100 text-gray-700 text-xs px-2 py-0.5 rounded-full">
                  {lang}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Contact host button */}
        <button
          onClick={openContactModal}
          className="text-sm font-medium text-orange-600 hover:text-orange-700 border border-orange-300 rounded-lg px-4 py-2 hover:bg-orange-50 transition"
        >
          Contact Host
        </button>
      </div>

      {/* Contact Host Modal */}
      {showModal && (
        <div
          className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
          onClick={() => !sending && setShowModal(false)}
        >
          <div className="bg-white rounded-2xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
            {sent ? (
              <div className="text-center py-8">
                <p className="text-4xl mb-3">&#10003;</p>
                <p className="text-lg font-semibold text-green-600">Message sent!</p>
                <p className="text-sm text-gray-500 mt-1">
                  {host.name} will get back to you soon.
                </p>
              </div>
            ) : (
              <>
                <h3 className="text-lg font-bold mb-1">
                  Message {host.name}
                </h3>
                {listingTitle && (
                  <p className="text-sm text-gray-500 mb-4">
                    About: <span className="font-medium text-gray-700">{listingTitle}</span>
                  </p>
                )}

                {/* Quick question chips */}
                <div className="mb-4">
                  <p className="text-xs text-gray-500 mb-2">Quick questions</p>
                  <div className="flex flex-wrap gap-1.5">
                    {QUICK_QUESTIONS.map((q) => (
                      <button
                        key={q}
                        onClick={() => handleQuickQuestion(q)}
                        className={`text-xs px-3 py-1.5 rounded-full border transition ${
                          message === q
                            ? 'bg-orange-50 border-orange-300 text-orange-600'
                            : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
                        }`}
                      >
                        {q}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Custom message */}
                <div className="mb-4">
                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    rows={4}
                    className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 resize-none"
                    placeholder="Write your message to the host..."
                  />
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => setShowModal(false)}
                    disabled={sending}
                    className="flex-1 px-4 py-2.5 rounded-xl border text-sm font-medium hover:bg-gray-50 transition disabled:opacity-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSendMessage}
                    disabled={sending || !message.trim()}
                    className="flex-1 px-4 py-2.5 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 transition disabled:opacity-50"
                  >
                    {sending ? 'Sending...' : 'Send Message'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
