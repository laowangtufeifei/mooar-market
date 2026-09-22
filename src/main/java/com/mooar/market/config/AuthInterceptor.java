package com.mooar.market.config;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.common.SessionStore;
import com.mooar.market.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionStore sessionStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sessionId = request.getHeader("sessionID");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = request.getParameter("sessionID");
        }
        Long userId = sessionStore.getUserId(sessionId);
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
