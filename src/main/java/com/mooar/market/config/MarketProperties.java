package com.mooar.market.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    private Jwt jwt = new Jwt();
    private Chain chain = new Chain();
    private Item item = new Item();
    private Scan scan = new Scan();
    private Admin admin = new Admin();
    private Fee fee = new Fee();
    private Search search = new Search();
    private Fsl fsl = new Fsl();

    @Data
    public static class Jwt {
        private boolean enable;
        private String publicKey;
    }

    /**
     * FSL 登录对接：拿前端给的 authorization_code 换 access_token 再拉用户资料
     */
    @Data
    public static class Fsl {
        private String url;
        private String appKey;
        private String appSecret;
        /**
         * 用户信息里钱包地址所在的字段名（如 "address" / "solAddress"），
         * 配了才会把地址绑定到 user.address，用来和扫块建的用户合并
         */
        private String addressField = "evmAddress";
        /** 单次 HTTP 超时（毫秒） */
        private int timeoutMs = 5000;
        /** code -> userId 缓存时长（秒），同一个 code 重复登录直接复用 */
        private long codeCacheSeconds = 600;
    }

    /**
     * 链上配置。扫哪些合约不再写在这里，由 item 表的 contract + state 决定
     */
    @Data
    public static class Chain {
        private String rpc;
        private long chainId;
        private String masterAddress;
        private String encryptKey;
        private long gasLimit;
    }

    @Data
    public static class Item {
        private String imagePrefix;
    }

    @Data
    public static class Scan {
        private long startBlock;
        private int confirmBlocks;
    }

    @Data
    public static class Admin {
        private String token;
        /** 统计口径时区，决定"按天/按月"的切分边界 */
        private String statsZone = "Asia/Shanghai";
    }

    @Data
    public static class Fee {
        private long withdrawFee;
        private int tradeFeeRate;
    }

    /**
     * 挂单筛选结果缓存
     */
    @Data
    public static class Search {
        private long cacheTtlSeconds = 60;
        private long cacheMaxSize = 2000;
        private int maxPageSize = 100;
    }
}
