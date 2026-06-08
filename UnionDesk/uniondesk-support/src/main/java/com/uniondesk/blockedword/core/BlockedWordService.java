package com.uniondesk.blockedword.core;

import com.uniondesk.blockedword.entity.BlockedWordPo;
import com.uniondesk.blockedword.repository.BlockedWordRepository;
import com.uniondesk.blockedword.web.BlockedWordDtos;
import com.uniondesk.common.web.PageResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class BlockedWordService {

    private static final int MAX_BATCH_SIZE = 200;
    private static final String DUPLICATE_REASON = "已存在";

    private final BlockedWordRepository blockedWordRepository;

    public BlockedWordService(BlockedWordRepository blockedWordRepository) {
        this.blockedWordRepository = blockedWordRepository;
    }

    public PageResult<BlockedWordDtos.BlockedWordView> listGlobalPage(int page, int pageSize, String keyword) {
        return listPage(null, page, pageSize, keyword);
    }

    public PageResult<BlockedWordDtos.BlockedWordView> listDomainPage(
            long domainId, int page, int pageSize, String keyword) {
        return listPage(domainId, page, pageSize, keyword);
    }

    @Transactional
    public BlockedWordDtos.BlockedWordView createGlobal(String word) {
        String normalizedWord = normalizeWord(word);
        if (blockedWordRepository.existsGlobal(normalizedWord)) {
            throw new IllegalArgumentException("该屏蔽词已存在");
        }
        return insertWord(null, normalizedWord);
    }

    @Transactional
    public BlockedWordDtos.BlockedWordView createDomain(long domainId, String word) {
        String normalizedWord = normalizeWord(word);
        if (blockedWordRepository.existsInDomain(domainId, normalizedWord)) {
            throw new IllegalArgumentException("该屏蔽词已存在");
        }
        return insertWord(domainId, normalizedWord);
    }

    @Transactional
    public BlockedWordDtos.BatchCreateBlockedWordResult createGlobalBatch(List<String> words) {
        return createBatch(null, words);
    }

    @Transactional
    public BlockedWordDtos.BatchCreateBlockedWordResult createDomainBatch(long domainId, List<String> words) {
        return createBatch(domainId, words);
    }

    @Transactional
    public void deleteGlobal(long wordId) {
        int updated = blockedWordRepository.deleteByIdGlobal(wordId);
        if (updated == 0) {
            throw new IllegalArgumentException("屏蔽词不存在");
        }
    }

    @Transactional
    public void deleteDomain(long domainId, long wordId) {
        int updated = blockedWordRepository.deleteByIdAndDomainId(wordId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("屏蔽词不存在");
        }
    }

    private PageResult<BlockedWordDtos.BlockedWordView> listPage(
            Long domainId, int page, int pageSize, String keyword) {
        String keywordLike = toKeywordLike(keyword);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;

        long total;
        List<BlockedWordPo> rows;
        if (domainId == null) {
            total = blockedWordRepository.countByGlobal(keywordLike);
            rows = blockedWordRepository.findPageByGlobal(keywordLike, normalizedPageSize, offset);
        }
        else {
            total = blockedWordRepository.countByDomain(domainId, keywordLike);
            rows = blockedWordRepository.findPageByDomain(domainId, keywordLike, normalizedPageSize, offset);
        }
        return new PageResult<>(total, rows.stream().map(this::toView).toList());
    }

    private BlockedWordDtos.BatchCreateBlockedWordResult createBatch(Long domainId, List<String> words) {
        List<String> parsed = parseBatchWords(words);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("请至少输入一个屏蔽词");
        }
        if (parsed.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("单次最多添加 200 个屏蔽词");
        }

        int createdCount = 0;
        List<BlockedWordDtos.BatchCreateSkippedItem> skipped = new ArrayList<>();
        Set<String> seenInBatch = new LinkedHashSet<>();

        for (String word : parsed) {
            if (!seenInBatch.add(word)) {
                skipped.add(new BlockedWordDtos.BatchCreateSkippedItem(word, DUPLICATE_REASON));
                continue;
            }
            boolean exists = domainId == null
                    ? blockedWordRepository.existsGlobal(word)
                    : blockedWordRepository.existsInDomain(domainId, word);
            if (exists) {
                skipped.add(new BlockedWordDtos.BatchCreateSkippedItem(word, DUPLICATE_REASON));
                continue;
            }
            insertWord(domainId, word);
            createdCount++;
        }
        return new BlockedWordDtos.BatchCreateBlockedWordResult(createdCount, skipped);
    }

    private BlockedWordDtos.BlockedWordView insertWord(Long domainId, String word) {
        BlockedWordPo po = new BlockedWordPo();
        po.setBusinessDomainId(domainId);
        po.setWord(word);
        blockedWordRepository.save(po);
        BlockedWordPo saved = blockedWordRepository.findById(po.getId());
        return toView(saved != null ? saved : po);
    }

    private List<String> parseBatchWords(List<String> words) {
        Set<String> normalized = new LinkedHashSet<>();
        if (words == null) {
            return List.of();
        }
        for (String raw : words) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            for (String part : raw.split("[\\n,，、]+")) {
                if (StringUtils.hasText(part)) {
                    normalized.add(part.trim());
                }
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeWord(String word) {
        if (!StringUtils.hasText(word)) {
            throw new IllegalArgumentException("屏蔽词不能为空");
        }
        return word.trim();
    }

    private String toKeywordLike(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    private BlockedWordDtos.BlockedWordView toView(BlockedWordPo po) {
        return new BlockedWordDtos.BlockedWordView(po.getId(), po.getWord(), po.getCreatedAt());
    }
}
