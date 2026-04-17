package com.safar.booking.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Helper for publishing JSON payloads on a StringSerializer-backed KafkaTemplate.
 * The booking-service producer is configured with StringSerializer, so entities
 * and maps must be JSON-stringified before send — sending them raw causes
 * {@code ClassCastException} at serialization time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaJsonPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Serialize {@code payload} to JSON and publish. Falls back to {@code {"id":"<key>"}} on failure. */
    public void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Kafka {} payload serialization failed for key {}: {}", topic, key, e.getMessage());
            kafkaTemplate.send(topic, key, "{\"id\":\"" + key + "\"}");
        }
    }
}
