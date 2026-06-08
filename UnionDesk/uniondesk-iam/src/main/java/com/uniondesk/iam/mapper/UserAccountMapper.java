package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.UserAccountPo;
import com.uniondesk.iam.entity.UserSummaryPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    List<UserAccountPo> selectByEmploymentStatus(@Param("offboardedOnly") boolean offboardedOnly);

    UserAccountPo selectById(@Param("id") long id);

    int updateSelective(@Param("id") long id,
                        @Param("username") String username,
                        @Param("nickname") String nickname,
                        @Param("mobile") String mobile,
                        @Param("email") String email,
                        @Param("remark") String remark,
                        @Param("passwordHash") String passwordHash,
                        @Param("accountType") String accountType,
                        @Param("status") Integer status);

    int offboard(@Param("id") long id, @Param("offboardedAt") LocalDateTime offboardedAt,
                 @Param("offboardedBy") Long offboardedBy, @Param("offboardReason") String offboardReason);

    int restore(@Param("id") long id);

    int countTicketReferences(@Param("userId") long userId);

    int countConsultationReferences(@Param("userId") long userId);

    int clearOffboardedBy(@Param("userId") long userId);

    void deleteLoginLogsByUsername(@Param("userId") long userId);

    void deleteSessions(@Param("userId") long userId);

    void deleteUserOrganizations(@Param("userId") long userId);

    void deleteById(@Param("id") long id);

    int revokeSessionsOnOffboard(@Param("userId") long userId, @Param("revokedAt") LocalDateTime revokedAt);

    UserSummaryPo selectSummaryById(@Param("id") long id);
}
