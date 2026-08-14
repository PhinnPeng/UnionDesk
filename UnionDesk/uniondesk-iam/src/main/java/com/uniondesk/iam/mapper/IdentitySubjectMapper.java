package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.IdentitySubjectPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentitySubjectMapper extends BaseMapper<IdentitySubjectPo> {

    default Long selectIdByPhone(String phone) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .select(IdentitySubjectPo::getId)
                .where(IdentitySubjectPo::getPhone).eq(phone), Long.class);
    }

    default IdentitySubjectPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .select(IdentitySubjectPo::getId, IdentitySubjectPo::getStatus,
                        IdentitySubjectPo::getMergedIntoId)
                .where(IdentitySubjectPo::getId).eq(id));
    }

    default Long selectMergedIntoId(long id) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .select(IdentitySubjectPo::getMergedIntoId)
                .where(IdentitySubjectPo::getId).eq(id), Long.class);
    }

    int insertRow(IdentitySubjectPo po);

    void updatePhone(@Param("id") long id, @Param("phone") String phone);
}
