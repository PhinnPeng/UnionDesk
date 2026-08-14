package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.DomainCustomerPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DomainCustomerMapper extends BaseMapper<DomainCustomerPo> {

    default List<DomainCustomerPo> listCustomers(
            long domainId, String status, String keyword, int limit, long offset) {
        QueryWrapper qw = baseCustomerQuery(domainId, status, keyword)
                .orderBy(DomainCustomerPo::getId, false)
                .limit(offset, limit);
        return selectListByQuery(qw);
    }

    default long countCustomers(long domainId, String status, String keyword) {
        return selectCountByQuery(baseCustomerQuery(domainId, status, keyword));
    }

    default DomainCustomerPo selectById(long id, long domainId) {
        return selectOneByQuery(baseCustomerQuery(domainId, null, null)
                .and(DomainCustomerPo::getId).eq(id));
    }

    default int countByDomainAndCustomer(long domainId, long customerAccountId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(DomainCustomerPo.class)
                .where(DomainCustomerPo::getBusinessDomainId).eq(domainId)
                .and(DomainCustomerPo::getCustomerAccountId).eq(customerAccountId)
                .and("domain_customer.deleted_at IS NULL"));
    }

    default int countActiveByDomainAndCustomer(long domainId, long customerAccountId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(DomainCustomerPo.class)
                .where(DomainCustomerPo::getBusinessDomainId).eq(domainId)
                .and(DomainCustomerPo::getCustomerAccountId).eq(customerAccountId)
                .and(DomainCustomerPo::getStatus).eq("active")
                .and("domain_customer.deleted_at IS NULL"));
    }

    default int updateStatus(
            String status, LocalDateTime activatedAt, LocalDateTime disabledAt, long id, long domainId) {
        DomainCustomerPo set = new DomainCustomerPo();
        set.setStatus(status);
        set.setActivatedAt(activatedAt);
        set.setDisabledAt(disabledAt);
        return updateByQuery(set, QueryWrapper.create()
                .where(DomainCustomerPo::getId).eq(id)
                .and(DomainCustomerPo::getBusinessDomainId).eq(domainId)
                .and("domain_customer.deleted_at IS NULL"));
    }

    private QueryWrapper baseCustomerQuery(long domainId, String status, String keyword) {
        QueryWrapper qw = QueryWrapper.create()
                .select(
                        "domain_customer.id",
                        "domain_customer.business_domain_id",
                        "domain_customer.customer_account_id",
                        "ca.subject_id",
                        "ca.username",
                        "ca.nickname",
                        "ca.phone",
                        "ca.email",
                        "ca.real_name",
                        "ca.id_card_no",
                        "domain_customer.status",
                        "domain_customer.source",
                        "domain_customer.activated_at",
                        "domain_customer.disabled_at",
                        "domain_customer.created_at",
                        "domain_customer.updated_at")
                .from(DomainCustomerPo.class)
                .join("customer_account").as("ca").on("ca.id = domain_customer.customer_account_id")
                .join("identity_subject").as("s").on("s.id = ca.subject_id")
                .where(DomainCustomerPo::getBusinessDomainId).eq(domainId)
                .and(DomainCustomerPo::getStatus).eq(status, If::hasText)
                .and("domain_customer.deleted_at IS NULL");
        if (keyword != null) {
            qw.and("(ca.username LIKE ? OR ca.nickname LIKE ? OR ca.phone LIKE ? OR ca.email LIKE ?)",
                    keyword, keyword, keyword, keyword);
        }
        return qw;
    }
}
