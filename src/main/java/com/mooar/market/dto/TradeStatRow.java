package com.mooar.market.dto;

import lombok.Data;

/**
 * 成交统计的聚合结果（按 item + 天 分组）
 */
@Data
public class TradeStatRow {
    private Long itemId;
    /** 按统计时区折算出的 epoch day，Java 侧再转成日期/月份 */
    private Long dayIndex;
    private Long tradeCount;
    private Long tradeAmount;
    private Long tradeValue;
}
