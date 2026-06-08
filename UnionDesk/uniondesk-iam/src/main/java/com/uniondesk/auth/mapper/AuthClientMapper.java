package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.AuthClientPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthClientMapper {

    AuthClientPo selectByClientCode(@Param("clientCode") String clientCode);
}
