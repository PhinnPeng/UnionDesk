package com.uniondesk.notification.mapper;

import com.uniondesk.notification.entity.InboxMessagePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InboxMessageMapper {

    void insert(InboxMessagePo po);

    Long countUnread(@Param("recipientSubjectId") long recipientSubjectId);

    List<InboxMessagePo> listByRecipient(@Param("recipientSubjectId") long recipientSubjectId,
                                         @Param("unreadOnly") boolean unreadOnly,
                                         @Param("limit") int limit);

    int markRead(@Param("recipientSubjectId") long recipientSubjectId,
                 @Param("inboxMessageId") long inboxMessageId);

    int markReadBatch(@Param("recipientSubjectId") long recipientSubjectId,
                      @Param("messageIds") List<Long> messageIds);

    Long findLatestId(@Param("recipientSubjectId") long recipientSubjectId,
                      @Param("notificationLogId") long notificationLogId);
}
