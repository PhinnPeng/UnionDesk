package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.AuthLoginSessionPo;
import com.uniondesk.auth.entity.OnlineSessionPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthLoginSessionMapper {

    void insertSession(AuthLoginSessionPo po);

    AuthLoginSessionPo selectBySidAndType(@Param("sid") String sid, @Param("sessionType") String sessionType);

    int updateLastSeen(@Param("sid") String sid, @Param("sessionType") String sessionType, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    int updateLastSeenAndExpires(@Param("sid") String sid, @Param("sessionType") String sessionType,
                                 @Param("lastSeenAt") LocalDateTime lastSeenAt, @Param("expiresAt") LocalDateTime expiresAt);

    int revokeBySid(@Param("sid") String sid, @Param("sessionType") String sessionType,
                    @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedReason") String revokedReason);

    int revokeByUserId(@Param("userId") long userId, @Param("sessionType") String sessionType,
                       @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedReason") String revokedReason);

    int expireBySid(@Param("sid") String sid, @Param("sessionType") String sessionType,
                    @Param("revokedAt") LocalDateTime revokedAt);

    int revokeAllActiveByUserId(@Param("userId") long userId);

    List<OnlineSessionPo> selectOnlineSessions(@Param("sessionType") String sessionType, @Param("limit") int limit);
}
