package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.VenueStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 运营者修改景点资料时允许提交的字段。 */
@Getter
@Setter
@Schema(description = "运营者修改当前景点资料的参数；与可选封面文件一起使用 multipart/form-data 提交")
public class VenueUpdateDTO {

    @Schema(description = "景点名称", example = "海湾科技馆")
    @NotBlank(message = "景点名称不能为空")
    @Size(max = 100, message = "景点名称不能超过100个字符")
    private String name;

    @Schema(description = "景点详细地址", example = "广州市番禺区海湾大道 88 号")
    @NotBlank(message = "景点地址不能为空")
    @Size(max = 255, message = "景点地址不能超过255个字符")
    private String address;

    @Schema(description = "景点展示简介")
    @Size(max = 2000, message = "景点简介不能超过2000个字符")
    private String description;

    @Schema(description = "景点运营状态", example = "ENABLED")
    @NotNull(message = "运营状态不能为空")
    private VenueStatus status;

    @Schema(description = "WGS84 经度，可不填写", example = "113.26436")
    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private BigDecimal longitude;

    @Schema(description = "WGS84 纬度，可不填写", example = "23.12908")
    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private BigDecimal latitude;
}
