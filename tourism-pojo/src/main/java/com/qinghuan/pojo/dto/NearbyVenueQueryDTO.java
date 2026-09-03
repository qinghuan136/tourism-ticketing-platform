package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 根据游客当前坐标查询附近景点。 */
@Getter
@Setter
public class NearbyVenueQueryDTO {

    @Schema(description = "当前位置经度", example = "113.26436")
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private Double longitude;

    @Schema(description = "当前位置纬度", example = "23.12908")
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private Double latitude;

    @Schema(description = "搜索半径，单位公里", example = "5", defaultValue = "5")
    @DecimalMin(value = "0.1", message = "搜索半径至少为0.1公里")
    @DecimalMax(value = "50", message = "搜索半径不能超过50公里")
    private Double radiusKm = 5.0;

    @Schema(description = "最多返回的景点数量", example = "20", defaultValue = "20")
    @Min(value = 1, message = "返回数量至少为1")
    @Max(value = 100, message = "返回数量不能超过100")
    private Integer limit = 20;
}
