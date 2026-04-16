package com.safar.chef.controller;

import com.safar.chef.entity.ChefPhoto;
import com.safar.chef.service.ChefPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chefs/photos")
@RequiredArgsConstructor
public class ChefPhotoController {

    private final ChefPhotoService photoService;

    @PostMapping
    public ResponseEntity<ChefPhoto> addPhoto(Authentication auth,
                                               @RequestParam String url,
                                               @RequestParam(required = false) String caption,
                                               @RequestParam(required = false, defaultValue = "FOOD") String photoType,
                                               @RequestParam(required = false, defaultValue = "IMAGE") String mediaType) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.addPhoto(userId, url, caption, photoType, mediaType));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(Authentication auth, @PathVariable UUID photoId) {
        UUID userId = UUID.fromString(auth.getName());
        photoService.deletePhoto(userId, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chefId}")
    public ResponseEntity<List<ChefPhoto>> getPhotos(@PathVariable UUID chefId) {
        return ResponseEntity.ok(photoService.getPhotos(chefId));
    }
}
