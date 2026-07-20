package com.bloodlink.bloodlink.dto;

import java.util.UUID;

public record LoginResponse(
    UUID userId,
    String fullName,
    String email,
    UUID bloodBankId,
    String bloodBankName,
    String city,
    String state
) {
}
