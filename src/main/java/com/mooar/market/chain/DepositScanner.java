package com.mooar.market.chain;

import com.mooar.market.common.ItemType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooar.market.common.AppShutdown;
import com.mooar.market.common.NftState;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.entity.BalanceLog;
import com.mooar.market.entity.DepositLog;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Nft;
import com.mooar.market.entity.User;
import com.mooar.market.entity.UserAsset;
import com.mooar.market.mapper.BalanceLogMapper;
import com.mooar.market.mapper.DepositLogMapper;
import com.mooar.market.mapper.NftMapper;
import com.mooar.market.mapper.SysKvMapper;
import com.mooar.market.mapper.UserAssetMapper;
import com.mooar.market.mapper.UserMapper;
import com.mooar.market.service.ItemCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepositScanner {

    private static final Logger log = LoggerFactory.getLogger(DepositScanner.class);

    private static final String TRANSFER_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    private static final String TRANSFER_SINGLE_TOPIC =
            "0xc3d58168c5ae7397731d063d5bbf3d657854427343f4c083240f7aacaa2d0f62";
    private static final String CURSOR_KEY = "deposit_scan_block";
    private static final int BATCH_SIZE = 2000;

    private final Web3j web3j;
    private final MarketProperties properties;
    private final ItemCache itemCache;
    private final UserMapper userMapper;
    private final UserAssetMapper userAssetMapper;
    private final NftMapper nftMapper;
    private final DepositLogMapper depositLogMapper;
    private final BalanceLogMapper balanceLogMapper;
    private final SysKvMapper sysKvMapper;
    private final ObjectMapper objectMapper;
    private final AppShutdown appShutdown;

    private final Map<Long, Long> blockTimeCache = new HashMap<>();

    /**
     * 扫描主地址收到的转账事件并幂等入账
     */
    public void scan() {
        try {
            long latest = web3j.ethBlockNumber().send().getBlockNumber().longValue();
            long confirmed = latest - properties.getScan().getConfirmBlocks();
            long from = resolveFrom(confirmed);
            if (from > confirmed) {
                return;
            }
            while (from <= confirmed) {
                long batchTo = Math.min(from + BATCH_SIZE - 1, confirmed);
                scanRange(from, batchTo);
                sysKvMapper.put(CURSOR_KEY, String.valueOf(batchTo));
                if (appShutdown.isShuttingDown()) {
                    log.info("扫块中断，服务正在关闭，游标保留在 {}，下次启动续扫", batchTo);
                    return;
                }
                from = batchTo + 1;
            }
        } catch (Exception e) {
            if (appShutdown.isShuttingDown() || Thread.currentThread().isInterrupted()) {
                log.info("扫块中断，服务正在关闭，下次启动按游标续扫");
                return;
            }
            log.error("deposit scan error", e);
        }
    }

    private long resolveFrom(long confirmed) {
        String v = sysKvMapper.get(CURSOR_KEY);
        if (v != null) {
            return Long.parseLong(v) + 1;
        }
        long start = properties.getScan().getStartBlock();
        long init = start > 0 ? start : confirmed;
        sysKvMapper.put(CURSOR_KEY, String.valueOf(init - 1));
        return init;
    }

    /**
     * 扫块合约来自 item 表（state = 1），按 type 分流：
     * 1=ERC20、3=ERC721 走 Transfer 事件，2=ERC1155 走 TransferSingle 事件
     */
    private void scanRange(long from, long to) throws Exception {
        String master = properties.getChain().getMasterAddress().toLowerCase();
        for (Item item : itemCache.getScanItems()) {
            if (item.getType() == ItemType.ERC1155) {
                processTransferSingle(item.getContract(), from, to, master);
            } else {
                processTransfer(item.getContract(), from, to, master, item.getType() == ItemType.ERC721);
            }
        }
    }

    private void processTransfer(String contract, long from, long to, String master, boolean isNft) throws Exception {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(from)),
                DefaultBlockParameter.valueOf(BigInteger.valueOf(to)), contract);
        filter.addSingleTopic(TRANSFER_TOPIC);
        EthLog ethLog = web3j.ethGetLogs(filter).send();
        List<Log> logs = toLogs(ethLog);
        for (Log l : logs) {
            List<String> topics = l.getTopics();
            if (topics.size() < 3) {
                continue;
            }
            String toAddr = parseAddress(topics.get(2));
            if (!toAddr.equals(master)) {
                continue;
            }
            String fromAddr = parseAddress(topics.get(1));
            BigInteger value;
            if (isNft) {
                if (topics.size() < 4) {
                    continue;
                }
                value = Numeric.toBigInt(topics.get(3));
                if (!fitsLong(value)) {
                    log.error("tokenId 超出 long 范围，跳过入账 contract={} tokenId={} tx={}",
                            contract, value, l.getTransactionHash());
                    continue;
                }
            } else {
                value = Numeric.toBigInt(l.getData());
            }
            long tokenId = isNft ? value.longValue() : 0L;
            String amount = isNft ? "1" : value.toString();
            int logIndex = l.getLogIndex().intValue();
            if (!markProcessed(l, fromAddr, master, contract, tokenId, amount)) {
                continue;
            }
            handleErcTransfer(contract, fromAddr, value, l.getTransactionHash(), logIndex);
        }
    }

    private void processTransferSingle(String contract, long from, long to, String master) throws Exception {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(from)),
                DefaultBlockParameter.valueOf(BigInteger.valueOf(to)), contract);
        filter.addSingleTopic(TRANSFER_SINGLE_TOPIC);
        EthLog ethLog = web3j.ethGetLogs(filter).send();
        List<Log> logs = toLogs(ethLog);
        for (Log l : logs) {
            List<String> topics = l.getTopics();
            if (topics.size() < 4) {
                continue;
            }
            String toAddr = parseAddress(topics.get(3));
            if (!toAddr.equals(master)) {
                continue;
            }
            String fromAddr = parseAddress(topics.get(2));
            String data = l.getData().startsWith("0x") ? l.getData().substring(2) : l.getData();
            if (data.length() < 128) {
                continue;
            }
            BigInteger tokenId = new BigInteger(data.substring(0, 64), 16);
            BigInteger value = new BigInteger(data.substring(64, 128), 16);
            if (!fitsLong(tokenId)) {
                log.error("tokenId 超出 long 范围，跳过入账 contract={} tokenId={} tx={}",
                        contract, tokenId, l.getTransactionHash());
                continue;
            }
            int logIndex = l.getLogIndex().intValue();
            if (!markProcessed(l, fromAddr, master, contract, tokenId.longValue(), value.toString())) {
                continue;
            }
            handleErc1155Transfer(contract, fromAddr, tokenId, value, l.getTransactionHash(), logIndex);
        }
    }

    private void handleErcTransfer(String contract, String fromAddr, BigInteger value, String txHash, int logIndex) {
        Item item = itemCache.getByContractAndTokenId(contract, 0L);
        if (item == null) {
            return;
        }
        User user = getOrCreateUser(fromAddr);
        if (item.getType() == ItemType.ERC721) {
            handleErc721Transfer(item, contract, user, value.longValue(), txHash);
        } else {
            long systemAmount = value.divide(BigInteger.valueOf(item.getChainScale())).longValue();
            if (systemAmount <= 0) {
                log.warn("链上金额换算后为 0，跳过入账：contract={} 链上金额={} chainScale={} from={} tx={}，" +
                        "请检查 item.chain_scale 是否与该代币精度匹配", contract, value, item.getChainScale(), fromAddr, txHash);
            }
            creditAsset(user.getId(), item, systemAmount, txHash, logIndex);
        }
    }

    /**
     * tokenId 是 uint256，超出 long 能表示的范围时必须跳过：
     * longValue() 是静默截断，截断后的 id 可能撞上另一个 NFT（uk_contract_token），
     * 导致把别人的 NFT 归属改掉
     */
    private boolean fitsLong(BigInteger value) {
        return value.bitLength() <= 63;
    }

    /**
     * 721 转入：属性不会变，只回填归属，不改动 metadata / 槽位
     */
    private void handleErc721Transfer(Item item, String contract, User user, long tokenId, String txHash) {
        Nft nft = nftMapper.selectByContractAndTokenId(contract.toLowerCase(), tokenId);
        if (nft == null) {
            log.warn("NFT 未登记，跳过入账 contract={} tokenId={} tx={}", contract, tokenId, txHash);
            return;
        }
        if (NftState.isLocked(nft.getState())) {
            log.warn("NFT 处于挂单/提现中，跳过归属更新 nftId={} tokenId={} tx={}", nft.getId(), tokenId, txHash);
            return;
        }
        if (user.getId().equals(nft.getOwnerId())) {
            return;
        }
        long now = System.currentTimeMillis();
        int updated = nftMapper.updateOwnerOnDeposit(nft.getId(), user.getId(), NftState.NORMAL,
                NftState.LISTING, NftState.WITHDRAWING, now);
        if (updated == 0) {
            log.warn("NFT 归属更新失败 nftId={} tokenId={} tx={}", nft.getId(), tokenId, txHash);
            return;
        }
        log.info("入账NFT user={} item={} tokenId={} tx={}", user.getId(), item.getId(), tokenId, txHash);
    }

    private void handleErc1155Transfer(String contract, String fromAddr, BigInteger tokenId, BigInteger value,
                                       String txHash, int logIndex) {
        Item item = itemCache.getByContractAndTokenId(contract, tokenId.longValue());
        if (item == null) {
            return;
        }
        User user = getOrCreateUser(fromAddr);
        creditAsset(user.getId(), item, value.longValue(), txHash, logIndex);
    }

    private void creditAsset(Long userId, Item item, long systemAmount, String txHash, int logIndex) {
        if (systemAmount <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        UserAsset asset = userAssetMapper.selectByUserAndItem(userId, item.getId());
        if (asset == null) {
            UserAsset a = new UserAsset();
            a.setUserId(userId);
            a.setItemId(item.getId());
            a.setAvailable(systemAmount);
            a.setFrozen(0L);
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
            userAssetMapper.insert(a);
            asset = a;
        } else {
            userAssetMapper.addAvailable(userId, item.getId(), systemAmount, now);
            asset = userAssetMapper.selectByUserAndItem(userId, item.getId());
        }
        BalanceLog balanceLog = new BalanceLog();
        balanceLog.setUserId(userId);
        balanceLog.setItemId(item.getId());
        balanceLog.setChangeAmount(systemAmount);
        balanceLog.setBalanceAfter(asset.getAvailable());
        balanceLog.setBizType(1);
        balanceLog.setRefId(0L);
        balanceLog.setCreatedAt(now);
        balanceLogMapper.insert(balanceLog);
        log.info("入账资产 user={} item={} amount={} tx={}", userId, item.getId(), systemAmount, txHash);
    }

    private User getOrCreateUser(String address) {
        User user = userMapper.selectByAddress(address);
        if (user != null) {
            return user;
        }
        long now = System.currentTimeMillis();
        User u = new User();
        u.setFid("");
        u.setName("");
        u.setAvatar("");
        u.setEmail("");
        u.setStepnEmail("");
        u.setAddress(address);
        u.setState(0);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        userMapper.insert(u);
        return u;
    }

    private boolean markProcessed(Log l, String fromAddr, String toAddr, String contract, long tokenId, String amount) {
        DepositLog dl = new DepositLog();
        dl.setTxHash(l.getTransactionHash());
        dl.setLogIndex(l.getLogIndex().intValue());
        dl.setBlockNumber(l.getBlockNumber().longValue());
        dl.setBlockTime(blockTime(l.getBlockNumber().longValue()));
        dl.setFromAddr(fromAddr);
        dl.setToAddr(toAddr);
        dl.setContract(contract.toLowerCase());
        dl.setTokenId(tokenId);
        dl.setAmount(amount);
        dl.setJsonData(buildJson(l));
        dl.setCreatedAt(System.currentTimeMillis());
        return depositLogMapper.insertIgnore(dl) == 1;
    }

    private long blockTime(long blockNumber) {
        Long cached = blockTimeCache.get(blockNumber);
        if (cached != null) {
            return cached;
        }
        try {
            EthBlock block = web3j.ethGetBlockByNumber(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)), false).send();
            long ts = block.getBlock().getTimestamp().longValue();
            blockTimeCache.put(blockNumber, ts);
            if (blockTimeCache.size() > 1024) {
                blockTimeCache.clear();
            }
            return ts;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String buildJson(Log l) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("topics", l.getTopics());
            data.put("data", l.getData());
            data.put("blockHash", l.getBlockHash());
            data.put("transactionIndex", l.getTransactionIndexRaw());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String parseAddress(String topic) {
        String hex = topic.startsWith("0x") ? topic.substring(2) : topic;
        if (hex.length() > 40) {
            hex = hex.substring(hex.length() - 40);
        }
        return "0x" + hex.toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private List<Log> toLogs(EthLog ethLog) {
        List<Log> logs = new ArrayList<>();
        if (ethLog.getLogs() == null) {
            return logs;
        }
        for (EthLog.LogResult result : ethLog.getLogs()) {
            logs.add((Log) result.get());
        }
        return logs;
    }
}
