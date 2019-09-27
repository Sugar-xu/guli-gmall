package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        //设置每页数据
        skuLsParams.setPageSize(2);
        SkuLsResult searchResult = listService.search(skuLsParams);
        //获取平台属性值id
        List<String> attrValueIdList = searchResult.getAttrValueIdList();
        //根据id获取平台属性和平台属性值
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(attrValueIdList);
        //创建面包屑集合
        ArrayList<BaseAttrValue> baseAttrValues = new ArrayList<>();
        //保存检索关键字到作用域
        request.setAttribute("keyword",skuLsParams.getKeyword());
        //设置url参数
        String urlParam = makeUrlParam(skuLsParams);
        //判断url中是否有baseAttrInfoList相同的valueId,如果有则删除
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if (valueId.equals(baseAttrValue.getId())){
                            iterator.remove();

                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            //制作面包屑
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            //传递用户点击的面包屑id，重新制作新的urlparam
                            String newUrlParam = makeUrlParam(skuLsParams,valueId);
                            baseAttrValueed.setUrlParam(newUrlParam);
                            baseAttrValues.add(baseAttrValueed);
                        }
                    }

                }
             }

        }
        //保存分页数据
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        request.setAttribute("totalPages",searchResult.getTotalPages());
        //保存面包屑集合到作用域
        request.setAttribute("breadList",baseAttrValues);
        //保存url参数到作用域
        request.setAttribute("urlParam",urlParam);
        //保存平台属性信息到作用域
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);
        //保存商品信息到作用域
        request.setAttribute("skuLsResult",searchResult);

        return "list";
    }

    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam="";
        if (skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        if (skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        if (skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {
                if (excludeValueIds!=null&&excludeValueIds.length>0){
                    if (excludeValueIds[0].equals(valueId)){
                        continue;
                    }
                }
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }

        return urlParam;
    }
}
