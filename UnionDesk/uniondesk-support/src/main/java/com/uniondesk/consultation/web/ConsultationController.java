package com.uniondesk.consultation.web;

import com.uniondesk.common.web.ApiResponse;
import com.uniondesk.common.demo.DemoDataService;
import com.uniondesk.common.demo.DemoDtos.ConsultationFlowResponse;
import com.uniondesk.common.demo.DemoDtos.ConsultationMessageRequest;
import com.uniondesk.common.demo.DemoDtos.ConsultationMessagePayloadRequest;
import com.uniondesk.common.demo.DemoDtos.ConsultationMessageView;
import com.uniondesk.common.demo.DemoDtos.ConsultationSummaryView;
import com.uniondesk.common.demo.DemoDtos.ConvertConsultationToTicketRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultations")
@Profile("demo")
public class ConsultationController {

    private final DemoDataService demoDataService;

    public ConsultationController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @GetMapping
    public ApiResponse<List<ConsultationSummaryView>> list(
            @RequestParam(required = false) Long businessDomainId,
            @RequestParam(required = false) Long customerId) {
        return ApiResponse.ok(demoDataService.listConsultations(businessDomainId, customerId));
    }

    @GetMapping("/{sessionNo}/messages")
    public ApiResponse<List<ConsultationMessageView>> messages(@PathVariable String sessionNo) {
        return ApiResponse.ok(demoDataService.listMessages(sessionNo));
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultationMessageView> sendMessage(@Valid @RequestBody ConsultationMessageRequest request) {
        return ApiResponse.ok(demoDataService.sendMessage(request));
    }

    @PostMapping("/{sessionNo}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultationMessageView> sendMessageBySession(
            @PathVariable String sessionNo,
            @Valid @RequestBody ConsultationMessagePayloadRequest request) {
        ConsultationMessageRequest mergedRequest = new ConsultationMessageRequest(
                sessionNo,
                request.businessDomainId(),
                request.senderUserId(),
                request.senderRole(),
                request.messageType(),
                request.content(),
                request.payload());
        return ApiResponse.ok(demoDataService.sendMessage(mergedRequest));
    }

    @PostMapping("/{sessionNo}/ticket")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultationFlowResponse> convertToTicket(
            @PathVariable String sessionNo,
            @Valid @RequestBody ConvertConsultationToTicketRequest request) {
        return ApiResponse.ok(demoDataService.convertConsultationToTicket(sessionNo, request));
    }
}
