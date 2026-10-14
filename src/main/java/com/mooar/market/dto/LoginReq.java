package com.mooar.market.dto;

import lombok.Data;

@Data
public class LoginReq {
    /** FSL 授权码 */
    private String code;
}
