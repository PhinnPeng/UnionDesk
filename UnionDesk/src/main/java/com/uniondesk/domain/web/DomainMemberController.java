package com.uniondesk.domain.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.DomainMemberService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}")
public class DomainMemberController {

    private final DomainMemberService domainMemberService;

    public DomainMemberController(DomainMemberService domainMemberService) {
        this.domainMemberService = domainMemberService;
    }

    @GetMapping("/members")
    @RequirePermission(PermissionCodes.DOMAIN_MEMBER_READ)
    public PageResult<DomainMemberDtos.DomainMemberView> listMembers(
            @PathVariable("domainId") long domainId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return domainMemberService.listMembers(domainId, page, pageSize, status, keyword);
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_MEMBER_CREATE)
    public DomainMemberDtos.DomainMemberView createMember(
            @PathVariable("domainId") long domainId,
            @Valid @RequestBody DomainMemberDtos.CreateDomainMemberRequest request) {
        return domainMemberService.createMember(domainId, request);
    }

    @PutMapping("/members/{memberId}/roles")
    @RequirePermission(PermissionCodes.DOMAIN_MEMBER_UPDATE_ROLES)
    public DomainMemberDtos.DomainMemberView updateMemberRoles(
            @PathVariable("domainId") long domainId,
            @PathVariable("memberId") long memberId,
            @Valid @RequestBody DomainMemberDtos.UpdateDomainMemberRolesRequest request) {
        return domainMemberService.updateMemberRoles(domainId, memberId, request);
    }

    @DeleteMapping("/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_MEMBER_DELETE)
    public void deleteMember(@PathVariable("domainId") long domainId, @PathVariable("memberId") long memberId) {
        domainMemberService.deleteMember(domainId, memberId);
    }
}
