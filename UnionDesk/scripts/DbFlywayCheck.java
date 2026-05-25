import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** 联调库 Flyway 版本与 current/ 脚本一致性检查（US-S0-07） */
public class DbFlywayCheck {
    private static final String URL = "jdbc:mysql://127.0.0.1:30306/uniondesk?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "uniondesk_app";
    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d{12})__");

    public static void main(String[] args) throws Exception {
        String password = System.getenv("UNIONDESK_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("请设置环境变量 UNIONDESK_DB_PASSWORD");
        }
        Path currentDir = Path.of("src/main/resources/db/migration/current");
        List<String> fileVersions = new ArrayList<>();
        if (Files.isDirectory(currentDir)) {
            try (Stream<Path> paths = Files.list(currentDir)) {
                paths.filter(p -> p.getFileName().toString().endsWith(".sql"))
                        .sorted()
                        .forEach(p -> {
                            Matcher m = VERSION_PATTERN.matcher(p.getFileName().toString());
                            if (m.find()) {
                                fileVersions.add(m.group(1));
                            }
                        });
            }
        }

        Class.forName("com.mysql.cj.jdbc.Driver");
        List<String> historyVersions = new ArrayList<>();
        String maxVersion = "";
        boolean maxSuccess = false;
        try (Connection conn = DriverManager.getConnection(URL, USER, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank")) {
            while (rs.next()) {
                String v = rs.getString(1);
                boolean ok = rs.getBoolean(2);
                historyVersions.add(v + (ok ? "" : " (failed)"));
                if (v.compareTo(maxVersion) > 0) {
                    maxVersion = v;
                    maxSuccess = ok;
                }
            }
        }

        System.out.println("=== Flyway history (installed_rank asc) ===");
        historyVersions.forEach(System.out::println);
        System.out.println("MAX_VERSION=" + maxVersion);
        System.out.println("MAX_SUCCESS=" + maxSuccess);
        System.out.println("REQUIRED_GE=202605250001");
        System.out.println("MEETS_REQUIRED=" + (maxVersion.compareTo("202605250001") >= 0 && maxSuccess));

        System.out.println("=== current/ file versions ===");
        fileVersions.forEach(v -> System.out.println(v));

        List<String> missing = new ArrayList<>();
        for (String fv : fileVersions) {
            if (!historyVersions.stream().anyMatch(h -> h.startsWith(fv))) {
                missing.add(fv);
            }
        }
        List<String> orphan = new ArrayList<>();
        for (String hv : historyVersions) {
            String bare = hv.split(" ")[0];
            if (!fileVersions.contains(bare) && bare.startsWith("202605")) {
                orphan.add(bare);
            }
        }
        System.out.println("MISSING_IN_HISTORY=" + (missing.isEmpty() ? "none" : String.join(",", missing)));
        System.out.println("ORPHAN_IN_HISTORY=" + (orphan.isEmpty() ? "none" : String.join(",", orphan)));
    }
}
