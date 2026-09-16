package com.qinghuan.pojo.remote.venue;

/** 其他服务创建业务快照时读取的最小景点信息。 */
public record VenueSummaryDTO(Long venueId, String venueName, String venueAddress, boolean enabled) {
}
