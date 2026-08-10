package com.qinghuan.venue;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.VenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "VenueController", description = "景点管理")
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
    @PutMapping("/operator/venue")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "修改当前景点信息")
    public ApiResponse<Void> updateCurrentVenue(
            @Valid @ModelAttribute VenueUpdateDTO newVenue,
            @RequestParam(value = "coverImage", required = false) MultipartFile newCover) {
        venueService.updateCurrentVenue(newVenue, newCover);
        return ApiResponse.success();
    }

}
