export function formatPaise(paise: number): string {
  return `₹${Math.round(paise / 100).toLocaleString('en-IN')}`;
}

export function formatPricingUnit(unit: 'NIGHT' | 'HOUR'): string {
  return unit === 'HOUR' ? 'hr' : 'night';
}

export function cn(...classes: (string | undefined | null | false)[]): string {
  return classes.filter(Boolean).join(' ');
}

export function toISODate(date: Date): string {
  return date.toISOString().split('T')[0];
}
