package com.projetoj.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projetoJOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestao operacional API")
                        .description("API REST corporativa para gerenciamento de usuarios, perfis, modulos e permissoes")
                        .version("v1")
                        .contact(new Contact().name("Gestao operacional")));
    }
}
