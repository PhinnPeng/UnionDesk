package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTeamTemplatePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTeamTemplateMapper extends BaseMapper<TicketTeamTemplatePo> {

    default List<TicketTeamTemplatePo> findAll(String keywordLike) {
        return selectListByQuery(keywordQuery(keywordLike)
                .orderBy(TicketTeamTemplatePo::getSortOrder, true)
                .orderBy(TicketTeamTemplatePo::getId, true));
    }

    default Page<TicketTeamTemplatePo> selectPage(Page<TicketTeamTemplatePo> page, String keywordLike) {
        return paginate(page, keywordQuery(keywordLike)
                .orderBy(TicketTeamTemplatePo::getSortOrder, true)
                .orderBy(TicketTeamTemplatePo::getId, true));
    }

    default List<TicketTeamTemplatePo> findActiveOptions() {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getStatus).eq(TicketTeamTemplatePo.STATUS_ACTIVE)
                .orderBy(TicketTeamTemplatePo::getSortOrder, true)
                .orderBy(TicketTeamTemplatePo::getId, true));
    }

    default TicketTeamTemplatePo findById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getId).eq(id));
    }

    default TicketTeamTemplatePo findByCode(String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getCode).eq(code));
    }

    default Integer findMaxSortOrder() {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketTeamTemplatePo::getSortOrder))
                .from(TicketTeamTemplatePo.class), Integer.class);
    }

    @Override
    default int update(TicketTeamTemplatePo po) {
        TicketTeamTemplatePo set = new TicketTeamTemplatePo();
        set.setName(po.getName());
        set.setDescription(po.getDescription());
        set.setIcon(po.getIcon());
        set.setStatus(po.getStatus());
        set.setVersion(po.getVersion());
        set.setUpdatedBy(po.getUpdatedBy());
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getId).eq(po.getId()));
    }

    default int updateSortOrder(long id, int sortOrder, Long updatedBy) {
        TicketTeamTemplatePo set = new TicketTeamTemplatePo();
        set.setSortOrder(sortOrder);
        set.setUpdatedBy(updatedBy);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getId).eq(id));
    }

    default int deleteById(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTeamTemplatePo.class)
                .where(TicketTeamTemplatePo::getId).eq(id)
                .and(TicketTeamTemplatePo::isSystem).eq(false));
    }

    private static QueryWrapper keywordQuery(String keywordLike) {
        QueryWrapper qw = QueryWrapper.create().from(TicketTeamTemplatePo.class);
        if (If.hasText(keywordLike)) {
            qw.where(qw2 -> {
                qw2.where(TicketTeamTemplatePo::getName).like(keywordLike)
                        .or(TicketTeamTemplatePo::getDescription).like(keywordLike)
                        .or(TicketTeamTemplatePo::getCode).like(keywordLike);
            });
        }
        return qw;
    }
}
