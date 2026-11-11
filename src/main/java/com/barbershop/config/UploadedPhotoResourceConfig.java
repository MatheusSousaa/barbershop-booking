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
        String uploadLocation = asDirectoryLocation(photoStorage.getUploadDirectory());
        String servicesLocation = asDirectoryLocation(photoStorage.getServicesDirectory());
        registry.addResourceHandler("/uploads/services/**")
                .addResourceLocations(servicesLocation);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

    private String asDirectoryLocation(java.nio.file.Path directory) {
        String location = directory.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        return location;
    }
}
