package com.mooar.market.dto;

import lombok.Data;

/**
 * 1155 挂单聚合结果：最低价 + 在售总量
 */
@Data
public class OrderAggRow {
    private Long minPrice;
    private Long totalAmount;
}
