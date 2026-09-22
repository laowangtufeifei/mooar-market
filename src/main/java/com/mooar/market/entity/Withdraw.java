package com.mooar.market.entity;

import lombok.Data;

@Data
public class Withdraw {
    private Long id;
    private Long userId;
    private Long itemId;
    private Long nftId;
    private Long amount;
    private Long fee;
    private String toAddr;
    private String txHash;
    private Integer state;
    private Integer retryCount;
    private Long nonce;
    private String errMsg;
    private Long createdAt;
    private Long updatedAt;
}
