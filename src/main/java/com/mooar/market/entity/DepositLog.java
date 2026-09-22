package com.mooar.market.entity;

import lombok.Data;

@Data
public class DepositLog {
    private Long id;
    private String txHash;
    private Integer logIndex;
    private Long blockNumber;
    private Long blockTime;
    private String fromAddr;
    private String toAddr;
    private String contract;
    private Long tokenId;
    private String amount;
    private String jsonData;
    private Long createdAt;
}
