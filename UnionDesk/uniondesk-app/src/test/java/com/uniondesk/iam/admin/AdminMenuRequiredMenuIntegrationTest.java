package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.iam.core.IamService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.uniondesk.support.IntegrationAuthSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminMenuRequiredMenuIntegrationTest {

    @Autowired
    private IamService iamService;

    @Autowired
    private AdminMenuService adminMenuService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        IntegrationAuthSupport.ensureActiveDefaultDomain(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void superAdminPermissionSnapshotIncludesPlatformHome() {
        Long adminUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_account WHERE username = 'admin' LIMIT 1",
                Long.class);

        assertThat(adminUserId).isNotNull();

        IamService.PermissionSnapshot snapshot = iamService.loadPermissionSnapshot(
                new UserContext(adminUserId, "super_admin", null, "sid-test", "ud-admin-web"));

        assertThat(snapshot.menuTree()).isNotEmpty();
        assertThat(extractTreeRoutes(objectMapper.valueToTree(snapshot.menuTree())))
                .contains("/platform/home");
    }

    @Test
    void replaceRolePermissionsAutoCarriesRequiredPlatformHome() {
        Integer roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM role WHERE code = 'super_admin' LIMIT 1",
                Integer.class);
        Long homeMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM iam_admin_menu WHERE route_path = '/platform/home' LIMIT 1",
                Long.class);


        Long homeRequiredButtonId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM iam_admin_menu
                        WHERE parent_id = ?
                          AND node_type = 'button'
                          AND required = 1
                        ORDER BY order_no, id
                        LIMIT 1
                        """,
                Long.class,
                homeMenuId);

        assertThat(roleId).isNotNull();
        assertThat(homeMenuId).isNotNull();
        assertThat(homeRequiredButtonId).isNotNull();

        AdminMenuService.RolePermissions permissions = adminMenuService.replaceRolePermissions(
                roleId,
                List.of(),
                List.of());

        assertThat(permissions.menuIds()).contains(homeMenuId);
        assertThat(permissions.buttonIds()).contains(homeRequiredButtonId);
    }

    @Test
    void updateMenuAllowsRequiredPlatformHome() {
        Map<String, Object> homeMenu = jdbcTemplate.queryForMap(
                """
                        SELECT
                            id,
                            scope,
                            route_path,
                            component_key,
                            permission_code,
                            parent_id,
                            order_no,
                            icon,
                            hidden,
                            status
                        FROM iam_admin_menu
                        WHERE route_path = '/platform/home'
                        LIMIT 1
                        """);
        Long homeMenuId = ((Number) homeMenu.get("id")).longValue();

        assertThat(homeMenuId).isNotNull();

        AdminMenuService.AdminMenuNode updated = adminMenuService.updateMenu(
                homeMenuId,
                new AdminMenuService.UpdateAdminMenuCommand(
                        null,
                        "New Home Title",
                        (String) homeMenu.get("route_path"),
                        (String) homeMenu.get("component_key"),
                        (String) homeMenu.get("permission_code"),
                        (String) homeMenu.get("scope"),
                        homeMenu.get("parent_id") == null ? null : ((Number) homeMenu.get("parent_id")).longValue(),
                        ((Number) homeMenu.get("order_no")).intValue(),
                        (String) homeMenu.get("icon"),
                        ((Number) homeMenu.get("hidden")).intValue() == 1,
                        ((Number) homeMenu.get("status")).intValue()));

        assertThat(updated.name()).isEqualTo("New Home Title");
        assertThat(updated.required()).isTrue();
    }

    @Test
    void platformMenuTreeMatchesCurrentUserPermissionSnapshot() throws Exception {
        String accessToken = loginAdminAndGetAccessToken();

        JsonNode snapshotResponse = objectMapper.readTree(mockMvc.perform(get("/api/v1/iam/me/permission-snapshot")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        JsonNode treeResponse = objectMapper.readTree(mockMvc.perform(get("/api/v1/iam/menus/tree")
                        .param("scope", "platform")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        Set<String> snapshotRoutes = extractTreeRoutes(snapshotResponse.path("menuTree"));
        Set<String> treeRoutes = extractTreeRoutes(treeResponse);

        assertThat(snapshotRoutes).contains("/platform/home");
        assertThat(treeRoutes).contains("/platform/home");
        assertThat(treeRoutes).isEqualTo(snapshotRoutes);
    }

    private String loginAdminAndGetAccessToken() throws Exception {
        String loginRequest = """
                {
                  "username": "admin",
                  "password": "admin123",
                  "captcha_token": "test-captcha-token"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResponse).path("accessToken").asText();
    }

    private Set<String> extractSnapshotRoutes(JsonNode menusNode) {
        Set<String> routes = new LinkedHashSet<>();
        if (menusNode != null && menusNode.isArray()) {
            for (JsonNode menuNode : menusNode) {
                String routePath = menuNode.path("path").asText(null);
                if (routePath != null && !routePath.isBlank()) {
                    routes.add(routePath);
                }
            }
        }
        return routes;
    }

    private Set<String> extractTreeRoutes(JsonNode treeNode) {
        Set<String> routes = new LinkedHashSet<>();
        collectTreeRoutes(treeNode, routes);
        return routes;
    }

    private void collectTreeRoutes(JsonNode node, Set<String> routes) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectTreeRoutes(child, routes);
            }
            return;
        }
        String routePath = node.path("routePath").asText(null);
        if (routePath == null || routePath.isBlank()) {
            routePath = node.path("path").asText(null);
        }
        if (routePath != null && !routePath.isBlank()) {
            routes.add(routePath);
        }
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                collectTreeRoutes(child, routes);
            }
        }
    }
}
