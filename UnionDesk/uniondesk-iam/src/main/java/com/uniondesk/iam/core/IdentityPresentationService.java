package com.uniondesk.iam.core;

import com.uniondesk.iam.entity.DomainMemberPresentationPo;
import com.uniondesk.iam.entity.StaffAccountPresentationPo;
import com.uniondesk.iam.repository.IdentityPresentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentityPresentationService {

    private final IdentityPresentationRepository identityPresentationRepository;

    public IdentityPresentationService(IdentityPresentationRepository identityPresentationRepository) {
        this.identityPresentationRepository = identityPresentationRepository;
    }

    public ResolvedStaffDomainView resolveStaffInDomain(long staffAccountId, long domainId) {
        StaffAccountPresentationPo staff = identityPresentationRepository.findStaffPresentation(staffAccountId).orElse(null);
        if (staff == null) {
            throw new IllegalArgumentException("员工账号不存在");
        }
        DomainMemberPresentationPo member = identityPresentationRepository
                .findDomainMemberPresentation(staffAccountId, domainId)
                .orElse(null);
        String nickname = firstNonBlank(
                member == null ? null : member.getDomainNickname(),
                staff.getNickname(),
                staff.getRealName(),
                staff.getUsername());
        return new ResolvedStaffDomainView(
                staff.getRealName(),
                nickname,
                firstNonBlank(member == null ? null : member.getDomainAvatarUrl(), staff.getAvatarUrl()),
                firstNonBlank(member == null ? null : member.getDomainContactPhone(), staff.getPhone()),
                firstNonBlank(member == null ? null : member.getDomainContactEmail(), staff.getEmail()));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public record ResolvedStaffDomainView(
            String realName,
            String nickname,
            String avatarUrl,
            String contactPhone,
            String contactEmail) {
    }
}
