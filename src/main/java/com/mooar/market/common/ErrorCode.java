package com.mooar.market.common;

import lombok.Getter;

/**
 * 错误码。msg 一律用英文：它会随响应返回给客户端，也会被写入 withdraw.err_msg 等库表字段
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),
    PARAM_ERROR(400, "invalid parameter"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    INTERNAL_ERROR(500, "internal error"),

    LOGIN_TOKEN_ERROR(1001, "invalid login credential"),
    USER_NOT_FOUND(1002, "user not found"),
    FSL_API_ERROR(1003, "login service unavailable, please retry later"),

    ITEM_NOT_FOUND(2001, "item not found"),
    ASSET_NOT_ENOUGH(2002, "insufficient balance"),
    NFT_NOT_FOUND(2003, "nft not found"),
    NFT_NOT_OWNER(2004, "nft does not belong to current user"),
    NFT_LOCKED(2005, "nft is listed or being withdrawn"),

    ORDER_NOT_FOUND(3001, "order not found"),
    ORDER_NOT_ACTIVE(3002, "order is no longer active"),
    ORDER_SELF_BUY(3003, "cannot buy your own order"),
    ORDER_AMOUNT_ERROR(3004, "invalid order amount"),
    ORDER_PRICE_CHANGED(3005, "price has changed, please place the order again"),
    STOCK_NOT_ENOUGH(3006, "insufficient stock"),

    WITHDRAW_NOT_FOUND(4001, "withdraw record not found"),
    WITHDRAW_STATE_ERROR(4002, "invalid withdraw state"),
    PRIVATE_KEY_NOT_SET(4003, "master private key is not configured");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
