package com.qinghuan.config.oss;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云 OSS 连接信息，敏感凭证由环境变量提供。
 */
@Data
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String region;

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String accessKeySecret;

    @NotBlank
    private String bucketName;

}
