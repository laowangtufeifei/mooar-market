package com.mooar.market.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    /** 第三方(FSL)用户 id，扫块先建的用户为空串，登录后回填 */
    private String fid;
    private String name;
    private String avatar;
    private String email;
    private String stepnEmail;
    /** 钱包地址(小写)，只由扫块回填，登录时可能还没有 */
    private String address;
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
