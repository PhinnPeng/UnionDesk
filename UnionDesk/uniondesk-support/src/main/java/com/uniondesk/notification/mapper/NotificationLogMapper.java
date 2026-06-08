package com.uniondesk.notification.mapper;

import com.uniondesk.notification.entity.NotificationLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationLogMapper {

    void insert(NotificationLogPo po);

    Long findLatestId(@Param("recipientSubjectId") long recipientSubjectId,
                      @Param("sourceId") long sourceId,
                      @Param("templateCode") String templateCode);
}
