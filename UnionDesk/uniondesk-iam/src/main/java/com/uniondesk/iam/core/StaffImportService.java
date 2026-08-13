package com.uniondesk.iam.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.iam.entity.ImportTaskPo;
import com.uniondesk.iam.repository.ImportTaskRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工 Excel 导入（异步执行）。
 * 导入列（首行为表头）：登录账号（必填）、姓名、手机号（必填）、邮箱、密码（可空，默认 123456）。
 */
@Service
public class StaffImportService {

    private static final Logger log = LoggerFactory.getLogger(StaffImportService.class);

    public static final String TASK_TYPE_STAFF_IMPORT = "staff_import";

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_ROW_COUNT = 5000;
    private static final int MAX_ERROR_ROWS = 100;
    private static final String DEFAULT_PASSWORD = "123456";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final ImportTaskRepository importTaskRepository;
    private final StaffAccountService staffAccountService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Path storageRoot;

    public StaffImportService(
            ImportTaskRepository importTaskRepository,
            StaffAccountService staffAccountService,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${uniondesk.import.storage-dir:./data/import}") String storageDir) {
        this.importTaskRepository = importTaskRepository;
        this.staffAccountService = staffAccountService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** 校验并保存上传文件，登记 pending 任务，返回任务 ID。 */
    public long createImportTask(MultipartFile file, long operatorId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "";
        String extension = fileExtension(fileName);
        if (!"xlsx".equals(extension) && !"xls".equals(extension)) {
            throw new IllegalArgumentException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        String fileKey = saveImportFile(file, fileName);
        ImportTaskPo po = new ImportTaskPo();
        po.setTaskType(TASK_TYPE_STAFF_IMPORT);
        po.setFileKey(fileKey);
        po.setFileName(fileName);
        po.setStatus(STATUS_PENDING);
        po.setCreatedBy(operatorId > 0 ? operatorId : null);
        importTaskRepository.insert(po);
        if (po.getId() == null) {
            throw new IllegalStateException("导入任务创建失败");
        }
        return po.getId();
    }

    /** 异步执行导入任务：解析 Excel 并逐行创建员工账号。 */
    @Async
    public void executeImport(long taskId) {
        ImportTaskPo task = importTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("导入任务不存在，taskId={}", taskId);
            return;
        }
        importTaskRepository.updateStatus(taskId, STATUS_PROCESSING, null);
        try {
            List<ImportRow> rows = parseExcel(resolveFile(task.getFileKey()));
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("文件中没有可导入的数据行");
            }
            int successCount = 0;
            int failCount = 0;
            List<ImportErrorRow> errors = new ArrayList<>();
            for (ImportRow row : rows) {
                try {
                    staffAccountService.create(new StaffAccountService.CreateStaffCommand(
                            row.username(),
                            row.realName(),
                            null,
                            row.phone(),
                            row.email(),
                            StringUtils.hasText(row.password()) ? row.password() : DEFAULT_PASSWORD,
                            List.of(),
                            List.of(),
                            List.of()));
                    successCount++;
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    failCount++;
                    if (errors.size() < MAX_ERROR_ROWS) {
                        errors.add(new ImportErrorRow(row.rowNumber(), safeMessage(ex)));
                    }
                }
            }
            LocalDateTime finishedAt = LocalDateTime.now(clock);
            String status = successCount > 0 ? STATUS_SUCCESS : STATUS_FAILED;
            importTaskRepository.updateResult(
                    taskId, status, rows.size(), successCount, failCount,
                    errors.isEmpty() ? null : toErrorSummaryJson(errors), finishedAt);
        } catch (Exception ex) {
            log.error("员工导入任务执行失败，taskId={}", taskId, ex);
            LocalDateTime finishedAt = LocalDateTime.now(clock);
            importTaskRepository.updateResult(
                    taskId, STATUS_FAILED, 0, 0, 0,
                    toErrorSummaryJson(List.of(new ImportErrorRow(0, "导入失败：" + safeMessage(ex)))), finishedAt);
        }
    }

    public ImportTaskView getTask(long taskId) {
        return importTaskRepository.findById(taskId).map(this::toView).orElse(null);
    }

