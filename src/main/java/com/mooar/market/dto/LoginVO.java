package com.mooar.market.dto;

import lombok.Data;

@Data
public class LoginVO {
    private String sessionId;
    private Long userId;
    private String fid;
    private String address;
    private String email;
}
