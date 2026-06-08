package com.uniondesk.attachment.repository;

import com.uniondesk.attachment.entity.AttachmentRefPo;
import com.uniondesk.attachment.mapper.AttachmentRefMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AttachmentRefRepository {

    private final AttachmentRefMapper mapper;

    public AttachmentRefRepository(AttachmentRefMapper mapper) {
        this.mapper = mapper;
    }

    public void save(AttachmentRefPo po) {
        mapper.insert(po);
    }
}
