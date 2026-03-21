import type { Metadata } from 'next';
import { api } from '@/lib/api';
import { formatPaise, formatPricingUnit } from '@/lib/utils';
import { getRatingLabel } from '@/lib/rating';
import ReviewsList from '@/components/ReviewsList';
import PhotoGallery from '@/components/PhotoGallery';
import SaveButton from '@/components/SaveButton';
import ShareButton from '@/components/ShareButton';
import AmenityGrid from '@/components/AmenityGrid';
import RoomCategorySelector from '@/components/RoomCategorySelector';
import NearbySection from '@/components/NearbySection';
import TrackView from '@/components/TrackView';
import HostedBySection from '@/components/HostedBySection';
import PropertyHighlights from '@/components/PropertyHighlights';
import ExpandableText from '@/components/ExpandableText';
import ListingBookingSection from '@/components/ListingBookingSection';
import { notFound } from 'next/navigation';

export const dynamic = 'force-dynamic';

interface Props {
  params: { id: string };
}

// OG meta tags for rich social media previews
export async function generateMetadata({ params }: Props): Promise<Metadata> {
  try {
    const listing = await api.getListing(params.id);
    const price = formatPaise(listing.basePricePaise);
    const description = `${listing.type.charAt(0) + listing.type.slice(1).toLowerCase()} in ${listing.city}, ${listing.state} — ${price}/night. ${listing.maxGuests} guests, ${listing.bedrooms ?? 0} bedrooms. ${listing.description?.slice(0, 120) ?? ''}`;
    const imageUrl = listing.primaryPhotoUrl
      ? (listing.primaryPhotoUrl.startsWith('http') ? listing.primaryPhotoUrl : `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${listing.primaryPhotoUrl}`)
      : undefined;

    return {
      title: `${listing.title} — Safar`,
      description,
      openGraph: {
        title: listing.title,
        description,
        type: 'website',
        url: `/listings/${params.id}`,
        ...(imageUrl ? { images: [{ url: imageUrl, width: 1200, height: 630, alt: listing.title }] } : {}),
        siteName: 'Safar',
      },
      twitter: {
        card: 'summary_large_image',
        title: listing.title,
        description,
        ...(imageUrl ? { images: [imageUrl] } : {}),
      },
    };
  } catch {
    return { title: 'Listing — Safar' };
  }
}

