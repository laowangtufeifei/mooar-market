package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.common.SessionStore;
import com.mooar.market.dto.LoginReq;
import com.mooar.market.dto.LoginVO;
import com.mooar.market.entity.User;
import com.mooar.market.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionStore sessionStore;

    /**
     * FSL 授权码登录，拿 code 换 sessionId
     */
    @PostMapping({"/fslLogin", "/login"})
    public Result<LoginVO> login(@RequestBody LoginReq req) {
        User user = authService.login(req.getCode());
        String sessionId = sessionStore.create(user.getId());
        LoginVO vo = new LoginVO();
        vo.setSessionId(sessionId);
        vo.setUserId(user.getId());
        vo.setFid(user.getFid());
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setStepnEmail(user.getStepnEmail());
        return Result.ok(vo);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "sessionID", required = false) String sessionId) {
        sessionStore.remove(sessionId);
        return Result.ok();
    }
}
