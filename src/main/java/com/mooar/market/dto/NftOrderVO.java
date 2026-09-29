package com.mooar.market.dto;

import lombok.Data;

@Data
public class NftOrderVO {
    private Long orderId;
    private Long nftId;
    private Long tokenId;
    private String image;
    private String metadata;
    private Long price;
}
