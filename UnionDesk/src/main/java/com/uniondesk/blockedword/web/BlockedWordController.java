package com.uniondesk.blockedword.web;

import com.uniondesk.blockedword.core.BlockedWordService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @RequirePermission(PermissionCodes.DOMAIN_BLOCKED_WORD_READ)
    public List<BlockedWordDtos.BlockedWordView> listBlockedWords(@PathVariable("domain_id") long domainId) {
        return blockedWordService.listBlockedWords(domainId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_BLOCKED_WORD_CREATE)
    public BlockedWordDtos.BlockedWordView createBlockedWord(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody BlockedWordDtos.CreateBlockedWordRequest request) {
        return blockedWordService.createBlockedWord(domainId, request.word());
    }

    @DeleteMapping("/{word_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_BLOCKED_WORD_DELETE)
    public void deleteBlockedWord(
            @PathVariable("domain_id") long domainId,
            @PathVariable("word_id") long wordId) {
        blockedWordService.deleteBlockedWord(domainId, wordId);
    }
}
