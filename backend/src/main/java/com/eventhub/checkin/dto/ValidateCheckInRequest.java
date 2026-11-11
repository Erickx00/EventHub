package com.eventhub.checkin.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateCheckInRequest(@NotBlank(message = "obrigatorio") String qrToken) {
}
