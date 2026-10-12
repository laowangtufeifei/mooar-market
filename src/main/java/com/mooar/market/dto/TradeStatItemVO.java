package com.mooar.market.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个 item 在统计区间内的成交汇总
 */
@Data
public class TradeStatItemVO {
    private Long itemId;
    private String itemName;
    private Integer type;
    /** 成交笔数 */
    private Long tradeCount;
    /** 成交量 */
    private Long tradeAmount;
    /** 成交额（GMT 分） */
    private Long tradeValue;
    /** 明细，只含有成交的周期，顺序与 axes.periods 一致 */
    private List<TradeStatPoint> points = new ArrayList<>();
}
