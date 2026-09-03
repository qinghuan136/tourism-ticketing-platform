package com.qinghuan.pojo.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("分页查询参数")
class PageQueryTest {

    @Test
    @DisplayName("默认从第一页开始且每页二十条")
    void defaults_shouldUseFirstPageAndTwentyItems() {
        PageQuery query = new PageQuery();

        assertAll(
                () -> assertEquals(1, query.getPage()),
                () -> assertEquals(20, query.getSize()),
                () -> assertEquals(0, query.offset())
        );
    }

    @Test
    @DisplayName("偏移量应根据页码和每页数量计算")
    void offset_shouldCalculateFromPageAndSize() {
        PageQuery query = new PageQuery();
        query.setPage(3);
        query.setSize(15);

        assertEquals(30, query.offset());
    }
}
