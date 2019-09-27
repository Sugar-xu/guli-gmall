package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //查询当前skuId的销售属性值，并锁定
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        //得到属性组合与skuid的映射关系 ，用于页面根据属性组合进行跳转
        Map skuValueIdsMap = manageService.getSkuValueIdsMap(skuInfo.getSpuId());
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        listService.incrHotScore(skuId);
        return "item";
    }
}

