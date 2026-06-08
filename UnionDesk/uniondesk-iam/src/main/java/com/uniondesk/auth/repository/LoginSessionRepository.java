package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.AuthLoginSessionPo;
import com.uniondesk.auth.entity.OnlineSessionPo;
import com.uniondesk.auth.mapper.AuthLoginSessionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class LoginSessionRepository {

    private final AuthLoginSessionMapper mapper;

    public LoginSessionRepository(AuthLoginSessionMapper mapper) {
        this.mapper = mapper;
    }

    public void insertSession(AuthLoginSessionPo po) {
        mapper.insertSession(po);
    }

    public Optional<AuthLoginSessionPo> findBySidAndType(String sid, String sessionType) {
        return Optional.ofNullable(mapper.selectBySidAndType(sid, sessionType));
    }

    public int updateLastSeen(String sid, String sessionType, LocalDateTime lastSeenAt) {
        return mapper.updateLastSeen(sid, sessionType, lastSeenAt);
    }

    public int updateLastSeenAndExpires(String sid, String sessionType, LocalDateTime lastSeenAt, LocalDateTime expiresAt) {
        return mapper.updateLastSeenAndExpires(sid, sessionType, lastSeenAt, expiresAt);
    }

    public int revokeBySid(String sid, String sessionType, LocalDateTime revokedAt, String revokedReason) {
        return mapper.revokeBySid(sid, sessionType, revokedAt, revokedReason);
    }

    public int revokeByUserId(long userId, String sessionType, LocalDateTime revokedAt, String revokedReason) {
        return mapper.revokeByUserId(userId, sessionType, revokedAt, revokedReason);
    }

    public int expireBySid(String sid, String sessionType, LocalDateTime revokedAt) {
        return mapper.expireBySid(sid, sessionType, revokedAt);
    }

    public List<OnlineSessionPo> findOnlineSessions(String sessionType, int limit) {
        return mapper.selectOnlineSessions(sessionType, limit);
    }
}
