package com.qinghuan.pojo.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("分页响应")
class PageResultTest {

    @Test
    @DisplayName("响应内容应防止外部集合修改")
    void constructor_shouldDefensivelyCopyItems() {
        List<String> source = new ArrayList<>(List.of("venue"));

        PageResult<String> result = new PageResult<>(source, 1, 1, 20);
        source.add("ticket");

        assertEquals(List.of("venue"), result.items());
    }

    @Test
    @DisplayName("非法分页元数据应被拒绝")
    void constructor_shouldRejectInvalidPaginationMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PageResult<>(List.of(), -1, 0, 0)
        );
    }
}
