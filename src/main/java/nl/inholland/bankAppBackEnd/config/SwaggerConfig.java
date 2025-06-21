package nl.inholland.bankAppBackEnd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for API documentation and Swagger UI.
 * Provides a neat, consistent, and well-documented API explorer for consumers.
 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank App API Documentation")
                        .version("1.0.0")
                        .description("Comprehensive API documentation for the Bank App project. All endpoints are documented and conform to REST standards."));
    }
}

