package com.uniondesk.blockedword.web;

import com.uniondesk.blockedword.core.BlockedWordService;
import com.uniondesk.common.web.PageResult;
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
@RequestMapping("/api/v1/admin/domains/{domain_id}/blocked-words")
public class BlockedWordController {

    private final BlockedWordService blockedWordService;

    public BlockedWordController(BlockedWordService blockedWordService) {
        this.blockedWordService = blockedWordService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_READ)
    public PageResult<BlockedWordDtos.BlockedWordView> listBlockedWords(
            @PathVariable("domain_id") long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return blockedWordService.listDomainPage(domainId, page, pageSize, keyword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE)
    public BlockedWordDtos.BlockedWordView createBlockedWord(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody BlockedWordDtos.CreateBlockedWordRequest request) {
        return blockedWordService.createDomain(domainId, request.word());
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE)
    public BlockedWordDtos.BatchCreateBlockedWordResult createBlockedWordsBatch(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody BlockedWordDtos.BatchCreateBlockedWordRequest request) {
        return blockedWordService.createDomainBatch(domainId, request.words());
    }

    @DeleteMapping("/{word_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE)
    public void deleteBlockedWord(
            @PathVariable("domain_id") long domainId,
            @PathVariable("word_id") long wordId) {
        blockedWordService.deleteDomain(domainId, wordId);
    }
}
