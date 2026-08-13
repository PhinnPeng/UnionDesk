package com.uniondesk.iam.core;

import com.uniondesk.iam.entity.StaffAccountPo;
import com.uniondesk.iam.repository.StaffAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffAccountService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StaffAccountRepository staffAccountRepository;
    private final IdentitySubjectService identitySubjectService;
    private final PlatformRoleService platformRoleService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public StaffAccountService(
            StaffAccountRepository staffAccountRepository,
            IdentitySubjectService identitySubjectService,
            PlatformRoleService platformRoleService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.staffAccountRepository = staffAccountRepository;
        this.identitySubjectService = identitySubjectService;
        this.platformRoleService = platformRoleService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public List<StaffAccount> listAll() {
        return staffAccountRepository.findAll().stream().map(this::toStaffAccount).toList();
    }

    public Optional<StaffAccount> findById(long staffAccountId) {
        return staffAccountRepository.findById(staffAccountId).map(this::toStaffAccount);
    }

    @Transactional
    public StaffAccount create(CreateStaffCommand command) {
        String username = requireText(command.username(), "登录账号不能为空");
        String phone = requireText(command.phone(), "手机号不能为空");
        String password = requireText(command.password(), "密码不能为空");
        String realName = trimToNull(command.realName());
        String nickname = trimToNull(command.nickname());
        String email = trimToNull(command.email());
        long subjectId = identitySubjectService.resolveSubjectIdByPhone(phone);
        identitySubjectService.requireActiveSubject(subjectId);
        StaffAccountPo po = new StaffAccountPo();
        po.setSubjectId(subjectId);
        po.setUsername(username);
        po.setRealName(realName);
        po.setNickname(nickname);
        po.setPhone(phone);
        po.setEmail(email);
        po.setPasswordHash(passwordEncoder.encode(password));
        try {
            staffAccountRepository.insert(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("登录账号或手机号已存在");
        }
        if (po.getId() == null) {
            throw new IllegalStateException("员工账号创建失败");
        }
        bindDomainMemberships(po.getId(), command.businessDomainIds(), command.roleCodes());
        bindPlatformRoles(po.getId(), command.roleCodes());
        if (command.organizationIds() != null) {
            staffAccountRepository.replaceOrganizations(po.getId(), command.organizationIds());
        }
        return findById(po.getId()).orElseThrow(() -> new IllegalStateException("员工账号创建失败"));
    }

    @Transactional
    public StaffAccount update(long staffAccountId, UpdateStaffCommand command) {
        findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
        try {
            staffAccountRepository.updateSelective(
                    staffAccountId,
                    command.username() != null ? requireText(command.username(), "登录账号不能为空") : null,
                    command.realName() != null ? trimToNull(command.realName()) : null,
                    command.nickname() != null ? trimToNull(command.nickname()) : null,
                    command.phone() != null ? requireText(command.phone(), "手机号不能为空") : null,
                    command.email() != null ? trimToNull(command.email()) : null,
                    command.password() != null ? passwordEncoder.encode(requireText(command.password(), "密码不能为空")) : null,
                    command.status() != null ? mapStatus(command.status()) : null);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("登录账号或手机号已存在");
        }
        if (command.roleCodes() != null || command.businessDomainIds() != null) {
            List<String> roleCodes = command.roleCodes() != null ? command.roleCodes() : listDomainRoleCodes(staffAccountId);
            List<Long> domainIds = command.businessDomainIds() != null ? command.businessDomainIds() : listBusinessDomainIds(staffAccountId);
            bindDomainMemberships(staffAccountId, domainIds, roleCodes);
            if (command.roleCodes() != null) {
                bindPlatformRoles(staffAccountId, command.roleCodes());
            }
        }
        if (command.organizationIds() != null) {
            staffAccountRepository.replaceOrganizations(staffAccountId, command.organizationIds());
        }
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    @Transactional
    public StaffAccount disable(long staffAccountId) {
        StaffAccount existing = findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
        if ("offboarded".equalsIgnoreCase(existing.employmentStatus())) {
            throw new IllegalArgumentException("已离职员工请使用恢复后再停用");
        }
        staffAccountRepository.updateStatus(staffAccountId, "disabled");
        staffAccountRepository.revokeActiveSessions(staffAccountId, "staff_disabled");
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    @Transactional
    public StaffAccount offboard(long staffAccountId, long operatorStaffId, String reason) {
        StaffAccount existing = findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
        if ("offboarded".equalsIgnoreCase(existing.employmentStatus())) {
            return existing;
        }
        platformRoleService.validateStaffStatusChange(staffAccountId, "offboarded");
        LocalDateTime now = LocalDateTime.now(clock);
        staffAccountRepository.offboard(
                staffAccountId,
                now,
                operatorStaffId > 0 ? operatorStaffId : null,
                StringUtils.hasText(reason) ? reason.trim() : null);
        staffAccountRepository.revokeActiveSessions(staffAccountId, "staff_offboarded");
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    @Transactional
    public StaffAccount restore(long staffAccountId) {
        StaffAccount existing = findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
        if ("offboarded".equalsIgnoreCase(existing.employmentStatus())) {
            staffAccountRepository.restoreEmployment(staffAccountId);
        } else {
            staffAccountRepository.updateStatus(staffAccountId, "active");
        }
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    public List<Long> listBusinessDomainIds(long staffAccountId) {
        return staffAccountRepository.findBusinessDomainIds(staffAccountId);
    }

    public List<String> listDomainRoleCodes(long staffAccountId) {
        return staffAccountRepository.findDomainRoleCodes(staffAccountId);
    }

    public List<Long> listOrganizationIds(long staffAccountId) {
        return staffAccountRepository.findOrganizationIds(staffAccountId);
    }

    private void bindPlatformRoles(long staffAccountId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        List<String> platformCodes = roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(code -> platformRoleService.getPlatformRoleIdByCode(code) != null)
                .distinct()
                .toList();
        if (!platformCodes.isEmpty()) {
            platformRoleService.assignPlatformRoles(staffAccountId, platformCodes);
        }
    }

    /**
     * 绑定员工在指定业务域的角色（角色模板 bind-members 复用入口）。
     * 注意：对每个业务域会先清空该员工在该域的现有角色再写入 roleCodes。
     */
    public void bindDomainMemberships(long staffAccountId, List<Long> businessDomainIds, List<String> roleCodes) {
        if (businessDomainIds == null || businessDomainIds.isEmpty()) {
            return;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        Set<Long> domainIds = new LinkedHashSet<>(businessDomainIds.stream().filter(Objects::nonNull).toList());
        Set<String> codes = new LinkedHashSet<>(roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(code -> platformRoleService.getPlatformRoleIdByCode(code) == null)
                .toList());
        if (codes.isEmpty()) {
            return;
        }
        for (Long domainId : domainIds) {
            ensureDomainExists(domainId);
            long memberId = ensureDomainMember(domainId, staffAccountId);
            staffAccountRepository.deleteDomainMemberRoles(memberId);
            for (String code : codes) {
                Long domainRoleId = staffAccountRepository.findDomainRoleId(domainId, code)
                        .orElseThrow(() -> new IllegalArgumentException("业务域角色不存在：" + code));
                staffAccountRepository.insertDomainMemberRole(memberId, domainRoleId);
            }
        }
    }

    private long ensureDomainMember(long domainId, long staffAccountId) {
        Optional<Long> memberId = staffAccountRepository.findDomainMemberId(domainId, staffAccountId);
        if (memberId.isPresent()) {
            return memberId.get();
        }
        staffAccountRepository.insertDomainMember(staffAccountId, domainId);
        return staffAccountRepository.findDomainMemberId(domainId, staffAccountId)
                .orElseThrow(() -> new IllegalStateException("域成员创建失败"));
    }

    private void ensureDomainExists(long domainId) {
        if (staffAccountRepository.countActiveDomain(domainId) == 0) {
            throw new IllegalArgumentException("业务域不存在");
        }
    }

    private StaffAccount toStaffAccount(StaffAccountPo po) {
        String employmentStatus = StringUtils.hasText(po.getEmploymentStatus())
                ? po.getEmploymentStatus()
                : "active";
        return new StaffAccount(
                po.getId(),
                po.getSubjectId(),
                po.getUsername(),
                po.getRealName(),
                po.getNickname(),
                po.getAvatarUrl(),
                po.getPhone(),
                po.getEmail(),
                po.getStatus(),
                employmentStatus,
                po.getOffboardedAt() == null ? null : po.getOffboardedAt().format(ISO_FORMATTER),
                po.getOffboardedBy(),
                po.getOffboardReason(),
                po.getSource(),
                po.getAuthVersion() == null ? 0 : po.getAuthVersion());
    }

    private static String mapStatus(Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("状态无效");
        }
        return status == 0 ? "disabled" : "active";
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record StaffAccount(
            long id,
            long subjectId,
            String username,
            String realName,
            String nickname,
            String avatarUrl,
            String phone,
            String email,
            String status,
            String employmentStatus,
            String offboardedAt,
            Long offboardedBy,
            String offboardReason,
            String source,
            int authVersion) {
    }

    public record CreateStaffCommand(
            String username,
            String realName,
            String nickname,
            String phone,
            String email,
            String password,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds) {
    }

    public record UpdateStaffCommand(
            String username,
            String realName,
            String nickname,
            String phone,
            String email,
            String password,
            Integer status,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds) {
    }
}
