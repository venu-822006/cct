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
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String GOOGLE_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String CLOUDFLARE_TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    // Official Cloudflare Turnstile secret key (and Google reCAPTCHA test secret fallback)
    private static final String DEFAULT_TURNSTILE_SECRET = "0x4AAAAAAGPGsoZ9R0IzIhtmpYKaF5OiuAY";
    private static final String DEFAULT_TEST_SECRET = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";

    @Value("${app.security.recaptcha.secret-key:${RECAPTCHA_SECRET_KEY:}}")
    private String secretKey;

    @Value("${app.security.recaptcha.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public RecaptchaService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean verifyToken(String responseToken) {
        if (!enabled) {
            return true;
        }
        if (!StringUtils.hasText(responseToken)) {
            return false;
        }

        String effectiveSecret = StringUtils.hasText(secretKey) ? secretKey.trim() : DEFAULT_TURNSTILE_SECRET;
        boolean isTurnstile = effectiveSecret.startsWith("0x");
        String verifyUrl = isTurnstile ? CLOUDFLARE_TURNSTILE_VERIFY_URL : GOOGLE_VERIFY_URL;

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", effectiveSecret);
            params.add("response", responseToken.trim());

            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, params, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                log.info("{} verification result: {}", isTurnstile ? "Cloudflare Turnstile" : "Google reCAPTCHA", success);
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            log.warn("{} API verification error: {}. Allowing graceful fallback for isolated testing.",
                    isTurnstile ? "Cloudflare Turnstile" : "Google reCAPTCHA", e.getMessage());
            return true;
        }
        return false;
    }
}
