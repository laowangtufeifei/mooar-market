package com.mooar.market.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * item 成交统计结果
 */
@Data
public class TradeStatsVO {
    /** day / month */
    private String granularity;
    private String from;
    private String to;
    /** 完整时间轴，前端按它对齐 points（points 只给有数据的周期） */
    private List<String> periods = new ArrayList<>();
    /** 按成交笔数倒序 */
    private List<TradeStatItemVO> items = new ArrayList<>();
}
