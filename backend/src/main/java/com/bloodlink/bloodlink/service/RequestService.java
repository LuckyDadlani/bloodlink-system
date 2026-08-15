package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.CreateRequestRequest;
import com.bloodlink.bloodlink.dto.CreateRequestResponse;
import com.bloodlink.bloodlink.dto.DonorAcceptRequest;
import com.bloodlink.bloodlink.dto.RankedDonorDto;
import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.*;
import com.bloodlink.bloodlink.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RequestService {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final HospitalProfileRepository hospitalProfileRepository;
    private final DonorNotificationRepository donorNotificationRepository;
    private final EligibleDonorViewRepository eligibleDonorViewRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final BankRequestDismissalRepository bankRequestDismissalRepository;
    private final MlServiceClient mlServiceClient;
    private final TelegramService telegramService;
    private final int topN;

    public RequestService(EmergencyRequestRepository emergencyRequestRepository,
                          HospitalProfileRepository hospitalProfileRepository,
                          DonorNotificationRepository donorNotificationRepository,
                          EligibleDonorViewRepository eligibleDonorViewRepository,
                          BloodInventoryRepository bloodInventoryRepository,
                          BankRequestDismissalRepository bankRequestDismissalRepository,
                          MlServiceClient mlServiceClient,
                          TelegramService telegramService,
                          @org.springframework.beans.factory.annotation.Value("${bloodlink.ml.top-n}") int topN) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.hospitalProfileRepository = hospitalProfileRepository;
        this.donorNotificationRepository = donorNotificationRepository;
        this.eligibleDonorViewRepository = eligibleDonorViewRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.bankRequestDismissalRepository = bankRequestDismissalRepository;
        this.mlServiceClient = mlServiceClient;
        this.telegramService = telegramService;
        this.topN = topN;
    }

    public CreateRequestResponse createRequest(CreateRequestRequest request) {

        HospitalProfile hospital = hospitalProfileRepository.findById(request.creatorHospitalId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid hospital ID"));

        EmergencyRequest newRequest = new EmergencyRequest();
        UUID emergencyId = UUID.randomUUID();
        Instant now = Instant.now();

        // Convert frontend format (O_POS) to DB format (O+).
        String dbValue = request.bloodGroupRequired()
            .replace("_POS", "+")
            .replace("_NEG", "-");
        String dbComponentValue = normalizeComponentValue(request.componentRequired());

        newRequest.setEmergencyId(emergencyId);
        newRequest.setHospitalId(request.creatorHospitalId());
        newRequest.setBloodGroupRequired(dbValue);
        newRequest.setComponentRequired(dbComponentValue);
        newRequest.setUnitsRequired(request.unitsRequired());
        newRequest.setUrgencyLevel(
            request.urgencyLevel() == null || request.urgencyLevel().isBlank()
                ? "HIGH"
                : request.urgencyLevel()
        );
        newRequest.setHospitalLatitude(hospital.getLatitude());
        newRequest.setHospitalLongitude(hospital.getLongitude());
        newRequest.setHospitalCity(hospital.getCity());
        newRequest.setStatus("CREATED");
        newRequest.setUnitsFulfilled(0);
        newRequest.setCreatedAt(now);
        newRequest.setTotalDonorsNotified(0);

        emergencyRequestRepository.save(newRequest);

        List<RankedDonorDto> rankedDonors = rankDonorsWithFallback(emergencyId, dbValue);

        RankedDonorDto topDonor = rankedDonors.isEmpty() ? null : rankedDonors.get(0);
        int selected = topDonor == null ? 0 : 1;

        if (topDonor != null) {

            DonorNotification notification = new DonorNotification();
            notification.setNotificationId(UUID.randomUUID());
            notification.setEmergencyId(emergencyId);
            notification.setDonorId(topDonor.donorId());
            notification.setBatchNumber(1);
            notification.setAiRankAtTime(1);
            notification.setAiProbabilityScore(
                topDonor.score() == null ? BigDecimal.ZERO : topDonor.score()
            );
            notification.setSentAt(now);
            notification.setDeliveryStatus("SENT");
            notification.setResponseReceived("NO_RESPONSE");
            notification.setWasSelected(true);

            donorNotificationRepository.save(notification);

            telegramService.sendMessage(
                "🚨 Urgent Blood Requirement\n\n" +
                "Blood Group: " + dbValue + "\n" +
                "Blood Bank: " + hospital.getCity() + " Blood Bank\n\n" +
                "If you are available, please call: 9329944373"
            );
        }

        newRequest.setStatus(selected > 0 ? "DONORS_NOTIFIED" : "ESCALATED_TO_DONORS");
        newRequest.setDonorNotificationStartedAt(now);
        newRequest.setTotalDonorsNotified(selected);

        emergencyRequestRepository.save(newRequest);

        return new CreateRequestResponse(
            emergencyId,
            newRequest.getStatus(),
            selected,
            rankedDonors
        );
    }

    @Transactional(readOnly = true)
    public List<EmergencyRequest> fetchRequests(UUID creatorHospitalId) {
        return emergencyRequestRepository.findByHospitalIdOrderByCreatedAtDesc(creatorHospitalId);
    }

    @Transactional
    public void acceptRequestByDonor(DonorAcceptRequest request) {
        Instant acceptedAt = Instant.now();

        int updated = emergencyRequestRepository.markFulfilledByFirstDonor(
            request.emergencyId(),
            request.donorId(),
            acceptedAt
        );

        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                "Request already fulfilled or invalid");
        }

        donorNotificationRepository.markAccepted(
            request.emergencyId(),
            request.donorId(),
            acceptedAt
        );
    }

    @Transactional(readOnly = true)
    public List<EmergencyRequest> getRequestsForBank(UUID bloodBankId) {
        return emergencyRequestRepository.findIncomingRequestsForBank(bloodBankId);
    }

    @Transactional
    public void fulfillRequest(UUID emergencyId, UUID bloodBankId, int unitsFulfilled) {
        EmergencyRequest request = emergencyRequestRepository.findById(emergencyId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!List.of("CREATED", "CHECKING_BANKS", "ESCALATED_TO_DONORS", "DONORS_NOTIFIED").contains(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request cannot be fulfilled in its current status");
        }

        // Validate units available in inventory
        BloodInventory inventory = bloodInventoryRepository.findByBankAndGroupAndComponent(
                bloodBankId, request.getBloodGroupRequired(), request.getComponentRequired())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Blood bank does not carry the required blood group and component"));

        if (inventory.getUnitsAvailable() < unitsFulfilled) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough units in stock to fulfill this request");
        }

        // Decrement inventory
        inventory.setUnitsAvailable(inventory.getUnitsAvailable() - unitsFulfilled);
        inventory.setLastUpdatedAt(Instant.now());
        bloodInventoryRepository.save(inventory);

        // Update request
        request.setStatus("FULFILLED_BY_BANK");
        request.setFulfilledByBankId(bloodBankId);
        request.setUnitsFulfilled(unitsFulfilled);
        request.setClosedAt(Instant.now());
        emergencyRequestRepository.save(request);
    }

    @Transactional
    public void dismissRequest(UUID emergencyId, UUID bloodBankId) {
        if (!bankRequestDismissalRepository.existsByBloodBankIdAndEmergencyId(bloodBankId, emergencyId)) {
            BankRequestDismissal dismissal = new BankRequestDismissal();
            dismissal.setDismissalId(UUID.randomUUID());
            dismissal.setBloodBankId(bloodBankId);
            dismissal.setEmergencyId(emergencyId);
            dismissal.setDismissedAt(Instant.now());
            bankRequestDismissalRepository.save(dismissal);
        }
    }

    private List<RankedDonorDto> rankDonorsWithFallback(UUID emergencyId, String bloodGroup) {
        try {
            return mlServiceClient.rankDonors(emergencyId);
        } catch (Exception ex) {
            System.err.println("ML service ranking failed: " + ex.getMessage());
            ex.printStackTrace();
            // fallback
            return eligibleDonorViewRepository.findByBloodGroup(bloodGroup)
                .stream()
                .sorted(Comparator.comparing(
                    (EligibleDonorView d) -> d.getTotalDonations() == null ? 0 : d.getTotalDonations()
                ).reversed())
                .map(d -> new RankedDonorDto(
                    d.getDonorId(),
                    BigDecimal.valueOf(
                        Math.min(1.0d,
                            (d.getTotalDonations() == null ? 0d : d.getTotalDonations()) / 20.0d
                        )
                    )
                ))
                .toList();
        }
    }

    private String normalizeComponentValue(String componentRequired) {
        if (componentRequired == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "componentRequired is required");
        }

        return switch (componentRequired.trim().toUpperCase()) {
            case "WHOLE_BLOOD" -> "Whole Blood";
            case "PACKED_RED_CELLS" -> "Packed Red Cells";
            case "PLATELETS" -> "Platelets";
            case "FRESH_FROZEN_PLASMA" -> "Fresh Frozen Plasma";
            case "CRYOPRECIPITATE" -> "Cryoprecipitate";
            default -> componentRequired;
        };
    }
}