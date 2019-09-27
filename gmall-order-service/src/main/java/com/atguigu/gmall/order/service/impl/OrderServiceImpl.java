package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //数据不完整，还需设置总金额，订单状态，订单交易编号，创建时间，失效时间，进度状态
        //总金额
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //订单交易编号
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+ new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建时间
        orderInfo.setCreateTime(new Date());
        //失效时间，设置为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfoMapper.insert(orderInfo);
        //保存订单详情
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //生成token并放入redis中
        String tradeCodeNo= UUID.randomUUID().toString();
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey = "user:"+userId+":tradeCode";
        jedis.setex(tradeNoKey,10*60,tradeCodeNo);
        jedis.close();
        return tradeCodeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        String tradeNoKey = "user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if (tradeCode!=null&&tradeCode.equals(tradeCodeNo)){
            return true;
        }
        return false;
    }

    @Override
    public void delTradeCode(String userId) {
        String tradeNoKey = "user:"+userId+":tradeCode";
        Jedis jedis = redisUtil.getJedis();
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        //获取orderInfo,并将订单详情放入orderInfo
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }
}
