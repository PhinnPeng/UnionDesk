package com.uniondesk.attachment.repository;

import com.uniondesk.attachment.entity.AttachmentPolicyPo;
import com.uniondesk.attachment.mapper.AttachmentPolicyMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AttachmentPolicyRepository {

    private final AttachmentPolicyMapper mapper;

    public AttachmentPolicyRepository(AttachmentPolicyMapper mapper) {
        this.mapper = mapper;
    }

    public AttachmentPolicyPo findDomainPolicy(long domainId) {
        return mapper.selectDomainPolicy(domainId);
    }

    public AttachmentPolicyPo findPlatformPolicy() {
        return mapper.selectPlatformPolicy();
    }
}
