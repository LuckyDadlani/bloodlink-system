package com.bloodlink.bloodlink.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RankedDonorDto(
    UUID donorId,
    BigDecimal score
) {
}
