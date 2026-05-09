package com.uniondesk.domain.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.InvitationCodeService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}")
public class InvitationCodeController {

    private final InvitationCodeService invitationCodeService;

    public InvitationCodeController(InvitationCodeService invitationCodeService) {
        this.invitationCodeService = invitationCodeService;
    }

    @GetMapping("/invitation-codes")
    @RequirePermission(PermissionCodes.DOMAIN_INVITATION_CODE_READ)
    public PageResult<InvitationCodeDtos.InvitationCodeView> listInvitationCodes(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return invitationCodeService.listInvitationCodes(domainId, page, pageSize);
    }

    @PostMapping("/invitation-codes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_INVITATION_CODE_CREATE)
    public InvitationCodeDtos.InvitationCodeView createInvitationCode(
            @PathVariable long domainId,
            @Valid @RequestBody InvitationCodeDtos.CreateInvitationCodeRequest request) {
        return invitationCodeService.createInvitationCode(domainId, request);
    }

    @DeleteMapping("/invitation-codes/{codeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_INVITATION_CODE_DELETE)
    public void deleteInvitationCode(@PathVariable long domainId, @PathVariable long codeId) {
        invitationCodeService.deleteInvitationCode(domainId, codeId);
    }
}
