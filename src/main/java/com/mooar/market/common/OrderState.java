package com.mooar.market.common;

/**
 * 挂单状态
 */
public final class OrderState {

    /** 在售 */
    public static final int ACTIVE = 0;
    /** 已售出（部分成交时数量会递减，全部吃完才置此状态） */
    public static final int SOLD = 1;
    /** 已取消 */
    public static final int CANCELED = 2;

    private OrderState() {
    }
}
