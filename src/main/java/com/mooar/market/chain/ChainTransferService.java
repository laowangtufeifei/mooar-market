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
     * 向指定地址转出资产（提现），返回交易哈希
     */
    public String sendTransfer(Item item, String toAddr, long systemAmount, Long nftId) {
        long nonceValue = -1;
        try {
            Credentials credentials = chainAccountService.getCredentials();
            Function function = buildFunction(item, toAddr, systemAmount, nftId);
            String data = FunctionEncoder.encode(function);
            nonceValue = currentNonce(credentials.getAddress());
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(properties.getChain().getGasLimit());
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
                throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(), "broadcast rejected: " + detail);
            }
            consumeNonce();
            return resp.getTransactionHash();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            transferLog.error("上链异常 item={} to={} amount={} nonce={} err={}",
                    item.getId(), toAddr, systemAmount, nonceValue, e.getMessage(), e);
            throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(),
                    "send tx error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Function buildFunction(Item item, String to, long systemAmount, Long nftId) {
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
                            new Uint256(BigInteger.valueOf(nftId))),
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
}
