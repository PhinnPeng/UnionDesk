package com.uniondesk.blockedword.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.blockedword.entity.BlockedWordPo;
import com.uniondesk.blockedword.repository.BlockedWordRepository;
import com.uniondesk.blockedword.web.BlockedWordDtos;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockedWordServiceTests {

    @Mock
    private BlockedWordRepository blockedWordRepository;

    private BlockedWordService blockedWordService;

    @BeforeEach
    void setUp() {
        blockedWordService = new BlockedWordService(blockedWordRepository);
    }

    @Test
    void listDomainPageMapsRows() {
        BlockedWordPo po = new BlockedWordPo();
        po.setId(11L);
        po.setWord("spam");
        po.setCreatedAt(LocalDateTime.of(2026, 5, 3, 12, 0));
        when(blockedWordRepository.countByDomain(1L, null)).thenReturn(1L);
        when(blockedWordRepository.findPageByDomain(eq(1L), isNull(), eq(20), eq(0L))).thenReturn(List.of(po));

        PageResult<BlockedWordDtos.BlockedWordView> page = blockedWordService.listDomainPage(1L, 1, 20, null);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).word()).isEqualTo("spam");
    }

    @Test
    void createDomainTrimsWordAndInsertsRow() {
        doAnswer(invocation -> {
            BlockedWordPo po = invocation.getArgument(0);
            po.setId(11L);
            po.setCreatedAt(LocalDateTime.of(2026, 5, 3, 12, 0));
            return null;
        }).when(blockedWordRepository).save(any(BlockedWordPo.class));
        when(blockedWordRepository.existsInDomain(1L, "spam")).thenReturn(false);
        when(blockedWordRepository.findById(11L)).thenAnswer(invocation -> {
            BlockedWordPo po = new BlockedWordPo();
            po.setId(11L);
            po.setBusinessDomainId(1L);
            po.setWord("spam");
            po.setCreatedAt(LocalDateTime.of(2026, 5, 3, 12, 0));
            return po;
        });

        BlockedWordDtos.BlockedWordView created = blockedWordService.createDomain(1L, "  spam  ");

        assertThat(created.word()).isEqualTo("spam");
        ArgumentCaptor<BlockedWordPo> captor = ArgumentCaptor.forClass(BlockedWordPo.class);
        verify(blockedWordRepository).save(captor.capture());
        assertThat(captor.getValue().getBusinessDomainId()).isEqualTo(1L);
    }

    @Test
    void createDomainRejectsDuplicate() {
        when(blockedWordRepository.existsInDomain(1L, "spam")).thenReturn(true);

        assertThatThrownBy(() -> blockedWordService.createDomain(1L, "spam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该屏蔽词已存在");
    }

    @Test
    void createDomainBatchSkipsDuplicates() {
        when(blockedWordRepository.existsInDomain(1L, "spam")).thenReturn(true);
        when(blockedWordRepository.existsInDomain(1L, "广告")).thenReturn(false);
        doAnswer(invocation -> {
            BlockedWordPo po = invocation.getArgument(0);
            po.setId(12L);
            po.setCreatedAt(LocalDateTime.of(2026, 5, 3, 12, 0));
            return null;
        }).when(blockedWordRepository).save(any(BlockedWordPo.class));
        when(blockedWordRepository.findById(12L)).thenAnswer(invocation -> {
            BlockedWordPo po = new BlockedWordPo();
            po.setId(12L);
            po.setWord("广告");
            return po;
        });

        BlockedWordDtos.BatchCreateBlockedWordResult result =
                blockedWordService.createDomainBatch(1L, List.of("spam", "广告", "spam"));

        assertThat(result.created_count()).isEqualTo(1);
        assertThat(result.skipped()).hasSize(1);
        assertThat(result.skipped().get(0).word()).isEqualTo("spam");
    }

    @Test
    void deleteDomainDeletesRow() {
        when(blockedWordRepository.deleteByIdAndDomainId(11L, 1L)).thenReturn(1);

        blockedWordService.deleteDomain(1L, 11L);

        verify(blockedWordRepository).deleteByIdAndDomainId(11L, 1L);
    }

    @Test
    void createDomainRejectsBlankWord() {
        assertThatThrownBy(() -> blockedWordService.createDomain(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("屏蔽词不能为空");
    }
}
