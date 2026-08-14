package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.ImportTaskPo;
import com.uniondesk.iam.mapper.ImportTaskMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ImportTaskRepository {

    private final ImportTaskMapper importTaskMapper;

    public ImportTaskRepository(ImportTaskMapper importTaskMapper) {
        this.importTaskMapper = importTaskMapper;
    }

    public void insert(ImportTaskPo po) {
        importTaskMapper.insertRow(po);
    }

    public Optional<ImportTaskPo> findById(long id) {
        return Optional.ofNullable(importTaskMapper.selectById(id));
    }

    public void updateStatus(long id, String status, LocalDateTime finishedAt) {
        importTaskMapper.updateStatus(id, status, finishedAt);
    }

    public void updateResult(long id, String status, int totalCount, int successCount,
                             int failCount, String errorSummary, LocalDateTime finishedAt) {
        importTaskMapper.updateResult(id, status, totalCount, successCount, failCount, errorSummary, finishedAt);
    }
}
