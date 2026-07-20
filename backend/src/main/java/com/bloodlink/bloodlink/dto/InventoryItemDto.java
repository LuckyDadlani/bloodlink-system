package com.bloodlink.bloodlink.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemDto(
    UUID inventoryId,
    String bloodGroup,
    String componentType,
    int unitsAvailable,
    Instant lastUpdatedAt,
    UUID lastUpdatedBy
) {
}
