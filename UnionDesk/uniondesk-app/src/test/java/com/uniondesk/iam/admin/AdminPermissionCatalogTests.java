package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.iam.core.PermissionCodes;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AdminPermissionCatalogTests {

    @Test
    void allPermissionCodesAreRegisteredOnce() throws IllegalAccessException {
        Set<String> expectedCodes = permissionCodes();
        Set<String> actualCodes = AdminPermissionCatalog.list().stream()
                .map(AdminPermissionCatalog.PermissionDefinition::code)
                .collect(Collectors.toSet());

        assertThat(AdminPermissionCatalog.list()).hasSize(expectedCodes.size());
        assertThat(actualCodes).containsExactlyInAnyOrderElementsOf(expectedCodes);
    }

    @Test
    void listPermissionCodesFiltersByScope() {
        AdminMenuService service = new AdminMenuService(null, org.mockito.Mockito.mock(com.uniondesk.common.event.UnionDeskEventPublisher.class), Clock.systemUTC());

        assertThat(service.listPermissionCodes("platform"))
                .allSatisfy(permission -> assertThat(permission.permissionScope()).isEqualTo("platform"));
        assertThat(service.listPermissionCodes("domain"))
                .allSatisfy(permission -> assertThat(permission.permissionScope()).isEqualTo("domain"));
        assertThat(service.listPermissionCodes("shared"))
                .extracting(AdminPermissionCatalog.PermissionDefinition::code)
                .containsExactlyInAnyOrder(
                        PermissionCodes.TICKET_READ,
                        PermissionCodes.TICKET_CREATE,
                        PermissionCodes.TICKET_VIEW_SELF,
                        PermissionCodes.TICKET_VIEW_DOMAIN_ALL,
                        PermissionCodes.TICKET_CLAIM,
                        PermissionCodes.TICKET_ASSIGN,
                        PermissionCodes.TICKET_REPLY_SELF,
                        PermissionCodes.TICKET_REPLY,
                        PermissionCodes.TICKET_CLOSE,
                        PermissionCodes.TICKET_WITHDRAW_SELF,
                        PermissionCodes.TICKET_MERGE,
                        PermissionCodes.ATTACHMENT_UPLOAD,
                        PermissionCodes.ATTACHMENT_DOWNLOAD,
                        PermissionCodes.INBOX_READ,
                        PermissionCodes.INBOX_MARK_READ,
                        PermissionCodes.CONSULTATION_REPLY);
        assertThat(service.listPermissionCodes(""))
                .hasSize(AdminPermissionCatalog.list().size());
        assertThat(service.listPermissionCodes(null))
                .hasSize(AdminPermissionCatalog.list().size());
    }

    @Test
    void catalogKeepsPermissionDefinitionsUnique() {
        Set<String> codes = AdminPermissionCatalog.list().stream()
                .map(AdminPermissionCatalog.PermissionDefinition::code)
                .collect(Collectors.toSet());

        assertThat(codes).hasSize(AdminPermissionCatalog.list().size());
    }

    @Test
    void userCreateRouteUsesDomainUserCreatePermission() {
        assertThat(AdminPermissionCatalog.findByRequest("POST", "/api/v1/iam/users"))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.code()).isEqualTo(PermissionCodes.DOMAIN_USER_CREATE);
                    assertThat(permission.name()).isEqualTo("Create domain user");
                    assertThat(permission.permissionScope()).isEqualTo("domain");
                });
    }

    @Test
    void platformUserCreatePermissionIsStillListedAsPlatformPermission() {
        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_USER_CREATE))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Create platform user");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                });
    }

    @Test
    void organizationAndPlaceholderPlatformPermissionsAreRegistered() {
        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_ORGANIZATION_READ))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("View organization structure");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isEqualTo("GET");
                    assertThat(permission.pathPattern()).isEqualTo("/api/v1/iam/organizations");
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_ORGANIZATION_CREATE))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Create organization");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isEqualTo("POST");
                    assertThat(permission.pathPattern()).isEqualTo("/api/v1/iam/organizations");
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_ORGANIZATION_UPDATE))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Update organization");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isEqualTo("PUT");
                    assertThat(permission.pathPattern()).isEqualTo("/api/v1/iam/organizations/*");
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_ORGANIZATION_DELETE))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Delete organization");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isEqualTo("DELETE");
                    assertThat(permission.pathPattern()).isEqualTo("/api/v1/iam/organizations/*");
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_USER_IMPORT))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Import platform users");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isNull();
                    assertThat(permission.pathPattern()).isNull();
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_USER_RESET_PASSWORD))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Reset platform user password");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isNull();
                    assertThat(permission.pathPattern()).isNull();
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_EXPORT))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Export offboard pool");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isNull();
                    assertThat(permission.pathPattern()).isNull();
                });

        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_BATCH_RESTORE))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.name()).isEqualTo("Batch restore offboard pool");
                    assertThat(permission.permissionScope()).isEqualTo("platform");
                    assertThat(permission.httpMethod()).isNull();
                    assertThat(permission.pathPattern()).isNull();
                });
    }

    @Test
    void domainMemberReadPermissionIsListed() {
        assertThat(AdminPermissionCatalog.findByCode(PermissionCodes.DOMAIN_MEMBER_READ))
                .hasValueSatisfying(permission -> {
                    assertThat(permission.httpMethod()).isEqualTo("GET");
                    assertThat(permission.pathPattern()).contains("/api/v1/admin/domains/*/members");
                });
    }

    private Set<String> permissionCodes() throws IllegalAccessException {
        return java.util.Arrays.stream(PermissionCodes.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType().equals(String.class))
                .map(this::readStringField)
                .collect(Collectors.toSet());
    }

    private String readStringField(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
