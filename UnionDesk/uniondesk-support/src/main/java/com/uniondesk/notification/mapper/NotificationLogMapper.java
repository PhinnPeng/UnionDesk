package com.uniondesk.notification.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.notification.entity.NotificationLogPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationLogMapper extends BaseMapper<NotificationLogPo> {

    default Long findLatestId(long recipientSubjectId, long sourceId, String templateCode) {
        NotificationLogPo po = selectOneByQuery(QueryWrapper.create()
                .from(NotificationLogPo.class)
                .where(NotificationLogPo::getRecipientSubjectId).eq(recipientSubjectId)
                .and(NotificationLogPo::getSourceId).eq(sourceId)
                .and(NotificationLogPo::getTemplateCode).eq(templateCode)
                .orderBy(NotificationLogPo::getId, false));
        return po == null ? null : po.getId();
    }
}
