package com.eventhub.shared.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Formato unico de pagina da API, independente do Page do Spring (que expoe
 * detalhes internos como pageable e sort).
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, R> PageResponse<R> from(Page<E> page, Function<E, R> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
