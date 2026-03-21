import { useEffect, useState, useRef, useCallback } from 'react';
import * as Network from 'expo-network';
import { syncQueue, type SyncResult } from '@/lib/offlineQueue';

export interface NetworkStatus {
  /** Whether the device has a network connection (wifi / cellular) */
  isConnected: boolean;
  /** Whether the internet is actually reachable (can be false behind captive portals) */
  isInternetReachable: boolean;
  /** Manually trigger a sync of the offline queue */
  syncNow: () => Promise<SyncResult | null>;
  /** Whether a sync is currently in progress */
  isSyncing: boolean;
  /** Result of the last auto-sync (null if none yet) */
  lastSyncResult: SyncResult | null;
}

/**
 * Hook that monitors network connectivity using Expo's Network API
 * and automatically syncs the offline booking queue when the device
 * comes back online.
 *
 * Usage:
 * ```tsx
 * const { isConnected, isInternetReachable, syncNow } = useNetworkStatus();
 * ```
 */
export function useNetworkStatus(): NetworkStatus {
  const [isConnected, setIsConnected] = useState(true);
  const [isInternetReachable, setIsInternetReachable] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastSyncResult, setLastSyncResult] = useState<SyncResult | null>(null);

  // Track previous connection state so we only auto-sync on reconnect
  const wasConnected = useRef(true);

  const doSync = useCallback(async (): Promise<SyncResult | null> => {
    if (isSyncing) return null;
    setIsSyncing(true);
    try {
      const result = await syncQueue();
      setLastSyncResult(result);
      return result;
    } catch {
      return null;
    } finally {
      setIsSyncing(false);
    }
  }, [isSyncing]);

  useEffect(() => {
    let mounted = true;

    // Poll network state every 3 seconds.
    // expo-network does not provide a subscription API, so polling is
    // the standard approach with the Expo managed workflow.
    async function checkNetwork() {
      try {
        const state = await Network.getNetworkStateAsync();
        if (!mounted) return;

        const connected = state.isConnected ?? false;
        const reachable = state.isInternetReachable ?? false;

        setIsConnected(connected);
        setIsInternetReachable(reachable);

        // Auto-sync when transitioning from offline -> online
        if (connected && reachable && !wasConnected.current) {
          doSync();
        }

        wasConnected.current = connected && reachable;
      } catch {
        // Network check failed; assume offline
        if (!mounted) return;
        setIsConnected(false);
        setIsInternetReachable(false);
        wasConnected.current = false;
      }
    }

    // Initial check
    checkNetwork();

    const interval = setInterval(checkNetwork, 3000);

    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, [doSync]);

  return {
    isConnected,
    isInternetReachable,
    syncNow: doSync,
    isSyncing,
    lastSyncResult,
  };
}
