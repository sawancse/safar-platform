package com.safar.media.service;

import com.safar.media.dto.ConfirmUploadRequest;
import com.safar.media.dto.PresignResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock S3Gateway s3Gateway;
    @Mock KafkaTemplate<String, String> kafka;

    @InjectMocks MediaService mediaService;

    private final UUID listingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "cdnDomain", "cdn.test.safar.in");
        ReflectionTestUtils.setField(mediaService, "bucket", "test-bucket");
    }

    @Test
    void generatePresignedUrl_photo_returnsValidResponse() {
        when(s3Gateway.generatePresignedUrl(anyString(), eq("image/jpeg")))
                .thenReturn("https://s3.amazonaws.com/presigned-url");

        PresignResponse resp = mediaService.generatePresignedUrl("PHOTO", "image/jpeg", listingId);

        assertThat(resp.uploadUrl()).isEqualTo("https://s3.amazonaws.com/presigned-url");
        assertThat(resp.s3Key()).contains("listings/" + listingId + "/photo/");
        assertThat(resp.cdnUrl()).startsWith("https://cdn.test.safar.in/");
        assertThat(resp.mediaId()).isNotNull();
    }

    @Test
    void confirmUpload_video_sufficientDuration_publishesKafkaEvent() {
        ConfirmUploadRequest req = new ConfirmUploadRequest(
                UUID.randomUUID(), listingId, "listings/" + listingId + "/video/test",
                "VIDEO", 90);

        mediaService.confirmUpload(req);

        verify(kafka).send(eq("media.uploaded"), eq(listingId.toString()), anyString());
    }

    @Test
    void confirmUpload_video_tooShort_throws() {
        ConfirmUploadRequest req = new ConfirmUploadRequest(
                UUID.randomUUID(), listingId, "listings/" + listingId + "/video/test",
                "VIDEO", 30);

        assertThatThrownBy(() -> mediaService.confirmUpload(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60 seconds");
    }

    @Test
    void confirmUpload_video_exactly60Seconds_succeeds() {
        ConfirmUploadRequest req = new ConfirmUploadRequest(
                UUID.randomUUID(), listingId, "listings/" + listingId + "/video/test",
                "VIDEO", 60);

        mediaService.confirmUpload(req);

        verify(kafka).send(eq("media.uploaded"), eq(listingId.toString()), anyString());
    }

    @Test
    void confirmUpload_photo_noDurationCheck_succeeds() {
        ConfirmUploadRequest req = new ConfirmUploadRequest(
                UUID.randomUUID(), listingId, "listings/" + listingId + "/photo/test",
                "PHOTO", 0);

        mediaService.confirmUpload(req);

        verify(kafka).send(eq("media.uploaded"), eq(listingId.toString()), anyString());
    }

    @Test
    void confirmUpload_kafkaEventContainsApprovedStatus() {
        ConfirmUploadRequest req = new ConfirmUploadRequest(
                UUID.randomUUID(), listingId, "listings/" + listingId + "/video/test",
                "VIDEO", 120);

        mediaService.confirmUpload(req);

        verify(kafka).send(eq("media.uploaded"), eq(listingId.toString()),
                argThat(event -> event.contains("APPROVED")));
    }
}
