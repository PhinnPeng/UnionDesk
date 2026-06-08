package com.uniondesk.attachment.mapper;

import com.uniondesk.attachment.entity.FileAttachmentPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileAttachmentMapper {

    void insert(FileAttachmentPo po);

    void updateConfirmed(FileAttachmentPo po);

    int updateStatus(@Param("id") long id, @Param("status") String status);

    FileAttachmentPo selectById(@Param("id") long id);

    Long selectLatestIdByStorageKey(@Param("storageKey") String storageKey);
}
