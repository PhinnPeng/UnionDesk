import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DbBackup {
    private static final String URL = "jdbc:mysql://127.0.0.1:30306/uniondesk?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "uniondesk_app";
    private static final Set<String> EXCLUDE_DATA = Set.of();
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        String password = System.getenv("UNIONDESK_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("请设置环境变量 UNIONDESK_DB_PASSWORD");
        }
        Path out = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("backups/uniondesk_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Files.createDirectories(out.getParent());
        try (Connection conn = DriverManager.getConnection(URL, USER, password)) {
            List<String> tables = loadTables(conn);
            List<String> order = topoSort(conn, tables);
            writeBackup(conn, order, out);
            System.out.println("备份完成: " + out.toAbsolutePath());
        }
    }

    private static List<String> loadTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private static List<String> topoSort(Connection conn, List<String> tables) throws SQLException {
        Set<String> tableSet = new HashSet<>(tables);
        Map<String, Set<String>> children = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String table : tables) {
            children.put(table, new TreeSet<>());
            indegree.put(table, 0);
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT TABLE_NAME, REFERENCED_TABLE_NAME
                     FROM information_schema.KEY_COLUMN_USAGE
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND REFERENCED_TABLE_NAME IS NOT NULL
                     """)) {
            while (rs.next()) {
                String child = rs.getString(1);
                String parent = rs.getString(2);
                if (child == null || parent == null || child.equals(parent)) {
                    continue;
                }
                if (!tableSet.contains(child) || !tableSet.contains(parent)) {
                    continue;
                }
                if (children.get(parent).add(child)) {
                    indegree.put(child, indegree.get(child) + 1);
                }
            }
        }

        PriorityQueue<String> queue = new PriorityQueue<>();
        for (String table : tables) {
            if (indegree.get(table) == 0) {
                queue.add(table);
            }
        }
        List<String> ordered = new ArrayList<>(tables.size());
        while (!queue.isEmpty()) {
            String table = queue.poll();
            ordered.add(table);
            for (String child : children.getOrDefault(table, Set.of())) {
                int next = indegree.get(child) - 1;
                indegree.put(child, next);
                if (next == 0) {
                    queue.add(child);
                }
            }
        }

        if (ordered.size() != tables.size()) {
            Set<String> remaining = new TreeSet<>(tables);
            remaining.removeAll(new HashSet<>(ordered));
            ordered.addAll(remaining);
        }
        return ordered;
    }

    private static void writeBackup(Connection conn, List<String> orderedTables, Path out) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer.write("-- UnionDesk 联调库全库备份（US-S0-07）");
            writer.newLine();
            writer.write("-- Source database: uniondesk @ 127.0.0.1:30306");
            writer.newLine();
            writer.write("-- Generated at: " + java.time.LocalDateTime.now());
            writer.newLine();
            writer.write("SET NAMES utf8mb4;");
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS=0;");
            writer.newLine();
            writer.newLine();

            for (String table : orderedTables) {
                writer.write("DROP TABLE IF EXISTS `" + table + "`;");
                writer.newLine();
                String ddl = showCreateTable(conn, table);
                writer.write(ddl);
                writer.write(";");
                writer.newLine();
                writer.newLine();
            }

            for (String table : orderedTables) {
                if (EXCLUDE_DATA.contains(table)) {
                    continue;
                }
                long rowCount = countRows(conn, table);
                if (rowCount == 0) {
                    continue;
                }
                List<String> pkColumns = primaryKeys(conn, table);
                writeSeedData(conn, writer, table, pkColumns);
            }
            writer.write("SET FOREIGN_KEY_CHECKS=1;");
            writer.newLine();
        }
    }

    private static String showCreateTable(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (!rs.next()) {
                throw new SQLException("No SHOW CREATE TABLE result for " + table);
            }
            return rs.getString(2);
        }
    }

    private static long countRows(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
            if (!rs.next()) {
                return 0L;
            }
            return rs.getLong(1);
        }
    }

    private static List<String> primaryKeys(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        Map<Short, String> ordered = new TreeMap<>();
        try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                short seq = rs.getShort("KEY_SEQ");
                ordered.put(seq, column);
            }
        }
        return new ArrayList<>(ordered.values());
    }

    private static void writeSeedData(Connection conn, BufferedWriter writer, String table, List<String> pkColumns) throws Exception {
        String orderBy = pkColumns.isEmpty()
                ? ""
                : " ORDER BY " + pkColumns.stream().map(DbBackup::quoteIdentifier).collect(Collectors.joining(", "));
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`" + orderBy)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Set<String> generatedColumns = generatedColumns(conn, table);
            List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String column = meta.getColumnLabel(i);
                if (!generatedColumns.contains(column)) {
                    columns.add(column);
                }
            }
            List<String> selfReferenceColumns = selfReferenceColumns(conn, table);
            List<String> updatableColumns = new ArrayList<>();
            Set<String> pkSet = new HashSet<>(pkColumns);
            for (String column : columns) {
                if (!pkSet.contains(column)) {
                    updatableColumns.add(column);
                }
            }
            if (columns.isEmpty()) {
                System.out.println("Skipping seed data for " + table + " because it has no non-generated columns");
                return;
            }
            if (updatableColumns.isEmpty()) {
                updatableColumns.add(columns.get(0));
            }

            List<Map<String, Object>> rows = loadRows(rs, columns);
            if (!selfReferenceColumns.isEmpty()) {
                rows = orderSelfReferentialRows(rows, pkColumns, selfReferenceColumns);
            }

            List<String> rowValues = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                rowValues.add(renderRow(row, columns));
            }
            if (rowValues.isEmpty()) {
                return;
            }

            writer.write("-- Seed data for `" + table + "`");
            writer.newLine();
            writer.write("INSERT INTO `" + table + "` (" + columns.stream().map(DbBackup::quoteIdentifier).collect(Collectors.joining(", ")) + ")");
            writer.newLine();
            writer.write("VALUES");
            writer.newLine();
            writer.write(String.join(",\n", rowValues));
            writer.newLine();
            writer.write(";");
            writer.newLine();
            writer.newLine();
        }
    }

    private static List<Map<String, Object>> loadRows(ResultSet rs, List<String> columns) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String column : columns) {
                row.put(column, rs.getObject(column));
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> selfReferenceColumns(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<String> columns = new ArrayList<>();
        try (ResultSet rs = meta.getImportedKeys(null, null, table)) {
            while (rs.next()) {
                String fkTable = rs.getString("FKTABLE_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                if (table.equals(fkTable) && table.equals(pkTable)) {
                    columns.add(rs.getString("FKCOLUMN_NAME"));
                }
            }
        }
        return columns;
    }

    private static Set<String> generatedColumns(Connection conn, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_NAME, EXTRA FROM information_schema.columns " +
                             "WHERE table_schema = DATABASE() " +
                             "AND table_name = '" + table + "'")) {
            while (rs.next()) {
                String extra = rs.getString("EXTRA");
                if (extra == null) {
                    continue;
                }
                String upper = extra.toUpperCase(Locale.ROOT);
                if (upper.contains("STORED GENERATED") || upper.contains("VIRTUAL GENERATED")) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return columns;
    }

    private static List<Map<String, Object>> orderSelfReferentialRows(
            List<Map<String, Object>> rows,
            List<String> pkColumns,
            List<String> selfReferenceColumns
    ) {
        if (rows.size() <= 1) {
            return rows;
        }
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byKey.put(rowKey(row, pkColumns), row);
        }
        Map<String, Set<String>> children = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String key : byKey.keySet()) {
            children.put(key, new TreeSet<>());
            indegree.put(key, 0);
        }
        for (Map<String, Object> row : rows) {
            String childKey = rowKey(row, pkColumns);
            for (String selfReferenceColumn : selfReferenceColumns) {
                Object parentValue = row.get(selfReferenceColumn);
                if (parentValue == null) {
                    continue;
                }
                String parentKey = keyOf(parentValue);
                if (parentKey.equals(childKey) || !byKey.containsKey(parentKey)) {
                    continue;
                }
                if (children.get(parentKey).add(childKey)) {
                    indegree.put(childKey, indegree.get(childKey) + 1);
                }
            }
        }

        PriorityQueue<String> queue = new PriorityQueue<>();
        for (String key : byKey.keySet()) {
            if (indegree.get(key) == 0) {
                queue.add(key);
            }
        }
        List<String> orderedKeys = new ArrayList<>(rows.size());
        while (!queue.isEmpty()) {
            String key = queue.poll();
            orderedKeys.add(key);
            for (String child : children.getOrDefault(key, Set.of())) {
                int next = indegree.get(child) - 1;
                indegree.put(child, next);
                if (next == 0) {
                    queue.add(child);
                }
            }
        }
        if (orderedKeys.size() != rows.size()) {
            Set<String> remaining = new TreeSet<>(byKey.keySet());
            remaining.removeAll(new HashSet<>(orderedKeys));
            orderedKeys.addAll(remaining);
        }
        List<Map<String, Object>> orderedRows = new ArrayList<>(rows.size());
        for (String key : orderedKeys) {
            orderedRows.add(byKey.get(key));
        }
        return orderedRows;
    }

    private static String rowKey(Map<String, Object> row, List<String> pkColumns) {
        if (pkColumns.isEmpty()) {
            return Integer.toString(System.identityHashCode(row));
        }
        List<String> parts = new ArrayList<>(pkColumns.size());
        for (String pkColumn : pkColumns) {
            parts.add(keyOf(row.get(pkColumn)));
        }
        return String.join("\u001f", parts);
    }

    private static String keyOf(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DATETIME_FMT);
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (value instanceof Time t) {
            return t.toLocalTime().toString();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(DATETIME_FMT);
        }
        if (value instanceof LocalDate ld) {
            return ld.toString();
        }
        if (value instanceof LocalTime lt) {
            return lt.toString();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime().format(DATETIME_FMT);
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.toLocalDateTime().format(DATETIME_FMT);
        }
        return String.valueOf(value);
    }

    private static String renderRow(Map<String, Object> row, List<String> columns) throws Exception {
        List<String> values = new ArrayList<>(columns.size());
        for (String column : columns) {
            values.add(renderValue(row.get(column)));
        }
        return "(" + String.join(", ", values) + ")";
    }

    private static String renderValue(Object value) throws Exception {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double || value instanceof BigDecimal || value instanceof BigInteger) {
            return value.toString();
        }
        if (value instanceof Timestamp ts) {
            return quote(ts.toLocalDateTime().format(DATETIME_FMT));
        }
        if (value instanceof java.sql.Date d) {
            return quote(d.toLocalDate().toString());
        }
        if (value instanceof Time t) {
            return quote(t.toLocalTime().toString());
        }
        if (value instanceof LocalDateTime ldt) {
            return quote(ldt.format(DATETIME_FMT));
        }
        if (value instanceof LocalDate ld) {
            return quote(ld.toString());
        }
        if (value instanceof LocalTime lt) {
            return quote(lt.toString());
        }
        if (value instanceof OffsetDateTime odt) {
            return quote(odt.toLocalDateTime().format(DATETIME_FMT));
        }
        if (value instanceof ZonedDateTime zdt) {
            return quote(zdt.toLocalDateTime().format(DATETIME_FMT));
        }
        if (value instanceof byte[] bytes) {
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        }
        if (value instanceof java.sql.Blob blob) {
            try {
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                StringBuilder hex = new StringBuilder("0x");
                for (byte b : bytes) {
                    hex.append(String.format(Locale.ROOT, "%02x", b));
                }
                return hex.toString();
            } finally {
                try {
                    blob.free();
                } catch (Exception ignored) {
                }
            }
        }
        if (value instanceof java.sql.Clob clob) {
            try {
                return quote(clob.getSubString(1, (int) clob.length()));
            } finally {
                try {
                    clob.free();
                } catch (Exception ignored) {
                }
            }
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("'", "''");
        return "'" + escaped + "'";
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
