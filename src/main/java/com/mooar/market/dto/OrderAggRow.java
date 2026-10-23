package com.mooar.market.dto;

import lombok.Data;

/**
 * 1155 挂单按道具聚合的结果：最低价 + 在售总量
 */
@Data
public class OrderAggRow {
    private Long itemId;
    private Long minPrice;
    private Long totalAmount;
    /** 挂单笔数 */
    private Integer listingCount;
}
