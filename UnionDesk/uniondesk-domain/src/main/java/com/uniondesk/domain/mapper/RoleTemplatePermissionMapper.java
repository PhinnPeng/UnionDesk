package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.entity.RoleTemplatePermissionPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleTemplatePermissionMapper {

    List<RoleTemplatePermissionPo> selectByTemplateId(@Param("templateId") long templateId);

    List<PermissionItemPo> selectPermissionItemsByTemplateId(@Param("templateId") long templateId);

    void deleteByTemplateId(@Param("templateId") long templateId);

    void insert(
            @Param("templateId") long templateId,
            @Param("permissionItemId") long permissionItemId);
}
