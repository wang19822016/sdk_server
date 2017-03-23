package com.seastar.config;

import com.seastar.dao.UserTokenDao;
import com.seastar.config.annotation.Authorization;
import com.seastar.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by osx on 17/3/6.
 */
@Component
public class AuthorizationInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private UserTokenDao userTokenDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 如果添加了Authorization必须验证
        if (((HandlerMethod) handler).getMethod().getAnnotation(Authorization.class) == null) {
            return true;
        }

        // 要求授权的接口一律使用Bearer token
        String token = Utils.getOAuthToken(request);
        if (token == null || userTokenDao.findOne(token) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}
