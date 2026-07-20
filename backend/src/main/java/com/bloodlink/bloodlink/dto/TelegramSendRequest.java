package com.bloodlink.bloodlink.dto;

import jakarta.validation.constraints.NotBlank;

public record TelegramSendRequest(
    @NotBlank String message,
    String chatId
) {
}
