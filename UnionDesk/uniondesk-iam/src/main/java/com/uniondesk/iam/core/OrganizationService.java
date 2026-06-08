package com.uniondesk.iam.core;

import com.uniondesk.iam.entity.OrganizationPo;
import com.uniondesk.iam.repository.OrganizationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<OrganizationUnit> listOrganizations() {
        return organizationRepository.findAll().stream().map(this::toOrganizationUnit).toList();
    }

    @Transactional
    public OrganizationUnit createOrganization(CreateOrganizationCommand command) {
        String code = normalizeRequiredText(command.code(), "组织编码");
        String name = normalizeRequiredText(command.name(), "组织名称");
        Long parentId = command.parentId();
        Long leaderUserId = command.leaderUserId();
        int orderNo = command.orderNo() == null ? 0 : command.orderNo();
        int status = normalizeStatus(command.status());
        String remark = normalizeOptionalText(command.remark());

        ensureParentExists(parentId);
        ensureLeaderExists(leaderUserId);

        OrganizationPo po = new OrganizationPo();
        po.setCode(code);
        po.setName(name);
        po.setParentId(parentId);
        po.setLeaderUserId(leaderUserId);
        po.setOrderNo(orderNo);
        po.setStatus(status);
        po.setRemark(remark);
        try {
            organizationRepository.insert(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("组织编码已存在");
        }

        return findOrganizationByCode(code).orElseThrow(() -> new IllegalStateException("组织创建失败"));
    }

    @Transactional
    public OrganizationUnit updateOrganization(long id, UpdateOrganizationCommand command) {
        OrganizationUnit existing = getOrganization(id);
        String code = StringUtils.hasText(command.code()) ? normalizeRequiredText(command.code(), "组织编码") : existing.code();
        String name = StringUtils.hasText(command.name()) ? normalizeRequiredText(command.name(), "组织名称") : existing.name();
        Long parentId = command.parentId();
        Long leaderUserId = command.leaderUserId() == null ? existing.leaderUserId() : command.leaderUserId();
        int orderNo = command.orderNo() == null ? existing.orderNo() : command.orderNo();
        int status = command.status() == null ? existing.status() : normalizeStatus(command.status());
        String remark = command.remark() == null ? existing.remark() : normalizeOptionalText(command.remark());

        if (parentId != null) {
            if (parentId == id) {
                throw new IllegalArgumentException("上级部门不能是自己");
            }
            if (isDescendant(id, parentId)) {
                throw new IllegalArgumentException("上级部门不能是自己的下级部门");
            }
            ensureParentExists(parentId);
        }
        ensureLeaderExists(leaderUserId);

        OrganizationPo po = new OrganizationPo();
        po.setId(id);
        po.setCode(code);
        po.setName(name);
        po.setParentId(parentId);
        po.setLeaderUserId(leaderUserId);
        po.setOrderNo(orderNo);
        po.setStatus(status);
        po.setRemark(remark);
        try {
            organizationRepository.update(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("组织编码已存在");
        }

        return getOrganization(id);
    }

    private boolean isDescendant(long parentId, long childId) {
        Long currentParentId = organizationRepository.findParentId(childId);
        if (currentParentId == null) {
            return false;
        }
        if (currentParentId == parentId) {
            return true;
        }
        return isDescendant(parentId, currentParentId);
    }

    @Transactional
    public void deleteOrganization(long id) {
        getOrganization(id);
        if (organizationRepository.countByParentId(id) > 0) {
            throw new IllegalArgumentException("请先删除该部门下的所有子部门");
        }
        if (organizationRepository.deleteById(id) == 0) {
            throw new IllegalArgumentException("组织不存在");
        }
    }

    private OrganizationUnit getOrganization(long id) {
        return findOrganizationById(id).orElseThrow(() -> new IllegalArgumentException("组织不存在"));
    }

    private Optional<OrganizationUnit> findOrganizationById(long id) {
        return organizationRepository.findById(id).map(this::toOrganizationUnit);
    }

    private Optional<OrganizationUnit> findOrganizationByCode(String code) {
        return organizationRepository.findByCode(code).map(this::toOrganizationUnit);
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == null) {
            return;
        }
        if (organizationRepository.countById(parentId) == 0) {
            throw new IllegalArgumentException("上级组织不存在");
        }
    }

    private void ensureLeaderExists(Long leaderUserId) {
        if (leaderUserId == null) {
            return;
        }
        if (organizationRepository.countUserAccountById(leaderUserId) == 0) {
            throw new IllegalArgumentException("负责人不存在");
        }
    }

    private OrganizationUnit toOrganizationUnit(OrganizationPo po) {
        return new OrganizationUnit(
                po.getId(),
                po.getCode(),
                po.getName(),
                po.getParentId(),
                po.getParentName(),
                po.getLeaderUserId(),
                po.getLeaderName(),
                po.getOrderNo() == null ? 0 : po.getOrderNo(),
                po.getStatus() == null ? 0 : po.getStatus(),
                po.getRemark(),
                po.getCreatedAt());
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("请输入" + fieldName);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int normalizeStatus(Integer status) {
        int normalized = status == null ? 1 : status;
        if (normalized != 0 && normalized != 1) {
            throw new IllegalArgumentException("组织状态值不合法");
        }
        return normalized;
    }

    public record OrganizationUnit(
            long id,
            String code,
            String name,
            Long parentId,
            String parentName,
            Long leaderUserId,
            String leaderName,
            int orderNo,
            int status,
            String remark,
            LocalDateTime createdAt) {
    }

    public record CreateOrganizationCommand(
            String code,
            String name,
            Long parentId,
            Long leaderUserId,
            Integer orderNo,
            Integer status,
            String remark) {
    }

    public record UpdateOrganizationCommand(
            String code,
            String name,
            Long parentId,
            Long leaderUserId,
            Integer orderNo,
            Integer status,
            String remark) {
    }

    public List<Long> listUserOrganizationIds(long userId) {
        return organizationRepository.findUserOrganizationIds(userId);
    }

    @Transactional
    public void replaceUserOrganizations(long userId, List<Long> organizationIds) {
        organizationRepository.deleteUserOrganizations(userId);
        if (organizationIds != null && !organizationIds.isEmpty()) {
            for (Long orgId : organizationIds.stream().filter(Objects::nonNull).distinct().toList()) {
                organizationRepository.insertUserOrganization(userId, orgId);
            }
        }
    }

    public List<Long> collectDescendantOrgIds(long orgId) {
        List<Long> result = new ArrayList<>();
        result.add(orgId);
        for (Long childId : organizationRepository.findChildIds(orgId)) {
            result.addAll(collectDescendantOrgIds(childId));
        }
        return result;
    }
}
