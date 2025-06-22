package com.spkt.libraSys.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "AdminAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SecurityScheme(
        name = "ManageAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SecurityScheme(
        name = "UserAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Documentation")
                        .version("v1.0")
                        .description("API Documentation for Example Application"))

                // Thêm cấu hình bảo mật cho từng loại API tùy theo từng security scheme
                .addSecurityItem(new SecurityRequirement().addList("AdminAuth"))
                .addSecurityItem(new SecurityRequirement().addList("ManageAuth"))
                .addSecurityItem(new SecurityRequirement().addList("UserAuth"))

                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("AdminAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("AdminAuth")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(In.HEADER)) // Định rõ vị trí của token (HEADER)
                        .addSecuritySchemes("ManageAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("ManageAuth")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(In.HEADER)) // Định rõ vị trí của token (HEADER)
                        .addSecuritySchemes("UserAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("UserAuth")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(In.HEADER))); // Định rõ vị trí của token (HEADER)
    }
}
