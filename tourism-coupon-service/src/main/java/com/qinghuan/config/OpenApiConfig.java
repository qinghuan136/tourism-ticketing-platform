package com.qinghuan.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI tourismOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("文旅预约票务平台 API")
                        .description("""
                                多景点分时预约、活动票务与入馆核销平台接口文档。

                                - 所有响应统一包含 `code`、`message` 和 `data`；失败时 `data` 通常为空。
                                - 标注 BearerAuth 的接口需要在 `Authorization` 请求头中携带 `Bearer <JWT>`。
                                - `/public/**`、`/auth/login` 和 `/auth/register` 无需登录。
                                - 分页页码从 1 开始；多数接口使用 `page` 和 `size`，工作人员分页使用 `page` 和 `pageSize`，具体以接口参数为准。
                                - 常见失败状态：400 参数或业务请求不合法，401 未登录或令牌失效，403 权限不足，404 资源不存在，409 业务状态冲突，500 服务内部错误。
                                """)
                        .version("1.0.0"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(SECURITY_SCHEME_NAME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                );
    }
}
