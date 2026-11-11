package com.eventhub.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

/**
 * Metadados do contrato. A especificacao e gerada a partir dos controllers/DTOs:
 * o documento nao pode divergir do codigo.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI eventHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventHub API")
                        .version("1.0.0")
                        .description("""
                                Plataforma de eventos, venda de ingressos e controle de entrada.

                                Acesso: use POST /api/auth/login para obter o access token e clique em
                                Authorize. Rotas publicas: /api/health, /api/auth/**, GET /api/events/**
                                e este documento.
                                """)
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
