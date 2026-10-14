package com.mooar.market.dto;

import lombok.Data;

@Data
public class LoginVO {
    private String sessionId;
    private Long userId;
    /** FSL 用户 id */
    private String fid;
    private String name;
    private String avatar;
    private String email;
    private String stepnEmail;
}
