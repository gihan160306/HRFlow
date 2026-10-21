
package com.hrflow.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Cấu hình springdoc-openapi: hiển thị Swagger UI với bearer JWT.
 * Cấu hình này phục vụ tra cứu và thử API ở môi trường dev.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI hrflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HRFlow API")
                        .version("1.0.0")
                        .description("Hệ thống quản lý nhân sự, chấm công, nghỉ phép, OT và tính lương. "
                                + "Mọi số tiền dùng VND (số nguyên)."))
                .servers(List.of(new Server()
                        .url("/")
                        .description("HRFlow API")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
