package com.mooar.market.dto;

/**
 * FSL 用户资料（登录时从 FSL 拉取）
 *
 * @param uid          FSL 用户 id
 * @param name         昵称
 * @param email        邮箱
 * @param stepnEmail   STEPN 邮箱
 * @param profileImage 头像
 * @param address      钱包地址，FSL 返回里带且配置了 addressField 时才有值，否则为 null
 */
public record FslUserInfo(long uid, String name, String email, String stepnEmail,
                          String profileImage, String address) {
}
