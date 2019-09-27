package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        //先从cookie中判断购物车中是否有此商品
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        //如果在购物车中没有找到该商品，则直接加入购物车集合，借助变量实现
        boolean ifExist = false;
        //coockie存放的是购物车商品的集合
        if (!StringUtils.isEmpty(cookieValue)){
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                if (skuId.equals(cartInfo.getSkuId())){
                    //购物车中有此商品,将变量值改为true
                    ifExist=true;
                    //设置数量，只需在原基础上添加数量
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    //初始化临时价格
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    break;
                }
            }
        }
        if (!ifExist){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);
        }
        String cartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request,response,cookieCartName,cartJson,COOKIE_CART_MAXAGE,true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        //从cookie中获取购物车列表
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (!StringUtils.isEmpty(cookieValue)){
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
        }
        return cartInfoList;
    }

    public void deleteCookieCart(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId,String isChecked) {
        List<CartInfo> cartList = getCartList(request);
        if (cartList!=null&&cartList.size()>0){
            for (CartInfo cartInfo : cartList) {
                if (cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }
        String newCartJson = JSON.toJSONString(cartList);
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
    }
}
