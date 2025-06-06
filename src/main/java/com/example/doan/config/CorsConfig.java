package com.example.doan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // Thêm cả localhost và 127.0.0.1
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Thêm PATCH method
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization") // Cho phép client đọc header Authorization
                        .allowCredentials(true)
                        .maxAge(3600L); // Cache CORS config trong 1 giờ
            }
        };
    }
}