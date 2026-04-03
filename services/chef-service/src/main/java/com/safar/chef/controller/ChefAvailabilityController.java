package com.safar.chef.controller;

import com.safar.chef.entity.ChefAvailability;
import com.safar.chef.service.ChefAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs/availability")
@RequiredArgsConstructor
public class ChefAvailabilityController {

    private final ChefAvailabilityService availabilityService;

    @PostMapping("/block")
    public ResponseEntity<ChefAvailability> blockDate(Authentication auth,
                                                       @RequestParam LocalDate date,
                                                       @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.blockDate(userId, date, reason));
    }

    @DeleteMapping("/unblock")
    public ResponseEntity<Void> unblockDate(Authentication auth, @RequestParam LocalDate date) {
        UUID userId = UUID.fromString(auth.getName());
        availabilityService.unblockDate(userId, date);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block-bulk")
    public ResponseEntity<List<ChefAvailability>> bulkBlock(Authentication auth,
                                                             @RequestBody Map<String, Object> body) {
        UUID userId = UUID.fromString(auth.getName());
        @SuppressWarnings("unchecked")
        List<String> dateStrings = (List<String>) body.get("dates");
        String reason = (String) body.get("reason");
        List<LocalDate> dates = dateStrings.stream().map(LocalDate::parse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.bulkBlockDates(userId, dates, reason));
    }

    @GetMapping("/{chefId}/calendar")
    public ResponseEntity<Map<String, Object>> getCalendar(@PathVariable UUID chefId,
                                                            @RequestParam LocalDate from,
                                                            @RequestParam LocalDate to) {
        return ResponseEntity.ok(availabilityService.getCalendar(chefId, from, to));
    }
}
