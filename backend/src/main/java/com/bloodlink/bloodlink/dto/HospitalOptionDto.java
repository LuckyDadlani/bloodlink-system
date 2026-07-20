package com.bloodlink.bloodlink.dto;

import java.util.UUID;

public record HospitalOptionDto(
    UUID hospitalId,
    String hospitalName,
    String city,
    String state
) {
}
