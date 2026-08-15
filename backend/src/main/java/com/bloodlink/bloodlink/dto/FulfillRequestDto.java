package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.Min;

public record FulfillRequestDto(
    @Min(1) int unitsFulfilled
) {}
