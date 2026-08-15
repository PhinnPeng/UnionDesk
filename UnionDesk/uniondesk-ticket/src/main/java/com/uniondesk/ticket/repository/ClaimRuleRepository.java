package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.ClaimRulePo;
import com.uniondesk.ticket.entity.ClaimRulePolicyPo;
import com.uniondesk.ticket.mapper.ClaimRuleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRuleRepository {

    private static final int MAX_PAGE_SIZE = 100;

    private final ClaimRuleMapper claimRuleMapper;

    public ClaimRuleRepository(ClaimRuleMapper claimRuleMapper) {
        this.claimRuleMapper = claimRuleMapper;
    }

    public List<ClaimRulePo> findRulesByDomainId(long domainId, int page, int pageSize) {
        int normalizedPageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        long offset = (long) (Math.max(page, 1) - 1) * normalizedPageSize;
        return claimRuleMapper.selectByDomainId(domainId, normalizedPageSize, offset);
    }

    public long countRulesByDomainId(long domainId) {
        return claimRuleMapper.countByDomainId(domainId);
    }

    public ClaimRulePo findRuleByIdAndDomainId(long ruleId, long domainId) {
        return claimRuleMapper.selectByIdAndDomainId(ruleId, domainId);
    }

    public ClaimRulePo findRequiredRuleByIdAndDomainId(long ruleId, long domainId) {
        ClaimRulePo po = claimRuleMapper.selectByIdAndDomainId(ruleId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("领取规则不存在");
        }
        return po;
    }

    public void saveRule(ClaimRulePo po) {
        claimRuleMapper.insert(po);
    }

    public void updateRule(ClaimRulePo po) {
        claimRuleMapper.updateByIdAndDomainId(po);
    }

    public int deleteRuleByIdAndDomainId(long ruleId, long domainId) {
        return claimRuleMapper.deleteByIdAndDomainId(ruleId, domainId);
    }

    public ClaimRulePolicyPo findPolicy(long domainId, long ticketTypeId, String priorityCode) {
        return claimRuleMapper.selectPolicy(domainId, ticketTypeId, priorityCode);
    }

    public Long findLeastLoadedAssignee(long domainId) {
        return claimRuleMapper.selectLeastLoadedAssignee(domainId);
    }

    public boolean isCandidate(long domainId, long staffAccountId) {
        return claimRuleMapper.countCandidateByStaffId(domainId, staffAccountId) > 0;
    }
}
