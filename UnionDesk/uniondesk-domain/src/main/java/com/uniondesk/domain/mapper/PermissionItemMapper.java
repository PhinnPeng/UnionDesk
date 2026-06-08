package com.uniondesk.domain.mapper;

import com.uniondesk.domain.entity.PermissionItemPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PermissionItemMapper {

    List<PermissionItemPo> selectAll();

    List<PermissionItemPo> selectByRoleId(@Param("roleId") long roleId);

    long countByIds(@Param("ids") List<Long> ids);
}
