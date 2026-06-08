package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.StaffAccountPo;
import com.uniondesk.iam.entity.StaffAccountPresentationPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StaffAccountMapper {

    List<StaffAccountPo> selectAll();

    StaffAccountPo selectById(@Param("id") long id);

    void insert(StaffAccountPo po);

    int updateSelective(@Param("id") long id,
                        @Param("username") String username,
                        @Param("realName") String realName,
                        @Param("nickname") String nickname,
                        @Param("phone") String phone,
                        @Param("email") String email,
                        @Param("passwordHash") String passwordHash,
                        @Param("status") String status);

    int updateStatus(@Param("id") long id, @Param("status") String status);

    StaffAccountPresentationPo selectPresentationById(@Param("id") long id);

    int revokeActiveSessions(@Param("userId") long userId, @Param("revokedReason") String revokedReason);

    int countActiveStaffByDomainRole(@Param("businessDomainId") long businessDomainId,
                                     @Param("roleCode") String roleCode,
                                     @Param("excludeStaffAccountId") long excludeStaffAccountId);
}
