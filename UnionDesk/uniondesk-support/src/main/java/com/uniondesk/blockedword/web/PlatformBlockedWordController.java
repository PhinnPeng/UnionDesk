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
@RequestMapping("/api/v1/admin/blocked-words")
public class PlatformBlockedWordController {

    private final BlockedWordService blockedWordService;

    public PlatformBlockedWordController(BlockedWordService blockedWordService) {
        this.blockedWordService = blockedWordService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_BLOCKED_WORD_READ)
    public PageResult<BlockedWordDtos.BlockedWordView> listBlockedWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return blockedWordService.listGlobalPage(page, pageSize, keyword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_BLOCKED_WORD_CREATE)
    public BlockedWordDtos.BlockedWordView createBlockedWord(
            @Valid @RequestBody BlockedWordDtos.CreateBlockedWordRequest request) {
        return blockedWordService.createGlobal(request.word());
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_BLOCKED_WORD_CREATE)
    public BlockedWordDtos.BatchCreateBlockedWordResult createBlockedWordsBatch(
            @Valid @RequestBody BlockedWordDtos.BatchCreateBlockedWordRequest request) {
        return blockedWordService.createGlobalBatch(request.words());
    }

    @DeleteMapping("/{word_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_BLOCKED_WORD_DELETE)
    public void deleteBlockedWord(@PathVariable("word_id") long wordId) {
        blockedWordService.deleteGlobal(wordId);
    }
}
