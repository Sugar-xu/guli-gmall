package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMappper spuInfoMappper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 catalog2 = new BaseCatalog2();
        catalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(catalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 catalog3 = new BaseCatalog3();
        catalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(catalog3);
    }

    /*@Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        BaseAttrInfo attrInfo = new BaseAttrInfo();
        attrInfo.setCatalog3Id(catalog3Id);
        return baseAttrInfoMapper.select(attrInfo);
    }*/

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //判断baseAttrInfo中是否有id值，如果有则进行修改操作
        if (baseAttrInfo.getId() != null) {
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //先清空baseAttrValue,在保存数据，不然已有的值会重复添加
        BaseAttrValue baseAttrValueDel = new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);

        List<BaseAttrValue> baseAttrValueList = baseAttrInfo.getAttrValueList();
        if (baseAttrValueList != null && baseAttrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : baseAttrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        return baseAttrValueList;
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        List<SpuInfo> spuInfoList = spuInfoMappper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getAllBaseSaleAttr() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectAll();
        return baseSaleAttrList;
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存spuInfo
        spuInfoMappper.insertSelective(spuInfo);
        //保存spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        //保存spuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                //保存spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrList(spuId);
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfoListById(String catalogId) {
        return baseAttrInfoMapper.getBaseAttrInfoList(catalogId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //skuInfo
        skuInfoMapper.insertSelective(skuInfo);
        //skuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        //skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        //skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {

        Jedis jedis = null;
        SkuInfo skuInfo = null;
        RLock lock = null;

        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://192.168.243.130:6379");
            RedissonClient redissonClient = Redisson.create(config);
            // 使用redisson 调用getLock
            lock = redissonClient.getLock("yourLock");
            // 加锁
            lock.lock(10, TimeUnit.SECONDS);
            jedis = redisUtil.getJedis();
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            //判断缓存中有无数据
            if (jedis.exists(skuKey)) {
                //取得该value
                String skuJson = jedis.get(skuKey);
                //将字符串转换成对象
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            } else {
                skuInfo = getSkuInfoDB(skuId);
                jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            if (lock != null) {
                lock.unlock();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        skuInfo.setSkuImageList(skuImageList);
        skuInfo.setSkuAttrValueList(skuAttrValueList);
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public Map<String, Object> getSkuValueIdsMap(String spuId) {
        List<Map> skuValuesMapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        Map<String, Object> map = new HashMap<>();
        for (Map valueIdsMap : skuValuesMapList) {
            String skuId = valueIdsMap.get("sku_id") + "";
            String valueIds = (String) valueIdsMap.get("value_ids");
            map.put(valueIds, skuId);
        }
        return map;
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList) {
        String valueIds=null;
        if (attrValueIdList!=null){
            valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        }
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoByIds(valueIds);
        return baseAttrInfoList;
    }
}