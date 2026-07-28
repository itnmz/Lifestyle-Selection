package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    Result queryById(Long id);

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    Result updateShop(Shop shop);

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

    /**
     * 根据商铺名称分页查询商铺信息
     * @param name 商铺名称
     * @param current 页码
     * @return 商铺列表
     */
    Result queryShopByName(String name, Integer current);
}
