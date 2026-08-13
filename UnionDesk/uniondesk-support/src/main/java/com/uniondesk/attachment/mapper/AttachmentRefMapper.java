package com.uniondesk.attachment.mapper;

import com.uniondesk.attachment.entity.AttachmentRefPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AttachmentRefMapper {

    void insert(AttachmentRefPo po);

    int countLinkedToCustomerTicket(
            @Param("attachmentId") long attachmentId,
            @Param("customerId") long customerId);
}
