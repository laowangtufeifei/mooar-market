package com.mooar.market.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.FslUserInfo;
import com.mooar.market.entity.User;
import com.mooar.market.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * FSL 授权码登录
 */
@Service
public class AuthService {

    private static final Logger loginLog = LoggerFactory.getLogger("LOGIN_LOG");

    private final UserMapper userMapper;
    private final FslApiService fslApiService;
    /** code -> userId，同一个 code 只能换一次 token，缓存后重复登录直接复用 */
    private final Cache<String, Long> codeCache;

    public AuthService(UserMapper userMapper, FslApiService fslApiService, MarketProperties properties) {
        this.userMapper = userMapper;
        this.fslApiService = fslApiService;
        this.codeCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getFsl().getCodeCacheSeconds()))
                .maximumSize(100_000)
                .build();
    }

    /**
     * code 换用户：能拿到地址就按地址合并扫块建的用户，否则按 fslUid 匹配/新建
     */
    public User login(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        Long cachedUserId = codeCache.getIfPresent(code);
        if (cachedUserId != null) {
            User cached = userMapper.selectById(cachedUserId);
            if (cached != null) {
                return cached;
            }
            codeCache.invalidate(code);
        }

        FslUserInfo info = fslApiService.login(code);
        long now = System.currentTimeMillis();
        String fid = String.valueOf(info.uid());
        User user = userMapper.selectByFid(fid);
        if (user == null && info.address() != null) {
            User byAddress = userMapper.selectByAddress(info.address());
            if (byAddress != null) {
                if (byAddress.getFid() == null || byAddress.getFid().isBlank()) {
                    userMapper.updateFid(byAddress.getId(), fid, now);
                    byAddress.setFid(fid);
                    user = byAddress;
                } else if (byAddress.getFid().equals(fid)) {
                    user = byAddress;
                } else {
                    loginLog.error("地址已绑定其他 FSL 账号，拒绝登录 address={} 已绑 fid={} 本次 fid={}",
                            info.address(), byAddress.getFid(), fid);
                    throw new BizException(ErrorCode.LOGIN_TOKEN_ERROR);
                }
            }
        }
        if (user == null) {
            User u = new User();
            u.setFid(fid);
            u.setName(info.name());
            u.setAvatar(info.profileImage());
            u.setEmail(info.email());
            u.setStepnEmail(info.stepnEmail());
            u.setAddress(info.address());
            u.setState(0);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            userMapper.insert(u);
            user = u;
            loginLog.info("新建用户 id={} fid={} name={} address={}", u.getId(), fid, info.name(), info.address());
        } else {
            userMapper.updateLoginInfo(user.getId(), info.name(), info.profileImage(),
                    info.email(), info.stepnEmail(), now);
            user.setName(info.name());
            user.setAvatar(info.profileImage());
            user.setEmail(info.email());
            user.setStepnEmail(info.stepnEmail());
            user.setUpdatedAt(now);
            bindAddressIfFree(user, info.address(), now);
            loginLog.info("登录用户 id={} fid={} name={}", user.getId(), fid, info.name());
        }
        codeCache.put(code, user.getId());
        return user;
    }

    /**
     * 补齐钱包地址：address 上有唯一键，被别的用户占了就不绑，避免登录报错
     */
    private void bindAddressIfFree(User user, String address, long now) {
        if (address == null || user.getAddress() != null) {
            return;
        }
        User owner = userMapper.selectByAddress(address);
        if (owner == null || owner.getId().equals(user.getId())) {
            userMapper.updateAddress(user.getId(), address, now);
            user.setAddress(address);
        } else {
            loginLog.warn("地址已被 userId={} 占用，本次不绑定 userId={} address={}",
                    owner.getId(), user.getId(), address);
        }
    }
}
