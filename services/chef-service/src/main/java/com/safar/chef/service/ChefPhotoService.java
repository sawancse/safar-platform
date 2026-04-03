package com.safar.chef.service;

import com.safar.chef.entity.ChefPhoto;
import com.safar.chef.entity.ChefProfile;
import com.safar.chef.repository.ChefPhotoRepository;
import com.safar.chef.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefPhotoService {

    private final ChefPhotoRepository photoRepo;
    private final ChefProfileRepository chefProfileRepo;

    private static final int MAX_PHOTOS = 20;

    @Transactional
    public ChefPhoto addPhoto(UUID userId, String url, String caption, String photoType) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (photoRepo.countByChefId(chef.getId()) >= MAX_PHOTOS) {
            throw new IllegalArgumentException("Maximum " + MAX_PHOTOS + " photos allowed");
        }

        ChefPhoto photo = ChefPhoto.builder()
                .chefId(chef.getId())
                .url(url)
                .caption(caption)
                .photoType(photoType != null ? photoType : "FOOD")
                .sortOrder(photoRepo.countByChefId(chef.getId()))
                .build();
        log.info("Chef {} added photo type={}", chef.getId(), photoType);
        return photoRepo.save(photo);
    }

    @Transactional
    public void deletePhoto(UUID userId, UUID photoId) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        ChefPhoto photo = photoRepo.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));
        if (!photo.getChefId().equals(chef.getId())) {
            throw new IllegalArgumentException("Not authorized");
        }
        photoRepo.delete(photo);
    }

    @Transactional(readOnly = true)
    public List<ChefPhoto> getPhotos(UUID chefId) {
        return photoRepo.findByChefIdOrderBySortOrder(chefId);
    }
}
