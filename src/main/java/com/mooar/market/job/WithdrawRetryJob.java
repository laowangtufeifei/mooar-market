package com.mooar.market.job;

import com.mooar.market.chain.ChainTransferService;
import com.mooar.market.common.AppShutdown;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Withdraw;
import com.mooar.market.mapper.WithdrawMapper;
import com.mooar.market.service.ItemCache;
import com.mooar.market.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WithdrawRetryJob {

    private static final Logger withdrawLog = LoggerFactory.getLogger("WITHDRAW_LOG");
    private static final int MAX_RETRY = 5;
    /** withdraw.err_msg 的列宽 */
    private static final int ERR_MSG_MAX_LENGTH = 256;

    private final WithdrawMapper withdrawMapper;
    private final WithdrawService withdrawService;
    private final ChainTransferService chainTransferService;
    private final ItemCache itemCache;
    private final Web3j web3j;
    private final AppShutdown appShutdown;

    @Scheduled(fixedDelay = 10000)
    public void process() {
        appShutdown.runQuietly(() -> {
            processPending();
            processConfirming();
        });
    }

    private void processPending() {
        List<Withdraw> pending = withdrawMapper.selectByState(WithdrawService.STATE_PENDING, 50);
        for (Withdraw w : pending) {
            try {
                Item item = itemCache.getById(w.getItemId());
                String txHash = chainTransferService.sendTransfer(item, w.getToAddr(), w.getAmount(), w.getNftId());
                withdrawMapper.updateTxHash(w.getId(), WithdrawService.STATE_PENDING,
                        WithdrawService.STATE_BROADCAST, txHash, System.currentTimeMillis());
                withdrawLog.info("提现上链 id={} item={} to={} txHash={}",
                        w.getId(), w.getItemId(), w.getToAddr(), txHash);
            } catch (Exception e) {
                if (appShutdown.isShuttingDown() || Thread.currentThread().isInterrupted()) {
                    withdrawLog.info("提现上链中断，服务正在关闭 id={}", w.getId());
                    return;
                }
                withdrawLog.error("提现上链失败 id={} err={}", w.getId(), e.getMessage());
                fail(w, e.getMessage());
            }
        }
    }

    private void processConfirming() {
        List<Withdraw> confirming = withdrawMapper.selectByState(WithdrawService.STATE_BROADCAST, 50);
        for (Withdraw w : confirming) {
            try {
                Optional<TransactionReceipt> receiptOpt = web3j.ethGetTransactionReceipt(w.getTxHash())
                        .send().getTransactionReceipt();
                if (receiptOpt.isEmpty()) {
                    continue;
                }
                TransactionReceipt receipt = receiptOpt.get();
                if (receipt.isStatusOK()) {
                    withdrawMapper.updateTxHash(w.getId(), WithdrawService.STATE_BROADCAST,
                            WithdrawService.STATE_SUCCESS, w.getTxHash(), System.currentTimeMillis());
                    withdrawService.finalizeWithdraw(w);
                    withdrawLog.info("提现成功 id={} item={} to={} txHash={}",
                            w.getId(), w.getItemId(), w.getToAddr(), w.getTxHash());
                } else {
                    fail(w, "receipt failed");
                }
            } catch (Exception e) {
                if (appShutdown.isShuttingDown() || Thread.currentThread().isInterrupted()) {
                    withdrawLog.info("提现确认中断，服务正在关闭 id={}", w.getId());
                    return;
                }
                withdrawLog.warn("提现确认异常 id={} err={}", w.getId(), e.getMessage());
            }
        }
    }

    private void fail(Withdraw w, String errMsg) {
        int retry = w.getRetryCount() + 1;
        long now = System.currentTimeMillis();
        String dbMsg = truncate(errMsg);
        if (retry >= MAX_RETRY) {
            // 只标记转人工，不回滚资产：提现一提交就冻结，失败也不还给用户，避免资产对不上与重复出金
            withdrawMapper.updateFail(w.getId(), WithdrawService.STATE_MANUAL, retry, dbMsg, now);
            withdrawLog.error("提现超限转人工 id={} retry={} err={}", w.getId(), retry, errMsg);
        } else {
            withdrawMapper.updateFail(w.getId(), WithdrawService.STATE_PENDING, retry, dbMsg, now);
            withdrawLog.warn("提现失败待重试 id={} retry={} err={}", w.getId(), retry, errMsg);
        }
    }

    /**
     * err_msg 是 VARCHAR(256)，超长会抛 DataTooLongException 打断重试循环；日志里仍保留完整消息
     */
    private String truncate(String errMsg) {
        if (errMsg == null) {
            return "";
        }
        return errMsg.length() > ERR_MSG_MAX_LENGTH ? errMsg.substring(0, ERR_MSG_MAX_LENGTH) : errMsg;
    }
}
