package com.uniondesk.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditActionCatalogTests {

    @Test
    void label_mapsPlatformAndLegacyCodes() {
        assertThat(AuditActionCatalog.label(AuditActionCodes.PLATFORM_DOMAIN_UPDATE)).isEqualTo("业务域更新");
        assertThat(AuditActionCatalog.label(AuditActionCodes.LEGACY_DOMAIN_UPDATE)).isEqualTo("业务域更新");
    }

    @Test
    void resolveActionLabel_usesDynamicStatusLabel() {
        String detail = AuditDetailTextBuilder.buildDomainStatusDetail("演示域", "demo", 0, 1);

        assertThat(AuditActionCatalog.resolveActionLabel(AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS, detail))
                .isEqualTo("业务域启用");
    }
}
