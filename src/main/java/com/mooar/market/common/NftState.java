package com.mooar.market.common;

/**
 * NFT 状态
 */
public final class NftState {

    /** 在用户包裹中，系统内可用 */
    public static final int NORMAL = 0;
    /** 挂单中 */
    public static final int LISTING = 1;
    /** 提现中，等待上链 */
    public static final int WITHDRAWING = 2;
    /** 已在链上，已提出系统 */
    public static final int ONCHAIN = 3;

    private NftState() {
    }

    /**
     * 锁定态：挂单中 / 提现中，期间不能改归属、不能再次挂单或提现
     */
    public static boolean isLocked(Integer state) {
        return state != null && (state == LISTING || state == WITHDRAWING);
    }
}
