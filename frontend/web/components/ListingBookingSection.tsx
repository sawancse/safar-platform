'use client';

import { useState } from 'react';
import BookingPanel from '@/components/BookingPanel';
import RoomTypeSelector, { type RoomSelection } from '@/components/RoomTypeSelector';
import type { Listing, RoomType } from '@/types';

interface Props {
  listing: Listing;
  roomTypes: RoomType[];
  perNightLabel: string;
  beforeRooms: React.ReactNode;
  afterRooms: React.ReactNode;
}

export default function ListingBookingSection({ listing, roomTypes, perNightLabel, beforeRooms, afterRooms }: Props) {
  const [roomSelections, setRoomSelections] = useState<RoomSelection[]>([]);

  // Derive selectedRoomType for backward compat with BookingPanel
  // If single selection, pass that room type. If multiple, pass null (use total).
  const selectedRoomType = roomSelections.length === 1
    ? roomTypes.find(rt => rt.id === roomSelections[0].roomTypeId) ?? null
    : null;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div className="lg:col-span-2 space-y-8">
        {beforeRooms}

        {/* Choose Your Rooms — multi-select with quantity steppers */}
        {roomTypes.length > 0 && (
          <RoomTypeSelector
            roomTypes={roomTypes}
            perNightLabel={perNightLabel}
            listingId={listing.id}
            onSelect={setRoomSelections}
          />
        )}

        {afterRooms}
      </div>

      <div className="lg:col-span-1">
        <BookingPanel
          listing={listing}
          selectedRoomType={selectedRoomType}
          roomSelections={roomSelections}
        />
      </div>
    </div>
  );
}
