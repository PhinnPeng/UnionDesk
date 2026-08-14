package com.uniondesk.blockedword.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mybatisflex.core.paginate.Page;
import com.uniondesk.blockedword.entity.BlockedWordPo;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 试点集成冒烟：验证 BlockedWordMapper 从 XML 迁移到 BaseMapper/QueryWrapper 后，
 * insert（Auto 回填 + onInsertValue）、paginate（count 自动生成）、越界页行为与迁移前语义等价。
 */
@SpringBootTest
class BlockedWordRepositoryIntegrationTest {

    @Autowired
    private BlockedWordRepository repository;

    @Test
    void paginateGlobalMatchesInsertedRowsAndOutOfRangePage() {
        String marker = "flex-smoke-" + UUID.randomUUID().toString().substring(0, 8);
        BlockedWordPo po = new BlockedWordPo();
        po.setWord(marker);
        repository.save(po);
        assertThat(po.getId()).isNotNull();
        try {
            Page<BlockedWordPo> page = repository.findPageByGlobal(Page.of(1, 20), marker);
            assertThat(page.getRecords()).anyMatch(r -> marker.equals(r.getWord()));
            assertThat(page.getTotalRow()).isGreaterThanOrEqualTo(1);

            Page<BlockedWordPo> outOfRange = repository.findPageByGlobal(Page.of(999, 20), marker);
            assertThat(outOfRange.getRecords()).isEmpty();
            assertThat(outOfRange.getTotalRow()).isEqualTo(page.getTotalRow());
        } finally {
            repository.deleteByIdGlobal(po.getId());
        }
    }
}
