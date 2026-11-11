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
    TICKET_TYPE_NOT_FOUND,
    TICKET_SOLD_OUT,
    TICKET_LIMIT_EXCEEDED,
    SALES_CLOSED,
    TICKET_TYPE_HAS_SALES,
    INVALID_TICKET_TYPE,
    ORDER_NOT_FOUND,
    ORDER_NOT_PENDING,
    ORDER_EXPIRED,
    EMPTY_ORDER,
    EVENT_NOT_PUBLISHED,
    INVALID_QR_TOKEN,
    TICKET_ALREADY_USED,
    TICKET_NOT_FOUND,
    INVALID_PERIOD,
    FORBIDDEN,
    INTERNAL_ERROR
}
