package com.safar.booking.service;

import com.safar.booking.dto.CreateCleaningJobRequest;
import com.safar.booking.entity.CleanerProfile;
import com.safar.booking.entity.CleaningJob;
import com.safar.booking.entity.enums.CleaningJobStatus;
import com.safar.booking.repository.CleanerProfileRepository;
import com.safar.booking.repository.CleaningJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleaningNetworkServiceTest {

    @Mock CleanerProfileRepository cleanerRepo;
    @Mock CleaningJobRepository jobRepo;
    @Mock ListingServiceClient listingClient;
    @Mock KafkaTemplate<String, String> kafka;
    @InjectMocks CleaningNetworkService service;

    private final UUID LISTING_ID = UUID.randomUUID();
    private final UUID CLEANER_ID = UUID.randomUUID();
    private final UUID JOB_ID = UUID.randomUUID();

    @Test
    void createJob_autoAssignsHighestRatedCleaner() {
        CleanerProfile topRated = CleanerProfile.builder()
                .id(CLEANER_ID)
                .userId(UUID.randomUUID())
                .fullName("Top Cleaner")
                .phone("9999999999")
                .cities("Mumbai")
                .ratePerHourPaise(50000L)
                .rating(new BigDecimal("4.90"))
                .verified(true)
                .available(true)
                .build();

        CleanerProfile lowerRated = CleanerProfile.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .fullName("Other Cleaner")
                .phone("8888888888")
                .cities("Mumbai")
                .ratePerHourPaise(40000L)
                .rating(new BigDecimal("4.50"))
                .verified(true)
                .available(true)
                .build();

        when(jobRepo.save(any())).thenAnswer(i -> {
            CleaningJob j = i.getArgument(0);
            if (j.getId() == null) j.setId(JOB_ID);
            return j;
        });
        when(listingClient.getCity(LISTING_ID)).thenReturn("Mumbai");
        when(cleanerRepo.findAvailableByCity("Mumbai")).thenReturn(List.of(topRated, lowerRated));

        CreateCleaningJobRequest req = new CreateCleaningJobRequest(
                LISTING_ID, OffsetDateTime.now().plusDays(1), new BigDecimal("2.0"), "Turnover clean");

        CleaningJob result = service.createJob(req);

        assertThat(result.getCleanerId()).isEqualTo(CLEANER_ID);
        assertThat(result.getStatus()).isEqualTo(CleaningJobStatus.ASSIGNED);
    }

    @Test
    void createJob_noCleanerLeavesUnassigned() {
        when(jobRepo.save(any())).thenAnswer(i -> {
            CleaningJob j = i.getArgument(0);
            if (j.getId() == null) j.setId(JOB_ID);
            return j;
        });
        when(listingClient.getCity(LISTING_ID)).thenReturn("Delhi");
        when(cleanerRepo.findAvailableByCity("Delhi")).thenReturn(List.of());

        CreateCleaningJobRequest req = new CreateCleaningJobRequest(
                LISTING_ID, OffsetDateTime.now().plusDays(1), new BigDecimal("2.0"), null);

        CleaningJob result = service.createJob(req);

        assertThat(result.getStatus()).isEqualTo(CleaningJobStatus.UNASSIGNED);
        assertThat(result.getCleanerId()).isNull();
    }

    @Test
    void completeJob_setsStatusCompleted() {
        CleaningJob job = CleaningJob.builder()
                .id(JOB_ID)
                .listingId(LISTING_ID)
                .cleanerId(CLEANER_ID)
                .scheduledAt(OffsetDateTime.now().minusHours(2))
                .estimatedHours(new BigDecimal("2.0"))
                .status(CleaningJobStatus.ASSIGNED)
                .amountPaise(100000L)
                .build();

        CleanerProfile cleaner = CleanerProfile.builder()
                .id(CLEANER_ID)
                .userId(UUID.randomUUID())
                .fullName("Cleaner")
                .phone("9999999999")
                .cities("Mumbai")
                .ratePerHourPaise(50000L)
                .jobCount(5)
                .build();

        when(jobRepo.findByCleanerIdAndId(CLEANER_ID, JOB_ID)).thenReturn(Optional.of(job));
        when(jobRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cleanerRepo.findById(CLEANER_ID)).thenReturn(Optional.of(cleaner));
        when(cleanerRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        CleaningJob result = service.completeJob(CLEANER_ID, JOB_ID);

        assertThat(result.getStatus()).isEqualTo(CleaningJobStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(kafka).send(eq("cleaning.job.completed"), eq(JOB_ID.toString()));
        assertThat(cleaner.getJobCount()).isEqualTo(6);
    }
}
