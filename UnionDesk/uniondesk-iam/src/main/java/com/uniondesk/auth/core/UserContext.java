package com.uniondesk.auth.core;

public record UserContext(long userId, String role, Long businessDomainId, String sessionId, String clientCode) {
}
