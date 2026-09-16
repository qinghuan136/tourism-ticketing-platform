package com.qinghuan.pojo.remote.user;

/**
 * 用户服务提供给订单服务的参观人下单快照。
 * 只包含归属校验和订单明细快照所需字段，不暴露用户服务内部实体。
 */
public record VisitorForOrderDTO(
        Long id,
        String name,
        String idType,
        String idNumber,
        String fingerprint) {
}
