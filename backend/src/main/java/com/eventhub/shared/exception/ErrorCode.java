package com.eventhub.shared.exception;

/**
 * Codigos estaveis de erro. O frontend pode reagir a eles sem depender do texto
 * da mensagem, que pode mudar ou ser traduzido.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    EMAIL_ALREADY_USED,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    REFRESH_TOKEN_EXPIRED,
    PASSWORD_MISMATCH,
    USER_NOT_FOUND,
    EVENT_NOT_FOUND,
    EVENT_NOT_EDITABLE,
    EVENT_NOT_PUBLISHABLE,
    INVALID_PERIOD,
    FORBIDDEN,
    INTERNAL_ERROR
}
