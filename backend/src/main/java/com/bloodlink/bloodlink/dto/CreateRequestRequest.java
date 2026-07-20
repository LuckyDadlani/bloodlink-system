package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRequestRequest(
    @NotNull UUID creatorHospitalId,
    @NotBlank String bloodGroupRequired,
    @NotBlank String componentRequired,
    @Min(1) int unitsRequired,
    String urgencyLevel
) {
}
