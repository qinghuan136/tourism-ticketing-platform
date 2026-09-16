package com.qinghuan.config.oss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 创建应用内复用的 OSS Client，并在应用关闭时释放连接资源。
 */
@Configuration
@EnableConfigurationProperties(AliyunOssProperties.class)
public class AliyunOssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(AliyunOssProperties properties) {
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        ClientBuilderConfiguration clientConfiguration = new ClientBuilderConfiguration();
        clientConfiguration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientConfiguration)
                .region(properties.getRegion())
                .build();
    }
}
