package com.mooar.market.entity;

import lombok.Data;

@Data
public class Order {
    private Long id;
    private Long sellerId;
    private Long itemId;
    private Long nftId;
    private Long amount;
    private Long price;
    private Integer state;
    private Long buyerId;
    private Long createdAt;
    private Long updatedAt;
}
