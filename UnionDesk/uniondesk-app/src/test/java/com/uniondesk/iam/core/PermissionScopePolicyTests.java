package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionScopePolicyTests {

    private final PermissionScopePolicy policy = new PermissionScopePolicy();

    @Test
    void globalRoleCanOwnPlatformPrefixPermission() {
        assertThat(policy.canRoleOwnPermission("global", "platform")).isTrue();
        assertThat(policy.canRoleOwnPermission("global", "platform", "platform.menu.read")).isTrue();
    }

    @Test
    void globalRoleCannotOwnDomainScopedOrNonPlatformPrefixPermission() {
        assertThat(policy.canRoleOwnPermission("global", "domain")).isFalse();
        assertThat(policy.canRoleOwnPermission("global", "domain", "domain.user.read")).isFalse();
        assertThat(policy.canRoleOwnPermission("global", "platform", "domain.admin.read")).isFalse();
        assertThat(policy.canRoleOwnPermission("global", "shared")).isFalse();
        assertThat(policy.canRoleOwnPermission("global", "shared", "ticket.create")).isFalse();
    }

    @Test
    void domainRoleCanOwnSharedScopePermission() {
        assertThat(policy.canRoleOwnPermission("domain", "shared")).isTrue();
        assertThat(policy.canRoleOwnPermission("domain", "shared", "ticket.create")).isTrue();
        assertThat(policy.canRoleOwnPermission("domain", "shared", "ticket.view.self")).isTrue();
        assertThat(policy.canRoleOwnPermission("domain", "shared", "platform.menu.read")).isFalse();
    }

    @Test
    void domainRoleCannotOwnPlatformPermission() {
        assertThat(policy.canRoleOwnPermission("domain", "platform")).isFalse();
        assertThat(policy.canRoleOwnPermission("domain", "platform", "platform.menu.read")).isFalse();
        assertThat(policy.canRoleOwnPermission("domain", "domain")).isTrue();
        assertThat(policy.canRoleOwnPermission("domain", "domain", "domain.menu.read")).isTrue();
        assertThat(policy.canRoleOwnPermission("domain", "domain", "ticket.create")).isTrue();
    }

    @Test
    void globalBindingDoesNotMakeDomainPermissionEffective() {
        assertThat(policy.isPermissionEffective("global", "global", null, "domain", 11L)).isFalse();
        assertThat(policy.isPermissionEffective("global", "global", null, "shared", 11L)).isFalse();
    }

    @Test
    void domainBindingLimitsDomainPermissionToBoundDomain() {
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", 11L)).isTrue();
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", 22L)).isFalse();
    }

    @Test
    void domainBindingMakesSharedPermissionEffectiveWithinBoundDomain() {
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "shared", 11L)).isTrue();
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "shared", 22L)).isFalse();
        assertThat(policy.isPermissionEffective("domain", "domain", null, "shared", 11L)).isFalse();
    }

    @Test
    void domainBindingCanPassRouteLevelCheckBeforeObjectDomainIsKnown() {
        assertThat(policy.isPermissionEffective("domain", "domain", 11L, "domain", null)).isTrue();
    }
}
