package com.qinghuan.common.response;

import com.qinghuan.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("统一响应")
class ApiResponseTest {

    @Test
    @DisplayName("成功响应应包含数据和成功码")
    void success_shouldContainDataAndSuccessCode_whenDataProvided() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertAll(
                () -> assertEquals(ErrorCode.SUCCESS.getCode(), response.code()),
                () -> assertEquals(ErrorCode.SUCCESS.getMessage(), response.message()),
                () -> assertEquals("payload", response.data())
        );
    }

    @Test
    @DisplayName("失败响应应使用指定错误码和消息")
    void failure_shouldContainSpecifiedError_whenCustomMessageProvided() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.CONFLICT, "库存不足");

        assertAll(
                () -> assertEquals(ErrorCode.CONFLICT.getCode(), response.code()),
                () -> assertEquals("库存不足", response.message()),
                () -> assertNull(response.data())
        );
    }
}
