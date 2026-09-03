package com.qinghuan.config.oss;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("阿里云 OSS 配置")
class AliyunOssConfigTest {

    @Test
    @DisplayName("未设置环境变量时应使用可解析的本地占位配置")
    void ossClient_shouldStartWithApplicationDefaults() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(
                loader.load(
                        "application.yml",
                        new FileSystemResource("src/main/resources/application.yml")
                ).get(0)
        );
        PropertySourcesPropertyResolver resolver =
                new PropertySourcesPropertyResolver(propertySources);

        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setEndpoint(resolver.getProperty("aliyun.oss.endpoint"));
        properties.setRegion(resolver.getProperty("aliyun.oss.region"));
        properties.setAccessKeyId(resolver.getProperty("aliyun.oss.access-key-id"));
        properties.setAccessKeySecret(resolver.getProperty("aliyun.oss.access-key-secret"));
        properties.setBucketName(resolver.getProperty("aliyun.oss.bucket-name"));

        assertEquals(
                "https://oss-cn-hangzhou.aliyuncs.com",
                properties.getEndpoint()
        );
        OSS ossClient = assertDoesNotThrow(
                () -> new AliyunOssConfig().ossClient(properties)
        );
        ossClient.shutdown();
    }
}
