package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.IamRoleBindingPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IamRoleBindingMapper extends BaseMapper<IamRoleBindingPo> {

    default int selectIdByBinding(long userId, int roleId, long domainId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(IamRoleBindingPo.class)
                .where(IamRoleBindingPo::getUserId).eq(userId)
                .and(IamRoleBindingPo::getRoleId).eq(roleId)
                .and(IamRoleBindingPo::getBindingScope).eq("domain")
                .and(IamRoleBindingPo::getBusinessDomainId).eq(domainId));
    }

    @Insert("INSERT INTO iam_role_binding ("
            + " user_id, role_id, binding_scope, business_domain_id, status, created_at, updated_at"
            + ")"
            + " SELECT #{userId}, #{roleId}, 'domain', #{domainId}, 1,"
            + "        CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)"
            + " FROM DUAL"
            + " WHERE NOT EXISTS ("
            + "  SELECT 1 FROM iam_role_binding"
            + "  WHERE user_id = #{userId} AND role_id = #{roleId}"
            + "    AND binding_scope = 'domain' AND business_domain_id = #{domainId}"
            + " )")
    void insertIfNotExists(
            @Param("userId") long userId,
            @Param("roleId") int roleId,
            @Param("domainId") long domainId);

    @Select("SELECT id FROM role WHERE code = #{code} LIMIT 1")
    Integer selectLegacyRoleId(@Param("code") String code);
}
