export type ListingType = 'HOME' | 'ROOM' | 'UNIQUE' | 'COMMERCIAL' |
  'VILLA' | 'RESORT' | 'HOMESTAY' | 'HOSTEL' | 'GUESTHOUSE' |
  'FARMSTAY' | 'BNB' | 'LODGE' | 'CHALET' | 'APARTMENT' | 'VACATION_HOME' |
  'PG' | 'COLIVING' | 'HOTEL' | 'BUDGET_HOTEL' | 'HOSTEL_DORM';

export type CancellationPolicy = 'FREE' | 'MODERATE' | 'STRICT';
export type MealPlan = 'NONE' | 'BREAKFAST' | 'HALF_BOARD' | 'FULL_BOARD' | 'ALL_INCLUSIVE' | 'KITCHEN_AVAILABLE';

export interface Listing {
  id: string;
  hostId: string;
  title: string;
  description: string;
  type: ListingType;
  commercialCategory?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  lat: number;
  lng: number;
  maxGuests: number;
  petFriendly?: boolean;
  maxPets?: number;
  bedrooms?: number;
  bathrooms?: number;
  totalRooms?: number;
  amenities?: string[];
  basePricePaise: number;
  pricingUnit: 'NIGHT' | 'HOUR' | 'MONTH';
  minBookingHours?: number;
  instantBook: boolean;
  status: string;
  avgRating?: number;
  reviewCount?: number;
  primaryPhotoUrl?: string;
  primaryVideoUrl?: string;
  isVerified?: boolean;
  gstApplicable: boolean;
  gstin?: string;
  starRating?: number;
  cancellationPolicy?: CancellationPolicy;
  mealPlan?: MealPlan;
  bedTypes?: string[];
  accessibilityFeatures?: string[];
  freeCancellation?: boolean;
  noPrepayment?: boolean;
  checkInFrom?: string;
  checkInUntil?: string;
  checkOutFrom?: string;
  checkOutUntil?: string;
  childrenAllowed?: boolean;
  parkingType?: 'NONE' | 'FREE' | 'PAID';
  breakfastIncluded?: boolean;
  areaSqft?: number;
  operatingHoursFrom?: string;
  operatingHoursUntil?: string;
  medicalStay?: boolean;
  hospitalNames?: string[];
  medicalSpecialties?: string[];
  // PG/Co-living
  occupancyType?: 'MALE' | 'FEMALE' | 'COED';
  genderPolicy?: 'MALE_ONLY' | 'FEMALE_ONLY' | 'COED';
  foodType?: 'VEG' | 'NON_VEG' | 'BOTH' | 'NONE';
  gateClosingTime?: string;
  noticePeriodDays?: number;
  securityDepositPaise?: number;
  // Hotel
  hotelChain?: string;
  frontDesk24h?: boolean;
  checkoutTime?: string;
  checkinTime?: string;
  // Hotel enhancements
  coupleFriendly?: boolean;
  propertyHighlights?: string; // comma-separated
  earlyBirdDiscountPercent?: number;
  earlyBirdDaysBefore?: number;
  zeroPaymentBooking?: boolean;
  locationHighlight?: string;
  aashrayReady?: boolean;
  aashrayDiscountPercent?: number;
  longTermMonthlyPaise?: number;
  minStayDays?: number;
  verificationNotes?: string;
  weeklyDiscountPercent?: number;
  monthlyDiscountPercent?: number;
  cleaningFeePaise?: number;
  visibilityBoostPercent?: number;
  preferredPartner?: boolean;
  panoramaUrl?: string;
  videoTourUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export type InclusionCategory = 'MEAL' | 'DISCOUNT' | 'FLEXIBILITY' | 'WELLNESS' | 'TRANSPORT' | 'AMENITY' | 'EXPERIENCE';
export type InclusionMode = 'INCLUDED' | 'PAID_ADDON' | 'COMPLIMENTARY';
export type ChargeType = 'PER_NIGHT' | 'PER_STAY' | 'PER_PERSON' | 'PER_HOUR' | 'PER_USE';

export interface RoomTypeInclusion {
  id: string;
  roomTypeId: string;
  category: InclusionCategory;
  name: string;
  description?: string;
  inclusionMode: InclusionMode;
  chargePaise: number;
  chargeType: ChargeType;
  discountPercent: number;
  terms?: string;
  isHighlight: boolean;
  sortOrder: number;
  isActive: boolean;
}

export interface RoomType {
  id: string;
  listingId: string;
  name: string;
  description?: string;
  count: number;
  basePricePaise: number;
  maxGuests: number;
  bedType?: string;
  bedCount?: number;
  areaSqft?: number;
  amenities?: string[];
  sortOrder: number;
  availableCount?: number;
  stayMode?: 'NIGHTLY' | 'HOURLY' | 'MONTHLY';
  sharingType?: 'PRIVATE' | 'TWO_SHARING' | 'THREE_SHARING' | 'FOUR_SHARING' | 'DORMITORY';
  roomVariant?: 'AC' | 'NON_AC' | 'FURNISHED' | 'SEMI_FURNISHED';
  totalBeds?: number;
  occupiedBeds?: number;
  primaryPhotoUrl?: string;
  photoUrls?: string[];
  inclusions?: RoomTypeInclusion[];
}

export interface PgPackage {
  id: string;
  listingId: string;
  name: string;
  description?: string;
  monthlyPricePaise: number;
  includesMeals: boolean;
  includesLaundry: boolean;
  includesWifi: boolean;
  includesHousekeeping: boolean;
  sortOrder: number;
  isActive: boolean;
}

export interface LocationSuggestion {
  id: string;
  name: string;
  displayName: string;
  type: string;
  city: string;
  state?: string;
  lat: number;
  lng: number;
  defaultRadiusKm: number;
}

export interface FilterAggregations {
  types: Record<string, number>;
  amenities: Record<string, number>;
  starRatings: Record<string, number>;
  mealPlans: Record<string, number>;
  cancellationPolicies: Record<string, number>;
  bedTypes: Record<string, number>;
  accessibilityFeatures: Record<string, number>;
  petFriendlyCount: number;
  instantBookCount: number;
  freeCancellationCount: number;
  noPrepaymentCount: number;
  priceRanges: Record<string, number>;
  ratingRanges: Record<string, number>;
  bedroomCounts: Record<string, number>;
  bathroomCounts: Record<string, number>;
}

export interface SearchResponse {
  content: Listing[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  aggregations?: FilterAggregations;
}

export interface Booking {
  id: string;
  bookingRef: string;
  listingId: string;
  listingTitle?: string;
  guestId: string;
  hostId: string;
  checkIn: string;
  checkOut: string;
  guestsCount: number;
  roomsCount?: number;
  nights: number;
  status: string;
  baseAmountPaise: number;
  insuranceAmountPaise: number;
  gstAmountPaise: number;
  totalAmountPaise: number;
  hostPayoutPaise?: number;
  guestFirstName?: string;
  guestLastName?: string;
  guestEmail?: string;
  guestPhone?: string;
  roomTypeId?: string;
  roomTypeName?: string;
  cancellationReason?: string;
  createdAt: string;
  updatedAt?: string;
  specialRequests?: string;
  checkedInAt?: string;
  completedAt?: string;
  hostEarningsPaise?: number;
  platformFeePaise?: number;
  cleaningFeePaise?: number;
  commissionRate?: number;
  hasReview?: boolean;
  reviewRating?: number;
  reviewedAt?: string;
  noticePeriodDays?: number;
  securityDepositPaise?: number;
  securityDepositStatus?: string;
  inclusionsTotalPaise?: number;
  roomSelections?: {
    id: string;
    roomTypeId: string;
    roomTypeName: string;
    count: number;
    pricePerUnitPaise: number;
    totalPaise: number;
  }[];
  guests?: {
    id: string;
    fullName: string;
    email?: string;
    phone?: string;
    age?: number;
    idType?: string;
    idNumber?: string;
    roomAssignment?: string;
    isPrimary?: boolean;
  }[];
  inclusions?: {
    id: string;
    inclusionId: string;
    category: string;
    name: string;
    description?: string;
    inclusionMode: string;
    chargePaise: number;
    chargeType: string;
    discountPercent: number;
    terms?: string;
    quantity: number;
    totalPaise: number;
  }[];
}

export interface CreateBookingRequest {
  listingId: string;
  checkIn: string;
  checkOut: string;
  guestsCount: number;
  adultsCount?: number;
  childrenCount?: number;
  infantsCount?: number;
  petsCount?: number;
  roomsCount?: number;
  roomTypeId?: string;
  guestFirstName: string;
  guestLastName: string;
  guestEmail: string;
  guestPhone: string;
  bookingFor?: string;
  travelForWork?: boolean;
  airportShuttle?: boolean;
  specialRequests?: string;
  arrivalTime?: string;
  nonRefundable?: boolean;
  paymentMode?: string;
  selectedInclusionIds?: string[];
}

export interface PaymentOrder {
  orderId: string;
  keyId: string;
  razorpayKeyId: string;
  razorpayOrderId: string;
  amountPaise: number;
  currency: string;
  bookingId: string;
}

export interface SavedPaymentMethod {
  id: string;
  type: 'UPI' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'NET_BANKING';
  label?: string;
  isDefault?: boolean;
  upiId?: string;
  cardLast4?: string;
  cardNetwork?: string;
  cardHolder?: string;
  cardExpiry?: string;
  bankName?: string;
  bankAccountLast4?: string;
  createdAt: string;
}

export interface CoTraveler {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  gender?: string;
  createdAt: string;
}

export interface UserProfile {
  userId: string;
  name: string;
  displayName?: string;
  email?: string;
  avatarUrl?: string;
  phone?: string;
  role?: string;
  language?: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  address?: string;
  passportName?: string;
  passportNumber?: string;
  passportExpiry?: string;
  updatedAt?: string;
}

export interface AuthUser {
  id: string;
  phone: string;
  email?: string;
  name: string;
  role: string;
  kycStatus: string;
  avatarUrl?: string;
  language?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  user: AuthUser;
}

export interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
  category?: string;
}

export interface Review {
  id: string;
  bookingId: string;
  listingId: string;
  guestId: string;
  hostId?: string;
  rating: number;
  comment?: string;
  reply?: string;
  repliedAt?: string;
  replyUpdatedAt?: string;
  createdAt: string;
  guestPhotoUrls?: string[];
  guestName?: string;
  ratingCleanliness?: number;
  ratingLocation?: number;
  ratingValue?: number;
  ratingCommunication?: number;
  ratingCheckIn?: number;
  ratingAccuracy?: number;
  ratingStaff?: number;
  ratingFacilities?: number;
  ratingComfort?: number;
  ratingFreeWifi?: number;
  categoryComments?: string; // JSON: {"cleanliness":"Spotless","location":"Near metro"}
}

export interface ReviewStats {
  listingId: string;
  totalReviews: number;
  averageRating: number;
  avgCleanliness?: number;
  avgLocation?: number;
  avgValue?: number;
  avgCommunication?: number;
  avgCheckIn?: number;
  avgAccuracy?: number;
  avgStaff?: number;
  avgFacilities?: number;
  avgComfort?: number;
  avgFreeWifi?: number;
}

export interface HostReviewStats {
  totalReviews: number;
  pendingReplies: number;
  repliedReviews: number;
}

export interface AvailabilityResponse {
  date: string;
  isAvailable: boolean;
  priceOverridePaise?: number;
  minStayNights?: number;
  maxStayNights?: number;
}

export interface AvailabilityDayDto {
  date: string;
  isAvailable: boolean;
  priceOverridePaise: number | null;
  minStayNights: number | null;
  maxStayNights: number | null;
  hasBooking: boolean;
}

export interface BulkAvailabilityRequest {
  fromDate: string;
  toDate: string;
  isAvailable: boolean;
  priceOverridePaise?: number | null;
  minStayNights?: number | null;
  maxStayNights?: number | null;
}

/* ── Host Subscription ─────────────────────────────────────── */

export type SubscriptionTier = 'STARTER' | 'PRO' | 'COMMERCIAL';
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'PAUSED' | 'CANCELLED';

export interface HostSubscription {
  id: string;
  hostId: string;
  tier: SubscriptionTier;
  status: SubscriptionStatus;
  trialEndsAt?: string;
  billingCycle: string;
  amountPaise: number;
  nextBillingAt?: string;
  createdAt: string;
  commissionDiscountPercent?: number;
  preferredPartner?: boolean;
}

export interface ActivateSubscriptionResponse {
  razorpaySubId: string;
  paymentLink: string;
  tier: SubscriptionTier;
  status: SubscriptionStatus;
}

/* ── Miles ─────────────────────────────────────────────────── */

export type MilesTier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';

export interface MilesBalance {
  userId: string;
  balance: number;
  tier: MilesTier;
  lifetimeMiles: number;
}

export interface MilesHistoryEntry {
  id: string;
  type: 'EARNED' | 'REDEEMED';
  miles: number;
  description: string;
  bookingId?: string;
  createdAt: string;
}

export interface MilesHistoryResponse {
  content: MilesHistoryEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/* ── Experiences ───────────────────────────────────────────── */

export type ExperienceCategory = 'CULINARY' | 'CULTURAL' | 'WELLNESS' | 'ADVENTURE' | 'CREATIVE';

export interface Experience {
  id: string;
  title: string;
  description: string;
  category: ExperienceCategory;
  city: string;
  pricePaise: number;
  durationMinutes: number;
  avgRating?: number;
  reviewCount?: number;
  hostName: string;
  imageUrl?: string;
  maxParticipants: number;
  createdAt: string;
}

/* ── Medical Tourism ───────────────────────────────────────── */

export interface Hospital {
  id: string;
  name: string;
  city: string;
  specialties: string[];
  accreditations: string[];
  rating?: number;
  imageUrl?: string;
}

export interface MedicalStayPackage {
  id: string;
  listingId: string;
  listingTitle?: string;
  city?: string;
  state?: string;
  basePricePaise?: number;
  primaryPhotoUrl?: string;
  amenities?: string[];
  hospitalId: string;
  hospitalName?: string;
  hospitalCity?: string;
  specialties?: string[];
  accreditations?: string[];
  hospitalRating?: number;
  distanceKm?: number;
  includesPickup?: boolean;
  includesTranslator?: boolean;
  caregiverFriendly?: boolean;
  medicalPricePaise?: number;
  minStayNights?: number;
  recoveryDays?: number;
}

/* ── Nomad Network ─────────────────────────────────────────── */

export type NomadPostCategory = 'TIP' | 'MEETUP' | 'SKILL_SWAP' | 'RECOMMENDATION' | 'QUESTION';

export interface NomadPost {
  id: string;
  authorId: string;
  authorName: string;
  title: string;
  body: string;
  category: NomadPostCategory;
  city: string;
  upvotes: number;
  commentCount: number;
  createdAt: string;
}

export interface NomadComment {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
}

/* ── iCal Feeds ───────────────────────────────────────────── */

export interface ICalFeed {
  id: string;
  listingId: string;
  feedUrl: string;
  feedName: string;
  lastSyncedAt: string | null;
  syncIntervalHours: number;
  isActive: boolean;
  createdAt: string;
}

/* ── Pricing Rules ────────────────────────────────────────── */

export type PricingRuleType = 'SEASONAL' | 'WEEKEND' | 'LAST_MINUTE' | 'EARLY_BIRD';
export type PriceAdjustmentType = 'FIXED_PRICE' | 'PERCENT_INCREASE' | 'PERCENT_DECREASE';

export interface PricingRule {
  id: string;
  listingId: string;
  roomTypeId: string | null;
  name: string;
  ruleType: PricingRuleType;
  fromDate: string | null;
  toDate: string | null;
  daysOfWeek: string | null;
  priceAdjustmentType: PriceAdjustmentType;
  adjustmentValue: number;
  priority: number;
  isActive: boolean;
  createdAt: string;
}

/* ── Occupancy Report ─────────────────────────────────────── */

export interface OccupancyReport {
  overallOccupancyPercent: number;
  adrPaise: number;
  revparPaise: number;
  totalRevenuePaise: number;
  totalBookings: number;
  totalNights: number;
  listings: ListingOccupancy[];
  monthlyBreakdown: MonthlyBreakdown[];
}

export interface ListingOccupancy {
  listingId: string;
  listingTitle: string;
  occupancyPercent: number;
  revenuePaise: number;
  bookedNights: number;
  totalAvailableNights: number;
  bookingCount: number;
}

export interface MonthlyBreakdown {
  month: string;
  occupancyPercent: number;
  revenuePaise: number;
  bookingCount: number;
}

/* ── Bucket List ───────────────────────────────────────────── */

export interface BucketListItem {
  id: string;
  listingId: string;
  listingTitle: string;
  listingCity: string;
  listingImageUrl?: string;
  addedAt: string;
}

/* ── Messaging ────────────────────────────────────────────── */

export interface Conversation {
  id: string;
  participant1Id: string;
  participant2Id: string;
  listingId: string;
  bookingId?: string;
  lastMessageText?: string;
  lastMessageAt?: string;
  unreadCount: number;
  createdAt: string;
  // Enriched fields from backend
  otherParticipantName?: string;
  listingTitle?: string;
  bookingRef?: string;
}

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  messageType: 'TEXT' | 'SYSTEM' | 'BOOKING_UPDATE';
  readAt?: string;
  createdAt: string;
}

export interface QuickReplyTemplate {
  id: string;
  content: string;
  sortOrder: number;
}
