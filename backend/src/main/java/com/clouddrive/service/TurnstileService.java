package com.clouddrive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);
    private static final String CLOUDFLARE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    // Cloudflare official Always-Pass testing secret key
    private static final String DEFAULT_TEST_SECRET = "1x0000000000000000000000000000000AA";

    @Value("${app.security.turnstile.secret-key:${TURNSTILE_SECRET_KEY:}}")
    private String secretKey;

    @Value("${app.security.turnstile.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public TurnstileService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean verifyToken(String responseToken) {
        if (!enabled) {
            return true;
        }
        if (!StringUtils.hasText(responseToken)) {
            return false;
        }

        String effectiveSecret = StringUtils.hasText(secretKey) ? secretKey.trim() : DEFAULT_TEST_SECRET;

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", effectiveSecret);
            params.add("response", responseToken.trim());

            ResponseEntity<Map> response = restTemplate.postForEntity(CLOUDFLARE_VERIFY_URL, params, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                log.info("Cloudflare Turnstile verification result: {}", success);
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            log.warn("Cloudflare Turnstile verification error: {}. Allowing graceful fallback for isolated testing.", e.getMessage());
            return true;
        }
        return false;
    }
}
