package com.uniondesk.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.auth.entity.AuthClientPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthClientMapper extends BaseMapper<AuthClientPo> {

    default AuthClientPo selectByClientCode(String clientCode) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AuthClientPo.class)
                .where(AuthClientPo::getClientCode).eq(clientCode));
    }
}
