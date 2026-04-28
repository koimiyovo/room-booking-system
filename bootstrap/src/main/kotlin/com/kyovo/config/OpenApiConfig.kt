package com.kyovo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig
{
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Room Booking System API")
                .description("API de gestion des réservations de salles")
                .version("1.0.0")
                .contact(Contact().name("Koimi Yovo").email("yovoedem33@gmail.com"))
        )
}
