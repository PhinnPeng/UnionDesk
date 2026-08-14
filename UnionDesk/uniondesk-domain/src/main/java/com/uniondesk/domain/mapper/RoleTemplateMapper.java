package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.RoleTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleTemplateMapper extends BaseMapper<RoleTemplatePo> {

    default RoleTemplatePo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(RoleTemplatePo.class)
                .where(RoleTemplatePo::getId).eq(id));
    }

    default RoleTemplatePo selectByCode(String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(RoleTemplatePo.class)
                .where(RoleTemplatePo::getCode).eq(code));
    }

    default List<RoleTemplatePo> selectAll() {
        return selectListByQuery(QueryWrapper.create()
                .from(RoleTemplatePo.class)
                .orderBy(RoleTemplatePo::getPreset, false)
                .orderBy(RoleTemplatePo::getId, true));
    }

    default void update(
            long id, String name, String description,
            String syncStrategy, String lockedFields, int version) {
        RoleTemplatePo set = new RoleTemplatePo();
        set.setName(name);
        set.setDescription(description);
        set.setSyncStrategy(syncStrategy);
        set.setLockedFields(lockedFields);
        set.setVersion(version);
        updateByQuery(set, QueryWrapper.create()
                .where(RoleTemplatePo::getId).eq(id));
    }
}
