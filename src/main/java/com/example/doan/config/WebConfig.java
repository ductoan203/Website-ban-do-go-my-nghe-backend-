package com.example.doan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối đến thư mục uploads
        String uploadDir = Paths.get("uploads").toFile().getAbsolutePath();

        // Khi truy cập http://localhost:8080/doan/uploads/abc.jpg thì Spring Boot sẽ trả file từ uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");

    }
}
