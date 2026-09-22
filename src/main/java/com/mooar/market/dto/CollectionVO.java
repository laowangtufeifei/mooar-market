package com.mooar.market.dto;

import lombok.Data;

@Data
public class CollectionVO {
    private Long itemId;
    private String name;
    private Integer type;
    private String image;
    private Long minPrice;
    private Long stock;
    private Integer listingCount;
}
