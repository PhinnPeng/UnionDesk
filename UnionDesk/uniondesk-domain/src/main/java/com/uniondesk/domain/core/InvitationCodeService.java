package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.entity.InvitationCodePo;
import com.uniondesk.domain.repository.InvitationCodeRepository;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.domain.web.InvitationCodeDtos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InvitationCodeService {

    private final InvitationCodeRepository invitationCodeRepository;
    private final DomainService domainService;

    public InvitationCodeService(InvitationCodeRepository invitationCodeRepository, DomainService domainService) {
        this.invitationCodeRepository = invitationCodeRepository;
        this.domainService = domainService;
    }

    public PageResult<InvitationCodeDtos.InvitationCodeView> listInvitationCodes(long domainId, int page, int pageSize) {
        loadDomain(domainId);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;
        long total = invitationCodeRepository.countByDomainId(domainId);
        List<InvitationCodePo> pos = invitationCodeRepository.findByDomainId(domainId, normalizedPageSize, offset);
        return new PageResult<>(total, pos.stream().map(this::toInvitationCodeView).toList());
    }

    @Transactional
    public InvitationCodeDtos.InvitationCodeView createInvitationCode(
            long domainId,
            InvitationCodeDtos.CreateInvitationCodeRequest request) {
        DomainDtos.DomainView domain = loadDomain(domainId);
        if (!DomainAccessPolicy.isAllowed(domain.invitation_enabled())) {
            throw DomainErrorCodes.INVITATION_DISALLOWED.toException();
        }
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long id = invitationCodeRepository.insert(
                domainId,
                code,
                normalizeText(request.channel()),
                request.expiresAt(),
                request.maxUses());
        return loadInvitationCodeById(domainId, id);
    }

    @Transactional
    public void deleteInvitationCode(long domainId, long codeId) {
        loadDomain(domainId);
        int updated = invitationCodeRepository.deactivate(codeId, domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("invitation code not found");
        }
    }

    @Transactional
    public InvitationCodeDtos.InvitationCodeView validateAndUse(long domainId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("invitation code is required");
        }
        InvitationCodePo invitationCode = loadActiveInvitationCode(domainId, code.trim());
        LocalDateTime now = LocalDateTime.now();
        if (invitationCode.getExpiresAt() != null && !invitationCode.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("invitation code expired");
        }
        if (invitationCode.getMaxUses() != null
                && invitationCode.getUsedCount() >= invitationCode.getMaxUses()) {
            throw new IllegalArgumentException("invitation code used up");
        }
        invitationCodeRepository.incrementUsedCount(invitationCode.getId());
        return loadInvitationCodeById(domainId, invitationCode.getId());
    }

    private InvitationCodeDtos.InvitationCodeView loadInvitationCodeById(long domainId, long codeId) {
        InvitationCodePo po = invitationCodeRepository.findByIdAndDomain(codeId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("invitation code not found");
        }
        return toInvitationCodeView(po);
    }

    private InvitationCodePo loadActiveInvitationCode(long domainId, String code) {
        InvitationCodePo po = invitationCodeRepository.findActiveByDomainAndCode(domainId, code);
        if (po == null) {
            throw new IllegalArgumentException("invitation code not found");
        }
        return po;
    }

    private InvitationCodeDtos.InvitationCodeView toInvitationCodeView(InvitationCodePo po) {
        return new InvitationCodeDtos.InvitationCodeView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getCode(),
                po.getChannel(),
                po.getExpiresAt(),
                po.getMaxUses(),
                po.getUsedCount(),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private DomainDtos.DomainView loadDomain(long domainId) {
        return domainService.getDomain(domainId);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
