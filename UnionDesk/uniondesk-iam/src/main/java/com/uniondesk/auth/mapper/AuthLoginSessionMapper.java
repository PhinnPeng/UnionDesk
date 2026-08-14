package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.AuthLoginSessionPo;
import com.uniondesk.auth.entity.OnlineSessionPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthLoginSessionMapper extends BaseMapper<AuthLoginSessionPo> {

    default int insertSession(AuthLoginSessionPo po) {
        return insert(po);
    }

    default AuthLoginSessionPo selectBySidAndType(String sid, String sessionType) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AuthLoginSessionPo.class)
                .where(AuthLoginSessionPo::getSid).eq(sid)
                .and(AuthLoginSessionPo::getSessionType).eq(sessionType));
    }

    default int updateLastSeen(String sid, String sessionType, LocalDateTime lastSeenAt) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setLastSeenAt(lastSeenAt);
        return updateByQuery(update, true, activeSessionQuery(sid, sessionType));
    }

    default int updateLastSeenAndExpires(String sid, String sessionType,
                                         LocalDateTime lastSeenAt, LocalDateTime expiresAt) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setLastSeenAt(lastSeenAt);
        update.setExpiresAt(expiresAt);
        return updateByQuery(update, true, activeSessionQuery(sid, sessionType));
    }

    default int updateBusinessDomainAndRefreshToken(String sid,
                                                    String sessionType,
                                                    long businessDomainId,
                                                    String refreshTokenHash,
                                                    LocalDateTime lastSeenAt) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setBusinessDomainId(businessDomainId);
        update.setRefreshTokenHash(refreshTokenHash);
        update.setLastSeenAt(lastSeenAt);
        return updateByQuery(update, true, activeSessionQuery(sid, sessionType));
    }

    default int revokeBySid(String sid, String sessionType,
                            LocalDateTime revokedAt, String revokedReason) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setSessionStatus("revoked");
        update.setRevokedAt(revokedAt);
        update.setRevokedReason(revokedReason);
        return updateByQuery(update, true, activeSessionQuery(sid, sessionType));
    }

    default int revokeByUserId(long userId, String sessionType,
                               LocalDateTime revokedAt, String revokedReason) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setSessionStatus("revoked");
        update.setRevokedAt(revokedAt);
        update.setRevokedReason(revokedReason);
        return updateByQuery(update, true, QueryWrapper.create()
                .from(AuthLoginSessionPo.class)
                .where(AuthLoginSessionPo::getUserId).eq(userId)
                .and(AuthLoginSessionPo::getSessionStatus).eq("active")
                .and(AuthLoginSessionPo::getSessionType).eq(sessionType));
    }

    default int expireBySid(String sid, String sessionType, LocalDateTime revokedAt) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setSessionStatus("expired");
        update.setRevokedAt(revokedAt);
        update.setRevokedReason("expired");
        return updateByQuery(update, true, activeSessionQuery(sid, sessionType));
    }

    default int revokeAllActiveByUserId(long userId) {
        AuthLoginSessionPo update = new AuthLoginSessionPo();
        update.setSessionStatus("revoked");
        return updateByQuery(update, true, QueryWrapper.create()
                .from(AuthLoginSessionPo.class)
                .where(AuthLoginSessionPo::getUserId).eq(userId)
                .and(AuthLoginSessionPo::getSessionStatus).eq("active"));
    }

    List<OnlineSessionPo> selectOnlineSessions(@Param("sessionType") String sessionType, @Param("limit") int limit);

    private QueryWrapper activeSessionQuery(String sid, String sessionType) {
        return QueryWrapper.create()
                .from(AuthLoginSessionPo.class)
                .where(AuthLoginSessionPo::getSid).eq(sid)
                .and(AuthLoginSessionPo::getSessionType).eq(sessionType)
                .and(AuthLoginSessionPo::getSessionStatus).eq("active");
    }
}
