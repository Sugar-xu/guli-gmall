package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    void addToCart(String skuId,String userId,Integer skuNum);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoListCk, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
