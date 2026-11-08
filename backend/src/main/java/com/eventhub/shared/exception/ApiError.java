package com.eventhub.shared.exception;

import java.time.Instant;

/**
 * Envelope unico de erro da API. Nunca inclui stack trace nem detalhe interno.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId) {
}
