package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.dto.SetPrivateKeyReq;
import com.mooar.market.service.ChainAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chain-account")
@RequiredArgsConstructor
public class AdminController {

    private final ChainAccountService chainAccountService;

    @PostMapping("/private-key")
    public Result<?> setPrivateKey(@RequestBody SetPrivateKeyReq req) {
        chainAccountService.setPrivateKey(req.getPrivateKey());
        return Result.ok();
    }
}
