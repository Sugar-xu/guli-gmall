package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

public interface OrderService {
    String saveOrder(OrderInfo orderInfo);

    String getTradeNo(String userId);

    boolean checkTradeCode(String userId,String tradeCodeNo);

    void delTradeCode(String userId);

    boolean checkStock(String skuId, Integer skuNum);

    OrderInfo getOrderInfo(String orderId);

    void updateOrderStatus(String orderId, ProcessStatus paid);
}
