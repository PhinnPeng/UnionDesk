package com.uniondesk.attachment.repository;

import com.uniondesk.attachment.entity.FileAttachmentPo;
import com.uniondesk.attachment.mapper.FileAttachmentMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FileAttachmentRepository {

    private final FileAttachmentMapper mapper;

    public FileAttachmentRepository(FileAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    public void save(FileAttachmentPo po) {
        mapper.insert(po);
    }

    public void updateConfirmed(FileAttachmentPo po) {
        mapper.updateConfirmed(po);
    }

    public int updateStatus(long id, String status) {
        return mapper.updateStatus(id, status);
    }

    public FileAttachmentPo findById(long id) {
        FileAttachmentPo po = mapper.selectById(id);
        if (po == null) {
            throw new IllegalArgumentException("attachment not found");
        }
        return po;
    }

    public long findLatestIdByStorageKey(String storageKey) {
        Long id = mapper.selectLatestIdByStorageKey(storageKey);
        if (id == null) {
            throw new IllegalStateException("attachment not found");
        }
        return id;
    }
}