export default async function ListingDetailPage({ params }: Props) {
  let listing;
  try {
    listing = await api.getListing(params.id);
  } catch {
    notFound();
  }

  const [reviews, reviewStats, roomTypes] = await Promise.all([
    api.getListingReviews(params.id).catch((e) => {
      console.error('[ListingPage] Failed to fetch reviews:', e.message);
      return [];
    }),
    api.getListingReviewStats(params.id).catch(() => null),
    api.getRoomTypes(params.id).catch(() => []),
  ]);
  const media = await api.getListingMedia(params.id).catch((e) => {
    console.error('[ListingPage] Failed to fetch media:', e.message);
    return [];
  });

  // Normalize reviews — handle paginated response from updated API
  const reviewList = Array.isArray(reviews) ? reviews : (reviews as any)?.content ?? [];
  const reviewCount = listing.reviewCount ?? reviewList.length;

  const isHotel = listing.type === 'HOTEL' || listing.type === 'BUDGET_HOTEL' || listing.type === 'HOSTEL_DORM';
  const isPG = listing.type === 'PG' || listing.type === 'COLIVING';
  const ratingInfo = listing.avgRating && listing.avgRating > 0 ? getRatingLabel(listing.avgRating) : null;

  // Per-night price display
  const perNightLabel = listing.pricingUnit === 'HOUR' ? 'per hour'
    : (isPG ? 'per month' : 'per night');

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <TrackView listingId={params.id} />

      {/* Title & badges */}
      <div className="mb-4">
        <div className="flex flex-wrap gap-2 mb-2">
          {listing.status === 'VERIFIED' && (
            <span className="bg-green-100 text-green-700 text-xs font-semibold px-2 py-1 rounded-full">
              ✓ Verified by Safar
            </span>
          )}
          {listing.instantBook && (
            <span className="bg-blue-100 text-blue-700 text-xs font-semibold px-2 py-1 rounded-full">
              ⚡ Instant Book
            </span>
          )}
          {listing.coupleFriendly && (
            <span className="bg-pink-100 text-pink-700 text-xs font-semibold px-2 py-1 rounded-full">
              💑 Couple Friendly
            </span>
          )}
          {listing.zeroPaymentBooking && (
            <span className="bg-emerald-100 text-emerald-700 text-xs font-semibold px-2 py-1 rounded-full">
              Book with ₹0 payment
            </span>
          )}
          {listing.earlyBirdDiscountPercent != null && listing.earlyBirdDiscountPercent > 0 && (
            <span className="bg-amber-100 text-amber-700 text-xs font-semibold px-2 py-1 rounded-full">
              🐦 Early Bird: {listing.earlyBirdDiscountPercent}% off
            </span>
          )}
          <span className="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full capitalize">
            {listing.type.toLowerCase().replace('_', ' ')}
          </span>
          {listing.starRating != null && listing.starRating > 0 && (
            <span className="bg-yellow-100 text-yellow-700 text-xs font-semibold px-2 py-1 rounded-full">
              {'★'.repeat(listing.starRating)} {listing.starRating}-Star
            </span>
          )}
        </div>

        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold">{listing.title}</h1>
          <div className="flex items-center gap-4">
            <ShareButton
              title={listing.title}
              city={listing.city}
              state={listing.state}
              price={formatPaise(listing.basePricePaise)}
              imageUrl={listing.primaryPhotoUrl}
              listingId={params.id}
            />
            <SaveButton listingId={params.id} />
          </div>
        </div>

        <div className="flex items-center gap-3 mt-1 flex-wrap">
          <p className="text-gray-500">
            {listing.city}, {listing.state}
          </p>
          {/* MakeMyTrip-style rating badge */}
          {ratingInfo && (
            <div className="flex items-center gap-1.5">
              <span className={`${ratingInfo.bg} ${ratingInfo.text} text-xs font-bold px-2 py-1 rounded`}>
                {listing.avgRating!.toFixed(1)}
              </span>
              <span className="text-sm font-semibold text-gray-700">{ratingInfo.label}</span>
              <span className="text-sm text-gray-400">({reviewCount} reviews)</span>
            </div>
          )}
          {/* Price per night inline */}
          <span className="text-sm font-bold text-gray-800 ml-auto">
            {formatPaise(listing.basePricePaise)} <span className="text-gray-400 font-normal">/ {perNightLabel}</span>
          </span>
        </div>

        {/* Location highlight */}
        {listing.locationHighlight && (
          <p className="text-sm text-blue-600 font-medium mt-1.5">
            📍 {listing.locationHighlight}
          </p>
        )}
      </div>

      <ListingBookingSection listing={listing} roomTypes={roomTypes} perNightLabel={perNightLabel}
        beforeRooms={<>
          {/* Photo & Video Gallery */}
          <PhotoGallery
            media={media}
            listingName={listing.title}
            panoramaUrl={listing.panoramaUrl ?? undefined}
            videoTourUrl={listing.videoTourUrl ?? undefined}
          />

          {/* Property Highlights */}
          {(listing.propertyHighlights || listing.locationHighlight || listing.coupleFriendly ||
            listing.earlyBirdDiscountPercent || listing.zeroPaymentBooking || listing.breakfastIncluded ||
            listing.freeCancellation) && (
            <PropertyHighlights
              highlights={listing.propertyHighlights || ''}
              locationHighlight={listing.locationHighlight}
              coupleFriendly={listing.coupleFriendly}
              earlyBirdDiscountPercent={listing.earlyBirdDiscountPercent}
              earlyBirdDaysBefore={listing.earlyBirdDaysBefore}
              zeroPaymentBooking={listing.zeroPaymentBooking}
              freeCancellation={listing.freeCancellation}
              breakfastIncluded={listing.breakfastIncluded}
            />
          )}

          {/* Stats */}
          <div className="flex gap-6 text-sm text-gray-600 border-b pb-4 flex-wrap">
            {listing.maxGuests && <span>👥 Up to {listing.maxGuests} guests</span>}
            {listing.bedrooms != null && <span>🛏 {listing.bedrooms} bedrooms</span>}
            {listing.bathrooms != null && <span>🚿 {listing.bathrooms} bathrooms</span>}
            {listing.totalRooms != null && listing.totalRooms > 1 && <span>🚪 {listing.totalRooms} rooms</span>}
            {listing.petFriendly && <span className="text-green-600 font-medium">🐾 Pet-friendly</span>}
            {listing.coupleFriendly && <span className="text-pink-600 font-medium">💑 Couple-friendly</span>}
          </div>

          {/* About this Safar */}
          <ExpandableText text={listing.description} />

          {/* Hosted By */}
          <HostedBySection hostId={listing.hostId} listingId={params.id} listingTitle={listing.title} />

          {/* Legacy room selector for HOME/ROOM types without room types */}
          {roomTypes.length === 0 && (listing.type === 'HOME' || listing.type === 'ROOM') && (
            <RoomCategorySelector
              basePricePaise={listing.basePricePaise}
              pricingUnit={listing.pricingUnit}
            />
          )}

          {/* Amenities — categorized */}
          {listing.amenities && listing.amenities.length > 0 && (
            <AmenityGrid amenities={listing.amenities} />
          )}
        </>}
        afterRooms={<>
          {/* Medical Stay Info */}
          {listing.medicalStay && (
            <div className="bg-blue-50 border border-blue-200 rounded-xl p-6">
              <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
                <span>🏥</span> Medical Stay Property
              </h2>
              <p className="text-gray-600 text-sm mb-4">
                This property is partnered with nearby hospitals for medical tourism stays.
                Recovery-friendly amenities and hospital proximity included.
              </p>
              {listing.hospitalNames && listing.hospitalNames.length > 0 && (
                <div className="mb-3">
                  <p className="text-sm font-medium text-gray-700 mb-2">Partner Hospitals</p>
                  <div className="flex flex-wrap gap-2">
                    {listing.hospitalNames.map((name: string, i: number) => (
                      <span key={i} className="bg-white px-3 py-1 rounded-full text-sm border border-blue-200 text-blue-700">
                        🏥 {name}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {listing.medicalSpecialties && listing.medicalSpecialties.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-gray-700 mb-2">Available Specialties</p>
                  <div className="flex flex-wrap gap-2">
                    {listing.medicalSpecialties.map((spec: string, i: number) => (
                      <span key={i} className="bg-blue-100 px-3 py-1 rounded-full text-xs font-medium text-blue-700">
                        {spec}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              <a href="/medical" className="inline-block mt-4 text-sm font-medium text-blue-600 hover:text-blue-800">
                View medical tourism packages →
              </a>
            </div>
          )}

          {/* PG / Co-living Details */}
          {isPG && (
            <div className="bg-purple-50 border border-purple-200 rounded-xl p-6">
              <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
                <span>🏘️</span> PG Details
              </h2>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                {listing.occupancyType && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Occupancy</p>
                    <span className={`inline-block text-sm font-semibold px-3 py-1 rounded-full ${
                      listing.occupancyType === 'MALE' ? 'bg-blue-100 text-blue-700' :
                      listing.occupancyType === 'FEMALE' ? 'bg-pink-100 text-pink-700' :
                      'bg-green-100 text-green-700'
                    }`}>
                      {listing.occupancyType === 'MALE' ? 'Male Only' : listing.occupancyType === 'FEMALE' ? 'Female Only' : 'Co-ed'}
                    </span>
                  </div>
                )}
                {listing.foodType && listing.foodType !== 'NONE' && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Food</p>
                    <p className="text-sm font-medium text-gray-800">
                      {listing.foodType === 'VEG' ? '🟢 Veg' : listing.foodType === 'NON_VEG' ? '🔴 Non-veg' : '🟡 Both'}
                    </p>
                  </div>
                )}
                {listing.gateClosingTime && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Gate Closing</p>
                    <p className="text-sm font-medium text-gray-800">{listing.gateClosingTime}</p>
                  </div>
                )}
                {listing.noticePeriodDays != null && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Notice Period</p>
                    <p className="text-sm font-medium text-gray-800">{listing.noticePeriodDays} days</p>
                  </div>
                )}
                {listing.securityDepositPaise != null && listing.securityDepositPaise > 0 && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Security Deposit</p>
                    <p className="text-sm font-medium text-gray-800">{formatPaise(listing.securityDepositPaise)}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Hotel Info */}
          {isHotel && (
            <div className="bg-amber-50 border border-amber-200 rounded-xl p-6">
              <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
                <span>🏨</span> Hotel Info
              </h2>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                {listing.hotelChain && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Hotel Chain</p>
                    <p className="text-sm font-medium text-gray-800">{listing.hotelChain}</p>
                  </div>
                )}
                {listing.frontDesk24h && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Front Desk</p>
                    <span className="inline-block text-sm font-semibold px-3 py-1 rounded-full bg-green-100 text-green-700">
                      24h Available
                    </span>
                  </div>
                )}
                {listing.checkinTime && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Check-in</p>
                    <p className="text-sm font-medium text-gray-800">{listing.checkinTime}</p>
                  </div>
                )}
                {listing.checkoutTime && (
                  <div>
                    <p className="text-xs text-gray-500 mb-1">Check-out</p>
                    <p className="text-sm font-medium text-gray-800">{listing.checkoutTime}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Location */}
          <NearbySection
            lat={listing.lat}
            lng={listing.lng}
            city={listing.city}
            state={listing.state}
            addressLine1={listing.addressLine1}
            addressLine2={listing.addressLine2}
            pincode={listing.pincode}
          />

          {/* Divider */}
          <hr className="border-gray-200" />

          {/* Rating & Reviews */}
          <ReviewsList reviews={reviewList} avgRating={listing.avgRating} stats={reviewStats} />
        </>}
      />
    </div>
  );
}
