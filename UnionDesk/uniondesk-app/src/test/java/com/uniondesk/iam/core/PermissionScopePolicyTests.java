package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionScopePolicyTests {

    private final PermissionScopePolicy policy = new PermissionScopePolicy();

    @Test
    void globalRoleCanOwnPlatformAndDomainPermissions() {
        assertThat(policy.canRoleOwnPermission("global", "platform")).isTrue();
        assertThat(policy.canRoleOwnPermission("global", "domain")).isTrue();
    }

    @Test
    void domainRoleCannotOwnPlatformPermission() {
        assertThat(policy.canRoleOwnPermission("domain", "platform")).isFalse();
        assertThat(policy.canRoleOwnPermission("domain", "domain")).isTrue();
    }

    @Test
    void globalBindingMakesDomainPermissionEffectiveForAnyDomain() {
        assertThat(policy.isPermissionEffective("global", "global", null, "domain", 11L)).isTrue();
        assertThat(policy.isPermissionEffective("global", "global", null, "domain", 22L)).isTrue();
    }

    @Test
    void domainBindingLimitsDomainPermissionToBoundDomain() {
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", 11L)).isTrue();
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", 22L)).isFalse();
    }

    @Test
    void domainBindingCanPassRouteLevelCheckBeforeObjectDomainIsKnown() {
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", null)).isTrue();
    }
}
