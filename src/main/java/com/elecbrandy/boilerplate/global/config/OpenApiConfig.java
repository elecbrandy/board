package com.elecbrandy.boilerplate.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Boilerplate API Documentation")
                .description("Spring Boot 보일러플레이트 프로젝트 API 문서입니다.<br>" +
                        "**인증이 필요한 API**는 우측 상단의 `Authorize` 버튼을 눌러 발급받은 Access Token을 입력해 주세요.")
                .version("v1.0.0");

        // JWT 인증 설정
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        // 서버 환경 설정 (Swagger UI에서 선택 가능)
        Server localServer = new Server().url("http://localhost:8080").description("Local Environment");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer)) // prodServer 추가 가능
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}