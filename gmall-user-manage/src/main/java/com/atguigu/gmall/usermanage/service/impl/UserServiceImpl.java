package com.atguigu.gmall.usermanage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> addressList = userAddressMapper.select(userAddress);
        return addressList;
    }

    @Override
    public List<UserInfo> getUserInfoListAll() {

        return  userMapper.selectAll();
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userMapper.insertSelective(userInfo);
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",userInfo.getName());
        userMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void delUser(UserInfo userInfo) {
        userMapper.deleteByPrimaryKey(userInfo);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPwd);
        //判断是否有此用户
        UserInfo user = userMapper.selectOne(userInfo);
        //生成token并放入缓存
        if (user!=null){
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix+user.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(user));
            jedis.close();
            return user;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis=null;
        try {
            jedis = redisUtil.getJedis();
            String key=userKey_prefix+userId+userinfoKey_suffix;
            String userJson = jedis.get(key);

            if (!StringUtils.isEmpty(userJson)){
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return userInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        return null;
    }
}
