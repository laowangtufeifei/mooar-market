package com.mooar.market.dto;

import lombok.Data;

/**
 * 单个统计周期（某天或某月）的成交数据，只返回有成交的周期
 */
@Data
public class TradeStatPoint {
    /** 天：yyyy-MM-dd；月：yyyy-MM */
    private String period;
    private Long count;
    /** 成交量（笔数×数量，721 等于笔数） */
    private Long amount;
    /** 成交额（GMT 分） */
    private Long value;
}
