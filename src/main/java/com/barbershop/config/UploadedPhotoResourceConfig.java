package com.barbershop.config;

import com.barbershop.service.ServicePhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UploadedPhotoResourceConfig implements WebMvcConfigurer {

    private final ServicePhotoStorage photoStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = photoStorage.getServicesDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/services/**")
                .addResourceLocations(location);
    }
}
