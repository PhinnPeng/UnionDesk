package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.BusinessDomainPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BusinessDomainMapper extends BaseMapper<BusinessDomainPo> {

    default int countActiveById(long id) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(BusinessDomainPo.class)
                .where(BusinessDomainPo::getId).eq(id)
                .and("deleted_at IS NULL"));
    }

    default int countByIds(List<Long> ids) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(BusinessDomainPo.class)
                .where(BusinessDomainPo::getId).in(ids));
    }

    default List<BusinessDomainPo> selectAll() {
        return selectListByQuery(QueryWrapper.create()
                .from(BusinessDomainPo.class)
                .orderBy(BusinessDomainPo::getId, true));
    }
}
