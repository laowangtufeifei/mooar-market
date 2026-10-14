package com.mooar.market.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    OK(0, "ok"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统错误"),

    LOGIN_TOKEN_ERROR(1001, "登录凭证无效"),
    USER_NOT_FOUND(1002, "用户不存在"),
    FSL_API_ERROR(1003, "登录服务异常，请稍后重试"),

    ITEM_NOT_FOUND(2001, "道具不存在"),
    ASSET_NOT_ENOUGH(2002, "资产余额不足"),
    NFT_NOT_FOUND(2003, "NFT 不存在"),
    NFT_NOT_OWNER(2004, "NFT 不属于当前用户"),
    NFT_LOCKED(2005, "NFT 已挂单或提现中"),

    ORDER_NOT_FOUND(3001, "挂单不存在"),
    ORDER_NOT_ACTIVE(3002, "挂单已失效"),
    ORDER_SELF_BUY(3003, "不能购买自己的挂单"),
    ORDER_AMOUNT_ERROR(3004, "挂单数量非法"),
    ORDER_PRICE_CHANGED(3005, "价格已变化，请重新下单"),
    STOCK_NOT_ENOUGH(3006, "库存不足"),

    WITHDRAW_NOT_FOUND(4001, "提现记录不存在"),
    WITHDRAW_STATE_ERROR(4002, "提现状态异常"),
    PRIVATE_KEY_NOT_SET(4003, "主地址私钥未配置");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
