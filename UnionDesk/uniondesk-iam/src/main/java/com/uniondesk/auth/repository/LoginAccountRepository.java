package com.uniondesk.auth.repository;

import com.uniondesk.auth.entity.LoginAccountPo;
import com.uniondesk.auth.mapper.LoginAccountMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class LoginAccountRepository {

    private final LoginAccountMapper mapper;

    public LoginAccountRepository(LoginAccountMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<LoginAccountPo> findStaffByIdentifier(String column, String identifier) {
        return Optional.ofNullable(mapper.selectStaffByIdentifier(column, identifier));
    }

    public Optional<LoginAccountPo> findCustomerByIdentifier(String column, String identifier) {
        return Optional.ofNullable(mapper.selectCustomerByIdentifier(column, identifier));
    }

    public Optional<LoginAccountPo> findStaffById(long id) {
        return Optional.ofNullable(mapper.selectStaffById(id));
    }

    public Optional<LoginAccountPo> findCustomerById(long id) {
        return Optional.ofNullable(mapper.selectCustomerById(id));
    }

    public List<String> findStaffDomainRoleCodes(long userId) {
        return mapper.selectStaffDomainRoleCodes(userId);
    }

    public List<String> findStaffPlatformRoleCodes(long userId) {
        return mapper.selectStaffPlatformRoleCodes(userId);
    }

    public List<Long> findCustomerAccessibleDomainIds(long userId) {
        return mapper.selectCustomerAccessibleDomainIds(userId);
    }

    public List<Long> findAllDomainIds() {
        return mapper.selectAllDomainIds();
    }

    public List<Long> findStaffAccessibleDomainIds(long userId) {
        return mapper.selectStaffAccessibleDomainIds(userId);
    }

    public void updateStaffPassword(long id, String passwordHash) {
        mapper.updateStaffPassword(id, passwordHash);
    }

    public void updateCustomerPassword(long id, String passwordHash) {
        mapper.updateCustomerPassword(id, passwordHash);
    }
}
