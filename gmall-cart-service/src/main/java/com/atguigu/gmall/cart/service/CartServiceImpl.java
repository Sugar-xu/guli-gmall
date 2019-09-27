package com.atguigu.gmall.cart.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    private RedisUtil redisUtil;

    //登录时添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //根据useId,skuId判断原来是否有此商品
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        //若有此商品只需添加数量
        if (cartInfoExist != null) {
            //此时需要添加数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            //因为skuPrice不是数据库的字段，所以进行初始化
            cartInfoExist.setSkuPrice(cartInfoExist.getSkuPrice());
            //修改数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            //更新缓存
        } else {
            //根据skuId获取skuInfo
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //为cartInfo赋值
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //保存到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            //更新缓存
            cartInfoExist = cartInfo1;//统一更新缓存使用
        }
        //更新缓存
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoExist));
        //购物车设置过期时间，一般不设置过期时间，如果设置的话可以和用户的过期时间一致
//        String userKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;
//        //获取用户的过期时间
//        Long ttl = jedis.ttl("userKey");
//        jedis.expire(cartKey, ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //从redis缓存中获取购物车列表
        //redis中如果不存在，从数据库中查，并放入缓存中
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<CartInfo> cartInfoList = new ArrayList<>();
        List<String> cartInfoJson = jedis.hvals(cartKey);
        if (cartInfoJson!=null&&cartInfoJson.size()>0){
            for (String cartInfoStr : cartInfoJson) {
                cartInfoList.add(JSON.parseObject(cartInfoStr,CartInfo.class));
            }
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        }else{
           cartInfoList = loadCartCache(userId);
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoListCk, String userId) {
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        boolean isMatch = false;
        for (CartInfo cartInfoCk : cartInfoListCk) {
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoCk.getSkuId().equals(cartInfoDB.getSkuId())){
                    cartInfoDB.setSkuNum(cartInfoCk.getSkuNum()+cartInfoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            if (!isMatch){
                cartInfoCk.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCk);
            }
        }
        List<CartInfo> cartInfoList = loadCartCache(userId);
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartInfoListCk) {
                if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    if ("1".equals(cartInfoCK.getIsChecked())){
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                        checkCart(cartInfoDB.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }

            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        //获取jedis中的购物车
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        //锁定商品
        cartInfo.setIsChecked(isChecked);
        String cartCheckedJson = JSON.toJSONString(cartInfo);
        jedis.hset(cartKey,skuId,cartCheckedJson);
        //新增到已选中购物车
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,cartCheckedJson);
        }else {
            jedis.hdel(cartCheckedKey,skuId);
        }

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        ArrayList<CartInfo> cartInfoList = new ArrayList<>();
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        //从redis中获取已选中购物车商品的集合
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedJson = jedis.hvals(cartCheckedKey);
        if (cartCheckedJson!=null&&cartCheckedJson.size()>0){
            for (String cartChecked : cartCheckedJson) {
                CartInfo cartInfo = JSON.parseObject(cartChecked, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
        }

        return cartInfoList;
    }

    public List<CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null||cartInfoList.size()==0){
            return null;
        }
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
            jedis.hset(cartKey,cartInfo.getSkuId(),cartJson);
        }
        jedis.close();
        return cartInfoList;
    }
}
