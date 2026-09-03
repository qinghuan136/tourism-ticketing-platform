package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Schema(description = "工作人员账号分页筛选条件")
public class UserAccountPageQueryDTO {
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer page = 1;
    @Schema(description = "每页数量", example = "20", defaultValue = "20")
    private Integer pageSize = 20;
    @Schema(description = "登录名或姓名关键字", example = "张")
    private String keyword;
    @Schema(description = "账号状态筛选")
    private String status;
    @Schema(description = "角色编码筛选；工作人员列表通常使用 STAFF")
    private String roleCode;
    @Schema(description = "所属景点 ID；实际数据范围仍由当前运营者权限限制")
    private Long venueId;
}
