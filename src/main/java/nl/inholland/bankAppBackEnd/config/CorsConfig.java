package nl.inholland.bankAppBackEnd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Allow all API endpoints
                .allowedOrigins("http://localhost:5173")  // Allow frontend URL (Vue app)
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Allow these HTTP methods
                .allowCredentials(true);  // Allow credentials like cookies or headers
    }
}
