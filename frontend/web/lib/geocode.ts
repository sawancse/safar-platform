/**
 * Geocode an Indian pincode + city using OpenStreetMap Nominatim (free, no API key).
 * Returns {lat, lng} or null if not found.
 */
export async function geocodeIndianAddress(
  pincode: string,
  city: string,
  state: string
): Promise<{ lat: number; lng: number } | null> {
  try {
    const query = encodeURIComponent(`${pincode} ${city} ${state} India`);
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&countrycodes=in&limit=1&q=${query}`,
      { headers: { 'User-Agent': 'SafarPlatform/1.0' } }
    );
    if (!res.ok) return null;
    const data = await res.json();
    if (data.length === 0) return null;
    return {
      lat: parseFloat(data[0].lat),
      lng: parseFloat(data[0].lon),
    };
  } catch {
    return null;
  }
}
