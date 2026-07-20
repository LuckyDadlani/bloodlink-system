package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InventoryUpdateRequest(
    @NotNull UUID updatedBy,
    @Min(0) int unitsAvailable
) {
}
