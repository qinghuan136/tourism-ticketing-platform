package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 新建或修改草稿场次的请求参数。
 */
@Getter
@Setter
@Schema(description = "新建场次或修改草稿场次的参数")
public class SessionWriteDTO {

    @Schema(description = "参观日期", example = "2026-10-01")
    @NotNull(message = "参观日期不能为空")
    private LocalDate visitDate;

    @Schema(description = "场次开始时间", example = "09:00:00")
    @NotNull(message = "场次开始时间不能为空")
    private LocalTime startTime;

    @Schema(description = "场次结束时间", example = "11:00:00")
    @NotNull(message = "场次结束时间不能为空")
    private LocalTime endTime;

    @Schema(description = "开始接受预约的时间", example = "2026-09-01T10:00:00")
    @NotNull(message = "预约开始时间不能为空")
    private LocalDateTime bookingStartAt;

    @Schema(description = "停止接受预约的时间", example = "2026-09-30T18:00:00")
    @NotNull(message = "预约结束时间不能为空")
    private LocalDateTime bookingEndAt;

    @Schema(description = "场次总容量；各票种配额之和不能超过该值", example = "500")
    @NotNull(message = "场次容量不能为空")
    @Positive(message = "场次容量必须大于0")
    private Integer totalCapacity;

    @Schema(description = "本场次的票种、售价与发行配额")
    @Valid
    @NotEmpty(message = "至少需要配置一个票种")
    private List<SessionTicketTypeConfigDTO> ticketTypes;
}
