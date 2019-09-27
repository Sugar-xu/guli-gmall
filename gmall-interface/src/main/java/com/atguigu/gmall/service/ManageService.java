package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;
import java.util.Map;

public interface ManageService {

    //获取一级分类
    List<BaseCatalog1> getCatalog1();

    //获取二级分类
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    //获取三级分类
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类获取平台属性
    //List<BaseAttrInfo> getAttrList(String catalog3Id);

    //保存属性信息
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //获取平台属性值（根据功能）
    List<BaseAttrValue> getAttrValueList(String attrId);

    //根据平台属性id获取平台属性（根据业务）
    //先根据id查询是否有无此平台属性，再获取此平台属性值列表
    BaseAttrInfo getAttrInfo(String attrId);

    //获取spu属性信息
    List<SpuInfo> getSpuList(SpuInfo spuInfo);

    //获取所有销售属性
    List<BaseSaleAttr> getAllBaseSaleAttr();

    void saveSpuInfo(SpuInfo spuInfo);

    List<SpuImage> getSpuImageList(SpuImage spuImage);

    List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String spuId);

    List<BaseAttrInfo> getBaseAttrInfoListById(String catalogId);

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(String skuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    Map<String,Object> getSkuValueIdsMap(String spuId);

    List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList);
}
