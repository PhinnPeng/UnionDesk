package com.uniondesk.auth.core;

public enum LoginIdentifierType {
    USERNAME,
    EMAIL,
    MOBILE;

    public static LoginIdentifierType detect(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("login identifier is required");
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return EMAIL;
        }
        if (trimmed.matches("^\\d{6,20}$")) {
            return MOBILE;
        }
        return USERNAME;
    }
}
