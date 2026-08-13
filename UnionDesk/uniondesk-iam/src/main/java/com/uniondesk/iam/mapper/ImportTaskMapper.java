package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.ImportTaskPo;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImportTaskMapper {

    void insert(ImportTaskPo po);

    ImportTaskPo selectById(@Param("id") long id);

    int updateStatus(@Param("id") long id,
                     @Param("status") String status,
                     @Param("finishedAt") LocalDateTime finishedAt);

    int updateResult(@Param("id") long id,
                     @Param("status") String status,
                     @Param("totalCount") int totalCount,
                     @Param("successCount") int successCount,
                     @Param("failCount") int failCount,
                     @Param("errorSummary") String errorSummary,
                     @Param("finishedAt") LocalDateTime finishedAt);
}
