package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 根据地点名称查询附近景点。 */
@Getter
@Setter
public class NearbyVenueByNameQueryDTO {

    @Schema(description = "用于地理编码的地点名称", example = "广州塔")
    @NotBlank(message = "地点名称不能为空")
    @Size(max = 100, message = "地点名称不能超过100个字符")
    private String name;

    /** 可选城市，用于减少同名地点的歧义。 */
    @Schema(description = "可选城市，用于减少同名地点歧义", example = "广州")
    @Size(max = 50, message = "城市名称不能超过50个字符")
    private String city;

    @Schema(description = "搜索半径，单位公里", example = "5", defaultValue = "5")
    @DecimalMin(value = "0.1", message = "搜索半径至少为0.1公里")
    @DecimalMax(value = "50", message = "搜索半径不能超过50公里")
    private Double radiusKm = 5.0;

    @Schema(description = "最多返回的景点数量", example = "20", defaultValue = "20")
    @Min(value = 1, message = "返回数量至少为1")
    @Max(value = 100, message = "返回数量不能超过100")
    private Integer limit = 20;
}
