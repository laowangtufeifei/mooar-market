package com.mooar.market.common;

/**
 * NFT 状态：0 正常 1 挂单中 2 提现中
 */
public final class NftState {

    public static final int NORMAL = 0;
    public static final int LISTING = 1;
    public static final int WITHDRAWING = 2;

    private NftState() {
    }
}
