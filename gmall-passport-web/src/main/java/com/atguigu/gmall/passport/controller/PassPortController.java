package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserService;
import lombok.experimental.var;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassPortController {

    @Value("${token.key}")
    private String signKey;

    @Reference
    private UserService userService;


    @RequestMapping("index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        //获取服务器ip地址
        String salt = request.getHeader("X-forwarded-for");
        //判断是否登录并生成token
        UserInfo user = userService.login(userInfo);
        if (user != null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("userId", user.getId());
            map.put("nickName", user.getNickName());

            String token = JwtUtil.encode(signKey, map, salt);
            return token;
        } else {
            return "fail";
        }
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        //获取token和salt
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //解密获取userId
        Map<String, Object> map = JwtUtil.decode(token, signKey, salt);

        if (map != null && map.size() > 0) {
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }

        }
        return "fail";
    }
}
