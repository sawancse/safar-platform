import AsyncStorage from '@react-native-async-storage/async-storage';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const QUEUE_KEY = 'safar_offline_booking_queue';

export interface QueuedBookingRequest {
  id: string;
  listingId: string;
  checkInDate: string;
  checkOutDate: string;
  guests: number;
  /** ISO timestamp when the request was queued */
  queuedAt: string;
  /** Number of sync attempts so far */
  attempts: number;
  /** Last error message, if any */
  lastError?: string;
}

/**
 * Read the entire offline booking queue from AsyncStorage.
 */
export async function getQueuedBookings(): Promise<QueuedBookingRequest[]> {
  try {
    const raw = await AsyncStorage.getItem(QUEUE_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as QueuedBookingRequest[];
  } catch {
    return [];
  }
}

/**
 * Persist the queue back to AsyncStorage.
 */
async function saveQueue(queue: QueuedBookingRequest[]): Promise<void> {
  await AsyncStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
}

/**
 * Add a booking request to the offline queue.
 * Called when the device is offline and the user tries to book.
 */
export async function queueBooking(request: {
  listingId: string;
  checkInDate: string;
  checkOutDate: string;
  guests: number;
}): Promise<QueuedBookingRequest> {
  const entry: QueuedBookingRequest = {
    id: `offline-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`,
    listingId: request.listingId,
    checkInDate: request.checkInDate,
    checkOutDate: request.checkOutDate,
    guests: request.guests,
    queuedAt: new Date().toISOString(),
    attempts: 0,
  };

  const queue = await getQueuedBookings();
  queue.push(entry);
  await saveQueue(queue);

  return entry;
}

/**
 * Remove a single queued booking by its offline ID.
 */
export async function removeQueuedBooking(id: string): Promise<void> {
  const queue = await getQueuedBookings();
  const filtered = queue.filter((item) => item.id !== id);
  await saveQueue(filtered);
}

/**
 * Clear all queued bookings.
 */
export async function clearQueue(): Promise<void> {
  await AsyncStorage.removeItem(QUEUE_KEY);
}

export interface SyncResult {
  synced: number;
  failed: number;
  errors: Array<{ id: string; error: string }>;
}

/**
 * Replay all queued bookings against the API.
 * Should be called when the device comes back online.
 *
 * Each queued booking is attempted once per sync call.
 * Successfully synced bookings are removed from the queue.
 * Failed bookings remain in the queue with an incremented attempt count.
 *
 * Returns a summary of what was synced and what failed.
 */
export async function syncQueue(): Promise<SyncResult> {
  const token = await getAccessToken();
  if (!token) {
    return { synced: 0, failed: 0, errors: [{ id: 'auth', error: 'Not authenticated' }] };
  }

  const queue = await getQueuedBookings();
  if (queue.length === 0) {
    return { synced: 0, failed: 0, errors: [] };
  }

  const result: SyncResult = { synced: 0, failed: 0, errors: [] };
  const remaining: QueuedBookingRequest[] = [];

  for (const entry of queue) {
    try {
      await api.createBooking(
        {
          listingId: entry.listingId,
          checkInDate: entry.checkInDate,
          checkOutDate: entry.checkOutDate,
          guests: entry.guests,
        },
        token,
      );
      result.synced++;
    } catch (error: any) {
      const message = error?.message ?? 'Unknown error';
      result.failed++;
      result.errors.push({ id: entry.id, error: message });
      remaining.push({
        ...entry,
        attempts: entry.attempts + 1,
        lastError: message,
      });
    }
  }

  await saveQueue(remaining);
  return result;
}
