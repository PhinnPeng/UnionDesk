package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.DomainCustomerPo;
import com.uniondesk.domain.entity.StaffAccountPo;
import com.uniondesk.domain.mapper.DomainCustomerMapper;
import com.uniondesk.domain.mapper.StaffAccountMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DomainCustomerRepository {

    private final DomainCustomerMapper domainCustomerMapper;
    private final StaffAccountMapper staffAccountMapper;

    public DomainCustomerRepository(
            DomainCustomerMapper domainCustomerMapper,
            StaffAccountMapper staffAccountMapper) {
        this.domainCustomerMapper = domainCustomerMapper;
        this.staffAccountMapper = staffAccountMapper;
    }

    public List<DomainCustomerPo> findCustomers(
            long domainId, String status, String keyword, int limit, long offset) {
        return domainCustomerMapper.listCustomers(domainId, status, keyword, limit, offset);
    }

    public long countCustomers(long domainId, String status, String keyword) {
        return domainCustomerMapper.countCustomers(domainId, status, keyword);
    }

    public DomainCustomerPo findById(long id, long domainId) {
        return domainCustomerMapper.selectById(id, domainId);
    }

    public long insert(long customerAccountId, long businessDomainId, String source, LocalDateTime activatedAt) {
        DomainCustomerPo po = new DomainCustomerPo();
        po.setCustomerAccountId(customerAccountId);
        po.setBusinessDomainId(businessDomainId);
        po.setSource(source);
        po.setActivatedAt(activatedAt);
        domainCustomerMapper.insert(po);
        Long id = po.getId();
        if (id == null) {
            throw new IllegalStateException("域客户创建失败");
        }
        return id;
    }

    public int countByDomainAndCustomer(long domainId, long customerAccountId) {
        return domainCustomerMapper.countByDomainAndCustomer(domainId, customerAccountId);
    }

    public int updateStatus(
            String status, LocalDateTime activatedAt, LocalDateTime disabledAt, long id, long domainId) {
        return domainCustomerMapper.updateStatus(status, activatedAt, disabledAt, id, domainId);
    }

    public StaffAccountPo findStaffInDomain(long domainId, long staffAccountId) {
        return staffAccountMapper.selectStaffInDomain(domainId, staffAccountId);
    }
}
