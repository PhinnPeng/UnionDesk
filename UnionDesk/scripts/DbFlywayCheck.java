import java.math.BigInteger;
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

/** 联调库 Flyway 最终基线与 current/ 脚本一致性检查。 */
public class DbFlywayCheck {
    private static final String URL = "jdbc:mysql://127.0.0.1:30306/uniondesk?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
    private static final String USER = "uniondesk_app";
    private static final String FINAL_BASELINE_VERSION = "20260816150000";
    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d{12,14})__");

    public static void main(String[] args) throws Exception {
        String password = System.getenv("UNIONDESK_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("请设置环境变量 UNIONDESK_DB_PASSWORD");
        }
        Path currentDir = Path.of("uniondesk-app/src/main/resources/db/migration/current");
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
                if (maxVersion.isEmpty() || new BigInteger(v).compareTo(new BigInteger(maxVersion)) > 0) {
                    maxVersion = v;
                    maxSuccess = ok;
                }
            }
        }

        System.out.println("=== Flyway history (installed_rank asc) ===");
        historyVersions.forEach(System.out::println);
        System.out.println("MAX_VERSION=" + maxVersion);
        System.out.println("MAX_SUCCESS=" + maxSuccess);
        boolean meetsRequired = !maxVersion.isEmpty()
                && new BigInteger(maxVersion).compareTo(new BigInteger(FINAL_BASELINE_VERSION)) >= 0
                && maxSuccess;
        System.out.println("REQUIRED_BASELINE=" + FINAL_BASELINE_VERSION);
        System.out.println("MEETS_REQUIRED=" + meetsRequired);

        System.out.println("=== current/ file versions ===");
        fileVersions.forEach(v -> System.out.println(v));

        List<String> missing = new ArrayList<>();
        if (!fileVersions.equals(List.of(FINAL_BASELINE_VERSION))) {
            missing.add("current must contain only " + FINAL_BASELINE_VERSION);
        }
        List<String> legacyHistory = new ArrayList<>();
        for (String hv : historyVersions) {
            String bare = hv.split(" ")[0];
            if (!FINAL_BASELINE_VERSION.equals(bare) && bare.matches("\\d{12,14}")) {
                legacyHistory.add(bare);
            }
        }
        System.out.println("MISSING_IN_HISTORY=" + (missing.isEmpty() ? "none" : String.join(",", missing)));
        System.out.println("LEGACY_HISTORY=" + (legacyHistory.isEmpty() ? "none" : String.join(",", legacyHistory)));
    }
}
