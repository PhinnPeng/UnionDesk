package com.uniondesk.iam.core;

import com.uniondesk.iam.entity.CustomerAccountPo;
import com.uniondesk.iam.repository.CustomerAccountRepository;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerAccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final IdentitySubjectService identitySubjectService;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(
            CustomerAccountRepository customerAccountRepository,
            IdentitySubjectService identitySubjectService,
            PasswordEncoder passwordEncoder) {
        this.customerAccountRepository = customerAccountRepository;
        this.identitySubjectService = identitySubjectService;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<CustomerAccount> findById(long customerAccountId) {
        return customerAccountRepository.findById(customerAccountId).map(this::toCustomerAccount);
    }

    public void ensureUsernameAvailable(String username) {
        if (customerAccountRepository.countByUsername(username.trim()) > 0) {
            throw new IllegalArgumentException("登录名已存在");
        }
    }

    @Transactional
    public long create(CreateCustomerCommand command) {
        String username = requireText(command.username(), "登录名不能为空");
        String phone = requireText(command.phone(), "手机号不能为空");
        String nickname = StringUtils.hasText(command.nickname()) ? command.nickname().trim() : username;
        String email = trimToNull(command.email());
        ensureUsernameAvailable(username);
        long subjectId = identitySubjectService.resolveSubjectIdByPhone(phone);
        identitySubjectService.requireActiveSubject(subjectId);
        CustomerAccountPo po = new CustomerAccountPo();
        po.setSubjectId(subjectId);
        po.setUsername(username);
        po.setNickname(nickname);
        po.setPhone(phone);
        po.setEmail(email);
        po.setPasswordHash(passwordEncoder.encode(command.password()));
        po.setMustChangePassword(command.mustChangePassword() ? 1 : 0);
        try {
            customerAccountRepository.insert(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("登录名或手机号已存在");
        }
        if (po.getId() == null) {
            throw new IllegalStateException("客户账号创建失败");
        }
        return po.getId();
    }

    public Optional<Long> findIdByUsernameOrPhone(String username, String phone) {
        return customerAccountRepository.findIdByUsernameOrPhone(username, phone);
    }

    private CustomerAccount toCustomerAccount(CustomerAccountPo po) {
        return new CustomerAccount(
                po.getId(),
                po.getSubjectId(),
                po.getUsername(),
                po.getNickname(),
                po.getPhone(),
                po.getEmail(),
                po.getStatus());
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record CustomerAccount(
            long id,
            long subjectId,
            String username,
            String nickname,
            String phone,
            String email,
            String status) {
    }

    public record CreateCustomerCommand(
            String username,
            String nickname,
            String phone,
            String email,
            String password,
            boolean mustChangePassword) {
    }
}