    private ImportTaskView toView(ImportTaskPo po) {
        return new ImportTaskView(
                po.getId(),
                po.getTaskType(),
                po.getFileName(),
                po.getStatus(),
                po.getTotalCount() == null ? 0 : po.getTotalCount(),
                po.getSuccessCount() == null ? 0 : po.getSuccessCount(),
                po.getFailCount() == null ? 0 : po.getFailCount(),
                parseErrorSummary(po.getErrorSummary()),
                po.getCreatedAt() == null ? null : po.getCreatedAt().format(ISO_FORMATTER),
                po.getFinishedAt() == null ? null : po.getFinishedAt().format(ISO_FORMATTER));
    }

    private List<ImportErrorRow> parseErrorSummary(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ImportErrorRow>>() {
            });
        } catch (IOException ex) {
            return null;
        }
    }

    private String toErrorSummaryJson(List<ImportErrorRow> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (IOException ex) {
            return "[]";
        }
    }

    private List<ImportRow> parseExcel(Path filePath) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 文件中没有工作表");
            }
            Map<String, Integer> columnIndexes = resolveColumnIndexes(sheet);
            Integer usernameIndex = columnIndexes.get("username");
            Integer phoneIndex = columnIndexes.get("phone");
            if (usernameIndex == null || phoneIndex == null) {
                throw new IllegalArgumentException("模板表头不正确，需包含列：登录账号、手机号（可选：姓名、邮箱、密码）");
            }
            List<ImportRow> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                String username = cellAt(row, columnIndexes.get("username"));
                String phone = cellAt(row, columnIndexes.get("phone"));
                if (!StringUtils.hasText(username) && !StringUtils.hasText(phone)) {
                    continue;
                }
                rows.add(new ImportRow(
                        rowIndex + 1,
                        username,
                        cellAt(row, columnIndexes.get("realName")),
                        phone,
                        cellAt(row, columnIndexes.get("email")),
                        cellAt(row, columnIndexes.get("password"))));
            }
            if (rows.size() > MAX_ROW_COUNT) {
                throw new IllegalArgumentException("单次导入最多支持 " + MAX_ROW_COUNT + " 行数据");
            }
            return rows;
        }
    }

    private Map<String, Integer> resolveColumnIndexes(Sheet sheet) {
        Row header = sheet.getRow(0);
        Map<String, Integer> indexes = new HashMap<>();
        if (header == null) {
            return indexes;
        }
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String headerText = cellText(header.getCell(i)).toLowerCase(Locale.ROOT);
            String columnKey = switch (headerText) {
                case "登录账号", "loginname", "login_name" -> "username";
                case "姓名", "displayname", "realname" -> "realName";
                case "手机号", "phone", "mobile" -> "phone";
                case "邮箱", "email" -> "email";
                case "密码", "password" -> "password";
                default -> null;
            };
            if (columnKey != null && !indexes.containsKey(columnKey)) {
                indexes.put(columnKey, i);
            }
        }
        return indexes;
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (StringUtils.hasText(cellText(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (!Double.isInfinite(value) && value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private static String cellAt(Row row, Integer index) {
        if (index == null) {
            return "";
        }
        return cellText(row.getCell(index));
    }

    private String saveImportFile(MultipartFile file, String fileName) {
        try {
            String day = LocalDate.now(clock).toString();
            String safeName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileKey = TASK_TYPE_STAFF_IMPORT + "/" + day + "/" + UUID.randomUUID() + "-" + safeName;
            Path target = storageRoot.resolve(fileKey).normalize();
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return fileKey;
        } catch (IOException ex) {
            throw new IllegalStateException("导入文件保存失败", ex);
        }
    }

    private Path resolveFile(String fileKey) {
        return storageRoot.resolve(fileKey).normalize();
    }

    private static String fileExtension(String fileName) {
        if (!fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static String safeMessage(Exception ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    public record ImportRow(
            int rowNumber,
            String username,
            String realName,
            String phone,
            String email,
            String password) {
    }

    public record ImportErrorRow(int row, String message) {
    }

    public record ImportTaskView(
            long id,
            String taskType,
            String fileName,
            String status,
            int totalCount,
            int successCount,
            int failCount,
            List<ImportErrorRow> errorSummary,
            String createdAt,
            String finishedAt) {
    }
}
