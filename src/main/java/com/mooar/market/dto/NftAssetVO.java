package com.mooar.market.dto;

import lombok.Data;

@Data
public class NftAssetVO {
    private Long nftId;
    private Long tokenId;
    private String image;
    private String metadata;
    private Integer state;
    /** 挂单中的订单 id，非挂单为 null */
    private Long orderId;
    /** 挂单价（GMT 分），非挂单为 null */
    private Long price;
}
