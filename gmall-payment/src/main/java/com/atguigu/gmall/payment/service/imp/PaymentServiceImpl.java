package com.atguigu.gmall.payment.service.imp;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private AlipayClient alipayClient;
    @Reference
    private OrderService orderService;
    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;


    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQuery);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    @Override
    public boolean refund(String orderId) {
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("refund_amount", orderInfo.getTotalAmount());

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Map createNative(OrderInfo orderInfo) {
        //创建封装参数的map
        Map paramMap = new HashMap<>();
        paramMap.put("appid",appid);
        paramMap.put("mch_id",partner);
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        paramMap.put("body",orderInfo.getTradeBody());
        paramMap.put("out_trade_no",  "ATGUIGU-"+orderInfo.getId()+"-"+System.currentTimeMillis());
        paramMap.put("total_fee",orderInfo.getTotalAmount().multiply(new BigDecimal(100)).toBigInteger().toString());
        paramMap.put("spbill_create_ip","127.0.0.1");
        paramMap.put("notify_url","http://order.gmall.com/trade");
        paramMap.put("trade_type","NATIVE");

        try {
            //将封装好的参数转换成xml，通过httpClient调用微信接口
            String xmlParam = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setXmlParam(xmlParam);
            httpClient.setHttps(true);
            httpClient.post();
            //获得xml形式的返回结果,转换成map形式，获取codeUrl返回到页面
            String result = httpClient.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, String> map = new HashMap<>();
            map.put("code_url",resultMap.get("code_url"));
            map.put("total_fee",resultMap.get("total_fee"));
            map.put("out_trade_no",resultMap.get("out_trade_no"));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        //创建连接
        Connection connection = activeMQUtil.getConnection();

        try {
            //打开连接
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建消息生产者
            MessageProducer producer = session.createProducer(payment_result_queue);
            //创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);
            //发送消息
            producer.send(activeMQMapMessage);
            //提交
            session.commit();
            //关闭连接
            closeAll(connection, session, producer);

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    private void closeAll(Connection connection, Session session, MessageProducer producer) throws JMSException {
        producer.close();
        session.close();
        connection.close();
    }
}
