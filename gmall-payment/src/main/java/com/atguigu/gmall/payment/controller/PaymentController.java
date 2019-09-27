package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.config.StreamUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tk.mybatis.spring.annotation.MapperScan;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.APP_ID;
import static com.alipay.api.AlipayConstants.FORMAT;

@Controller
@ComponentScan(basePackages = "com.atguigu.gmall")
@MapperScan(basePackages ="com.atguigu.gmall.payment.mapper" )
public class PaymentController {

    @Reference
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    AlipayClient alipayClient;

    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @RequestMapping("index")
    @LoginRequire
    public String index(String orderId, HttpServletRequest request){
        request.setAttribute("orderId",orderId);
        //获取总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //获取页面中的支付信息并保存到数据库
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        //保存数据到支付信息
        paymentService.savePaymentInfo(paymentInfo);

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        //设置同步回调
//        alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //设置异步回调
//        alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp");//在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        //设置参数,用map封装参数
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        alipayRequest.setBizContent(JSON.toJSONString(map));
//                "{" +
//                "    \"out_trade_no\":\"20150320010101001\"," +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                "    \"total_amount\":88.88," +
//                "    \"subject\":\"Iphone6 16G\"," +
//                "    \"body\":\"Iphone6 16G\"," +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                "    \"extend_params\":{" +
//                "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                "    }"+
//                "  }");//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        //System.out.println(form);
        return form;
    }

    //同步回调
//    http://payment.gmall.com/alipay/callback/return
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){

        return "redirect:"+AlipayConfig.return_order_url;
    }

    //异步回调，异步回调是支付宝直接访问服务器，由于没有外网服务器也没用内网穿透，此处只是模拟异步回调
    @RequestMapping("alipay/callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request)  {
        //验签传递的参数封装在paramMap中
        boolean flag = false;
        try {
            //验签
            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (flag){
            //交易状态
            String trade_status = paramMap.get("trade_status");
            //判断交易状态
            if ("TRADE_SUCCESS".equals(trade_status)||"TRADE_FINISHED".equals(trade_status)){
                //更新交易状态与回调时间
                //根据返回的交易单号查数据库是否有无此交易信息
                String out_trade_no = paramMap.get("out_trade_no");
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                //判断订单状态
                if(paymentInfo.getPaymentStatus()==PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }
                //更新状态和回调时间,内容
                PaymentInfo paymentInfo1 = new PaymentInfo();
                paymentInfo1.setPaymentStatus(PaymentStatus.PAID);
                paymentInfo1.setCallbackTime(new Date());
                paymentInfo1.setCallbackContent(JSON.toJSONString(paramMap));
                paymentService.updatePaymentInfo(out_trade_no,paymentInfo1);
                //发送消息给订单：orderId,result
                return "success";
            }
        }else {
            return "failure";
        }
        return "failure";
    }
    //支付宝退款
    //http://payment.gmall.com/refund?orderId=
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        boolean flag = paymentService.refund(orderId);
        return flag+"";
    }

    //微信支付接口
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(String orderId){
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        Map map = paymentService.createNative(orderInfo);
        return map;
    }

    //异步回调是微信直接访问服务器，由于没有外网服务器也没用内网穿透，此处只是模拟异步回调
    @RequestMapping("wx/callback/notify")
    public String callbackWX(HttpServletRequest request,HttpServletResponse response) throws Exception {
        //微信是以流的形式返回结果，所以从流中获取数据
        ServletInputStream inputStream = request.getInputStream();
        String xmlString = StreamUtil.inputStream2String(inputStream,"UTF-8");
        //验签
        if (WXPayUtil.isSignatureValid(xmlString, partnerkey)){
            //获取订单状态
            Map<String, String> paramMap = WXPayUtil.xmlToMap(xmlString);
            String result_code = paramMap.get("result_code");
            if (result_code!=null&&result_code.equals("SUCCESS")){
                //准备返回值,以xml的形式返回
                Map<String, String> returnMap = new HashMap<>();
                returnMap.put("return_code","SUCCESS");
                returnMap.put("return_msg","OK");
                String returnXml = WXPayUtil.mapToXml(returnMap);
                //设置返回的媒体类型为xml，否则无法被接收
                response.setContentType("text/xml");
                return returnXml;
            }
        }else {
            return null;
        }
        return null;
    }

    //发送异步通知
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "发送成功！";
    }
}
