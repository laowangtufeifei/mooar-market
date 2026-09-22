package com.mooar.market.entity;

import lombok.Data;

@Data
public class Trade {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Long nftId;
    private Long amount;
    private Long price;
    private Long total;
    private Long fee;
    private Long sellerId;
    private Long buyerId;
    private Long createdAt;
}
