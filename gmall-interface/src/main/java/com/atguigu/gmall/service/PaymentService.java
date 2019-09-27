package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    void updatePaymentInfo(String out_trade_no,PaymentInfo paymentInfo);

    boolean refund(String orderId);

    Map createNative(OrderInfo orderInfo);

    void sendPaymentResult(PaymentInfo paymentInfo,String result);
}
