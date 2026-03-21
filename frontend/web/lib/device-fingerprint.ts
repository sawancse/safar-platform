/**
 * Generate a simple device fingerprint from browser properties.
 * Not cryptographically unique but sufficient for "remember this device" feature.
 */
export function generateDeviceFingerprint(): string {
  const components = [
    navigator.userAgent,
    navigator.language,
    screen.width + 'x' + screen.height,
    screen.colorDepth,
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    navigator.hardwareConcurrency || 0,
    navigator.maxTouchPoints || 0,
  ];

  // Simple hash
  const str = components.join('|');
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  return 'df_' + Math.abs(hash).toString(36);
}

/**
 * Get a human-readable device name.
 */
export function getDeviceName(): string {
  const ua = navigator.userAgent;
  if (ua.includes('iPhone')) return 'iPhone';
  if (ua.includes('iPad')) return 'iPad';
  if (ua.includes('Android')) return 'Android Device';
  if (ua.includes('Mac')) return 'Mac';
  if (ua.includes('Windows')) return 'Windows PC';
  if (ua.includes('Linux')) return 'Linux PC';
  return 'Browser';
}
