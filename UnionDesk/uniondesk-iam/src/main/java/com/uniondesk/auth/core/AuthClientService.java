package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.AuthClientPo;
import com.uniondesk.auth.repository.AuthClientRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthClientService {

    private final AuthClientRepository authClientRepository;

    public AuthClientService(AuthClientRepository authClientRepository) {
        this.authClientRepository = authClientRepository;
    }

    public Optional<AuthClient> findByCode(String clientCode) {
        if (!StringUtils.hasText(clientCode)) {
            return Optional.empty();
        }
        return authClientRepository.findByClientCode(clientCode.trim())
                .map(this::toAuthClient);
    }

    private AuthClient toAuthClient(AuthClientPo po) {
        return new AuthClient(po.getClientCode(), po.getAllowedAccountType(), po.getStatus());
    }

    public record AuthClient(String clientCode, String allowedAccountType, int status) {
    }
}
