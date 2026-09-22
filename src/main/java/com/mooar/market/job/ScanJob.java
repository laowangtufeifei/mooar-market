package com.mooar.market.job;

import com.mooar.market.chain.DepositScanner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScanJob {

    private final DepositScanner depositScanner;

    @Scheduled(fixedDelay = 15000)
    public void scan() {
        depositScanner.scan();
    }
}
