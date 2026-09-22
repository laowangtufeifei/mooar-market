package com.mooar.market.entity;

import lombok.Data;

@Data
public class BalanceLog {
    private Long id;
    private Long userId;
    private Long itemId;
    private Long changeAmount;
    private Long balanceAfter;
    private Integer bizType;
    private Long refId;
    private Long createdAt;
}
