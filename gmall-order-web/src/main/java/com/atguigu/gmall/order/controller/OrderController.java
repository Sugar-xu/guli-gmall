package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private UserService userService;
    @Reference
    private CartService cartService;
    @Reference
    private OrderService orderService;
    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String getUserAddressList(HttpServletRequest request){
        String userId = (String)request.getAttribute("userId");
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
        request.setAttribute("userAddressList",userAddressList);
        //生成流水号，防止无刷新重复提交订单
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);

        //创建一个保存清单的集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        List<CartInfo> cartInfoCheckedList = cartService.getCartCheckedList(userId);
        for (CartInfo cartInfo : cartInfoCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        //将清单集合放在域中
        request.setAttribute("orderDetailList",orderDetailList);
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){
        String userId =(String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        //获取流水号，验证是否重复提交
        String tradeNo = request.getParameter("tradeNo");
        boolean result = orderService.checkTradeCode(userId, tradeNo);
        if(!result){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }
        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if (!flag){
                request.setAttribute("errMsg","商品库存不足，请重新下单！");
                return "tradeFail";
            }
            //验证价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
            if (res!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"价格不匹配！");
                //价格有变化，需要重新查询价格并放入缓存
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
        }
        //保存订单并返回订单id
        String orderId = orderService.saveOrder(orderInfo);
        //删除流水号
        orderService.delTradeCode(userId);

        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }
}
