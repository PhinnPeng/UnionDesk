package com.uniondesk.blockedword.repository;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.blockedword.entity.BlockedWordPo;
import com.uniondesk.blockedword.mapper.BlockedWordMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BlockedWordRepository {

    private final BlockedWordMapper mapper;

    public BlockedWordRepository(BlockedWordMapper mapper) {
        this.mapper = mapper;
    }

    public List<BlockedWordPo> findByDomainId(long domainId) {
        return mapper.selectByDomainId(domainId);
    }

    public Page<BlockedWordPo> findPageByGlobal(Page<BlockedWordPo> page, String keywordLike) {
        return mapper.selectPageByGlobal(page, keywordLike);
    }

    public Page<BlockedWordPo> findPageByDomain(long domainId, Page<BlockedWordPo> page, String keywordLike) {
        return mapper.selectPageByDomain(domainId, page, keywordLike);
    }

    public boolean existsGlobal(String word) {
        return mapper.countByGlobalAndWord(word) > 0;
    }

    public boolean existsInDomain(long domainId, String word) {
        return mapper.countByDomainAndWord(domainId, word) > 0;
    }

    public BlockedWordPo findById(long id) {
        return mapper.selectById(id);
    }

    public void save(BlockedWordPo po) {
        mapper.insert(po);
    }

    public int deleteByIdAndDomainId(long id, long domainId) {
        return mapper.deleteByIdAndDomainId(id, domainId);
    }

    public int deleteByIdGlobal(long id) {
        return mapper.deleteByIdGlobal(id);
    }
}
