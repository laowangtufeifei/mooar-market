package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.common.UserContext;
import com.mooar.market.dto.AssetVO;
import com.mooar.market.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/list")
    public Result<List<AssetVO>> list() {
        return Result.ok(assetService.getAssets(UserContext.get()));
    }
}
