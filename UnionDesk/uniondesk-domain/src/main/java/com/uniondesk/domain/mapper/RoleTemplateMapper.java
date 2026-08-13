package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.RoleTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleTemplateMapper {

    RoleTemplatePo selectById(@Param("id") long id);

    RoleTemplatePo selectByCode(@Param("code") String code);

    List<RoleTemplatePo> selectAll();

    void insert(RoleTemplatePo po);

    void update(
            @Param("id") long id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("syncStrategy") String syncStrategy,
            @Param("lockedFields") String lockedFields,
            @Param("version") int version);

    void deleteById(@Param("id") long id);
}
