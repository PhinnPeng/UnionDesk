package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.QuickReplyTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuickReplyTemplateMapper extends BaseMapper<QuickReplyTemplatePo> {

    default List<QuickReplyTemplatePo> findByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(QuickReplyTemplatePo.class)
                .where(QuickReplyTemplatePo::getBusinessDomainId).eq(domainId)
                .orderBy(QuickReplyTemplatePo::getSortOrder, true)
                .orderBy(QuickReplyTemplatePo::getId, true));
    }

    default QuickReplyTemplatePo findByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(QuickReplyTemplatePo.class)
                .where(QuickReplyTemplatePo::getId).eq(id)
                .and(QuickReplyTemplatePo::getBusinessDomainId).eq(domainId));
    }

    default QuickReplyTemplatePo findActiveByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(QuickReplyTemplatePo.class)
                .where(QuickReplyTemplatePo::getId).eq(id)
                .and(QuickReplyTemplatePo::getBusinessDomainId).eq(domainId)
                .and(QuickReplyTemplatePo::getStatus).eq("active"));
    }

    default int update(long id, long domainId, String scopeType, String title, String content, int sortOrder) {
        QuickReplyTemplatePo set = new QuickReplyTemplatePo();
        set.setScopeType(scopeType);
        set.setTitle(title);
        set.setContent(content);
        set.setSortOrder(sortOrder);
        return updateByQuery(set, QueryWrapper.create()
                .from(QuickReplyTemplatePo.class)
                .where(QuickReplyTemplatePo::getId).eq(id)
                .and(QuickReplyTemplatePo::getBusinessDomainId).eq(domainId));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(QuickReplyTemplatePo.class)
                .where(QuickReplyTemplatePo::getId).eq(id)
                .and(QuickReplyTemplatePo::getBusinessDomainId).eq(domainId));
    }
}
