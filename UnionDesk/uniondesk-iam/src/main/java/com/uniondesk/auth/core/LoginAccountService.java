package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.LoginAccountPo;
import com.uniondesk.auth.repository.LoginAccountRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAccountService {

    private final LoginAccountRepository loginAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginAccountService(LoginAccountRepository loginAccountRepository, PasswordEncoder passwordEncoder) {
        this.loginAccountRepository = loginAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<LoginAccount> findByIdentifier(String identifier, LoginIdentifierType type) {
        return findByIdentifier(identifier, type, "staff");
    }

    public Optional<LoginAccount> findByIdentifier(String identifier, LoginIdentifierType type, String accountType) {
        String normalized = normalizeAccountType(accountType);
        String column = identifierColumn(type);
        Optional<LoginAccountPo> po = "customer".equals(normalized)
                ? loginAccountRepository.findCustomerByIdentifier(column, identifier.trim())
                : loginAccountRepository.findStaffByIdentifier(column, identifier.trim());
        return po.map(row -> toLoginAccount(row, normalized));
    }

    public Optional<LoginAccount> findById(long userId) {
        return findById(userId, "staff");
    }

    public Optional<LoginAccount> findById(long userId, String accountType) {
        String normalized = normalizeAccountType(accountType);
        Optional<LoginAccountPo> po = "customer".equals(normalized)
                ? loginAccountRepository.findCustomerById(userId)
                : loginAccountRepository.findStaffById(userId);
        return po.map(row -> toLoginAccount(row, normalized));
    }

    public List<String> loadRoleCodes(long userId) {
        return loadRoleCodes(userId, "staff");
    }

    public List<String> loadRoleCodes(long userId, String accountType) {
        if ("customer".equalsIgnoreCase(normalizeAccountType(accountType))) {
            return List.of("customer");
        }
        List<String> roles = new ArrayList<>(loginAccountRepository.findStaffDomainRoleCodes(userId));
        roles.addAll(loginAccountRepository.findStaffPlatformRoleCodes(userId));
        roles.sort(Comparator.comparingInt(LoginAccountService::rolePriority));
        return List.copyOf(roles);
    }

    public List<Long> loadAccessibleDomainIds(long userId, List<String> roleCodes) {
        return loadAccessibleDomainIds(userId, "staff", roleCodes);
    }

    public List<Long> loadAccessibleDomainIds(long userId, String accountType, List<String> roleCodes) {
        if ("customer".equalsIgnoreCase(normalizeAccountType(accountType))) {
            return loginAccountRepository.findCustomerAccessibleDomainIds(userId);
        }
        if (roleCodes != null && (roleCodes.contains("super_admin") || roleCodes.contains("platform_admin"))) {
            return loginAccountRepository.findAllDomainIds();
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return loginAccountRepository.findStaffAccessibleDomainIds(userId);
    }

    public long resolveDefaultDomainId() {
        return loginAccountRepository.findAllDomainIds().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no active business domain configured"));
    }

    public void updatePassword(String accountType, long accountId, String rawPassword) {
        String passwordHash = passwordEncoder.encode(rawPassword);
        if ("customer".equalsIgnoreCase(normalizeAccountType(accountType))) {
            loginAccountRepository.updateCustomerPassword(accountId, passwordHash);
            return;
        }
        loginAccountRepository.updateStaffPassword(accountId, passwordHash);
    }

    private String identifierColumn(LoginIdentifierType type) {
        return switch (type) {
            case USERNAME -> "username";
            case EMAIL -> "email";
            case MOBILE -> "phone";
        };
    }

    private LoginAccount toLoginAccount(LoginAccountPo po, String accountType) {
        return new LoginAccount(
                po.getId(),
                po.getUsername(),
                po.getMobile(),
                po.getEmail(),
                po.getPasswordHash(),
                po.getStatus(),
                accountType,
                po.getEmploymentStatus());
    }

    private String normalizeAccountType(String accountType) {
        if (!StringUtils.hasText(accountType)) {
            return "staff";
        }
        String normalized = accountType.trim().toLowerCase();
        return switch (normalized) {
            case "admin", "staff" -> "staff";
            case "customer" -> "customer";
            default -> normalized;
        };
    }

    private static int rolePriority(String role) {
        return switch (role) {
            case "super_admin" -> 0;
            case "platform_admin" -> 1;
            case "domain_admin" -> 2;
            case "agent" -> 3;
            case "customer" -> 4;
            default -> 10;
        };
    }

    public record LoginAccount(
            long id,
            String username,
            String mobile,
            String email,
            String passwordHash,
            int status,
            String accountType,
            String employmentStatus) {
    }
}
