package com.eventhub.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Excecao de dominio com codigo estavel e status HTTP. O service lanca; o
 * GlobalExceptionHandler traduz para o envelope de erro da API.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    private BusinessException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BusinessException conflict(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.CONFLICT, message);
    }

    public static BusinessException badRequest(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.UNAUTHORIZED, message);
    }

    public static BusinessException notFound(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException forbidden(ErrorCode code, String message) {
        return new BusinessException(code, HttpStatus.FORBIDDEN, message);
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
