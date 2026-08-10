package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.VenueStatus;
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
public class VenueUpdateDTO {

    @NotBlank(message = "景点名称不能为空")
    @Size(max = 100, message = "景点名称不能超过100个字符")
    private String name;

    @NotBlank(message = "景点地址不能为空")
    @Size(max = 255, message = "景点地址不能超过255个字符")
    private String address;

    @Size(max = 2000, message = "景点简介不能超过2000个字符")
    private String description;

    @NotNull(message = "运营状态不能为空")
    private VenueStatus status;

    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private BigDecimal latitude;
}
