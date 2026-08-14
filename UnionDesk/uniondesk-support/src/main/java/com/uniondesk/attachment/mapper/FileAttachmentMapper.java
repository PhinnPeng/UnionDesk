package com.uniondesk.attachment.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.attachment.entity.FileAttachmentPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileAttachmentMapper extends BaseMapper<FileAttachmentPo> {

    default int updateConfirmed(FileAttachmentPo po) {
        FileAttachmentPo set = new FileAttachmentPo();
        set.setId(po.getId());
        set.setBusinessDomainId(po.getBusinessDomainId());
        set.setUploaderSubjectId(po.getUploaderSubjectId());
        set.setPortalType(po.getPortalType());
        set.setFileName(po.getFileName());
        set.setMimeType(po.getMimeType());
        set.setFileSize(po.getFileSize());
        set.setStorageType(po.getStorageType());
        set.setStorageKey(po.getStorageKey());
        set.setChecksum(po.getChecksum());
        set.setStatus("confirmed");
        return updateByQuery(set, QueryWrapper.create()
                .from(FileAttachmentPo.class)
                .where(FileAttachmentPo::getId).eq(po.getId()));
    }

    default int updateStatus(long id, String status) {
        FileAttachmentPo set = new FileAttachmentPo();
        set.setStatus(status);
        return updateByQuery(set, QueryWrapper.create()
                .from(FileAttachmentPo.class)
                .where(FileAttachmentPo::getId).eq(id));
    }

    default FileAttachmentPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(FileAttachmentPo.class)
                .where(FileAttachmentPo::getId).eq(id));
    }

    default Long selectLatestIdByStorageKey(String storageKey) {
        FileAttachmentPo po = selectOneByQuery(QueryWrapper.create()
                .from(FileAttachmentPo.class)
                .where(FileAttachmentPo::getStorageKey).eq(storageKey)
                .orderBy(FileAttachmentPo::getId, false));
        return po == null ? null : po.getId();
    }
}
