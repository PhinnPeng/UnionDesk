package com.uniondesk.iam.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final JdbcTemplate jdbcTemplate;

    public OrganizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrganizationUnit> listOrganizations() {
        return jdbcTemplate.query("""
                SELECT
                    o.id,
                    o.code,
                    o.name,
                    o.parent_id,
                    parent.name AS parent_name,
                    o.leader_user_id,
                    leader.username AS leader_name,
                    o.order_no,
                    o.status,
                    o.remark,
                    o.created_at
                FROM platform_organization o
                LEFT JOIN platform_organization parent ON parent.id = o.parent_id
                LEFT JOIN user_account leader ON leader.id = o.leader_user_id
                ORDER BY COALESCE(o.parent_id, 0), o.order_no, o.id
                """, this::mapOrganizationUnit);
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

        try {
            jdbcTemplate.update("""
                            INSERT INTO platform_organization (
                                code,
                                name,
                                parent_id,
                                leader_user_id,
                                order_no,
                                status,
                                remark,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                            """,
                    code,
                    name,
                    parentId,
                    leaderUserId,
                    orderNo,
                    status,
                    remark);
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

        try {
            jdbcTemplate.update("""
                            UPDATE platform_organization
                            SET
                                code = ?,
                                name = ?,
                                parent_id = ?,
                                leader_user_id = ?,
                                order_no = ?,
                                status = ?,
                                remark = ?,
                                updated_at = CURRENT_TIMESTAMP(3)
                            WHERE id = ?
                            """,
                    code,
                    name,
                    parentId,
                    leaderUserId,
                    orderNo,
                    status,
                    remark,
                    id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("组织编码已存在");
        }

        return getOrganization(id);
    }

    private boolean isDescendant(long parentId, long childId) {
        try {
            Long currentParentId = jdbcTemplate.queryForObject(
                    "SELECT parent_id FROM platform_organization WHERE id = ?",
                    Long.class,
                    childId);
            if (currentParentId == null) {
                return false;
            }
            if (currentParentId == parentId) {
                return true;
            }
            return isDescendant(parentId, currentParentId);
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    @Transactional
    public void deleteOrganization(long id) {
        getOrganization(id);
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform_organization WHERE parent_id = ?",
                Integer.class,
                id);
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("请先删除该部门下的所有子部门");
        }
        int updated = jdbcTemplate.update("DELETE FROM platform_organization WHERE id = ?", id);
        if (updated == 0) {
            throw new IllegalArgumentException("组织不存在");
        }
    }

    private OrganizationUnit getOrganization(long id) {
        return findOrganizationById(id).orElseThrow(() -> new IllegalArgumentException("组织不存在"));
    }

    private Optional<OrganizationUnit> findOrganizationById(long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                            SELECT
                                o.id,
                                o.code,
                                o.name,
                                o.parent_id,
                                parent.name AS parent_name,
                                o.leader_user_id,
                                leader.username AS leader_name,
                                o.order_no,
                                o.status,
                                o.remark,
                                o.created_at
                            FROM platform_organization o
                            LEFT JOIN platform_organization parent ON parent.id = o.parent_id
                            LEFT JOIN user_account leader ON leader.id = o.leader_user_id
                            WHERE o.id = ?
                            LIMIT 1
                            """,
                    this::mapOrganizationUnit,
                    id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<OrganizationUnit> findOrganizationByCode(String code) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                            SELECT
                                o.id,
                                o.code,
                                o.name,
                                o.parent_id,
                                parent.name AS parent_name,
                                o.leader_user_id,
                                leader.username AS leader_name,
                                o.order_no,
                                o.status,
                                o.remark,
                                o.created_at
                            FROM platform_organization o
                            LEFT JOIN platform_organization parent ON parent.id = o.parent_id
                            LEFT JOIN user_account leader ON leader.id = o.leader_user_id
                            WHERE o.code = ?
                            LIMIT 1
                            """,
                    this::mapOrganizationUnit,
                    code));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM platform_organization WHERE id = ?",
                Integer.class,
                parentId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("上级组织不存在");
        }
    }

    private void ensureLeaderExists(Long leaderUserId) {
        if (leaderUserId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_account WHERE id = ?",
                Integer.class,
                leaderUserId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("负责人不存在");
        }
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

    private OrganizationUnit mapOrganizationUnit(ResultSet rs, int rowNum) throws SQLException {
        return new OrganizationUnit(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getObject("parent_id", Long.class),
                rs.getString("parent_name"),
                rs.getObject("leader_user_id", Long.class),
                rs.getString("leader_name"),
                rs.getInt("order_no"),
                rs.getInt("status"),
                rs.getString("remark"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
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
        return jdbcTemplate.queryForList(
                "SELECT organization_id FROM user_organization WHERE user_id = ? ORDER BY organization_id",
                Long.class,
                userId);
    }

    @Transactional
    public void replaceUserOrganizations(long userId, List<Long> organizationIds) {
        jdbcTemplate.update("DELETE FROM user_organization WHERE user_id = ?", userId);
        if (organizationIds != null && !organizationIds.isEmpty()) {
            for (Long orgId : organizationIds.stream().filter(Objects::nonNull).distinct().toList()) {
                jdbcTemplate.update(
                        "INSERT INTO user_organization (user_id, organization_id) VALUES (?, ?)",
                        userId,
                        orgId);
            }
        }
    }

    public List<Long> collectDescendantOrgIds(long orgId) {
        List<Long> result = new ArrayList<>();
        result.add(orgId);
        List<Long> childIds = jdbcTemplate.queryForList(
                "SELECT id FROM platform_organization WHERE parent_id = ?",
                Long.class,
                orgId);
        for (Long childId : childIds) {
            result.addAll(collectDescendantOrgIds(childId));
        }
        return result;
    }
}
