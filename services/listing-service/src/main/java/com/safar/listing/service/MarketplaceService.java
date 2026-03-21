package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.AppInstallation;
import com.safar.listing.entity.MarketplaceApp;
import com.safar.listing.entity.enums.AppStatus;
import com.safar.listing.repository.AppInstallationRepository;
import com.safar.listing.repository.MarketplaceAppRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceService {

    private final MarketplaceAppRepository appRepository;
    private final AppInstallationRepository installationRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Transactional
    public MarketplaceAppResponse registerApp(UUID developerId, RegisterAppRequest req) {
        String clientId = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String clientSecret = generateRandomString(64);
        String webhookSecret = req.webhookUrl() != null && !req.webhookUrl().isBlank()
                ? generateRandomString(64) : null;

        MarketplaceApp app = MarketplaceApp.builder()
                .developerId(developerId)
                .appName(req.appName())
                .description(req.description())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUris(req.redirectUris() != null ? req.redirectUris() : "")
                .scopes(req.scopes() != null ? req.scopes() : "")
                .status(AppStatus.PENDING)
                .webhookUrl(req.webhookUrl())
                .webhookSecret(webhookSecret)
                .rateLimitRpm(60)
                .build();

        MarketplaceApp saved = appRepository.save(app);
        log.info("Marketplace app '{}' registered by developer {}", saved.getAppName(), developerId);
        return toResponse(saved);
    }

    @Transactional
    public MarketplaceAppResponse approveApp(UUID appId) {
        MarketplaceApp app = appRepository.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("App not found: " + appId));
        if (app.getStatus() != AppStatus.PENDING) {
            throw new IllegalStateException("Only PENDING apps can be approved");
        }
        app.setStatus(AppStatus.APPROVED);
        MarketplaceApp saved = appRepository.save(app);
        log.info("Marketplace app '{}' approved", saved.getAppName());
        return toResponse(saved);
    }

    @Transactional
    public AppInstallationResponse installApp(UUID hostId, UUID appId, InstallAppRequest req) {
        MarketplaceApp app = appRepository.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("App not found: " + appId));
        if (app.getStatus() != AppStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED apps can be installed");
        }
        if (installationRepository.existsByAppIdAndHostId(appId, hostId)) {
            throw new IllegalStateException("App is already installed for this host");
        }

        String accessToken = generateRandomString(64);
        String refreshToken = generateRandomString(64);

        AppInstallation installation = AppInstallation.builder()
                .appId(appId)
                .hostId(hostId)
                .scopesGranted(req.scopesGranted() != null ? req.scopesGranted() : "")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(90))
                .build();

        AppInstallation saved = installationRepository.save(installation);
        log.info("App {} installed by host {}", appId, hostId);
        return toInstallationResponse(saved);
    }

    public Page<MarketplaceAppResponse> getPublicApps(Pageable pageable) {
        return appRepository.findByStatus(AppStatus.APPROVED, pageable)
                .map(this::toResponse);
    }

    public List<MarketplaceAppResponse> getDeveloperApps(UUID developerId) {
        return appRepository.findByDeveloperId(developerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }

    private MarketplaceAppResponse toResponse(MarketplaceApp app) {
        return new MarketplaceAppResponse(
                app.getId(),
                app.getDeveloperId(),
                app.getAppName(),
                app.getDescription(),
                app.getClientId(),
                app.getRedirectUris(),
                app.getScopes(),
                app.getStatus(),
                app.getWebhookUrl(),
                app.getRateLimitRpm(),
                app.getCreatedAt()
        );
    }

    private AppInstallationResponse toInstallationResponse(AppInstallation inst) {
        return new AppInstallationResponse(
                inst.getId(),
                inst.getAppId(),
                inst.getHostId(),
                inst.getScopesGranted(),
                inst.getAccessToken(),
                inst.getExpiresAt(),
                inst.getInstalledAt()
        );
    }
}
