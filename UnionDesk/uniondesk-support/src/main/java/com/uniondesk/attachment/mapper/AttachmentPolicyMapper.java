package com.uniondesk.attachment.mapper;

import com.uniondesk.attachment.entity.AttachmentPolicyPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttachmentPolicyMapper {

    AttachmentPolicyPo selectDomainPolicy(@Param("scopeId") long scopeId);

    AttachmentPolicyPo selectPlatformPolicy();
}
