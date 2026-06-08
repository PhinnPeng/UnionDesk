package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionCodesTests {

    @Test
    void ticketPermissionsExposeDataScopeSuffixes() {
        assertThat(PermissionCodes.TICKET_VIEW_SELF).isEqualTo("ticket.view.self");
        assertThat(PermissionCodes.TICKET_VIEW_DOMAIN_ALL).isEqualTo("ticket.view.domain_all");
        assertThat(PermissionCodes.TICKET_REPLY_SELF).isEqualTo("ticket.reply.self");
        assertThat(PermissionCodes.TICKET_WITHDRAW_SELF).isEqualTo("ticket.withdraw.self");
    }

    @Test
    void organizationAndPlatformUserActionPermissionsExposeExpectedCodes() {
        assertThat(PermissionCodes.PLATFORM_ORGANIZATION_READ).isEqualTo("platform.organization.read");
        assertThat(PermissionCodes.PLATFORM_ORGANIZATION_CREATE).isEqualTo("platform.organization.create");
        assertThat(PermissionCodes.PLATFORM_ORGANIZATION_UPDATE).isEqualTo("platform.organization.update");
        assertThat(PermissionCodes.PLATFORM_ORGANIZATION_DELETE).isEqualTo("platform.organization.delete");
        assertThat(PermissionCodes.PLATFORM_USER_IMPORT).isEqualTo("platform.user.import");
        assertThat(PermissionCodes.PLATFORM_USER_RESET_PASSWORD).isEqualTo("platform.user.reset_password");
        assertThat(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_EXPORT).isEqualTo("platform.user.offboard_pool.export");
        assertThat(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_BATCH_RESTORE).isEqualTo("platform.user.offboard_pool.batch_restore");
    }
}
