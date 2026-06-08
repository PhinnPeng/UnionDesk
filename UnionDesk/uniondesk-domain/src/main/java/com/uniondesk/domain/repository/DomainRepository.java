package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.AuditLogPo;
import com.uniondesk.domain.entity.BusinessDomainPo;
import com.uniondesk.domain.mapper.AuditLogMapper;
import com.uniondesk.domain.mapper.BusinessDomainMapper;
import com.uniondesk.domain.mapper.IdentitySubjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class DomainRepository {

    private final BusinessDomainMapper businessDomainMapper;
    private final AuditLogMapper auditLogMapper;
    private final IdentitySubjectMapper identitySubjectMapper;

    public DomainRepository(
            BusinessDomainMapper businessDomainMapper,
            AuditLogMapper auditLogMapper,
            IdentitySubjectMapper identitySubjectMapper) {
        this.businessDomainMapper = businessDomainMapper;
        this.auditLogMapper = auditLogMapper;
        this.identitySubjectMapper = identitySubjectMapper;
    }

    public List<BusinessDomainPo> findAdminDomains(
            Integer status, boolean includeDeleted, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo, int limit, long offset) {
        return businessDomainMapper.selectAdminDomains(status, includeDeleted, keyword, createdFrom, createdTo, limit, offset);
    }

    public long countAdminDomains(
            Integer status, boolean includeDeleted, String keyword,
            LocalDateTime createdFrom, LocalDateTime createdTo) {
        return businessDomainMapper.countAdminDomains(status, includeDeleted, keyword, createdFrom, createdTo);
    }

    public List<BusinessDomainPo> findBriefDomains(String keyword, int limit, long offset) {
        return businessDomainMapper.selectBriefDomains(keyword, limit, offset);
    }

    public long countBriefDomains(String keyword) {
        return businessDomainMapper.countBriefDomains(keyword);
    }

    public BusinessDomainPo findById(long id) {
        return businessDomainMapper.selectById(id);
    }

    public Long findIdByCode(String code) {
        return businessDomainMapper.selectIdByCode(code);
    }

    public void insert(BusinessDomainPo po) {
        businessDomainMapper.insert(po);
    }

    public int updateDomain(
            String code, String name, String description, String logo,
            String visibilityPolicy, String visibilityPolicyCodes,
            String registrationEnabled, String invitationEnabled,
            int status, Long updatedBy, long id) {
        return businessDomainMapper.updateDomain(code, name, description, logo,
                visibilityPolicy, visibilityPolicyCodes, registrationEnabled, invitationEnabled,
                status, updatedBy, id);
    }

    public int softDelete(long id, Long updatedBy) {
        return businessDomainMapper.softDelete(id, updatedBy);
    }

    public void insertAuditLog(AuditLogPo po) {
        auditLogMapper.insert(po);
    }

    public long ensureIdentitySubject(long userId) {
        Long existing = identitySubjectMapper.selectIdById(userId);
        if (existing != null) {
            return existing;
        }
        String phone = identitySubjectMapper.selectPhoneByUserId(userId, "user-" + userId);
        if (!StringUtils.hasText(phone)) {
            phone = "user-" + userId;
        }
        try {
            identitySubjectMapper.insertPerson(userId, phone);
        } catch (DuplicateKeyException ignored) {
            Long subjectId = identitySubjectMapper.selectIdByPhone(phone);
            if (subjectId != null) {
                return subjectId;
            }
        }
        return userId;
    }
}
