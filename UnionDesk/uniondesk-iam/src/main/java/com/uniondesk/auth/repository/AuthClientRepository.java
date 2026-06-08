package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.AuthClientPo;
import com.uniondesk.auth.mapper.AuthClientMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AuthClientRepository {

    private final AuthClientMapper mapper;

    public AuthClientRepository(AuthClientMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<AuthClientPo> findByClientCode(String clientCode) {
        return Optional.ofNullable(mapper.selectByClientCode(clientCode));
    }
}
