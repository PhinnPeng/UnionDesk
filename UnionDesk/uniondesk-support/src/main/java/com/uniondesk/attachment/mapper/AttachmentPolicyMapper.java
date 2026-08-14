package com.uniondesk.attachment.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.attachment.entity.AttachmentPolicyPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentPolicyMapper extends BaseMapper<AttachmentPolicyPo> {

    default AttachmentPolicyPo selectDomainPolicy(long scopeId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(AttachmentPolicyPo.class)
                .where(AttachmentPolicyPo::getScopeType).eq("domain")
                .and(AttachmentPolicyPo::getScopeId).eq(scopeId));
    }

    default AttachmentPolicyPo selectPlatformPolicy() {
        return selectOneByQuery(QueryWrapper.create()
                .from(AttachmentPolicyPo.class)
                .where(AttachmentPolicyPo::getScopeType).eq("platform")
                .and(AttachmentPolicyPo::getScopeId).eq(0L));
    }
}
