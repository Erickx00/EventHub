package com.eventhub.event.dto;

import java.time.Instant;

import com.eventhub.event.entity.Category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PUT e substituicao completa: todos os campos vem no corpo. Sem @Future porque
 * um evento publicado e editado pode ter data de inicio no passado recente.
 */
public record UpdateEventRequest(
        @NotBlank(message = "obrigatorio") @Size(min = 3, max = 160, message = "entre 3 e 160 caracteres") String name,

        @Size(max = 5000, message = "no maximo 5000 caracteres") String description,

        @NotNull(message = "obrigatorio") Category category,

        @NotBlank(message = "obrigatorio") @Size(max = 120, message = "no maximo 120 caracteres") String city,

        @Size(max = 160, message = "no maximo 160 caracteres") String venue,

        @NotNull(message = "obrigatorio") Instant startDate,

        @NotNull(message = "obrigatorio") Instant endDate) {
}
