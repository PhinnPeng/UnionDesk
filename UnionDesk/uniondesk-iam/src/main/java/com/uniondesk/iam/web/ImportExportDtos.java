package com.uniondesk.iam.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class ImportExportDtos {

    private ImportExportDtos() {
    }

    public record ImportTaskCreateResponse(
            long taskId) {
    }

    public record ImportErrorRow(
            int row,
            String message) {
    }

    public record ImportTaskView(
            long id,
            @JsonProperty("task_type") String taskType,
            @JsonProperty("file_name") String fileName,
            String status,
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("success_count") int successCount,
            @JsonProperty("fail_count") int failCount,
            @JsonProperty("error_summary") List<ImportErrorRow> errorSummary,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("finished_at") String finishedAt) {
    }
}
