package com.kyovo.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig
{
    @Bean
    fun modelResolver(): ModelResolver
    {
        return ModelResolver(ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE))
    }

    @Bean
    fun openAPI(): OpenAPI
    {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Room Booking System API")
                    .description("API for managing room bookings")
                    .version("1.0.0")
                    .contact(Contact().name("Koimi Yovo").email("yovoedem33@gmail.com"))
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
    }
}
