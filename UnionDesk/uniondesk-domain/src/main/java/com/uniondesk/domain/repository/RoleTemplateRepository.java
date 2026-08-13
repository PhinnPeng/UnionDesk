package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.entity.RoleTemplateDomainPo;
import com.uniondesk.domain.entity.RoleTemplatePermissionPo;
import com.uniondesk.domain.entity.RoleTemplatePo;
import com.uniondesk.domain.mapper.RoleTemplateDomainMapper;
import com.uniondesk.domain.mapper.RoleTemplateMapper;
import com.uniondesk.domain.mapper.RoleTemplatePermissionMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RoleTemplateRepository {

    private final RoleTemplateMapper roleTemplateMapper;
    private final RoleTemplatePermissionMapper roleTemplatePermissionMapper;
    private final RoleTemplateDomainMapper roleTemplateDomainMapper;

    public RoleTemplateRepository(
            RoleTemplateMapper roleTemplateMapper,
            RoleTemplatePermissionMapper roleTemplatePermissionMapper,
            RoleTemplateDomainMapper roleTemplateDomainMapper) {
        this.roleTemplateMapper = roleTemplateMapper;
        this.roleTemplatePermissionMapper = roleTemplatePermissionMapper;
        this.roleTemplateDomainMapper = roleTemplateDomainMapper;
    }

    public RoleTemplatePo findById(long id) {
        return roleTemplateMapper.selectById(id);
    }

    public RoleTemplatePo findByCode(String code) {
        return roleTemplateMapper.selectByCode(code);
    }

    public List<RoleTemplatePo> findAll() {
        return roleTemplateMapper.selectAll();
    }

    public void insert(RoleTemplatePo po) {
        roleTemplateMapper.insert(po);
    }

    public void update(
            long id,
            String name,
            String description,
            String syncStrategy,
            String lockedFields,
            int version) {
        roleTemplateMapper.update(id, name, description, syncStrategy, lockedFields, version);
    }

    public void deleteById(long id) {
        roleTemplateMapper.deleteById(id);
    }

    public List<RoleTemplatePermissionPo> findPermissionsByTemplateId(long templateId) {
        return roleTemplatePermissionMapper.selectByTemplateId(templateId);
    }

    public List<PermissionItemPo> findPermissionItemsByTemplateId(long templateId) {
        return roleTemplatePermissionMapper.selectPermissionItemsByTemplateId(templateId);
    }

    public void deletePermissionsByTemplateId(long templateId) {
        roleTemplatePermissionMapper.deleteByTemplateId(templateId);
    }

    public void insertPermission(long templateId, long permissionItemId) {
        roleTemplatePermissionMapper.insert(templateId, permissionItemId);
    }

    public List<RoleTemplateDomainPo> findDomainsByTemplateId(long templateId) {
        return roleTemplateDomainMapper.selectByTemplateId(templateId);
    }

    public RoleTemplateDomainPo findDomainByTemplateAndDomain(long templateId, long domainId) {
        return roleTemplateDomainMapper.selectByTemplateAndDomain(templateId, domainId);
    }

    public int countDomainsByTemplateId(long templateId) {
        return roleTemplateDomainMapper.countByTemplateId(templateId);
    }

    public void insertDomain(long templateId, long domainId, long instanceDomainRoleId, String syncMode) {
        roleTemplateDomainMapper.insert(templateId, domainId, instanceDomainRoleId, syncMode);
    }

    public void deleteDomainByTemplateAndDomain(long templateId, long domainId) {
        roleTemplateDomainMapper.deleteByTemplateAndDomain(templateId, domainId);
    }

    public void touchDomain(long templateId, long domainId) {
        roleTemplateDomainMapper.touch(templateId, domainId);
    }
}
