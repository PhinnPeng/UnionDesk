package com.uniondesk.iam.core;

import com.uniondesk.iam.entity.IdentitySubjectPo;
import com.uniondesk.iam.repository.IdentitySubjectRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentitySubjectService {

    private static final int MAX_MERGE_DEPTH = 16;

    private final IdentitySubjectRepository identitySubjectRepository;

    public IdentitySubjectService(IdentitySubjectRepository identitySubjectRepository) {
        this.identitySubjectRepository = identitySubjectRepository;
    }

    public long resolveSubjectIdByPhone(String phone) {
        String normalized = requirePhone(phone);
        Long existing = identitySubjectRepository.findIdByPhone(normalized).orElse(null);
        if (existing != null) {
            return resolveEffectiveSubjectId(existing);
        }
        IdentitySubjectPo po = new IdentitySubjectPo();
        po.setSubjectType("person");
        po.setPhone(normalized);
        po.setStatus("active");
        identitySubjectRepository.insert(po);
        if (po.getId() == null) {
            throw new IllegalStateException("身份主体创建失败");
        }
        return po.getId();
    }

    public long resolveEffectiveSubjectId(long subjectId) {
        long current = subjectId;
        Set<Long> visited = new HashSet<>();
        for (int depth = 0; depth < MAX_MERGE_DEPTH; depth++) {
            if (!visited.add(current)) {
                throw new IllegalStateException("无效的主体合并链");
            }
            Long mergedInto = identitySubjectRepository.findMergedIntoId(current).orElse(null);
            if (mergedInto == null) {
                return current;
            }
            current = mergedInto;
        }
        throw new IllegalStateException("无效的主体合并链");
    }

    public void requireActiveSubject(long subjectId) {
        long effectiveId = resolveEffectiveSubjectId(subjectId);
        IdentitySubjectPo row = identitySubjectRepository.findById(effectiveId).orElse(null);
        if (row == null) {
            throw new IllegalArgumentException("身份主体不可用");
        }
        if (!"active".equalsIgnoreCase(row.getStatus())) {
            throw new IllegalArgumentException("身份主体不可用");
        }
        if (row.getMergedIntoId() != null) {
            throw new IllegalArgumentException("身份主体不可用");
        }
    }

    private String requirePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        return phone.trim();
    }
}
