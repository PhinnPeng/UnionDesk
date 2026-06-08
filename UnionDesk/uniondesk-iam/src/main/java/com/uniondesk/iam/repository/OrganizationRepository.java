package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.OrganizationPo;
import com.uniondesk.iam.mapper.OrganizationMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepository {

    private final OrganizationMapper mapper;

    public OrganizationRepository(OrganizationMapper mapper) {
        this.mapper = mapper;
    }

    public List<OrganizationPo> findAll() {
        return mapper.selectAll();
    }

    public Optional<OrganizationPo> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Optional<OrganizationPo> findByCode(String code) {
        return Optional.ofNullable(mapper.selectByCode(code));
    }

    public void insert(OrganizationPo po) {
        mapper.insert(po);
    }

    public int update(OrganizationPo po) {
        return mapper.update(po);
    }

    public int deleteById(long id) {
        return mapper.deleteById(id);
    }

    public int countById(long id) {
        return mapper.countById(id);
    }

    public int countByParentId(long parentId) {
        return mapper.countByParentId(parentId);
    }

    public Long findParentId(long id) {
        return mapper.selectParentId(id);
    }

    public List<Long> findChildIds(long parentId) {
        return mapper.selectChildIds(parentId);
    }

    public List<Long> findUserOrganizationIds(long userId) {
        return mapper.selectUserOrganizationIds(userId);
    }

    public void deleteUserOrganizations(long userId) {
        mapper.deleteUserOrganizations(userId);
    }

    public void insertUserOrganization(long userId, long organizationId) {
        mapper.insertUserOrganization(userId, organizationId);
    }

    public int countUserAccountById(long id) {
        return mapper.countUserAccountById(id);
    }
}
