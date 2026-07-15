package com.hsf.hotel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path frontendDist = Paths.get("../frontend/dist").toAbsolutePath().normalize();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (java.nio.file.Files.exists(frontendDist)) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + frontendDist.toString() + "/")
                    .setCachePeriod(0);
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // No Thymeleaf views. All page routes handled by React Router via SPA fallback below.
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimiterInterceptor()).addPathPatterns("/api/**");
    }
}