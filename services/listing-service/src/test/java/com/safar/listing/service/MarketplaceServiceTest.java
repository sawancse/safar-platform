package com.safar.listing.service;

import com.safar.listing.dto.InstallAppRequest;
import com.safar.listing.dto.MarketplaceAppResponse;
import com.safar.listing.dto.RegisterAppRequest;
import com.safar.listing.entity.MarketplaceApp;
import com.safar.listing.entity.enums.AppStatus;
import com.safar.listing.repository.AppInstallationRepository;
import com.safar.listing.repository.MarketplaceAppRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    MarketplaceAppRepository appRepository;

    @Mock
    AppInstallationRepository installationRepository;

    @InjectMocks
    MarketplaceService marketplaceService;

    private final UUID developerId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID hostId = UUID.randomUUID();

    @Test
    void registerApp_generatesClientId() {
        RegisterAppRequest req = new RegisterAppRequest(
                "My App", "A great app", "https://example.com/callback", "read,write",
                "https://example.com/webhook"
        );

        when(appRepository.save(any(MarketplaceApp.class)))
                .thenAnswer(inv -> {
                    MarketplaceApp app = inv.getArgument(0);
                    app.setId(UUID.randomUUID());
                    return app;
                });

        MarketplaceAppResponse response = marketplaceService.registerApp(developerId, req);

        assertThat(response.clientId()).isNotNull();
        assertThat(response.clientId()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(response.status()).isEqualTo(AppStatus.PENDING);
        assertThat(response.appName()).isEqualTo("My App");
        assertThat(response.developerId()).isEqualTo(developerId);
    }

    @Test
    void installApp_duplicateInstall_rejected() {
        MarketplaceApp app = MarketplaceApp.builder()
                .id(appId)
                .developerId(developerId)
                .appName("Test App")
                .clientId("test-client-id")
                .clientSecret("test-secret")
                .status(AppStatus.APPROVED)
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(installationRepository.existsByAppIdAndHostId(appId, hostId)).thenReturn(true);

        InstallAppRequest req = new InstallAppRequest("read");

        assertThatThrownBy(() -> marketplaceService.installApp(hostId, appId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already installed");
    }

    @Test
    void approveApp_setsApproved() {
        MarketplaceApp app = MarketplaceApp.builder()
                .id(appId)
                .developerId(developerId)
                .appName("Pending App")
                .clientId("client-id")
                .clientSecret("secret")
                .status(AppStatus.PENDING)
                .build();

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(appRepository.save(any(MarketplaceApp.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MarketplaceAppResponse response = marketplaceService.approveApp(appId);

        assertThat(response.status()).isEqualTo(AppStatus.APPROVED);
    }
}
