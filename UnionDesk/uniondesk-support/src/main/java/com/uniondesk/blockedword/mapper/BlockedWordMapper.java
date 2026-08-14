package com.uniondesk.blockedword.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.blockedword.entity.BlockedWordPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlockedWordMapper extends BaseMapper<BlockedWordPo> {

    default List<BlockedWordPo> selectByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).eq(domainId)
                .orderBy(BlockedWordPo::getCreatedAt, false)
                .orderBy(BlockedWordPo::getId, false));
    }

    default BlockedWordPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getId).eq(id));
    }

    default Page<BlockedWordPo> selectPageByGlobal(Page<BlockedWordPo> page, String keywordLike) {
        return paginate(page, QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).isNull()
                .and(BlockedWordPo::getWord).like(keywordLike, If::hasText)
                .orderBy(BlockedWordPo::getCreatedAt, false)
                .orderBy(BlockedWordPo::getId, false));
    }

    default Page<BlockedWordPo> selectPageByDomain(long domainId, Page<BlockedWordPo> page, String keywordLike) {
        return paginate(page, QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).eq(domainId)
                .and(BlockedWordPo::getWord).like(keywordLike, If::hasText)
                .orderBy(BlockedWordPo::getCreatedAt, false)
                .orderBy(BlockedWordPo::getId, false));
    }

    default long countByGlobalAndWord(String word) {
        return selectCountByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).isNull()
                .and(BlockedWordPo::getWord).eq(word));
    }

    default long countByDomainAndWord(long domainId, String word) {
        return selectCountByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).eq(domainId)
                .and(BlockedWordPo::getWord).eq(word));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getId).eq(id)
                .and(BlockedWordPo::getBusinessDomainId).eq(domainId));
    }

    default int deleteByIdGlobal(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getId).eq(id)
                .and(BlockedWordPo::getBusinessDomainId).isNull());
    }
}
