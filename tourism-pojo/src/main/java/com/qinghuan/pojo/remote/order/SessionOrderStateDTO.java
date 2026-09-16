package com.qinghuan.pojo.remote.order;

/** 场次服务修改或删除场次前需要的订单占用状态。 */
public record SessionOrderStateDTO(boolean hasOrders, boolean hasUnresolvedOrders) {
}
