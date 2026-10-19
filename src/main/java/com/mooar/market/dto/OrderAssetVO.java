package com.mooar.market.dto;

import lombok.Data;

/**
 * 背包里的在售挂单：1155 一个订单可能挂多个，同一个道具可以有多个不同价格的订单
 */
@Data
public class OrderAssetVO {
    private Long orderId;
    /** 单价（GMT 分） */
    private Long price;
    /** 挂单数量（721 恒为 1） */
    private Long amount;
}
