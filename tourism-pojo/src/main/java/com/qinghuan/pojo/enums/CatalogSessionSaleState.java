package com.qinghuan.pojo.enums;

/**
 * 游客端场次销售状态。
 */
public enum CatalogSessionSaleState {

    /** 场次已经发布，但还没到bookingStartAt。 */
    NOT_STARTED,

    /** 当前处于预约窗口内，而且还有可售库存。 */
    ON_SALE,

    /** 当前处于预约窗口内，但场次或票种已经没有库存。 */
    SOLD_OUT
}