package nl.inholland.bankAppBackEnd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for API documentation and Swagger UI.
 * Provides a neat, consistent, and well-documented API explorer for consumers.
 * Includes security schemes for JWT authentication.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("Bank Application API")
                        .description("Comprehensive API documentation for the Sigma Banking Application. All endpoints are documented and conform to REST standards.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sigma Banking App")
                                .url("https://sigmabankappbackend.onrender.com")));
    }
}
