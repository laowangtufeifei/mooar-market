package com.mooar.market.common;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
