// Pandit / Puja catalog for the mobile app — mirrors safar-web's
// app/services/pandit/catalog.ts so web + mobile show the same pujas.

export type PanditTier = 'STANDARD' | 'PREMIUM' | 'LUXURY';

export type PujaService = {
  key: string;
  label: string;
  tier: PanditTier;
  pricePaise: number;
  durationHours: number;
  recommendedDakshinaPaise: number;
  inclusions: string[];
  samagri: string[];
  tags: string[];
};

export const OCCASIONS = [
  { key: 'HOUSEWARMING', label: 'Housewarming (Griha Pravesh)', icon: '🏠' },
  { key: 'DOSH_NIVARAN', label: 'Dosh Nivaran & Grah Shanti',   icon: '🪐' },
  { key: 'PAATH_JAAP',   label: 'Paath, Jaap & Havan',          icon: '📿' },
  { key: 'ANNIVERSARY',  label: 'Anniversary Pujas',            icon: '💝' },
  { key: 'BIRTHDAY',     label: 'Birthday / Ayush Homam',       icon: '🎂' },
  { key: 'CAR_VEHICLE',  label: 'Vehicle / Car Puja',           icon: '🚗' },
  { key: 'BABY',         label: 'Namkaran / Mundan',            icon: '👶' },
  { key: 'FESTIVAL',     label: 'Festival Pujas',               icon: '🪔' },
  { key: 'MARRIAGE',     label: 'Marriage / Engagement',        icon: '💍' },
  { key: 'OTHER',        label: 'Other / Custom',               icon: '🙏' },
];

export const ARRIVAL_SLOTS = [
  '06:00', '06:30', '07:00', '07:30', '08:00', '08:30',
  '09:00', '10:00', '11:00', '12:00', '14:00', '16:00',
  '17:00', '18:00', '19:00',
];

export function formatSlot(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number);
  const period = h >= 12 ? 'pm' : 'am';
  const hr12 = h % 12 === 0 ? 12 : h % 12;
  return m === 0 ? `${hr12} ${period}` : `${hr12}:${String(m).padStart(2, '0')} ${period}`;
}

export const LANGUAGES = [
  'Hindi', 'Sanskrit', 'Telugu', 'Tamil', 'Kannada', 'Malayalam',
  'Marathi', 'Bengali', 'Gujarati', 'Odia', 'Punjabi',
];

