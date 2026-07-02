package com.petrolbunk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the Angular frontend to call the API.
 * Local dev uses http://localhost:4200. In production, set the env var
 * FRONTEND_ORIGIN to your deployed frontend URL (e.g. https://nandha.vercel.app).
 * Multiple origins can be comma-separated.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_ORIGIN:http://localhost:4200}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = frontendOrigin.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
