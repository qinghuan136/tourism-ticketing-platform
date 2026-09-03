package com.qinghuan.config.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("阿里云 OSS 工具")
class OssUtilsTest {

    @Mock
    private OSS ossClient;

    private OssUtils ossUtils;

    @BeforeEach
    void setUp() {
        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setBucketName("tourism-test");
        properties.setEndpoint("https://oss-cn-hangzhou.aliyuncs.com");
        ossUtils = new OssUtils(ossClient, properties);
    }

    @Test
    void upload_shouldReturnObjectKeyAndPreserveMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "coverImage",
                "venue-cover.JPG",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectMetadata> metadataCaptor =
                ArgumentCaptor.forClass(ObjectMetadata.class);

        String objectKey = ossUtils.upload("/venue-covers/", file);

        verify(ossClient).putObject(
                eq("tourism-test"),
                objectKeyCaptor.capture(),
                any(InputStream.class),
                metadataCaptor.capture()
        );
        assertEquals(objectKeyCaptor.getValue(), objectKey);
        assertTrue(objectKey.matches(
                "venue-covers/\\d{4}/\\d{2}/\\d{2}/[a-f0-9]{32}\\.jpg"
        ));
        assertEquals(3, metadataCaptor.getValue().getContentLength());
        assertEquals("image/jpeg", metadataCaptor.getValue().getContentType());
    }

    @Test
    void delete_shouldDeleteObjectFromConfiguredBucket() {
        ossUtils.delete("venue-covers/old-cover.jpg");

        verify(ossClient).deleteObject(
                "tourism-test",
                "venue-covers/old-cover.jpg"
        );
    }

    @Test
    void getPublicUrl_shouldBuildBucketUrlFromObjectKey() {
        assertEquals(
                "https://tourism-test.oss-cn-hangzhou.aliyuncs.com/venue/cover.jpg",
                ossUtils.getPublicUrl("venue/cover.jpg")
        );
    }
}
