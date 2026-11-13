package ai.freightfox.optimizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${APP_CORS_ORIGIN:http://localhost:3000}") private String allowedOrigin;
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigin).allowedMethods("GET","POST","OPTIONS").allowedHeaders("Content-Type");
    }
}
