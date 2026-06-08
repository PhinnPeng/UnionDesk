package com.uniondesk.attachment.mapper;

import com.uniondesk.attachment.entity.AttachmentRefPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentRefMapper {

    void insert(AttachmentRefPo po);
}
