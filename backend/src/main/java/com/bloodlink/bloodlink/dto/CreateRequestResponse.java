package com.bloodlink.bloodlink.dto;

import java.util.List;
import java.util.UUID;

public record CreateRequestResponse(
    UUID emergencyId,
    String status,
    int donorsNotified,
    List<RankedDonorDto> rankedDonors
) {
}
