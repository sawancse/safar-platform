package com.safar.user.service;

import com.safar.user.dto.BucketListItemDto;
import com.safar.user.entity.BucketListItem;
import com.safar.user.repository.BucketListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BucketListServiceTest {

    @Mock BucketListRepository bucketListRepository;
    @InjectMocks BucketListService bucketListService;

    private final UUID GUEST_ID = UUID.randomUUID();
    private final UUID LISTING_ID = UUID.randomUUID();

    @Test
    void add_success_returnsDto() {
        when(bucketListRepository.existsByGuestIdAndListingId(GUEST_ID, LISTING_ID))
                .thenReturn(false);
        when(bucketListRepository.save(any())).thenAnswer(inv -> {
            BucketListItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        BucketListItemDto dto = bucketListService.add(GUEST_ID, LISTING_ID, "Must visit!");

        assertThat(dto.guestId()).isEqualTo(GUEST_ID);
        assertThat(dto.listingId()).isEqualTo(LISTING_ID);
        assertThat(dto.notes()).isEqualTo("Must visit!");
        verify(bucketListRepository).save(any());
    }

    @Test
    void add_duplicate_throwsConflict() {
        when(bucketListRepository.existsByGuestIdAndListingId(GUEST_ID, LISTING_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> bucketListService.add(GUEST_ID, LISTING_ID, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in your bucket list");
    }

    @Test
    void remove_existing_deletesSuccessfully() {
        bucketListService.remove(GUEST_ID, LISTING_ID);

        verify(bucketListRepository).deleteByGuestIdAndListingId(GUEST_ID, LISTING_ID);
    }
}
