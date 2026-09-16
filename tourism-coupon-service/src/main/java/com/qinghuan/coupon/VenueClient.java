package com.qinghuan.coupon;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 优惠券仅在创建快照或公开目录校验时读取景点服务。 */
@FeignClient(name = "venue-service", configuration = ServiceTokenFeignConfig.class)
public interface VenueClient {

    @GetMapping("/internal/venue/summary/{venueId}")
    ApiResponse<VenueSummaryDTO> getVenueSummary(@PathVariable("venueId") Long venueId);
}
