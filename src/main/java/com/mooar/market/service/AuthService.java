package com.mooar.market.service;

import com.mooar.market.common.JwtUtil;
import com.mooar.market.common.LoginUser;
import com.mooar.market.entity.User;
import com.mooar.market.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /**
     * 解析登录 token，按地址绑定或新建用户
     */
    public User login(String token) {
        LoginUser loginUser = jwtUtil.parse(token);
        long now = System.currentTimeMillis();
        User user = userMapper.selectByAddress(loginUser.address());
        if (user == null) {
            User u = new User();
            u.setFid(loginUser.fid());
            u.setEmail(loginUser.email() == null ? "" : loginUser.email());
            u.setAddress(loginUser.address());
            u.setState(0);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            userMapper.insert(u);
            return u;
        }
        boolean changed = false;
        if (user.getFid() == null || user.getFid().isBlank() || !user.getFid().equals(loginUser.fid())) {
            user.setFid(loginUser.fid());
            changed = true;
        }
        if (loginUser.email() != null && !loginUser.email().equals(user.getEmail())) {
            user.setEmail(loginUser.email());
            changed = true;
        }
        if (changed) {
            userMapper.updateLogin(user.getId(), user.getFid(), user.getEmail(), now);
        }
        return user;
    }
}
