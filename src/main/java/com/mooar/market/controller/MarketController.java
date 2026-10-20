package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.common.UserContext;
import com.mooar.market.dto.BuyBatchReq;
import com.mooar.market.dto.CreateOrderReq;
import com.mooar.market.dto.OrderDetailVO;
import com.mooar.market.dto.OrderItemVO;
import com.mooar.market.dto.TraitSearchReq;
import com.mooar.market.entity.Order;
import com.mooar.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @PostMapping("/order")
    public Result<Long> createOrder(@RequestBody CreateOrderReq req) {
        Long id = marketService.createOrder(UserContext.get(), req.getItemId(),
                req.getAmount(), req.getNftId(), req.getPrice());
        return Result.ok(id);
    }

    @GetMapping("/order/{orderId}")
    public Result<OrderDetailVO> orderDetail(@PathVariable Long orderId) {
        return Result.ok(marketService.orderDetail(orderId));
    }

    @PostMapping("/buy")
    public Result<?> buy(@RequestParam Long orderId) {
        marketService.buyOrder(UserContext.get(), orderId);
        return Result.ok();
    }

    @PostMapping("/cancel")
    public Result<?> cancel(@RequestParam Long orderId) {
        marketService.cancelOrder(UserContext.get(), orderId);
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<List<Order>> orders(@RequestParam Long itemId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(marketService.listActive(itemId, page, pageSize));
    }

    /**
     * 按属性筛选在售挂单。itemId 支持道具 id 或合约地址：
     * 传合约时，1155 会按 tokenId 分组返回（每个 tokenId 一条聚合），721 保持逐单返回
     */
    @PostMapping("/orders-filter")
    public Result<List<OrderItemVO>> ordersFilter(@RequestBody TraitSearchReq req) {
        return Result.ok(marketService.searchOrders(req.getItemId(), req));
    }

    @PostMapping("/preview")
    public Result<Long> preview(@RequestBody BuyBatchReq req) {
        long total = marketService.previewBuy(UserContext.get(), req.getItemId(), req.getAmount());
        return Result.ok(total);
    }

    @PostMapping("/buy-batch")
    public Result<?> buyBatch(@RequestBody BuyBatchReq req) {
        marketService.buyByAmount(UserContext.get(), req.getItemId(), req.getAmount(), req.getExpectedTotal());
        return Result.ok();
    }
}
