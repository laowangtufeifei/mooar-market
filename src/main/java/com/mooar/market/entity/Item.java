package com.mooar.market.entity;

import lombok.Data;

@Data
public class Item {
    private Long id;
    private String name;
    private Integer type;
    private String contract;
    private Long tokenId;
    private Long chainScale;
    private String imagePrefix;
    private String traitsJson;
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
