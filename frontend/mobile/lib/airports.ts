export interface Airport { code: string; city: string; name: string; }

export const INDIAN_AIRPORTS: Airport[] = [
  { code: 'DEL', city: 'Delhi', name: 'Indira Gandhi Intl' },
  { code: 'BOM', city: 'Mumbai', name: 'Chhatrapati Shivaji Intl' },
  { code: 'BLR', city: 'Bangalore', name: 'Kempegowda Intl' },
  { code: 'HYD', city: 'Hyderabad', name: 'Rajiv Gandhi Intl' },
  { code: 'MAA', city: 'Chennai', name: 'Chennai Intl' },
  { code: 'CCU', city: 'Kolkata', name: 'Netaji Subhas Chandra Bose' },
  { code: 'PNQ', city: 'Pune', name: 'Pune Airport' },
  { code: 'AMD', city: 'Ahmedabad', name: 'Sardar Vallabhbhai Patel' },
  { code: 'GOI', city: 'Goa', name: 'Dabolim' },
  { code: 'GOX', city: 'Goa', name: 'Manohar Intl (Mopa)' },
  { code: 'COK', city: 'Kochi', name: 'Cochin Intl' },
  { code: 'JAI', city: 'Jaipur', name: 'Jaipur Intl' },
  { code: 'LKO', city: 'Lucknow', name: 'Chaudhary Charan Singh' },
  { code: 'PAT', city: 'Patna', name: 'Jay Prakash Narayan' },
  { code: 'IXC', city: 'Chandigarh', name: 'Chandigarh Airport' },
  { code: 'NAG', city: 'Nagpur', name: 'Dr. Babasaheb Ambedkar' },
  { code: 'BBI', city: 'Bhubaneswar', name: 'Biju Patnaik Intl' },
  { code: 'GAU', city: 'Guwahati', name: 'Lokpriya Gopinath Bordoloi' },
  { code: 'TRV', city: 'Thiruvananthapuram', name: 'Trivandrum Intl' },
  { code: 'IXM', city: 'Madurai', name: 'Madurai Airport' },
  { code: 'VNS', city: 'Varanasi', name: 'Lal Bahadur Shastri' },
  { code: 'SXR', city: 'Srinagar', name: 'Srinagar Intl' },
  { code: 'IXB', city: 'Bagdogra', name: 'Bagdogra Airport' },
  { code: 'ATQ', city: 'Amritsar', name: 'Sri Guru Ram Dass Jee' },
  { code: 'IDR', city: 'Indore', name: 'Devi Ahilya Bai Holkar' },
  { code: 'RPR', city: 'Raipur', name: 'Swami Vivekananda' },
  { code: 'VTZ', city: 'Visakhapatnam', name: 'Visakhapatnam Airport' },
  { code: 'CJB', city: 'Coimbatore', name: 'Coimbatore Intl' },
  { code: 'IXR', city: 'Ranchi', name: 'Birsa Munda' },
  { code: 'UDR', city: 'Udaipur', name: 'Maharana Pratap' },
];

export const INTL_AIRPORTS: Airport[] = [
  { code: 'DXB', city: 'Dubai', name: 'Dubai Intl' },
  { code: 'AUH', city: 'Abu Dhabi', name: 'Zayed Intl' },
  { code: 'SIN', city: 'Singapore', name: 'Changi' },
  { code: 'BKK', city: 'Bangkok', name: 'Suvarnabhumi' },
  { code: 'KUL', city: 'Kuala Lumpur', name: 'KLIA' },
  { code: 'LHR', city: 'London', name: 'Heathrow' },
  { code: 'JFK', city: 'New York', name: 'John F. Kennedy' },
  { code: 'DOH', city: 'Doha', name: 'Hamad Intl' },
  { code: 'CDG', city: 'Paris', name: 'Charles de Gaulle' },
  { code: 'HKG', city: 'Hong Kong', name: 'Hong Kong Intl' },
  { code: 'CMB', city: 'Colombo', name: 'Bandaranaike Intl' },
  { code: 'KTM', city: 'Kathmandu', name: 'Tribhuvan Intl' },
  { code: 'SFO', city: 'San Francisco', name: 'San Francisco Intl' },
  { code: 'SYD', city: 'Sydney', name: 'Kingsford Smith' },
  { code: 'FRA', city: 'Frankfurt', name: 'Frankfurt Airport' },
];

export const ALL_AIRPORTS: Airport[] = [...INDIAN_AIRPORTS, ...INTL_AIRPORTS];

const INDIAN_CODES = new Set(INDIAN_AIRPORTS.map((a) => a.code));

export function isDomesticRoute(origin: string, destination: string): boolean {
  return INDIAN_CODES.has(origin) && INDIAN_CODES.has(destination);
}

export function findAirport(code: string): Airport | undefined {
  return ALL_AIRPORTS.find((a) => a.code === code);
}

export function searchAirports(q: string): Airport[] {
  const s = q.trim().toLowerCase();
  if (!s) return ALL_AIRPORTS;
  return ALL_AIRPORTS.filter(
    (a) => a.code.toLowerCase().includes(s) || a.city.toLowerCase().includes(s) || a.name.toLowerCase().includes(s),
  );
}

export const CABIN_CLASSES = [
  { value: 'economy', label: 'Economy' },
  { value: 'premium_economy', label: 'Premium Economy' },
  { value: 'business', label: 'Business' },
  { value: 'first', label: 'First' },
];

export function cabinLabel(v?: string): string {
  return CABIN_CLASSES.find((c) => c.value === v)?.label ?? (v ?? '');
}

/** Parse an ISO-8601 duration like "PT2H30M" to minutes. */
export function durationToMinutes(iso?: string): number {
  if (!iso) return 0;
  const m = /PT(?:(\d+)H)?(?:(\d+)M)?/.exec(iso);
  if (!m) return 0;
  return (parseInt(m[1] ?? '0', 10) * 60) + parseInt(m[2] ?? '0', 10);
}

export function formatDuration(iso?: string): string {
  const mins = durationToMinutes(iso);
  if (!mins) return '';
  return `${Math.floor(mins / 60)}h ${mins % 60}m`;
}

export function formatTime(isoDateTime?: string): string {
  if (!isoDateTime) return '';
  const d = new Date(isoDateTime);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false });
}
