package com.uniondesk.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditTargetFormatterTests {

    @Test
    void formatDomain_joinsNameAndCode() {
        assertThat(AuditTargetFormatter.formatDomain("演示域", "demo")).isEqualTo("演示域-demo");
    }

    @Test
    void formatRole_joinsNameAndCode() {
        assertThat(AuditTargetFormatter.formatRole("平台管理员", "platform_admin")).isEqualTo("平台管理员-platform_admin");
    }

    @Test
    void formatMember_fallsBackToMemberId() {
        assertThat(AuditTargetFormatter.formatMember(null, null, 42L)).isEqualTo("成员-42");
    }

    @Test
    void formatMember_prefersDisplayAndLogin() {
        assertThat(AuditTargetFormatter.formatMember("张三", "zhangsan", 1L)).isEqualTo("张三-zhangsan");
    }
}
