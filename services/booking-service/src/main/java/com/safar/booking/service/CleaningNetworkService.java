package com.safar.booking.service;

import com.safar.booking.dto.CreateCleaningJobRequest;
import com.safar.booking.dto.RegisterCleanerRequest;
import com.safar.booking.entity.CleanerProfile;
import com.safar.booking.entity.CleaningJob;
import com.safar.booking.entity.enums.CleaningJobStatus;
import com.safar.booking.repository.CleanerProfileRepository;
import com.safar.booking.repository.CleaningJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleaningNetworkService {

    private final CleanerProfileRepository cleanerRepo;
    private final CleaningJobRepository jobRepo;
    private final ListingServiceClient listingClient;
    private final KafkaTemplate<String, String> kafka;

    @Transactional
    public CleanerProfile registerCleaner(UUID userId, RegisterCleanerRequest req) {
        CleanerProfile profile = CleanerProfile.builder()
                .userId(userId)
                .fullName(req.fullName())
                .phone(req.phone())
                .cities(req.cities())
                .ratePerHourPaise(req.ratePerHourPaise())
                .build();
        return cleanerRepo.save(profile);
    }

    @Transactional
    public CleaningJob createJob(CreateCleaningJobRequest req) {
        CleaningJob job = CleaningJob.builder()
                .listingId(req.listingId())
                .scheduledAt(req.scheduledAt())
                .estimatedHours(req.estimatedHours() != null ? req.estimatedHours() : new BigDecimal("2.0"))
                .notes(req.notes())
                .build();

        CleaningJob saved = jobRepo.save(job);

        // Try to auto-assign highest-rated cleaner in the listing's city
        String city = listingClient.getCity(req.listingId());
        if (city != null) {
            assignCleaner(saved, city);
        }

        return saved;
    }

    @Transactional
    public void assignCleaner(CleaningJob job, String city) {
        List<CleanerProfile> available = cleanerRepo.findAvailableByCity(city);
        if (available.isEmpty()) {
            log.info("No available cleaners in city {} for job {}", city, job.getId());
            return;
        }

        // Pick highest rated (list is already sorted by rating DESC)
        CleanerProfile bestCleaner = available.get(0);

        job.setCleanerId(bestCleaner.getId());
        job.setStatus(CleaningJobStatus.ASSIGNED);
        job.setAmountPaise(Math.round(
                bestCleaner.getRatePerHourPaise() * job.getEstimatedHours().doubleValue()));
        jobRepo.save(job);

        log.info("Auto-assigned cleaner {} to job {}", bestCleaner.getId(), job.getId());
    }

    @Transactional
    public CleaningJob completeJob(UUID cleanerId, UUID jobId) {
        CleaningJob job = jobRepo.findByCleanerIdAndId(cleanerId, jobId)
                .orElseThrow(() -> new NoSuchElementException("Job not found for cleaner: " + jobId));

        job.setStatus(CleaningJobStatus.COMPLETED);
        job.setCompletedAt(OffsetDateTime.now());
        CleaningJob saved = jobRepo.save(job);

        // Increment job count on cleaner profile
        cleanerRepo.findById(cleanerId).ifPresent(cleaner -> {
            cleaner.setJobCount(cleaner.getJobCount() + 1);
            cleanerRepo.save(cleaner);
        });

        kafka.send("cleaning.job.completed", jobId.toString());
        log.info("Cleaning job {} completed by cleaner {}", jobId, cleanerId);

        return saved;
    }

    public List<CleaningJob> getJobsByListing(UUID listingId) {
        return jobRepo.findByListingId(listingId);
    }

    @Transactional
    public CleanerProfile verifyCleaner(UUID cleanerId) {
        CleanerProfile cleaner = cleanerRepo.findById(cleanerId)
                .orElseThrow(() -> new NoSuchElementException("Cleaner not found: " + cleanerId));
        cleaner.setVerified(true);
        return cleanerRepo.save(cleaner);
    }
}
