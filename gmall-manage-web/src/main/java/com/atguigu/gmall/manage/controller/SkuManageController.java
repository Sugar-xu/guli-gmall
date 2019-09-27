package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    //http://www.gmall.com/spuImageList?spuId=58
    //根据spuId获取图片
    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }

    //http://www.gmall.com/spuSaleAttrList?spuId=58
    //根据spuId查询销售属性及值
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrListBySpuId(spuId);
    }

    //http://www.gmall.com/saveSkuInfo
    //保存sku信息
    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        if(skuInfo!=null){
            manageService.saveSkuInfo(skuInfo);
        }

    }
    //商品上架，将商品信息保存到ES中
    @RequestMapping("onSale")
    public void onSale(String skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuInfo(skuLsInfo);
    }
}
