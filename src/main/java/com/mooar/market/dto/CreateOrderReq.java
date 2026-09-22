package com.mooar.market.dto;

import lombok.Data;

@Data
public class CreateOrderReq {
    private Long itemId;
    private Long amount;
    private Long nftId;
    private Long price;
}
