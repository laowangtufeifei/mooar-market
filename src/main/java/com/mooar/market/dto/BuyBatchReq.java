package com.mooar.market.dto;

import lombok.Data;

@Data
public class BuyBatchReq {
    private Long itemId;
    private Long amount;
    private Long expectedTotal;
}
