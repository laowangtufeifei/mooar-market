package com.mooar.market.dto;

import lombok.Data;

/**
 * 挂单连表查询的投影结果（order LEFT JOIN nft）
 */
@Data
public class OrderRow {
    private Long orderId;
    private Long itemId;
    private Long sellerId;
    private Long nftId;
    private Long tokenId;
    private String nftImage;
    private Long amount;
    private Long price;
}
