package com.mooar.market.entity;

import lombok.Data;

@Data
public class Nft {
    private Long id;
    private Long itemId;
    private String contract;
    private Long tokenId;
    private Long ownerId;
    private Integer state;
    private Long orderId;
    private String image;
    private String metadata;
    private Long createdAt;
    private Long updatedAt;
}
