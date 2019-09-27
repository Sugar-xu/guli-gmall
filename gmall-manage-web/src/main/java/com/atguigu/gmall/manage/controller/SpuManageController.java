package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    //http://localhost:8082/spuList?catalog3Id=61 通过三级分类Id获取销售属性
    //此处将参数catalog3Id封装为spuInfo对象接收，由于传递的参数不是json,所以不需要加@RequestBody
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo){

       return manageService.getSpuList(spuInfo);
    }
}
