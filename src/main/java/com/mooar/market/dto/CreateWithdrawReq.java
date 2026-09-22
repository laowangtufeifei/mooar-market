package com.mooar.market.dto;

import lombok.Data;

@Data
public class CreateWithdrawReq {
    private Long itemId;
    private Long amount;
    private Long nftId;
    private String toAddr;
}
