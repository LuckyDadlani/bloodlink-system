package com.bloodlink.bloodlink.service;

import com.bloodlink.bloodlink.dto.DashboardResponse;
import com.bloodlink.bloodlink.exception.ApiException;
import com.bloodlink.bloodlink.model.BloodBankProfile;
import com.bloodlink.bloodlink.model.EmergencyRequest;
import com.bloodlink.bloodlink.repository.BloodBankProfileRepository;
import com.bloodlink.bloodlink.repository.BloodInventoryRepository;
import com.bloodlink.bloodlink.repository.EmergencyRequestRepository;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final BloodBankProfileRepository bloodBankProfileRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;

    public DashboardService(BloodBankProfileRepository bloodBankProfileRepository,
                            BloodInventoryRepository bloodInventoryRepository,
                            EmergencyRequestRepository emergencyRequestRepository) {
        this.bloodBankProfileRepository = bloodBankProfileRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID bloodBankId, UUID creatorHospitalId) {

        BloodBankProfile profile = bloodBankProfileRepository.findById(bloodBankId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Blood bank profile not found"));

        int totalUnits = bloodInventoryRepository
            .findByBloodBankIdOrderByBloodGroupAscComponentTypeAsc(bloodBankId)
            .stream()
            .mapToInt(inv -> inv.getUnitsAvailable())
            .sum();

        long activeRequests = emergencyRequestRepository.countActiveByHospitalId(creatorHospitalId);
        long fulfilledRequests = emergencyRequestRepository.countFulfilledByHospitalId(creatorHospitalId);

        return new DashboardResponse(
            profile.getBloodBankName(),
            profile.getCity(),
            profile.getState(),
            totalUnits,
            activeRequests,
            fulfilledRequests,
            emergencyRequestRepository
                .findTop10ByHospitalIdOrderByCreatedAtDesc(creatorHospitalId)
                .stream()
                .map(this::toActivity)
                .toList()
        );
    }

    private DashboardResponse.ActivityItem toActivity(EmergencyRequest request) {
        return new DashboardResponse.ActivityItem(
            request.getEmergencyId() != null ? request.getEmergencyId().toString() : null,
            request.getBloodGroupRequired(), // ✅ STRING (correct)
            request.getUnitsRequired(),
            request.getStatus() != null ? request.getStatus() : "UNKNOWN",
            request.getCreatedAt() != null ? request.getCreatedAt().toString() : null
        );
    }
}