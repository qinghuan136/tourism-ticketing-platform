package com.qinghuan.venue;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.VenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "景点管理", description = "运营者查看和维护当前所属景点资料")
@SecurityRequirement(name = "BearerAuth")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /*
     * 获取当前景点信息
     */
    @GetMapping("/operator/venue")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "获取当前景点信息")
    public ApiResponse<VenueVO> getCurrentVenue() {
        return ApiResponse.success(venueService.getCurrentVenue());
    }

    /*
     * 修改当前景点信息
     */
    @PutMapping(value = "/operator/venue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole(AccountRole.OPERATOR)
    @Operation(
            summary = "修改当前景点信息",
            description = "使用 multipart/form-data 提交景点资料；coverImage 可选，未上传时保留原封面",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = VenueUpdateForm.class))))
    public ApiResponse<Void> updateCurrentVenue(
            @Parameter(hidden = true) @Valid @ModelAttribute VenueUpdateDTO newVenue,
            @Parameter(hidden = true) @RequestParam(value = "coverImage", required = false) MultipartFile newCover) {
        venueService.updateCurrentVenue(newVenue, newCover);
        return ApiResponse.success();
    }

    /** 仅用于描述 multipart/form-data 的完整 OpenAPI 请求结构。 */
    @Schema(name = "VenueUpdateForm")
    public static class VenueUpdateForm extends VenueUpdateDTO {
        @Schema(description = "景点封面图片，可不上传", type = "string", format = "binary")
        public MultipartFile coverImage;
    }

}
