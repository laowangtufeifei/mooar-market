package com.mooar.market.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssetVO {
    private Long itemId;
    private String name;
    private Integer type;
    private String image;
    private Long available;
    private Long frozen;
    private List<Long> nftIds = new ArrayList<>();
}
