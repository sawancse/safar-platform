package com.safar.user.service;

import com.safar.user.dto.CoTravelerDto;
import com.safar.user.dto.CoTravelerRequest;
import com.safar.user.entity.CoTraveler;
import com.safar.user.repository.CoTravelerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoTravelerService {

    private static final int MAX_CO_TRAVELERS = 10;

    private final CoTravelerRepository repository;

    public List<CoTravelerDto> getAll(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CoTravelerDto create(UUID userId, CoTravelerRequest req) {
        if (repository.countByUserId(userId) >= MAX_CO_TRAVELERS) {
            throw new IllegalStateException("Maximum of " + MAX_CO_TRAVELERS + " co-travelers allowed");
        }

        CoTraveler entity = CoTraveler.builder()
                .userId(userId)
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .dateOfBirth(req.dateOfBirth())
                .gender(req.gender())
                .build();

        return toDto(repository.save(entity));
    }

    @Transactional
    public CoTravelerDto update(UUID userId, UUID travelerId, CoTravelerRequest req) {
        CoTraveler entity = repository.findById(travelerId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Co-traveler not found"));

        entity.setFirstName(req.firstName().trim());
        entity.setLastName(req.lastName().trim());
        if (req.dateOfBirth() != null) entity.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) entity.setGender(req.gender());

        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID travelerId) {
        CoTraveler entity = repository.findById(travelerId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new NoSuchElementException("Co-traveler not found"));
        repository.delete(entity);
    }

    private CoTravelerDto toDto(CoTraveler t) {
        return new CoTravelerDto(t.getId(), t.getFirstName(), t.getLastName(),
                t.getDateOfBirth(), t.getGender(), t.getCreatedAt());
    }
}
