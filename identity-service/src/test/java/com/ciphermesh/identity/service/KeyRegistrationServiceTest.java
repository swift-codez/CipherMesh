package com.ciphermesh.identity.service;

import com.ciphermesh.events.UserRegisteredEvent;
import com.ciphermesh.identity.api.RegisterKeysRequest;
import com.ciphermesh.identity.api.SignedPreKeyRequest;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.messaging.IdentityEventPublisher;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.repository.SignedPreKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyRegistrationServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private SignedPreKeyRepository signedPreKeyRepository;
    @Mock
    private OneTimePreKeyRepository oneTimePreKeyRepository;
    @Mock
    private IdentityEventPublisher eventPublisher;

    @InjectMocks
    private KeyRegistrationService service;

    @Test
    void publishesUserRegisteredAfterSuccessfulRegistration() {
        when(deviceRepository.existsByUserIdAndDeviceId("alice", 1)).thenReturn(false);
        when(deviceRepository.save(any())).thenReturn(new Device("alice", 1, 42, new byte[]{1}));

        service.register(request("alice"));

        verify(eventPublisher).publishUserRegistered(any(UserRegisteredEvent.class));
    }

    @Test
    void doesNotPublishWhenDeviceAlreadyRegistered() {
        when(deviceRepository.existsByUserIdAndDeviceId("alice", 1)).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("alice")))
                .isInstanceOf(DeviceAlreadyRegisteredException.class);

        verifyNoInteractions(eventPublisher);
    }

    private static RegisterKeysRequest request(String userId) {
        return new RegisterKeysRequest(
                userId, 1, 42, encode("identity"),
                new SignedPreKeyRequest(100, encode("spk"), encode("sig")),
                List.of());
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
