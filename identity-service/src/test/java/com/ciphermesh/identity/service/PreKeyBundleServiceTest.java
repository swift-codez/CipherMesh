package com.ciphermesh.identity.service;

import com.ciphermesh.events.PreKeysLowEvent;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import com.ciphermesh.identity.domain.SignedPreKey;
import com.ciphermesh.identity.messaging.IdentityEventPublisher;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.repository.SignedPreKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreKeyBundleServiceTest {

    private static final int WATERMARK = 5;

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private SignedPreKeyRepository signedPreKeyRepository;
    @Mock
    private OneTimePreKeyRepository oneTimePreKeyRepository;
    @Mock
    private IdentityEventPublisher eventPublisher;

    private PreKeyBundleService service;
    private Device device;

    @BeforeEach
    void setUp() {
        service = new PreKeyBundleService(deviceRepository, signedPreKeyRepository,
                oneTimePreKeyRepository, eventPublisher, WATERMARK);

        device = new Device("bob", 1, 7, new byte[]{1});
        when(deviceRepository.findByUserId("bob")).thenReturn(List.of(device));
        when(signedPreKeyRepository.findByDeviceAndActiveTrue(device))
                .thenReturn(Optional.of(new SignedPreKey(device, 100, new byte[]{2}, new byte[]{3}, true)));
        when(oneTimePreKeyRepository.claimNextForUpdate(any()))
                .thenReturn(Optional.of(new OneTimePreKey(device, 1, new byte[]{4})));
    }

    @Test
    void publishesPreKeysLowWhenRemainingBelowWatermark() {
        when(oneTimePreKeyRepository.countByDevice(device)).thenReturn(2L);

        service.assembleBundle("bob");

        verify(eventPublisher).publishPreKeysLow(any(PreKeysLowEvent.class));
    }

    @Test
    void doesNotPublishWhenRemainingAtOrAboveWatermark() {
        when(oneTimePreKeyRepository.countByDevice(device)).thenReturn(50L);

        service.assembleBundle("bob");

        verify(eventPublisher, never()).publishPreKeysLow(any());
    }
}
