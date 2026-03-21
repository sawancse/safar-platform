/**
 * MakeMyTrip-style rating label system.
 * Returns label and color class based on numeric rating.
 */
export function getRatingLabel(rating: number): { label: string; bg: string; text: string } {
  if (rating >= 4.5) return { label: 'Excellent', bg: 'bg-green-600', text: 'text-white' };
  if (rating >= 4.0) return { label: 'Very Good', bg: 'bg-green-500', text: 'text-white' };
  if (rating >= 3.5) return { label: 'Good', bg: 'bg-teal-500', text: 'text-white' };
  if (rating >= 3.0) return { label: 'Average', bg: 'bg-orange-400', text: 'text-white' };
  return { label: 'Below Average', bg: 'bg-gray-400', text: 'text-white' };
}
