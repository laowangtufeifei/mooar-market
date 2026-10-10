package com.mooar.market.chain;

import com.mooar.market.common.ItemType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooar.market.common.AppShutdown;
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
import com.mooar.market.service.ItemTraitService;
import com.mooar.market.service.NftMetadataService;
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
    private final NftMetadataService nftMetadataService;
    private final ItemTraitService itemTraitService;
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

    private void scanRange(long from, long to) throws Exception {
        String master = properties.getChain().getMasterAddress().toLowerCase();
        processTransfer(properties.getChain().getGmtContract(), from, to, master, false);
        processTransfer(properties.getChain().getBadgeContract(), from, to, master, true);
        processTransfer(properties.getChain().getMORCHIContract(), from, to, master, true);
        processTransferSingle(properties.getChain().getBoxContract(), from, to, master);
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
            Nft exist = nftMapper.selectByContractAndTokenId(contract.toLowerCase(), value.longValue());
            long now = System.currentTimeMillis();
            boolean needMetadata = exist == null || exist.getMetadata() == null || exist.getMetadata().isBlank();
            String metadata = needMetadata ? nftMetadataService.fetchMetadata(contract, value.longValue()) : null;
            if (exist == null) {
                Nft nft = new Nft();
                nft.setItemId(item.getId());
                nft.setContract(contract.toLowerCase());
                nft.setTokenId(value.longValue());
                nft.setOwnerId(user.getId());
                nft.setState(0);
                nft.setOrderId(0L);
                nft.setMetadata(metadata);
                nft.setImage(nftMetadataService.parseImage(metadata));
                itemTraitService.applyTraits(nft, item.getId(), metadata);
                nft.setCreatedAt(now);
                nft.setUpdatedAt(now);
                nftMapper.insert(nft);
            } else {
                if (nftMapper.updateOwnerOnDeposit(exist.getId(), user.getId(), now) == 0) {
                    log.warn("NFT 处于挂单/提现中，跳过归属更新 user={} item={} tokenId={} tx={}",
                            user.getId(), item.getId(), value, txHash);
                }
                if (metadata != null && !metadata.isBlank()) {
                    Nft upd = new Nft();
                    upd.setId(exist.getId());
                    upd.setMetadata(metadata);
                    upd.setImage(nftMetadataService.parseImage(metadata));
                    itemTraitService.applyTraits(upd, item.getId(), metadata);
                    upd.setUpdatedAt(now);
                    nftMapper.updateTraits(upd);
                    log.info("补全NFT属性 user={} item={} tokenId={} tx={}", user.getId(), item.getId(), value, txHash);
                }
            }
            log.info("入账NFT user={} item={} tokenId={} tx={}", user.getId(), item.getId(), value, txHash);
        } else {
            long systemAmount = value.divide(BigInteger.valueOf(item.getChainScale())).longValue();
            creditAsset(user.getId(), item, systemAmount, txHash, logIndex);
        }
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
        u.setEmail("");
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
