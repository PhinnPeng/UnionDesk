package com.uniondesk.audit.semantics;

import com.uniondesk.iam.entity.AdminMenuPo;
import com.uniondesk.iam.repository.AdminMenuRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuditMenuPathResolver {

    private static final String NODE_TYPE_BUTTON = "button";

    private final AdminMenuRepository adminMenuRepository;

    public AuditMenuPathResolver(AdminMenuRepository adminMenuRepository) {
        this.adminMenuRepository = adminMenuRepository;
    }

    public List<String> resolvePaths(List<Long> menuIds, String scope) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        List<AdminMenuPo> allMenus = adminMenuRepository.findAll(scope);
        Map<Long, AdminMenuPo> menuById = new HashMap<>();
        for (AdminMenuPo menu : allMenus) {
            if (menu.getId() != null) {
                menuById.put(menu.getId(), menu);
            }
        }
        LinkedHashSet<Long> orderedIds = new LinkedHashSet<>(menuIds);
        List<String> paths = new ArrayList<>();
        for (Long menuId : orderedIds) {
            AdminMenuPo menu = menuById.get(menuId);
            if (menu == null || isRequired(menu)) {
                continue;
            }
            paths.add(buildPath(menu, menuById));
        }
        paths.sort(Comparator.naturalOrder());
        return paths;
    }

    public Set<Long> filterDiffIds(List<Long> beforeIds, List<Long> afterIds, String scope) {
        List<AdminMenuPo> allMenus = adminMenuRepository.findAll(scope);
        Map<Long, AdminMenuPo> menuById = new HashMap<>();
        for (AdminMenuPo menu : allMenus) {
            if (menu.getId() != null) {
                menuById.put(menu.getId(), menu);
            }
        }
        Set<Long> before = normalizeIds(beforeIds, menuById);
        Set<Long> after = normalizeIds(afterIds, menuById);
        Set<Long> added = new LinkedHashSet<>(after);
        added.removeAll(before);
        Set<Long> removed = new LinkedHashSet<>(before);
        removed.removeAll(after);
        Set<Long> diff = new LinkedHashSet<>();
        diff.addAll(added);
        diff.addAll(removed);
        return diff;
    }

    public List<String> resolveAddedPaths(List<Long> beforeIds, List<Long> afterIds, String scope) {
        return resolveDiffPaths(beforeIds, afterIds, scope, true);
    }

    public List<String> resolveRemovedPaths(List<Long> beforeIds, List<Long> afterIds, String scope) {
        return resolveDiffPaths(beforeIds, afterIds, scope, false);
    }

    private List<String> resolveDiffPaths(
            List<Long> beforeIds,
            List<Long> afterIds,
            String scope,
            boolean added) {
        List<AdminMenuPo> allMenus = adminMenuRepository.findAll(scope);
        Map<Long, AdminMenuPo> menuById = new HashMap<>();
        for (AdminMenuPo menu : allMenus) {
            if (menu.getId() != null) {
                menuById.put(menu.getId(), menu);
            }
        }
        Set<Long> before = normalizeIds(beforeIds, menuById);
        Set<Long> after = normalizeIds(afterIds, menuById);
        Set<Long> targetIds = added ? new LinkedHashSet<>(after) : new LinkedHashSet<>(before);
        if (added) {
            targetIds.removeAll(before);
        } else {
            targetIds.removeAll(after);
        }
        List<String> paths = new ArrayList<>();
        for (Long menuId : targetIds) {
            AdminMenuPo menu = menuById.get(menuId);
            if (menu == null) {
                continue;
            }
            paths.add(buildPath(menu, menuById));
        }
        paths.sort(Comparator.naturalOrder());
        return paths;
    }

    private Set<Long> normalizeIds(List<Long> ids, Map<Long, AdminMenuPo> menuById) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        if (ids == null) {
            return normalized;
        }
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            AdminMenuPo menu = menuById.get(id);
            if (menu != null && isRequired(menu)) {
                continue;
            }
            normalized.add(id);
        }
        return normalized;
    }

    private String buildPath(AdminMenuPo menu, Map<Long, AdminMenuPo> menuById) {
        List<String> segments = new ArrayList<>();
        AdminMenuPo current = menu;
        int guard = 0;
        while (current != null && guard++ < 32) {
            String segment = formatSegment(current);
            if (StringUtils.hasText(segment)) {
                segments.addFirst(segment);
            }
            Long parentId = current.getParentId();
            current = parentId == null ? null : menuById.get(parentId);
        }
        return String.join(" / ", segments);
    }

    private String formatSegment(AdminMenuPo menu) {
        String name = menu.getName() == null ? "" : menu.getName().trim();
        if (NODE_TYPE_BUTTON.equalsIgnoreCase(menu.getNodeType())) {
            return "[按钮] " + name;
        }
        return name;
    }

    private boolean isRequired(AdminMenuPo menu) {
        return menu.getRequired() != null && menu.getRequired() == 1;
    }
}
