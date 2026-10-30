package ru.inno.itpd.gateway;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info().title("LLM Gateway (by Absolute Cinema)").version("0.0.1"))
                .components(new Components().addSecuritySchemes("HTTPBearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
    }
}