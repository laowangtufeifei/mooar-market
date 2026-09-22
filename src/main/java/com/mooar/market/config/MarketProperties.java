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

    @Data
    public static class Jwt {
        private boolean enable;
        private String publicKey;
    }

    @Data
    public static class Chain {
        private String rpc;
        private long chainId;
        private String masterAddress;
        private String encryptKey;
        private String gmtContract;
        private String boxContract;
        private String badgeContract;
        private String MORCHIContract;
        private long boxTokenId;
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
    }

    @Data
    public static class Fee {
        private long withdrawFee;
        private int tradeFeeRate;
    }
}
