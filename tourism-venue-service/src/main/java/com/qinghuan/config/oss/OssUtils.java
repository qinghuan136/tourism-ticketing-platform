package com.qinghuan.config.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * OSS 对象上传基础工具。图片格式和大小由调用方在业务层校验。
 */
@Component
public class OssUtils {

    private static final DateTimeFormatter DATE_PATH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final OSS ossClient;
    private final AliyunOssProperties properties;

    public OssUtils(OSS ossClient, AliyunOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    /**
     * 上传文件并返回应保存到数据库的 objectKey。
     */
    public String upload(String directory, MultipartFile file) {
        // 构建 objectKey
        String objectKey = buildObjectKey(directory, file.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        if (StringUtils.hasText(file.getContentType())) {
            metadata.setContentType(file.getContentType());
        }

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(
                    properties.getBucketName(),
                    objectKey,
                    inputStream,
                    metadata
            );
            return objectKey;
        } catch (IOException exception) {
            throw new UncheckedIOException("读取上传文件失败", exception);
        }
    }

    /**
     * 删除不再使用的对象，例如景点封面替换成功后的旧图片。
     */
    public void delete(String objectKey) {
        if (StringUtils.hasText(objectKey)) {
            ossClient.deleteObject(properties.getBucketName(), objectKey);
        }
    }

    /**
     * 将数据库保存的 objectKey 转换成公共读地址。
     * 当前 MVP 按阿里云 OSS 标准域名拼接，适用于公开读 Bucket。
     */
    public String getPublicUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        String bucketEndpoint = endpoint.replace(
                "://", "://" + properties.getBucketName() + ".");
        return bucketEndpoint + "/" + objectKey.replaceAll("^/+", "");
    }

    private String buildObjectKey(String directory, String originalFilename) {
        String normalizedDirectory = normalizeDirectory(directory);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String generatedFilename = UUID.randomUUID().toString().replace("-", "");

        if (StringUtils.hasText(extension)) {
            generatedFilename += "." + extension.toLowerCase(Locale.ROOT);
        }

        String objectPath = datePath + "/" + generatedFilename;
        return normalizedDirectory.isEmpty()
                ? objectPath
                : normalizedDirectory + "/" + objectPath;
    }

    private String normalizeDirectory(String directory) {
        if (!StringUtils.hasText(directory)) {
            return "";
        }
        return directory.replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
