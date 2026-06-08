package com.uniondesk.blockedword.repository;

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

    public List<BlockedWordPo> findPageByGlobal(String keywordLike, int limit, long offset) {
        return mapper.selectPageByGlobal(keywordLike, limit, offset);
    }

    public long countByGlobal(String keywordLike) {
        return mapper.countByGlobal(keywordLike);
    }

    public List<BlockedWordPo> findPageByDomain(long domainId, String keywordLike, int limit, long offset) {
        return mapper.selectPageByDomain(domainId, keywordLike, limit, offset);
    }

    public long countByDomain(long domainId, String keywordLike) {
        return mapper.countByDomain(domainId, keywordLike);
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
