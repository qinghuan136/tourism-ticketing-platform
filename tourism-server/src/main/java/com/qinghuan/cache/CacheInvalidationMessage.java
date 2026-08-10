package com.qinghuan.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知所有实例删除某个本地缓存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationMessage {

    /**
     * VENUE_DETAIL 或 SESSION_STATIC。
     */
    private String cacheName;

    /**
     * venueId 或 sessionId。
     */
    private Long id;
}