export const PUJAS: PujaService[] = [
  { key: 'griha_pravesh_standard', label: 'Griha Pravesh (Standard)', tier: 'STANDARD', pricePaise: 350000, durationHours: 2, recommendedDakshinaPaise: 50000, inclusions: ['1 experienced pandit', 'Basic samagri kit', 'Mantra booklet'], samagri: ['Diyas, wicks and ghee', 'Kumkum, haldi, chandan', 'Camphor, incense', 'Flowers, coconut'], tags: ['HOUSEWARMING'] },
  { key: 'griha_pravesh_premium', label: 'Griha Pravesh (Premium)', tier: 'PREMIUM', pricePaise: 600000, durationHours: 3, recommendedDakshinaPaise: 75000, inclusions: ['2 pandits', 'Premium samagri kit', 'Havan setup', 'Personalised sankalp'], samagri: ['Extended samagri (15+ items)', 'Havan samagri & hawan kund', 'Silver tumbler & kalash', 'Mango leaves torans'], tags: ['HOUSEWARMING'] },
  { key: 'vastu_shanti', label: 'Vastu Shanti Puja', tier: 'LUXURY', pricePaise: 900000, durationHours: 4, recommendedDakshinaPaise: 100000, inclusions: ['3 pandits', 'Full vastu yagna with 9 dishas', 'Havan + Sankalp + Aarti', 'Custom mantra chanting'], samagri: ['Premium samagri kit', 'Navagraha samagri', 'Silver kalash set', 'Full havan kund + ghee'], tags: ['HOUSEWARMING'] },
  { key: 'bhoomi_pujan', label: 'Bhoomi Pujan (Ground-breaking)', tier: 'PREMIUM', pricePaise: 550000, durationHours: 3, recommendedDakshinaPaise: 75000, inclusions: ['2 pandits', 'Bhoomi pujan + Vastu purush sankalp', 'Foundation-stone ritual', 'Naag-devta & directional puja'], samagri: ['Bhoomi pujan samagri', 'Silver naag-naagin pair', 'Copper kalash, navadhanya'], tags: ['HOUSEWARMING'] },

  { key: 'kaal_sarp_dosh', label: 'Kaal Sarp Dosh Nivaran', tier: 'LUXURY', pricePaise: 1100000, durationHours: 4, recommendedDakshinaPaise: 150000, inclusions: ['2 senior pandits', 'Rahu-Ketu shanti japa', 'Naag-bali & sarpa-dosha homam', 'Personalised sankalp by birth chart'], samagri: ['Kaal sarp dosh samagri', 'Silver naag-naagin pair', 'Rudraksha & nag-kesar', 'Navagraha samidha for havan'], tags: ['DOSH_NIVARAN'] },
  { key: 'mangal_dosh_shanti', label: 'Mangal Dosh (Manglik) Shanti', tier: 'PREMIUM', pricePaise: 700000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Mangal grah shanti japa', 'Kumbh vivah guidance (if advised)', 'Red-coral & Hanuman puja'], samagri: ['Mangal dosh samagri', 'Red flowers, masoor dal, gud', 'Copper kalash', 'Havan samidha + ghee'], tags: ['DOSH_NIVARAN'] },
  { key: 'pitra_dosh_shanti', label: 'Pitra Dosh / Shradh Shanti', tier: 'PREMIUM', pricePaise: 650000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Pitra dosh nivaran & tarpan', 'Pind-daan guidance', 'Narayan bali sankalp'], samagri: ['Tarpan & pind-daan samagri', 'Black til, jau, kusha grass', 'Banana leaf & kheer items'], tags: ['DOSH_NIVARAN'] },
  { key: 'shani_shanti', label: 'Shani Shanti / Sade Sati Puja', tier: 'PREMIUM', pricePaise: 700000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['1 senior pandit', 'Shani grah japa (23,000 mantras)', 'Sade-sati / dhaiya relief sankalp', 'Til-tel abhishek'], samagri: ['Shani shanti samagri', 'Black til, mustard oil, iron items', 'Blue flowers, urad dal'], tags: ['DOSH_NIVARAN'] },

  { key: 'sundarkand_paath', label: 'Sundarkand Paath', tier: 'STANDARD', pricePaise: 350000, durationHours: 3, recommendedDakshinaPaise: 51000, inclusions: ['1 pandit (with bhajan mandali on request)', 'Full Sundarkand recitation', 'Hanuman puja + aarti', 'Prasad guidance'], samagri: ['Hanuman puja samagri', 'Sindoor, chameli oil', 'Boondi/laddu prasad guidance', 'Flowers + incense'], tags: ['PAATH_JAAP'] },
  { key: 'maha_mrityunjaya_jaap', label: 'Maha Mrityunjaya Jaap', tier: 'LUXURY', pricePaise: 1100000, durationHours: 5, recommendedDakshinaPaise: 150000, inclusions: ['3 pandits', '1.25 lakh Maha Mrityunjaya mantra jaap', 'Rudra havan + abhishek', 'Health & longevity sankalp'], samagri: ['Maha Mrityunjaya samagri', 'Rudraksha mala, bilva patra', 'Panchamrit abhishek items', 'Havan kund + ghee + samidha'], tags: ['PAATH_JAAP'] },
  { key: 'rudrabhishek', label: 'Rudrabhishek Puja', tier: 'PREMIUM', pricePaise: 650000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Shiva Rudri abhishek (11 path)', 'Panchamrit & jalabhishek', 'Bilva-archana + aarti'], samagri: ['Rudrabhishek samagri', 'Bilva patra, dhatura, bhang', 'Panchamrit (milk, curd, ghee, honey, sugar)', 'Gangajal + flowers'], tags: ['PAATH_JAAP'] },

  { key: 'satyanarayan_small', label: 'Satyanarayan Puja (Small)', tier: 'STANDARD', pricePaise: 300000, durationHours: 2, recommendedDakshinaPaise: 50000, inclusions: ['1 pandit', 'Satyanarayan katha booklet', 'Samagri kit', 'Prasad arrangement guide'], samagri: ['Diyas, wicks, ghee', 'Kumkum, chandan, haldi', 'Flowers, tulsi', 'Panchamrit ingredients'], tags: ['ANNIVERSARY'] },
  { key: 'silver_jubilee', label: 'Silver Jubilee Puja', tier: 'PREMIUM', pricePaise: 750000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Silver jubilee mantras', 'Couple-specific blessings', 'Extended havan'], samagri: ['Premium kit', 'Silver kalash', '25-year jubilee yagna samagri', 'Garlands for the couple'], tags: ['ANNIVERSARY'] },
  { key: 'golden_jubilee', label: 'Golden Jubilee Puja', tier: 'LUXURY', pricePaise: 1200000, durationHours: 4, recommendedDakshinaPaise: 150000, inclusions: ['3 pandits', 'Grand havan', 'Guest blessing ceremony', 'Personalised mantras + sankalp'], samagri: ['Luxury samagri kit', 'Brass + silver kalash set', 'Full yagna with gold-dipped items', 'Floral decoration for altar'], tags: ['ANNIVERSARY'] },

  { key: 'ayush_homam', label: 'Ayush Homam (Birthday)', tier: 'PREMIUM', pricePaise: 600000, durationHours: 3, recommendedDakshinaPaise: 75000, inclusions: ['2 pandits', 'Ayush homam yagna', 'Sankalp for long life', 'Prasad distribution guidance'], samagri: ['Ayush homam samagri', 'Havan kund + ghee', 'Special ayur herbs', 'Flowers + garland'], tags: ['BIRTHDAY'] },
  { key: 'navagraha_shanti', label: 'Navagraha Shanti Puja', tier: 'LUXURY', pricePaise: 1000000, durationHours: 4, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Full navagraha mantras', 'Individual graha puja', 'Sankalp for birth star'], samagri: ['Navagraha samagri kit', 'Nine-metal kalash', 'Graha-specific flowers and seeds'], tags: ['BIRTHDAY'] },

  { key: 'car_puja', label: 'Vehicle / Car Puja', tier: 'STANDARD', pricePaise: 150000, durationHours: 1, recommendedDakshinaPaise: 30000, inclusions: ['1 pandit', 'Car puja samagri', 'Wheel aarti', 'Vehicle stickers + lemon + chilli'], samagri: ['Coconut, flowers, kumkum', 'Incense + camphor', 'Lemon + chilli charm', 'Blessed red thread'], tags: ['CAR_VEHICLE'] },

  { key: 'namkaran', label: 'Namkaran (Naming)', tier: 'PREMIUM', pricePaise: 500000, durationHours: 2, recommendedDakshinaPaise: 75000, inclusions: ['1 senior pandit', 'Nakshatra calculation', 'Lucky letter + name guidance', 'Cradle puja'], samagri: ['Standard samagri kit', 'Baby cradle items', 'Gold-foil paper for name', 'Milk + honey + ghee'], tags: ['BABY'] },
  { key: 'annaprashan', label: 'Annaprashan (First Rice)', tier: 'PREMIUM', pricePaise: 500000, durationHours: 2, recommendedDakshinaPaise: 75000, inclusions: ['1 pandit', 'Annaprashan mantras', 'Silver spoon + bowl (loaned)', 'Family blessings ceremony'], samagri: ['Annaprashan samagri', 'Rice, ghee, honey, fruits', 'Flowers + kumkum'], tags: ['BABY'] },
  { key: 'mundan', label: 'Mundan (Tonsure) Ceremony', tier: 'STANDARD', pricePaise: 400000, durationHours: 2, recommendedDakshinaPaise: 51000, inclusions: ['1 pandit', 'Mundan sankalp & mantras', 'Choti-rakhne ritual', 'Coordination with barber (customer-arranged)'], samagri: ['Mundan samagri kit', 'Haldi, kumkum, akshat', 'Flowers + diya'], tags: ['BABY'] },

  { key: 'lakshmi_puja_diwali', label: 'Lakshmi Puja (Diwali)', tier: 'PREMIUM', pricePaise: 500000, durationHours: 2, recommendedDakshinaPaise: 100000, inclusions: ['1 pandit', 'Lakshmi mantra chanting', 'Wealth altar setup', 'Deep-daan guidance'], samagri: ['Premium samagri', 'Silver coin + kalash', '21 diyas', 'Marigold + lotus flowers'], tags: ['FESTIVAL'] },
  { key: 'ganesh_puja', label: 'Ganesh Chaturthi Puja', tier: 'STANDARD', pricePaise: 400000, durationHours: 2, recommendedDakshinaPaise: 51000, inclusions: ['1 pandit', 'Ganesh sthapana + visarjan prep', 'Modak prasad guidance', '16-step puja'], samagri: ['Ganesh puja kit', 'Durva grass, red flowers', 'Modak ingredients'], tags: ['FESTIVAL'] },

  { key: 'engagement', label: 'Engagement (Sagai) Puja', tier: 'PREMIUM', pricePaise: 800000, durationHours: 3, recommendedDakshinaPaise: 100000, inclusions: ['2 pandits', 'Ring-exchange mantras', 'Family sankalp', 'Ganesh + Lakshmi puja'], samagri: ['Engagement samagri', 'Ring-exchange plate', 'Haldi + kumkum for couples', 'Flowers + garlands'], tags: ['MARRIAGE'] },
  { key: 'wedding_pandit', label: 'Wedding Pandit (Full)', tier: 'LUXURY', pricePaise: 2500000, durationHours: 6, recommendedDakshinaPaise: 250000, inclusions: ['3 pandits', 'Complete wedding rituals', 'Pre-wedding pujas', 'Mandap + havan setup guidance'], samagri: ['Full wedding samagri', 'Havan kund set', 'Saptapadi + kanyadaan items', 'Mangalsutra blessings'], tags: ['MARRIAGE'] },

  { key: 'custom_puja', label: 'Custom / Any other puja', tier: 'STANDARD', pricePaise: 300000, durationHours: 2, recommendedDakshinaPaise: 50000, inclusions: ['1 pandit', 'Scope to be agreed via WhatsApp', 'Basic samagri kit'], samagri: ['Configurable on request'], tags: ['OTHER'] },
];

export function pujasForOccasion(key: string): PujaService[] {
  return PUJAS.filter(p => p.tags.includes(key));
}

/** Pandits needed — read off the inclusions ("2 pandits"). Defaults to 1. */
export function panditCountFor(puja: PujaService): number {
  for (const inc of puja.inclusions) {
    const m = inc.match(/(\d+)\s*(?:senior\s+|experienced\s+)?pandits?/i);
    if (m) return Math.max(1, parseInt(m[1], 10));
  }
  return 1;
}

/** Samagri quality tier derived from the puja tier. */
export function samagriTierFor(puja: PujaService): 'BASIC' | 'STANDARD' | 'PREMIUM' {
  return puja.tier === 'STANDARD' ? 'BASIC' : puja.tier === 'PREMIUM' ? 'STANDARD' : 'PREMIUM';
}
