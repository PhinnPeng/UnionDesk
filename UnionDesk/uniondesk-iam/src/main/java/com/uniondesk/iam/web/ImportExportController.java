package com.uniondesk.iam.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.iam.core.StaffAccountService;
import com.uniondesk.iam.core.StaffImportService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/import-export")
public class ImportExportController {

    private final StaffImportService staffImportService;
    private final StaffAccountService staffAccountService;
    private final Clock clock;

    public ImportExportController(
            StaffImportService staffImportService,
            StaffAccountService staffAccountService,
            Clock clock) {
        this.staffImportService = staffImportService;
        this.staffAccountService = staffAccountService;
        this.clock = clock;
    }

    @PostMapping("/staff/import")
    @RequirePermission(PermissionCodes.PLATFORM_USER_IMPORT)
    public ImportExportDtos.ImportTaskCreateResponse importStaff(@RequestParam("file") MultipartFile file) {
        long operatorId = UserContextHolder.current().map(UserContext::userId).orElse(0L);
        long taskId = staffImportService.createImportTask(file, operatorId);
        staffImportService.executeImport(taskId);
        return new ImportExportDtos.ImportTaskCreateResponse(taskId);
    }

    @GetMapping("/tasks/{taskId}")
    @RequirePermission(PermissionCodes.PLATFORM_USER_IMPORT)
    public ImportExportDtos.ImportTaskView getImportTask(@PathVariable long taskId) {
        StaffImportService.ImportTaskView view = staffImportService.getTask(taskId);
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message());
        }
        return new ImportExportDtos.ImportTaskView(
                view.id(),
                view.taskType(),
                view.fileName(),
                view.status(),
                view.totalCount(),
                view.successCount(),
                view.failCount(),
                view.errorSummary() == null
                        ? null
                        : view.errorSummary().stream()
                                .map(error -> new ImportExportDtos.ImportErrorRow(error.row(), error.message()))
                                .toList(),
                view.createdAt(),
                view.finishedAt());
    }

    @GetMapping("/staff/export")
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public ResponseEntity<byte[]> exportStaff() {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // UTF-8 BOM，保证 Excel 直接打开不乱码
        csv.append("登录账号,姓名,昵称,手机号,邮箱,状态\n");
        for (StaffAccountService.StaffAccount account : staffAccountService.listAll()) {
            csv.append(csvCell(account.username())).append(',')
                    .append(csvCell(account.realName())).append(',')
                    .append(csvCell(account.nickname())).append(',')
                    .append(csvCell(account.phone())).append(',')
                    .append(csvCell(account.email())).append(',')
                    .append(csvCell(employmentStatusText(account)))
                    .append('\n');
        }
        String fileName = "用户导出_" + LocalDate.now(clock) + ".csv";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String employmentStatusText(StaffAccountService.StaffAccount account) {
        if ("offboarded".equalsIgnoreCase(account.employmentStatus())) {
            return "离职";
        }
        return "active".equalsIgnoreCase(account.status()) ? "在职" : "停用";
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
