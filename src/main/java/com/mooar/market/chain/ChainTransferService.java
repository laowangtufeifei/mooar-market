package com.mooar.market.chain;

import com.mooar.market.common.ItemType;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.entity.Item;
import com.mooar.market.service.ChainAccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class ChainTransferService {

    private static final Logger transferLog = LoggerFactory.getLogger("WITHDRAW_LOG");

    private final Web3j web3j;
    private final MarketProperties properties;
    private final ChainAccountService chainAccountService;

    private final AtomicLong nonce = new AtomicLong(-1);
    private final Object nonceLock = new Object();

    /**
     * 向指定地址转出资产（提现），返回交易哈希。
     * tokenId 必须是**链上 tokenId**（721 要先用 nft.id 查 nft.token_id），不是数据库主键
     */
    public String sendTransfer(Item item, String toAddr, long systemAmount, Long tokenId) {
        long nonceValue = -1;
        try {
            Credentials credentials = chainAccountService.getCredentials();
            Function function = buildFunction(item, toAddr, systemAmount, tokenId);
            String data = FunctionEncoder.encode(function);
            nonceValue = currentNonce(credentials.getAddress());
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = estimateGas(credentials.getAddress(), item.getContract(), data);
            RawTransaction rawTx = RawTransaction.createTransaction(
                    BigInteger.valueOf(nonceValue), gasPrice, gasLimit,
                    item.getContract(), BigInteger.ZERO, data);
            long chainId = properties.getChain().getChainId();
            byte[] signed = TransactionEncoder.signMessage(rawTx, chainId, credentials);
            EthSendTransaction resp = web3j.ethSendRawTransaction(Numeric.toHexString(signed)).send();
            if (resp.hasError()) {
                String detail = resp.getError() == null ? "send raw transaction rejected"
                        : resp.getError().getMessage();
                transferLog.error("上链广播被拒 item={} to={} amount={} chainId={} err={}",
                        item.getId(), toAddr, systemAmount, chainId, detail);
                resetNonce();
                throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(), "broadcast rejected: " + detail);
            }
            consumeNonce();
            return resp.getTransactionHash();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            transferLog.error("上链异常 item={} to={} amount={} nonce={} err={}",
                    item.getId(), toAddr, systemAmount, nonceValue, e.getMessage(), e);
            resetNonce();
            throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(),
                    "send tx error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * gas 上限按合约实际消耗动态估算（+20% 余量）：不同合约差很多，
     * MORCHI(721) 要 13 万、box(1155) 要 23 万，写死的配置值很容易把交易烧成 out of gas。
     * 估算失败（比如本来就会 revert）时退回配置值
     */
    private BigInteger estimateGas(String from, String to, String data) {
        long fallback = properties.getChain().getGasLimit();
        try {
            Transaction call = Transaction.createEthCallTransaction(from, to, data);
            BigInteger used = web3j.ethEstimateGas(call).send().getAmountUsed();
            if (used != null && used.signum() > 0) {
                return used.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            }
        } catch (Exception e) {
            transferLog.warn("gas 估算失败，退回配置值 {} to={} err={}", fallback, to, e.getMessage());
        }
        return BigInteger.valueOf(fallback);
    }

    private Function buildFunction(Item item, String to, long systemAmount, Long tokenId) {
        String master = properties.getChain().getMasterAddress();
        if (item.getType() == ItemType.ERC20) {
            BigInteger chainAmount = BigInteger.valueOf(systemAmount)
                    .multiply(BigInteger.valueOf(item.getChainScale()));
            return new Function("transfer",
                    Arrays.asList(new Address(to), new Uint256(chainAmount)),
                    Collections.emptyList());
        } else if (item.getType() == ItemType.ERC1155) {
            return new Function("safeTransferFrom",
                    Arrays.asList(new Address(master), new Address(to),
                            new Uint256(BigInteger.valueOf(item.getTokenId())),
                            new Uint256(BigInteger.valueOf(systemAmount)),
                            new DynamicBytes(new byte[0])),
                    Collections.emptyList());
        } else {
            return new Function("safeTransferFrom",
                    Arrays.asList(new Address(master), new Address(to),
                            new Uint256(BigInteger.valueOf(tokenId))),
                    Collections.emptyList());
        }
    }

    /**
     * 只取当前 nonce 不递增：只有广播成功才 consumeNonce()，
     * 否则失败一次就烧掉一个 nonce，后面补不回来会让交易一直卡在 pending
     */
    private long currentNonce(String address) throws IOException {
        synchronized (nonceLock) {
            if (nonce.get() < 0) {
                BigInteger onChain = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                        .send().getTransactionCount();
                nonce.set(onChain.longValue());
            }
            return nonce.get();
        }
    }

    private void consumeNonce() {
        synchronized (nonceLock) {
            nonce.incrementAndGet();
        }
    }

    /**
     * 广播被拒/异常后作废本地 nonce，下次重新从链上取，
     * 否则缓存值落后于链上会一直报 nonce too low
     */
    private void resetNonce() {
        synchronized (nonceLock) {
            nonce.set(-1);
        }
    }
}
