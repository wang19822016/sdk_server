package com.seastar.config;

import com.seastar.config.annotation.Token;
import com.seastar.utils.JWT;
import com.seastar.utils.Utils;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 增加方法注入，将含有Token注解的方法参数注入当前登录用户
 * Created by osx on 17/3/6.
 */
@Component
public class TokenMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        //如果参数类型是User并且有CurrentUser注解则支持
        if (parameter.getParameterType().isAssignableFrom(JWT.class) && parameter.hasParameterAnnotation(Token.class)) {
            return true;
        }
        return false;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        //取出鉴权时存入的登录用户Id
        String token = Utils.getOAuthToken(webRequest);
        if (token == null) {
            throw new MissingServletRequestPartException("no token");
        }

        return new JWT(token);
    }
}
