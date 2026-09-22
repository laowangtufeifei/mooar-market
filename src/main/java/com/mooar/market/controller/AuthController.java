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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionStore sessionStore;

    @PostMapping("/fslLogin")
    public Result<LoginVO> login(@RequestBody LoginReq req) {
        User user = authService.login(req.getToken());
        String sessionId = sessionStore.create(user.getId());
        LoginVO vo = new LoginVO();
        vo.setSessionId(sessionId);
        vo.setUserId(user.getId());
        vo.setFid(user.getFid());
        vo.setAddress(user.getAddress());
        vo.setEmail(user.getEmail());
        return Result.ok(vo);
    }
}
