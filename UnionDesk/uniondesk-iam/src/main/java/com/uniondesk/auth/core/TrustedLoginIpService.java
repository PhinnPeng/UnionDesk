package com.uniondesk.auth.core;

import com.uniondesk.auth.repository.TrustedLoginIpRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrustedLoginIpService {

    public static final int MAX_TRUSTED_IPS_PER_USER = 10;

    private final TrustedLoginIpRepository trustedLoginIpRepository;
    private final Clock clock;

    public TrustedLoginIpService(TrustedLoginIpRepository trustedLoginIpRepository, Clock clock) {
        this.trustedLoginIpRepository = trustedLoginIpRepository;
        this.clock = clock;
    }

    public boolean isTrusted(long userId, String clientIp) {
        String normalized = normalizeIp(clientIp);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        return trustedLoginIpRepository.findByUserIdAndIp(userId, normalized).isPresent();
    }

    @Transactional
    public void upsertAndPrune(long userId, String clientIp) {
        String normalized = normalizeIp(clientIp);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        trustedLoginIpRepository.upsert(userId, normalized, now);
        int count = trustedLoginIpRepository.countByUserId(userId);
        int excess = count - MAX_TRUSTED_IPS_PER_USER;
        if (excess > 0) {
            trustedLoginIpRepository.deleteOldestByUserId(userId, excess);
        }
    }

    public String normalizeIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "";
        }
        return clientIp.trim();
    }
}
