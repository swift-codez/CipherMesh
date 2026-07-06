package com.ciphermesh.identity.api;

import com.ciphermesh.identity.service.PreKeyBundleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/keys")
public class PreKeyBundleController {

    private final PreKeyBundleService preKeyBundleService;

    public PreKeyBundleController(PreKeyBundleService preKeyBundleService) {
        this.preKeyBundleService = preKeyBundleService;
    }

    /**
     * Returns the pre-key bundle a peer needs to run X3DH against {@code userId}.
     * Consumes one of the device's one-time pre-keys as a side effect.
     */
    @GetMapping("/{userId}/bundle")
    public PreKeyBundleResponse bundle(@PathVariable String userId) {
        return preKeyBundleService.assembleBundle(userId);
    }
}
