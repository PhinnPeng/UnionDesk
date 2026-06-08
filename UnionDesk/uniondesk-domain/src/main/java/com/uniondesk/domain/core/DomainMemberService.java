package com.uniondesk.domain.core;

import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.event.DomainMemberStatusChangedEvent;
import com.uniondesk.common.event.UnionDeskEventPublisher;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.entity.DomainMemberPo;
import com.uniondesk.domain.entity.MemberRolePo;
import com.uniondesk.domain.entity.StaffCandidatePo;
import com.uniondesk.domain.repository.DomainMemberRepository;
import com.uniondesk.domain.web.DomainMemberDtos;
import com.uniondesk.domain.web.DomainRoleDtos;
import com.uniondesk.iam.core.IdentityPresentationService;
import com.uniondesk.iam.core.StaffAccountService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainMemberService {

    private final DomainMemberRepository domainMemberRepository;
    private final DomainService domainService;
    private final IdentityPresentationService identityPresentationService;
    private final StaffAccountService staffAccountService;
    private final UnionDeskEventPublisher eventPublisher;

    public DomainMemberService(
            DomainMemberRepository domainMemberRepository,
            DomainService domainService,
            IdentityPresentationService identityPresentationService,
            StaffAccountService staffAccountService,
            UnionDeskEventPublisher eventPublisher) {
        this.domainMemberRepository = domainMemberRepository;
        this.domainService = domainService;
        this.identityPresentationService = identityPresentationService;
        this.staffAccountService = staffAccountService;
        this.eventPublisher = eventPublisher;
    }

    public PageResult<DomainMemberDtos.DomainMemberView> listMembers(
            long domainId,
            int page,
            int pageSize,
            String status,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {
        requireDomain(domainId);
        String resolvedStatus = normalizeMemberStatus(status);
        String keywordLike = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;

        long total = domainMemberRepository.countMembers(
                domainId, resolvedStatus, keywordLike, createdFrom, createdTo);
        List<DomainMemberPo> pos = domainMemberRepository.findMembers(
                domainId, resolvedStatus, keywordLike, createdFrom, createdTo, normalizedPageSize, offset);
        Map<Long, List<DomainRoleDtos.DomainRoleView>> rolesByMemberId = loadRolesByMemberIds(
                pos.stream().map(DomainMemberPo::getId).toList());
        return new PageResult<>(
                total,
                pos.stream()
                        .map(po -> toMemberView(po, rolesByMemberId.getOrDefault(po.getId(), List.of())))
                        .toList());
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView createMember(long domainId, DomainMemberDtos.CreateDomainMemberRequest request) {
        requireDomain(domainId);
        long staffAccountId = Objects.requireNonNull(request.staff_account_id(), "staff_account_id");
        requireStaffAccount(staffAccountId);
        if (memberExists(domainId, staffAccountId)) {
            throw new IllegalArgumentException("domain member already exists");
        }
        domainMemberRepository.insertMember(staffAccountId, domainId);
        long memberId = requireMemberId(domainId, staffAccountId);
        List<String> newRoleCodes = domainMemberRepository.findRoleCodesByIds(domainId, normalizeIds(request.role_ids()));
        guardSingleDomainOwner(domainId, memberId, newRoleCodes);
        replaceMemberRoles(domainId, memberId, request.role_ids());
        return getMember(domainId, memberId);
    }

    public PageResult<DomainMemberDtos.StaffCandidateView> listStaffCandidates(
            long domainId,
            int page,
            int pageSize,
            String keyword) {
        requireDomain(domainId);
        String keywordLike = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;

        long total = domainMemberRepository.countStaffCandidates(domainId, keywordLike);
        List<StaffCandidatePo> pos = domainMemberRepository.findStaffCandidates(
                domainId, keywordLike, normalizedPageSize, offset);
        return new PageResult<>(total, pos.stream().map(this::toStaffCandidateView).toList());
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView createMemberWithStaff(
            long domainId,
            DomainMemberDtos.CreateMemberWithStaffRequest request) {
        requireDomain(domainId);
        List<Long> roleIds = normalizeIds(request.role_ids());
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        List<String> roleCodes = domainMemberRepository.findRoleCodesByIds(domainId, roleIds);
        guardSingleDomainOwner(domainId, -1L, roleCodes);
        StaffAccountService.StaffAccount created = staffAccountService.create(new StaffAccountService.CreateStaffCommand(
                request.username(),
                request.real_name(),
                request.nickname(),
                request.phone(),
                request.email(),
                request.password(),
                roleCodes,
                List.of(domainId)));
        long memberId = requireMemberId(domainId, created.id());
        return getMember(domainId, memberId);
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView updateMemberStatus(
            long domainId,
            long memberId,
            DomainMemberDtos.UpdateDomainMemberStatusRequest request) {
        requireDomain(domainId);
        String status = normalizeMemberStatus(request.status());
        if (status == null) {
            throw new IllegalArgumentException("无效的成员状态");
        }
        DomainMemberPo member = loadMember(domainId, memberId);
        String previousStatus = member.getStatus();
        if ("disabled".equals(status)) {
            List<String> roleCodes = domainMemberRepository.findRoleCodesByMemberId(memberId);
            if (roleCodes.contains("domain_admin")) {
                guardLastDomainAdmin(domainId, memberId);
            }
            if (roleCodes.contains("super_admin")) {
                guardLastDomainSuperAdmin(domainId, memberId);
            }
        }
        int updated = domainMemberRepository.updateMemberStatus(status, memberId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("domain member not found");
        }
        long operatorUserId = UserContextHolder.current().map(context -> context.userId()).orElse(0L);
        eventPublisher.publish(new DomainMemberStatusChangedEvent(
                domainId,
                memberId,
                member.getStaffAccountId(),
                previousStatus,
                status,
                operatorUserId));
        return getMember(domainId, memberId);
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView updateMemberRoles(long domainId, long memberId, DomainMemberDtos.UpdateDomainMemberRolesRequest request) {
        loadMember(domainId, memberId);
        List<String> currentRoleCodes = domainMemberRepository.findRoleCodesByMemberId(memberId);
        List<Long> newRoleIds = normalizeIds(request.role_ids());
        List<String> newRoleCodes = domainMemberRepository.findRoleCodesByIds(domainId, newRoleIds);
        if (currentRoleCodes.contains("domain_admin") && !newRoleCodes.contains("domain_admin")) {
            guardLastDomainAdmin(domainId, memberId);
        }
        if (currentRoleCodes.contains("super_admin") && !newRoleCodes.contains("super_admin")) {
            guardLastDomainSuperAdmin(domainId, memberId);
        }
        guardSingleDomainOwner(domainId, memberId, newRoleCodes);
        replaceMemberRoles(domainId, memberId, newRoleIds);
        return getMember(domainId, memberId);
    }

    @Transactional
    public void deleteMember(long domainId, long memberId) {
        loadMember(domainId, memberId);
        List<String> currentRoleCodes = domainMemberRepository.findRoleCodesByMemberId(memberId);
        if (currentRoleCodes.contains("domain_admin")) {
            guardLastDomainAdmin(domainId, memberId);
        }
        if (currentRoleCodes.contains("super_admin")) {
            guardLastDomainSuperAdmin(domainId, memberId);
        }
        int updated = domainMemberRepository.softDeleteMember(memberId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("domain member not found");
        }
    }

    void guardLastDomainAdmin(long domainId, long memberId) {
        if (!domainMemberRepository.findRoleCodesByMemberId(memberId).contains("domain_admin")) {
            return;
        }
        if (domainMemberRepository.countActiveDomainAdmins(domainId, memberId) == 0) {
            throw new IllegalStateException("请先指定另一位业务域管理员");
        }
    }

    void guardLastDomainSuperAdmin(long domainId, long memberId) {
        if (!domainMemberRepository.findRoleCodesByMemberId(memberId).contains("super_admin")) {
            return;
        }
        if (domainMemberRepository.countActiveDomainSuperAdmins(domainId, memberId) == 0) {
            throw new IllegalStateException("请先指定另一位业务域所有人");
        }
    }

    void guardSingleDomainOwner(long domainId, long memberId, List<String> newRoleCodes) {
        if (!newRoleCodes.contains("super_admin")) {
            return;
        }
        if (domainMemberRepository.countActiveDomainSuperAdmins(domainId, memberId) > 0) {
            throw new IllegalStateException("该业务域已存在所有人，请先转让后再授权");
        }
    }

    private DomainMemberDtos.DomainMemberView getMember(long domainId, long memberId) {
        DomainMemberPo po = loadMember(domainId, memberId);
        List<DomainRoleDtos.DomainRoleView> roles = loadRolesByMemberIds(List.of(memberId))
                .getOrDefault(memberId, List.of());
        return toMemberView(po, roles);
    }

    private void replaceMemberRoles(long domainId, long memberId, List<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeIds(roleIds);
        if (!normalizedRoleIds.isEmpty()) {
            ensureRolesBelongToDomain(domainId, normalizedRoleIds);
        }
        domainMemberRepository.deleteMemberRoles(memberId);
        for (Long roleId : normalizedRoleIds) {
            domainMemberRepository.insertMemberRole(memberId, roleId);
        }
    }

    private void ensureRolesBelongToDomain(long domainId, List<Long> roleIds) {
        long count = domainMemberRepository.countRolesByIds(domainId, roleIds);
        if (count != roleIds.size()) {
            throw new IllegalArgumentException("domain role not found");
        }
    }

    private Map<Long, List<DomainRoleDtos.DomainRoleView>> loadRolesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<DomainRoleDtos.DomainRoleView>> grouped = new LinkedHashMap<>();
        for (MemberRolePo rolePo : domainMemberRepository.findRolesByMemberIds(memberIds)) {
            grouped.computeIfAbsent(rolePo.getDomainMemberId(), ignored -> new ArrayList<>())
                    .add(toRoleView(rolePo));
        }
        return grouped;
    }

    private DomainRoleDtos.DomainRoleView toRoleView(MemberRolePo po) {
        return new DomainRoleDtos.DomainRoleView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getCode(),
                po.getName(),
                po.getPreset() != null && po.getPreset() == 1);
    }

    private DomainMemberDtos.StaffCandidateView toStaffCandidateView(StaffCandidatePo po) {
        return new DomainMemberDtos.StaffCandidateView(
                po.getId(),
                po.getUsername(),
                po.getRealName(),
                po.getNickname(),
                po.getPhone(),
                po.getEmail(),
                po.getStatus());
    }

    private DomainMemberDtos.DomainMemberView toMemberView(DomainMemberPo po, List<DomainRoleDtos.DomainRoleView> roles) {
        IdentityPresentationService.ResolvedStaffDomainView presentation =
                identityPresentationService.resolveStaffInDomain(po.getStaffAccountId(), po.getBusinessDomainId());
        return new DomainMemberDtos.DomainMemberView(
                po.getId(),
                po.getStaffAccountId(),
                po.getBusinessDomainId(),
                po.getUsername(),
                presentation.realName(),
                presentation.nickname(),
                po.getPhone(),
                po.getEmail(),
                po.getStatus(),
                po.getSource(),
                po.getActivatedAt(),
                po.getDisabledAt(),
                po.getDeletedAt(),
                po.getCreatedAt(),
                roles);
    }

    private DomainMemberPo loadMember(long domainId, long memberId) {
        DomainMemberPo po = domainMemberRepository.findMemberById(memberId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("domain member not found");
        }
        return po;
    }

    private boolean memberExists(long domainId, long staffAccountId) {
        return domainMemberRepository.countByDomainAndStaff(domainId, staffAccountId) > 0;
    }

    private long requireMemberId(long domainId, long staffAccountId) {
        Long memberId = domainMemberRepository.findMemberIdByDomainAndStaff(domainId, staffAccountId);
        if (memberId == null) {
            throw new IllegalStateException("domain member create failed");
        }
        return memberId;
    }

    private void requireStaffAccount(long staffAccountId) {
        if (domainMemberRepository.countStaffAccountById(staffAccountId) == 0) {
            throw new IllegalArgumentException("staff account not found");
        }
    }

    private void requireDomain(long domainId) {
        domainService.getDomain(domainId);
    }

    private String normalizeMemberStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "disabled".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                unique.add(value);
            }
        }
        return List.copyOf(unique);
    }
}
