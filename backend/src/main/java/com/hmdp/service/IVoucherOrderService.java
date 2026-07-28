package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 创建（秒杀）优惠劵订单
     * @param voucherId
     * @return
     */
    Result saveVoucher(Long voucherId);


    /**
     * 为使事务有效，不能在类内部调用，而是使用代理对象调用
     * @param voucherOrder
     */
    void createVoucherOrder(VoucherOrder voucherOrder);


}
