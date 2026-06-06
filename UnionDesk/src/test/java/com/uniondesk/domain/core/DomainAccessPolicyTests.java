package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomainAccessPolicyTests {

    @Test
    void normalizeDefaultsToAllowed() {
        assertEquals(DomainAccessPolicy.ALLOWED, DomainAccessPolicy.normalize(null));
        assertEquals(DomainAccessPolicy.ALLOWED, DomainAccessPolicy.normalize(""));
        assertEquals(DomainAccessPolicy.ALLOWED, DomainAccessPolicy.normalize("allowed"));
    }

    @Test
    void normalizeDisallowedCaseInsensitive() {
        assertEquals(DomainAccessPolicy.DISALLOWED, DomainAccessPolicy.normalize("disallowed"));
        assertEquals(DomainAccessPolicy.DISALLOWED, DomainAccessPolicy.normalize(" Disallowed "));
    }

    @Test
    void isAllowedReflectsNormalizedValue() {
        assertTrue(DomainAccessPolicy.isAllowed("allowed"));
        assertFalse(DomainAccessPolicy.isAllowed("disallowed"));
    }
}
