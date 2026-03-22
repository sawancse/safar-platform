'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';
const CHANNEL_ICONS: Record<string, string> = {
  AIRBNB: '🏠', BOOKING_COM: '🅱️', MMT: '🇮🇳', OYO: '🔴',
  AGODA: '🟣', EXPEDIA: '✈️', GOIBIBO: '🚂',
};

export default function HostChannelManagerTab({ listingId }: { listingId: string }) {
  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') ?? '' : '';
  const [status, setStatus] = useState<any>(null);
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [connected, setConnected] = useState(false);
  const [syncing, setSyncing] = useState('');

  useEffect(() => { loadStatus(); }, [listingId]);

  async function loadStatus() {
    setLoading(true);
    try {
      const s = await api.getChannelStatus(listingId, token!);
      setStatus(s);
      setConnected(s.status === 'CONNECTED');
      const l = await api.getChannelSyncLogs(listingId, token!);
      setLogs(l.content || []);
    } catch {
      setConnected(false);
    }
    setLoading(false);
  }

  async function connect() {
    try {
      await api.connectChannelManager(listingId, token!);
      loadStatus();
    } catch (e: any) {
      alert(e.message || 'Connection failed');
    }
  }

  async function disconnect() {
    if (!confirm('Disconnect from channel manager?')) return;
    await api.disconnectChannelManager(listingId, token!);
    loadStatus();
  }

  async function sync(type: string) {
    setSyncing(type);
    try {
      if (type === 'rates') await api.syncChannelRates(listingId, token!);
      if (type === 'availability') await api.syncChannelAvailability(listingId, token!);
      if (type === 'bookings') await api.pullChannelBookings(listingId, token!);
      loadStatus();
    } catch (e: any) {
      alert('Sync failed: ' + (e.message || ''));
    }
    setSyncing('');
  }

  if (loading) return <div className="p-6 text-center text-gray-500">Loading channel manager...</div>;

  if (!connected) {
    return (
      <div className="text-center py-12">
        <div className="text-4xl mb-4">📡</div>
        <h3 className="text-xl font-semibold mb-2">Connect to Channel Manager</h3>
        <p className="text-gray-500 mb-6 max-w-md mx-auto">
          Sync your listing with Airbnb, Booking.com, MakeMyTrip, OYO, Agoda and more via Channex.io
        </p>
        <button onClick={connect} className="bg-orange-500 text-white px-6 py-3 rounded-xl font-medium hover:bg-orange-600">
          Connect Now
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Status Card */}
      <div className="bg-white rounded-xl border p-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <div className={`w-3 h-3 rounded-full ${status?.status === 'CONNECTED' ? 'bg-green-500' : 'bg-yellow-500'}`} />
            <span className="font-semibold">{status?.status}</span>
          </div>
          {status?.lastSyncAt && (
            <p className="text-sm text-gray-500 mt-1">
              Last sync: {new Date(status.lastSyncAt).toLocaleString()}
            </p>
          )}
        </div>
        <button onClick={disconnect} className="text-red-500 text-sm hover:underline">Disconnect</button>
      </div>

      {/* Connected Channels */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="font-semibold text-lg mb-4">Available Channels</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {['AIRBNB', 'BOOKING_COM', 'MMT', 'OYO', 'AGODA', 'EXPEDIA', 'GOIBIBO'].map(ch => (
            <div key={ch} className="border rounded-lg p-3 flex items-center gap-2">
              <span className="text-xl">{CHANNEL_ICONS[ch]}</span>
              <span className="text-sm font-medium">{ch.replace('_', '.')}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Sync Controls */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="font-semibold text-lg mb-4">Manual Sync</h3>
        <div className="flex gap-3 flex-wrap">
          {[
            { key: 'rates', label: 'Push Rates', icon: '💰' },
            { key: 'availability', label: 'Push Availability', icon: '📅' },
            { key: 'bookings', label: 'Pull Bookings', icon: '📥' },
          ].map(({ key, label, icon }) => (
            <button key={key} onClick={() => sync(key)}
              disabled={syncing === key}
              className="flex items-center gap-2 border rounded-lg px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-50">
              <span>{icon}</span>
              {syncing === key ? 'Syncing...' : label}
            </button>
          ))}
        </div>
      </div>

      {/* Sync Logs */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="font-semibold text-lg mb-4">Recent Sync Activity</h3>
        {logs.length === 0 ? (
          <p className="text-gray-400 text-sm">No sync activity yet</p>
        ) : (
          <div className="space-y-2">
            {logs.slice(0, 10).map((log: any, i: number) => (
              <div key={i} className="flex items-center justify-between text-sm border-b pb-2">
                <div className="flex items-center gap-2">
                  <span className={log.success ? 'text-green-500' : 'text-red-500'}>{log.success ? '✓' : '✗'}</span>
                  <span className="font-medium">{log.direction} {log.syncType}</span>
                  {log.channelName && <span className="text-gray-400">{log.channelName}</span>}
                </div>
                <span className="text-gray-400">{new Date(log.syncedAt).toLocaleString()}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
