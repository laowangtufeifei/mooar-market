package com.mooar.market.entity;

import lombok.Data;

@Data
public class UserAsset {
    private Long id;
    private Long userId;
    private Long itemId;
    private Long available;
    private Long frozen;
    private Long createdAt;
    private Long updatedAt;
}
