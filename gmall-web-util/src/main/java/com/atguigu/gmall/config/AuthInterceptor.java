package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.WebConst;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token
        String token = request.getParameter("newToken");
        //将token放到cookie中
        if (token != null) {
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        }
        //当访问其他模块时，url中没有token,此时从cookie中获取token
        if (token == null) {
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        //解密token
        if (token != null) {
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
        }
        //拦截器拦截是否需要验证登陆
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation!=null){
            //此时有注解，需要验证
            //调用verify接口验证是否登录
            //http://passport.atguigu.com/verify?token=xxx&salt=xxx
            String salt = request.getHeader("X-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){
                //认证成功
                Map map = getUserMapByToken(token);
                String userId = (String)map.get("userId");
                request.setAttribute("userId",userId);
                return true;
            }else{
                //认证失败
                //判断LoginRequire注解中的值是否为true,为true则必须登录
                //methodAnnotation.autoRedirect()默认值为true
                if (methodAnnotation.autoRedirect()){
                    //此时必须登录
                    //但是若要保证登录成功之后继续返回此页面，则url为//http://passport.atguigu.com/index?orginUrl=编码后的当前页面url
                    String requestUrl = request.getRequestURL().toString();
                    String encodeURL = URLEncoder.encode(requestUrl, "UTF-8");
                    //重定向到此页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;//此时不能放行拦截,而是重定向到另一页面
                }
            }
        }
        return true;
    }

    //解密
    private Map getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

}
