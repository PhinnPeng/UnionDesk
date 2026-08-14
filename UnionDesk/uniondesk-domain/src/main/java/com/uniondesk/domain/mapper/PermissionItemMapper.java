package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.PermissionItemPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionItemMapper extends BaseMapper<PermissionItemPo> {

    default List<PermissionItemPo> selectAll() {
        return selectListByQuery(QueryWrapper.create()
                .from(PermissionItemPo.class)
                .orderBy(PermissionItemPo::getModule, true)
                .orderBy(PermissionItemPo::getType, true)
                .orderBy(PermissionItemPo::getId, true));
    }

    @Select("SELECT pi.id, pi.code, pi.name, pi.module, pi.type"
            + " FROM domain_role_permission drp"
            + " JOIN permission_item pi ON pi.id = drp.permission_item_id"
            + " WHERE drp.domain_role_id = #{roleId}"
            + " ORDER BY pi.module, pi.type, pi.id")
    List<PermissionItemPo> selectByRoleId(@Param("roleId") long roleId);

    default long countByIds(List<Long> ids) {
        return selectCountByQuery(QueryWrapper.create()
                .from(PermissionItemPo.class)
                .where(PermissionItemPo::getId).in(ids, If::notEmpty));
    }
}
