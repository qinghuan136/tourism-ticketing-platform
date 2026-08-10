package com.qinghuan.catalog;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueByNameQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueQueryDTO;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.NearbyVenueVO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SellableSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@Tag(name = "Catalog", description = "游客端公开可售内容查询")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/public/venues")
    @Operation(summary = "分页查询可预约景点")
    public ApiResponse<PageResult<CatalogVenueVO>> pageSellableVenues(
            @Valid CatalogVenuePageQueryDTO queryDTO) {
        return ApiResponse.success(catalogService.pageSellableVenues(queryDTO));
    }

    @GetMapping("/public/venues/nearby")
    @Operation(summary = "根据当前坐标查询附近景点")
    public ApiResponse<List<NearbyVenueVO>> listNearbyVenues(
            @Valid NearbyVenueQueryDTO queryDTO) {
        return ApiResponse.success(catalogService.listNearbyVenues(queryDTO));
    }

    @GetMapping("/public/venues/nearby/by-name")
    @Operation(summary = "根据地点名称查询附近景点")
    public ApiResponse<List<NearbyVenueVO>> listNearbyVenuesByName(
            @Valid NearbyVenueByNameQueryDTO queryDTO) {
        return ApiResponse.success(catalogService.listNearbyVenuesByName(queryDTO));
    }

    @GetMapping("/public/venues/{venueId}")
    @Operation(summary = "获取景点详情")
    public ApiResponse<CatalogVenueVO> getVenue(
            @PathVariable @Positive(message = "景点ID必须为正数") Long venueId) {
        return ApiResponse.success(catalogService.getVenue(venueId));
    }

    @GetMapping("/public/venues/{venueId}/sessions")
    @Operation(summary = "按日期查询可展示场次")
    public ApiResponse<List<SellableSessionVO>> listSellableSessions(
            @PathVariable @Positive(message = "景点ID必须为正数") Long venueId,
            @RequestParam
            @NotNull(message = "参观日期不能为空")
            @FutureOrPresent(message = "参观日期不能早于今天")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate) {
        return ApiResponse.success(catalogService.listSellableSessions(venueId, visitDate));
    }
}
