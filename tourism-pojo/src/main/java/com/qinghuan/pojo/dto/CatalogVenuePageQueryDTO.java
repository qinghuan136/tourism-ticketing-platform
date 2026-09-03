package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 游客端可预约景点分页查询条件。 */
@Getter
@Setter
public class CatalogVenuePageQueryDTO extends PageQuery {

    @Schema(description = "按景点名称或地址模糊搜索，可不填写", example = "科技馆")
    @Size(max = 100, message = "搜索关键词不能超过 100 个字符")
    private String keyword;
}
