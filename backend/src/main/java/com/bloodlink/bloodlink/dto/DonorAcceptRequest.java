package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DonorAcceptRequest(
    @NotNull UUID emergencyId,
    @NotNull UUID donorId
) {
}
