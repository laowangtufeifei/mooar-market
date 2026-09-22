package com.mooar.market.service;

import com.mooar.market.common.AesUtil;
import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.entity.ChainAccount;
import com.mooar.market.mapper.ChainAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;

@Service
@RequiredArgsConstructor
public class ChainAccountService {

    private final ChainAccountMapper chainAccountMapper;
    private final AesUtil aesUtil;
    private final MarketProperties properties;

    private volatile Credentials cached;
    private volatile String cachedAddress;

    /**
     * 通过管理接口传入私钥，加密后存入数据库
     */
    public void setPrivateKey(String privateKey) {
        if (privateKey == null || privateKey.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        String hex = privateKey.startsWith("0x") ? privateKey.substring(2) : privateKey;
        Credentials credentials;
        try {
            credentials = Credentials.create(hex);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        String address = credentials.getAddress().toLowerCase();
        String master = properties.getChain().getMasterAddress().toLowerCase();
        if (!address.equals(master)) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        String encrypted = aesUtil.encrypt(hex);
        long now = System.currentTimeMillis();
        ChainAccount existing = chainAccountMapper.selectByAddress(master);
        if (existing == null) {
            ChainAccount account = new ChainAccount();
            account.setAddress(master);
            account.setEncryptedPrivateKey(encrypted);
            account.setChainId(properties.getChain().getChainId());
            account.setState(0);
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
            chainAccountMapper.insert(account);
        } else {
            chainAccountMapper.updatePrivateKey(master, encrypted, now);
        }
        cached = credentials;
        cachedAddress = master;
    }

    /**
     * 从数据库解密主地址私钥，构造签名凭证
     */
    public Credentials getCredentials() {
        if (cached != null) {
            return cached;
        }
        String master = properties.getChain().getMasterAddress().toLowerCase();
        ChainAccount account = chainAccountMapper.selectByAddress(master);
        if (account == null || account.getEncryptedPrivateKey() == null || account.getEncryptedPrivateKey().isBlank()) {
            throw new BizException(ErrorCode.PRIVATE_KEY_NOT_SET);
        }
        String privateKey = aesUtil.decrypt(account.getEncryptedPrivateKey());
        cached = Credentials.create(privateKey);
        cachedAddress = master;
        return cached;
    }
}
