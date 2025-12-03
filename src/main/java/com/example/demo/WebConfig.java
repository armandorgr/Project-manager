package com.example.demo;

import com.example.demo.controller.resolvers.TokenResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final TokenResolver tokenResolver;

    public WebConfig(TokenResolver tokenResolver) {
        this.tokenResolver = tokenResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(tokenResolver);
    }

    private SecurityScheme cookieAccessScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("access_token")
                .description("Access token enviado en cookie HttpOnly llamada 'access_token'");
    }

    private SecurityScheme cookieRefreshScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("refresh_token")
                .description("Refresh token enviado en cookie HttpOnly llamada 'refresh_token'");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("CookieAccess", cookieAccessScheme())
                        .addSecuritySchemes("CookieRefresh", cookieRefreshScheme())
                        .addSecuritySchemes("", null)
                )
                // indicar que las operaciones por defecto requieren la cookie de acceso
                .addSecurityItem(new SecurityRequirement().addList("CookieAccess"))
                .info(new Info()
                        .title("Project Manager API")
                        .version("1.0")
                        .description("API documentada: autenticación mediante cookies HttpOnly"));
    }
}
