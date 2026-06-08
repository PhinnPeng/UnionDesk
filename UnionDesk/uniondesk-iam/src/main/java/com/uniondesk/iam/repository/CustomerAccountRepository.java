package com.uniondesk.iam.repository;

import com.uniondesk.iam.entity.CustomerAccountPo;
import com.uniondesk.iam.mapper.CustomerAccountMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerAccountRepository {

    private final CustomerAccountMapper mapper;

    public CustomerAccountRepository(CustomerAccountMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<CustomerAccountPo> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public int countByUsername(String username) {
        return mapper.countByUsername(username);
    }

    public void insert(CustomerAccountPo po) {
        mapper.insert(po);
    }

    public Optional<Long> findIdByUsernameOrPhone(String username, String phone) {
        return Optional.ofNullable(mapper.selectIdByUsernameOrPhone(username, phone));
    }
}
