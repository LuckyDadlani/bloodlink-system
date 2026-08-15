package com.bloodlink.bloodlink.controller;

import com.bloodlink.bloodlink.dto.CreateRequestRequest;
import com.bloodlink.bloodlink.dto.CreateRequestResponse;
import com.bloodlink.bloodlink.dto.DonorAcceptRequest;
import com.bloodlink.bloodlink.dto.TelegramSendRequest;
import com.bloodlink.bloodlink.model.EmergencyRequest;
import com.bloodlink.bloodlink.service.AccessService;
import com.bloodlink.bloodlink.service.RequestService;
import com.bloodlink.bloodlink.service.TelegramService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService service;
    private final TelegramService telegramService;
    private final AccessService accessService;

    public RequestController(RequestService service, TelegramService telegramService, AccessService accessService) {
        this.service = service;
        this.telegramService = telegramService;
        this.accessService = accessService;
    }

    @PostMapping
    public CreateRequestResponse createRequest(@Valid @RequestBody CreateRequestRequest request,
                                               @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return service.createRequest(request);
    }

    @GetMapping
    public List<EmergencyRequest> fetchRequests(@RequestParam UUID creatorHospitalId,
                                                @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return service.fetchRequests(creatorHospitalId);
    }

    @PostMapping("/accept")
    public Map<String, String> acceptRequest(@Valid @RequestBody DonorAcceptRequest request) {
        service.acceptRequestByDonor(request);
        return Map.of("status", "DONOR_CONFIRMED");
    }

    @PostMapping("/telegram/send")
    public Map<String, String> sendTelegram(@Valid @RequestBody TelegramSendRequest request,
                                            @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        telegramService.sendMessage(request.chatId(), request.message());
        return Map.of("status", "SENT");
    }

    @GetMapping("/bank/{bloodBankId}")
    public List<EmergencyRequest> fetchIncomingRequestsForBank(@PathVariable UUID bloodBankId,
                                                               @RequestHeader("X-User-Id") UUID userId) {
        accessService.requireBloodBankUser(userId);
        return service.getRequestsForBank(bloodBankId);
    }

    @PostMapping("/{emergencyId}/fulfill")
    public Map<String, String> fulfillRequest(@PathVariable UUID emergencyId,
                                              @Valid @RequestBody com.bloodlink.bloodlink.dto.FulfillRequestDto request,
                                              @RequestHeader("X-User-Id") UUID userId,
                                              @RequestHeader("X-Blood-Bank-Id") UUID bloodBankId) {
        accessService.requireBloodBankUser(userId);
        service.fulfillRequest(emergencyId, bloodBankId, request.unitsFulfilled());
        return Map.of("status", "FULFILLED");
    }

    @PostMapping("/{emergencyId}/dismiss")
    public Map<String, String> dismissRequest(@PathVariable UUID emergencyId,
                                              @RequestHeader("X-User-Id") UUID userId,
                                              @RequestHeader("X-Blood-Bank-Id") UUID bloodBankId) {
        accessService.requireBloodBankUser(userId);
        service.dismissRequest(emergencyId, bloodBankId);
        return Map.of("status", "DISMISSED");
    }
}