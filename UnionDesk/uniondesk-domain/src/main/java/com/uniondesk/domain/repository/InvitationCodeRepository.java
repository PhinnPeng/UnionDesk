package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.InvitationCodePo;
import com.uniondesk.domain.mapper.InvitationCodeMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InvitationCodeRepository {

    private final InvitationCodeMapper invitationCodeMapper;

    public InvitationCodeRepository(InvitationCodeMapper invitationCodeMapper) {
        this.invitationCodeMapper = invitationCodeMapper;
    }

    public List<InvitationCodePo> findByDomainId(long domainId, int limit, long offset) {
        return invitationCodeMapper.selectByDomainId(domainId, limit, offset);
    }

    public long countByDomainId(long domainId) {
        return invitationCodeMapper.countByDomainId(domainId);
    }

    public InvitationCodePo findByIdAndDomain(long id, long domainId) {
        return invitationCodeMapper.selectByIdAndDomain(id, domainId);
    }

    public InvitationCodePo findActiveByDomainAndCode(long domainId, String code) {
        return invitationCodeMapper.selectActiveByDomainAndCode(domainId, code);
    }

    public long insert(long businessDomainId, String code, String channel, LocalDateTime expiresAt, Integer maxUses) {
        InvitationCodePo po = new InvitationCodePo();
        po.setBusinessDomainId(businessDomainId);
        po.setCode(code);
        po.setChannel(channel);
        po.setExpiresAt(expiresAt);
        po.setMaxUses(maxUses);
        invitationCodeMapper.insert(po);
        Long id = po.getId();
        if (id == null) {
            throw new IllegalStateException("invitation code create failed");
        }
        return id;
    }

    public int deactivate(long id, long domainId) {
        return invitationCodeMapper.deactivate(id, domainId);
    }

    public int incrementUsedCount(long id) {
        return invitationCodeMapper.incrementUsedCount(id);
    }
}
