package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.common.UserContext;
import com.mooar.market.dto.CreateWithdrawReq;
import com.mooar.market.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("/create")
    public Result<Long> create(@RequestBody CreateWithdrawReq req) {
        Long id = withdrawService.createWithdraw(UserContext.get(), req.getItemId(),
                req.getAmount(), req.getNftId(), req.getToAddr());
        return Result.ok(id);
    }
}
