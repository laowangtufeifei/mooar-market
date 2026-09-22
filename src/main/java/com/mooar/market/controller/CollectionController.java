package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.dto.CollectionVO;
import com.mooar.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final MarketService marketService;

    @GetMapping("/list")
    public Result<List<CollectionVO>> list() {
        return Result.ok(marketService.listCollections());
    }

    @GetMapping("/{itemId}")
    public Result<Object> detail(@PathVariable Long itemId) {
        return Result.ok(marketService.collectionDetail(itemId));
    }
}
