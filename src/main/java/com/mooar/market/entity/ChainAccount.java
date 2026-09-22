package com.mooar.market.entity;

import lombok.Data;

@Data
public class ChainAccount {
    private Long id;
    private String address;
    private String encryptedPrivateKey;
    private Long chainId;
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
