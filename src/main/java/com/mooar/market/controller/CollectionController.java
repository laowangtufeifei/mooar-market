package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.dto.CollectionVO;
import com.mooar.market.entity.Nft;
import com.mooar.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final MarketService marketService;

    @GetMapping("/list")
    public Result<List<CollectionVO>> list() {
        return Result.ok(marketService.collectionsList());
    }

    /**
     * {itemId} 支持传道具 id 或合约地址
     */
    @GetMapping("/{itemId}")
    public Result<List<Object>> detail(@PathVariable String itemId) {
        return Result.ok(marketService.collectionDetail(itemId));
    }

    @GetMapping("/{itemId}/traits")
    public Result<List<Map<String, Object>>> traits(@PathVariable String itemId) {
        return Result.ok(marketService.listTraitDefs(marketService.resolveItemId(itemId)));
    }

    @GetMapping("/{itemId}/nfts")
    public Result<List<Nft>> nfts(@PathVariable String itemId,
                                  @RequestParam(required = false) String filters,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(marketService.searchNfts(marketService.resolveItemId(itemId), filters, page, pageSize));
    }
}
