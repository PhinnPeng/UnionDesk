package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.IdentitySubjectPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IdentitySubjectMapper extends BaseMapper<IdentitySubjectPo> {

    default Long selectIdById(long id) {
        IdentitySubjectPo po = selectOneByQuery(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .select(IdentitySubjectPo::getId)
                .where(IdentitySubjectPo::getId).eq(id));
        return po == null ? null : po.getId();
    }

    @Select("SELECT COALESCE(NULLIF(phone, ''), #{fallback})"
            + " FROM staff_account WHERE id = #{userId} LIMIT 1")
    String selectPhoneByUserId(@Param("userId") long userId, @Param("fallback") String fallback);

    @Insert("INSERT INTO identity_subject (id, subject_type, phone, status)"
            + " VALUES (#{id}, 'person', #{phone}, 'active')")
    void insertPerson(@Param("id") long id, @Param("phone") String phone);

    default Long selectIdByPhone(String phone) {
        IdentitySubjectPo po = selectOneByQuery(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .select(IdentitySubjectPo::getId)
                .where(IdentitySubjectPo::getPhone).eq(phone));
        return po == null ? null : po.getId();
    }
}
