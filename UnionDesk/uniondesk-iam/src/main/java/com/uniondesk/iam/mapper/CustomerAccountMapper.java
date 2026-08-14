package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.CustomerAccountPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerAccountMapper extends BaseMapper<CustomerAccountPo> {

    default CustomerAccountPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(CustomerAccountPo.class)
                .select(CustomerAccountPo::getId, CustomerAccountPo::getSubjectId,
                        CustomerAccountPo::getUsername, CustomerAccountPo::getNickname,
                        CustomerAccountPo::getPhone, CustomerAccountPo::getEmail,
                        CustomerAccountPo::getRealName, CustomerAccountPo::getIdCardNo,
                        CustomerAccountPo::getStatus)
                .where(CustomerAccountPo::getId).eq(id));
    }

    default int countByUsername(String username) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(CustomerAccountPo.class)
                .where(CustomerAccountPo::getUsername).eq(username));
    }

    int insertRow(CustomerAccountPo po);

    default Long selectIdByUsernameOrPhone(String username, String phone) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(CustomerAccountPo.class)
                .select(CustomerAccountPo::getId)
                .where(CustomerAccountPo::getUsername).eq(username)
                .or(CustomerAccountPo::getPhone).eq(phone), Long.class);
    }

    int updateProfile(CustomerAccountPo po);
}
