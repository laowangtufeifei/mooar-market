package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.common.NftState;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.entity.BalanceLog;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Nft;
import com.mooar.market.entity.UserAsset;
import com.mooar.market.entity.Withdraw;
import com.mooar.market.mapper.BalanceLogMapper;
import com.mooar.market.mapper.NftMapper;
import com.mooar.market.mapper.OrderMapper;
import com.mooar.market.mapper.UserAssetMapper;
import com.mooar.market.mapper.WithdrawMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private static final Logger withdrawLog = LoggerFactory.getLogger("WITHDRAW_LOG");

    public static final int STATE_PENDING = 0;
    public static final int STATE_BROADCAST = 1;
    public static final int STATE_SUCCESS = 2;
    public static final int STATE_FAIL = 3;
    public static final int STATE_MANUAL = 4;

    private final ItemCache itemCache;
    private final NftMapper nftMapper;
    private final OrderMapper orderMapper;
    private final UserAssetMapper userAssetMapper;
    private final WithdrawMapper withdrawMapper;
    private final BalanceLogMapper balanceLogMapper;
    private final MarketProperties properties;
    private final MarketSearchCache searchCache;

    /**
     * 发起提现：冻结资产 + 冻结 GMT 手续费，生成待上链记录
     */
    @Transactional
    public Long createWithdraw(Long userId, Long itemId, Long amount, Long nftId, String toAddr) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (toAddr == null || toAddr.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        long now = System.currentTimeMillis();
        long fee = properties.getFee().getWithdrawFee();
        Long gmtItemId = itemCache.getGmtItem().getId();

        if (item.getType() == ItemType.ERC721) {
            if (nftId == null) {
                throw new BizException(ErrorCode.PARAM_ERROR);
            }
            Nft nft = nftMapper.selectById(nftId);
            if (nft == null || !nft.getOwnerId().equals(userId)) {
                throw new BizException(ErrorCode.NFT_NOT_OWNER);
            }
            if (!Integer.valueOf(NftState.NORMAL).equals(nft.getState())) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
            if (orderMapper.countActiveByNft(nftId) > 0) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
            if (nftMapper.lockForWithdraw(nftId, userId, NftState.NORMAL, NftState.WITHDRAWING, now) == 0) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
            if (fee > 0 && userAssetMapper.freeze(userId, gmtItemId, fee, now) == 0) {
                nftMapper.updateState(nftId, NftState.WITHDRAWING, NftState.NORMAL, 0L, now);
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
            amount = 1L;
        } else if (item.getId().equals(gmtItemId)) {
            if (amount == null || amount <= 0) {
                throw new BizException(ErrorCode.PARAM_ERROR);
            }
            if (userAssetMapper.freeze(userId, gmtItemId, amount + fee, now) == 0) {
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
            nftId = 0L;
        } else {
            if (amount == null || amount <= 0) {
                throw new BizException(ErrorCode.PARAM_ERROR);
            }
            if (userAssetMapper.freeze(userId, itemId, amount, now) == 0) {
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
            if (fee > 0 && userAssetMapper.freeze(userId, gmtItemId, fee, now) == 0) {
                userAssetMapper.unfreeze(userId, itemId, amount, now);
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
            nftId = 0L;
        }

        Withdraw withdraw = new Withdraw();
        withdraw.setUserId(userId);
        withdraw.setItemId(itemId);
        withdraw.setNftId(nftId);
        withdraw.setAmount(amount);
        withdraw.setFee(fee);
        withdraw.setToAddr(toAddr.toLowerCase());
        withdraw.setTxHash("");
        withdraw.setState(STATE_PENDING);
        withdraw.setRetryCount(0);
        withdraw.setNonce(-1L);
        withdraw.setErrMsg("");
        withdraw.setCreatedAt(now);
        withdraw.setUpdatedAt(now);
        withdrawMapper.insert(withdraw);
        withdrawLog.info("发起提现 id={} user={} item={} amount={} nftId={} fee={} to={}",
                withdraw.getId(), userId, itemId, amount, nftId, fee, withdraw.getToAddr());
        return withdraw.getId();
    }

    /**
     * 提现上链成功后真正扣减冻结资产与手续费
     */
    public void finalizeWithdraw(Withdraw withdraw) {
        Item item = itemCache.getById(withdraw.getItemId());
        long now = System.currentTimeMillis();
        long fee = withdraw.getFee() == null ? 0L : withdraw.getFee();
        Long gmtItemId = itemCache.getGmtItem().getId();
        if (item.getType() == ItemType.ERC721) {
            nftMapper.release(withdraw.getNftId(), NftState.ONCHAIN, now);
            if (fee > 0) {
                userAssetMapper.subFrozen(withdraw.getUserId(), gmtItemId, fee, now);
                writeBalanceLog(withdraw.getUserId(), gmtItemId, -fee, now, 6, withdraw.getId());
            }
            searchCache.invalidate(withdraw.getItemId());
        } else if (item.getId().equals(gmtItemId)) {
            userAssetMapper.subFrozen(withdraw.getUserId(), gmtItemId, withdraw.getAmount() + fee, now);
            writeBalanceLog(withdraw.getUserId(), gmtItemId, -(withdraw.getAmount() + fee), now, 5, withdraw.getId());
        } else {
            userAssetMapper.subFrozen(withdraw.getUserId(), withdraw.getItemId(), withdraw.getAmount(), now);
            writeBalanceLog(withdraw.getUserId(), withdraw.getItemId(), -withdraw.getAmount(), now, 5, withdraw.getId());
            if (fee > 0) {
                userAssetMapper.subFrozen(withdraw.getUserId(), gmtItemId, fee, now);
                writeBalanceLog(withdraw.getUserId(), gmtItemId, -fee, now, 6, withdraw.getId());
            }
        }
    }

    /**
     * 提现失败后回滚冻结资产与手续费
     */
    public void rollbackWithdraw(Withdraw withdraw) {
        Item item = itemCache.getById(withdraw.getItemId());
        long now = System.currentTimeMillis();
        long fee = withdraw.getFee() == null ? 0L : withdraw.getFee();
        Long gmtItemId = itemCache.getGmtItem().getId();
        if (item.getType() == ItemType.ERC721) {
            nftMapper.updateState(withdraw.getNftId(), NftState.WITHDRAWING, NftState.NORMAL, 0L, now);
            if (fee > 0) {
                userAssetMapper.unfreeze(withdraw.getUserId(), gmtItemId, fee, now);
            }
        } else if (item.getId().equals(gmtItemId)) {
            userAssetMapper.unfreeze(withdraw.getUserId(), gmtItemId, withdraw.getAmount() + fee, now);
        } else {
            userAssetMapper.unfreeze(withdraw.getUserId(), withdraw.getItemId(), withdraw.getAmount(), now);
            if (fee > 0) {
                userAssetMapper.unfreeze(withdraw.getUserId(), gmtItemId, fee, now);
            }
        }
    }

    private void writeBalanceLog(Long userId, Long itemId, long change, long now, int bizType, Long refId) {
        UserAsset asset = userAssetMapper.selectByUserAndItem(userId, itemId);
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setItemId(itemId);
        log.setChangeAmount(change);
        log.setBalanceAfter(asset == null ? 0L : asset.getAvailable());
        log.setBizType(bizType);
        log.setRefId(refId);
        log.setCreatedAt(now);
        balanceLogMapper.insert(log);
    }
}
