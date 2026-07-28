package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询所有商铺类型
     * @return 商铺类型列表
     */
    public Result queryList() {
        String key = CACHE_SHOP_TYPE_KEY;
        //1.在redis中查询商铺类型
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);

        //2.如果命中，直接返回
        if(StrUtil.isNotBlank(shopTypeJson)){
            //将字符串转换为商铺类型list 并返回
            return Result.ok(JSONUtil.toList(shopTypeJson, ShopType.class));
        }

        //3.未命中，查询数据库
        List<ShopType> shopTypes = query()
                .orderByAsc("sort")//排序
                .list();

        //4.若数据库中不存在，则返回错误
        if(shopTypes == null){
            return Result.fail("店铺类型不存在");
        }

        //5.数据库中存在，则返回数据并将查询到的数据存储到（序列化）redis中
        String jsonStr = JSONUtil.toJsonStr(shopTypes);
        stringRedisTemplate.opsForValue().set(key,jsonStr);

        return Result.ok(shopTypes);
    }
}
