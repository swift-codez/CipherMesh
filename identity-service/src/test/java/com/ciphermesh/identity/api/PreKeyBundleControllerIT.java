package com.ciphermesh.identity.api;

import com.ciphermesh.identity.AbstractIntegrationTest;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.service.KeyRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class PreKeyBundleControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeyRegistrationService keyRegistrationService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OneTimePreKeyRepository oneTimePreKeyRepository;

    @Test
    void returnsBundleAndConsumesOneOneTimePreKey() throws Exception {
        register("frank", 2);

        mockMvc.perform(get("/v1/keys/{userId}/bundle", "frank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("frank"))
                .andExpect(jsonPath("$.identityKey").isNotEmpty())
                .andExpect(jsonPath("$.signedPreKey.keyId").value(100))
                .andExpect(jsonPath("$.signedPreKey.signature").isNotEmpty())
                .andExpect(jsonPath("$.oneTimePreKey.keyId").exists());

        Device device = deviceRepository.findByUserIdAndDeviceId("frank", 1).orElseThrow();
        assertThat(oneTimePreKeyRepository.countByDevice(device)).isEqualTo(1);
    }

    @Test
    void returnsNullOneTimePreKeyWhenPoolExhausted() throws Exception {
        register("grace", 1);

        // First request consumes the only one-time pre-key.
        mockMvc.perform(get("/v1/keys/{userId}/bundle", "grace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneTimePreKey.keyId").exists());

        // Pool is now empty; bundle still returned, one-time pre-key is null.
        mockMvc.perform(get("/v1/keys/{userId}/bundle", "grace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signedPreKey").exists())
                .andExpect(jsonPath("$.oneTimePreKey").value(nullValue()));
    }

    @Test
    void returns404ForUnknownUser() throws Exception {
        mockMvc.perform(get("/v1/keys/{userId}/bundle", "nobody"))
                .andExpect(status().isNotFound());
    }

    private void register(String userId, int oneTimePreKeyCount) {
        List<OneTimePreKeyRequest> oneTimePreKeys = java.util.stream.IntStream.rangeClosed(1, oneTimePreKeyCount)
                .mapToObj(i -> new OneTimePreKeyRequest(i, encode("otpk-" + i)))
                .toList();
        keyRegistrationService.register(new RegisterKeysRequest(
                userId,
                1,
                42,
                encode("identity-key"),
                new SignedPreKeyRequest(100, encode("signed-pre-key"), encode("signature")),
                oneTimePreKeys));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
