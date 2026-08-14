package com.uniondesk.notification.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.notification.entity.NotificationTemplatePo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplatePo> {

    default Page<NotificationTemplatePo> selectPageByDomainId(Page<NotificationTemplatePo> page, long scopeId) {
        return paginate(page, QueryWrapper.create()
                .from(NotificationTemplatePo.class)
                .where(NotificationTemplatePo::getScopeType).eq("domain")
                .and(NotificationTemplatePo::getScopeId).eq(scopeId)
                .orderBy(NotificationTemplatePo::getId, false));
    }

    default NotificationTemplatePo selectByIdAndDomainId(long id, long scopeId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(NotificationTemplatePo.class)
                .where(NotificationTemplatePo::getId).eq(id)
                .and(NotificationTemplatePo::getScopeType).eq("domain")
                .and(NotificationTemplatePo::getScopeId).eq(scopeId));
    }

    default int updateByIdAndDomainId(long id, long scopeId, String eventCategory, String channel, String code,
                                      String titleTemplate, String contentTemplate, boolean isSecurity,
                                      boolean isUnsubscribable, String status) {
        NotificationTemplatePo po = new NotificationTemplatePo();
        po.setId(id);
        po.setScopeType("domain");
        po.setScopeId(scopeId);
        po.setEventCategory(eventCategory);
        po.setChannel(channel);
        po.setCode(code);
        po.setTitleTemplate(titleTemplate);
        po.setContentTemplate(contentTemplate);
        po.setIsSecurity(isSecurity);
        po.setIsUnsubscribable(isUnsubscribable);
        po.setStatus(status);
        return updateByQuery(po, QueryWrapper.create()
                .from(NotificationTemplatePo.class)
                .where(NotificationTemplatePo::getId).eq(id)
                .and(NotificationTemplatePo::getScopeType).eq("domain")
                .and(NotificationTemplatePo::getScopeId).eq(scopeId));
    }
}
