package com.uniondesk.blockedword.mapper;

import com.uniondesk.blockedword.entity.BlockedWordPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BlockedWordMapper {

    List<BlockedWordPo> selectByDomainId(@Param("domainId") long domainId);

    BlockedWordPo selectById(@Param("id") long id);

    List<BlockedWordPo> selectPageByGlobal(
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByGlobal(@Param("keywordLike") String keywordLike);

    List<BlockedWordPo> selectPageByDomain(
            @Param("domainId") long domainId,
            @Param("keywordLike") String keywordLike,
            @Param("limit") int limit,
            @Param("offset") long offset);

    long countByDomain(@Param("domainId") long domainId, @Param("keywordLike") String keywordLike);

    long countByGlobalAndWord(@Param("word") String word);

    long countByDomainAndWord(@Param("domainId") long domainId, @Param("word") String word);

    void insert(BlockedWordPo po);

    int deleteByIdAndDomainId(@Param("id") long id, @Param("domainId") long domainId);

    int deleteByIdGlobal(@Param("id") long id);
}
