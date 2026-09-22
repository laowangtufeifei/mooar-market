package com.mooar.market.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String fid;
    private String email;
    private String address;
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
