package com.safar.user.service;

import com.safar.user.entity.NomadConnection;
import com.safar.user.entity.enums.ConnectionStatus;
import com.safar.user.repository.NomadConnectionRepository;
import com.safar.user.repository.NomadPostCommentRepository;
import com.safar.user.repository.NomadPostRepository;
import com.safar.user.repository.SkillSwapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NomadNetworkServiceTest {

    @Mock NomadPostRepository postRepo;
    @Mock NomadPostCommentRepository commentRepo;
    @Mock NomadConnectionRepository connectionRepo;
    @Mock SkillSwapRepository skillSwapRepo;
    @InjectMocks NomadNetworkService service;

    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();

    @Test
    void sendConnectionRequest_selfConnect_rejected() {
        assertThatThrownBy(() -> service.sendConnectionRequest(USER_A, USER_A))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot connect with yourself");
    }

    @Test
    void sendConnectionRequest_duplicate_rejected() {
        when(connectionRepo.existsByRequesterIdAndRecipientId(USER_A, USER_B)).thenReturn(true);

        assertThatThrownBy(() -> service.sendConnectionRequest(USER_A, USER_B))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Connection request already exists");
    }

    @Test
    void acceptConnection_changesStatusToAccepted() {
        UUID connectionId = UUID.randomUUID();
        NomadConnection connection = NomadConnection.builder()
                .id(connectionId)
                .requesterId(USER_A)
                .recipientId(USER_B)
                .status(ConnectionStatus.PENDING)
                .build();
        when(connectionRepo.findByIdAndRecipientId(connectionId, USER_B))
                .thenReturn(Optional.of(connection));
        when(connectionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        NomadConnection result = service.acceptConnection(USER_B, connectionId);

        assertThat(result.getStatus()).isEqualTo(ConnectionStatus.ACCEPTED);
    }
}
