package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import com.sun.org.apache.regexp.internal.RE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;
    @Autowired
    private CartCookieHandler cartCookieHandler;
    @Reference
    private ManageService manageService;

    //http://cart.gmall.com/addToCart
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //根据userId判断是否登录
        //userId在认证时，放入request域中
        //未登录时，购物车？
        //已登录时，购物车？
        String userId =(String) request.getAttribute("userId");
        //获取post提交的参数
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        if (userId!=null){
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            //未登录时添加购物车，userId为null
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuNum",skuNum);
        request.setAttribute("skuInfo",skuInfo);

        return "success";
    }

    //展示购物车列表
    @RequestMapping("cartList")
    //需要验证是否登录，但不是必须要登陆，分为登录时的购物车状态和未登录时的购物状态
    @LoginRequire(autoRedirect = false)
    public String getCartList(HttpServletRequest request,HttpServletResponse response){
        //获取用户id
        String userId =(String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (userId!=null){
            //查看登录时购物车列表
            //合并购物车
            List<CartInfo> cartInfoListCk = cartCookieHandler.getCartList(request);
            if (cartInfoListCk!=null&&cartInfoListCk.size()>0){
                cartInfoList=cartService.mergeToCartList(cartInfoListCk,userId);
                //合并完成之后删除原来cookie中的购物车
                cartCookieHandler.deleteCookieCart(request,response);
            }else {
                //cookie中购物车为空
                cartInfoList = cartService.getCartList(userId);
            }
        }else{
            //未登录时购物车列表
           cartInfoList =  cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String)request.getAttribute("userId");
        if (userId!=null){
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //合并已登录和未登录的购物车商品，这里设计根据cookie中的选择为准，具体看业务需求
        String userId =(String) request.getAttribute("userId");
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if (cartListCK!=null&&cartListCK.size()>0){
            cartService.mergeToCartList(cartListCK,userId);
            cartCookieHandler.deleteCookieCart(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }

}
