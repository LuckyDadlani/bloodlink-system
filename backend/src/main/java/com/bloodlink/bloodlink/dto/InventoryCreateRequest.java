package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InventoryCreateRequest(
    @NotNull UUID bloodBankId,
    @NotBlank String bloodGroup,
    @NotBlank String componentType,
    @Min(0) int unitsAvailable,
    @NotNull UUID createdBy
) {
}
