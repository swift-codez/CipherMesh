package com.ciphermesh.identity.api;

import com.ciphermesh.identity.AbstractIntegrationTest;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.repository.SignedPreKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class KeyRegistrationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SignedPreKeyRepository signedPreKeyRepository;

    @Autowired
    private OneTimePreKeyRepository oneTimePreKeyRepository;

    @Test
    void registersDeviceBundleAndReturns201() throws Exception {
        RegisterKeysRequest request = sampleRequest("alice", 1);

        mockMvc.perform(post("/v1/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.oneTimePreKeyCount").value(2));

        Optional<Device> device = deviceRepository.findByUserIdAndDeviceId("alice", 1);
        assertThat(device).isPresent();
        assertThat(signedPreKeyRepository.findByDeviceAndActiveTrue(device.get())).isPresent();
        assertThat(oneTimePreKeyRepository.countByDevice(device.get())).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateDeviceWith409() throws Exception {
        RegisterKeysRequest request = sampleRequest("bob", 1);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/v1/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsMissingIdentityKeyWith400() throws Exception {
        RegisterKeysRequest invalid = new RegisterKeysRequest(
                "carol", 1, 7, "  ",
                new SignedPreKeyRequest(100, encode("spk"), encode("sig")),
                List.of());

        mockMvc.perform(post("/v1/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    private static RegisterKeysRequest sampleRequest(String userId, int deviceId) {
        return new RegisterKeysRequest(
                userId,
                deviceId,
                42,
                encode("identity-key"),
                new SignedPreKeyRequest(100, encode("signed-pre-key"), encode("signature")),
                List.of(
                        new OneTimePreKeyRequest(1, encode("otpk-1")),
                        new OneTimePreKeyRequest(2, encode("otpk-2"))));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
}
