package com.uniondesk.common.repository;

import com.uniondesk.common.mapper.IdentityResolutionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class IdentityResolutionRepository {

    private final IdentityResolutionMapper mapper;

    public IdentityResolutionRepository(IdentityResolutionMapper mapper) {
        this.mapper = mapper;
    }

    public long ensureIdentitySubject(long userId) {
        Long subjectId = mapper.findCustomerSubjectId(userId);
        if (subjectId != null) {
            return subjectId;
        }
        subjectId = mapper.findStaffSubjectId(userId);
        if (subjectId != null) {
            return subjectId;
        }
        subjectId = mapper.findIdentitySubjectId(userId);
        if (subjectId != null) {
            return subjectId;
        }
        String phone = mapper.findUserAccountPhone(userId, "user-" + userId);
        if (!StringUtils.hasText(phone)) {
            phone = "user-" + userId;
        }
        try {
            mapper.insertIdentitySubject(userId, phone);
            return userId;
        } catch (DuplicateKeyException ignored) {
            Long existingSubjectId = mapper.findIdentitySubjectIdByPhone(phone);
            if (existingSubjectId != null) {
                return existingSubjectId;
            }
            throw ignored;
        }
    }
}
