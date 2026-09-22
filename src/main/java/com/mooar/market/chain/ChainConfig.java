package com.mooar.market.chain;

import com.mooar.market.config.MarketProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class ChainConfig {

    @Bean
    public Web3j web3j(MarketProperties properties) {
        return Web3j.build(new HttpService(properties.getChain().getRpc()));
    }
}
