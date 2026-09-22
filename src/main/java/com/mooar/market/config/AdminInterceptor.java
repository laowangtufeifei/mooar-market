package com.mooar.market.config;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final MarketProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("X-Admin-Token");
        if (token == null || !token.equals(properties.getAdmin().getToken())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
