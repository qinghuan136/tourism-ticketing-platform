package com.qinghuan.visitor;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** 生成跨账号一致的参观人证件身份指纹。 */
@Component
public class VisitorFingerprintGenerator {

    public String generate(String idType, String idNumber) {
        String normalized = idType.trim().toUpperCase(Locale.ROOT)
                + ":"
                + idNumber.trim().toUpperCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
