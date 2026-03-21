export function formatPaise(paise: number): string {
  const rupees = paise / 100;
  return `₹${rupees.toLocaleString('en-IN')}`;
}

export function toISODate(date: Date): string {
  return date.toISOString().split('T')[0];
}
