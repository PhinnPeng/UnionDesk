package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.ImportTaskPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImportTaskMapper extends BaseMapper<ImportTaskPo> {

    int insertRow(ImportTaskPo po);

    default ImportTaskPo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(ImportTaskPo.class)
                .where(ImportTaskPo::getId).eq(id));
    }

    default int updateStatus(long id, String status, LocalDateTime finishedAt) {
        ImportTaskPo update = new ImportTaskPo();
        update.setStatus(status);
        update.setFinishedAt(finishedAt);
        return updateByQuery(update, true, QueryWrapper.create()
                .from(ImportTaskPo.class)
                .where(ImportTaskPo::getId).eq(id));
    }

    default int updateResult(long id, String status, int totalCount, int successCount,
                             int failCount, String errorSummary, LocalDateTime finishedAt) {
        ImportTaskPo update = new ImportTaskPo();
        update.setStatus(status);
        update.setTotalCount(totalCount);
        update.setSuccessCount(successCount);
        update.setFailCount(failCount);
        update.setErrorSummary(errorSummary);
        update.setFinishedAt(finishedAt);
        return updateByQuery(update, true, QueryWrapper.create()
                .from(ImportTaskPo.class)
                .where(ImportTaskPo::getId).eq(id));
    }
}
