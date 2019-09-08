package com.atguigu.gmall.usermanage.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    //获取所有用户
    @GetMapping("getUserAll")
    public List<UserInfo> getUserAll(){
        List<UserInfo> userInfoList = userService.getUserInfoListAll();
        return userInfoList;
    }
    //添加用户
    @PostMapping("saveUser")
    public String saveUser(UserInfo userInfo){
        userService.addUser(userInfo);
        return"ok";
    }

    //根据id修改用户
    @PostMapping("updateUserById")
    public String updateUserById(UserInfo userInfo){
        userService.updateUser(userInfo);
        return"ok";
    }

    //根据用户名修改用户
    @PostMapping("updateUserByName")
    public String updateUserByName(UserInfo userInfo){
        userService.updateUserByName(userInfo);
        return"ok";
    }

    //根据id删除用户
    @DeleteMapping("deleteUserById")
    public String deleteUserById(UserInfo userInfo){
        userService.delUser(userInfo);
        return"ok";
    }
}
