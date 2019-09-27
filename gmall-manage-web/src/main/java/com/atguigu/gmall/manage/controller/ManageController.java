package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;

import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1() {

        return manageService.getCatalog1();
    }

    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        return manageService.getCatalog2(catalog1Id);
    }

    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {

        return manageService.getCatalog3(catalog2Id);
    }

    //根据三级分类id查询平台属性及值，用多表联查的方式效率更高
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfo(String catalog3Id) {

        return manageService.getBaseAttrInfoListById(catalog3Id);
    }

    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
    }

    //http://localhost:8082/getAttrValueList?attrId=23
    //根据功能写
/*
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        return manageService.getAttrValueList(attrId);
    }
*/

    //根据业务写获取平台销售属性值
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }

    //获取所有销售属性
    //http://localhost:8082/baseSaleAttrList
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList() {
        return manageService.getAllBaseSaleAttr();
    }

    //保存商品spu信息
    //http://localhost:8082/saveSpuInfo
    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        if (spuInfo != null) {
            manageService.saveSpuInfo(spuInfo);
        }
    }


}

