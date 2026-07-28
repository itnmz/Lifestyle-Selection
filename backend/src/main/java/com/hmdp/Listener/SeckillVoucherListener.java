package com.hmdp.Listener;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消息监听器
 */
@Slf4j
@Component
public class SeckillVoucherListener {

    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 监听秒杀订单队列
     * 这里的第二个参数 Channel 和 第三个参数 Message 是可选的，用于手动确认(ACK)
     */
    @RabbitListener(queues = RabbitMQConfig.SECKIL_ORDER_QUEUE)
    public void handleSeckillOrder(VoucherOrder voucherOrder) {
        log.info("收到秒杀订单消息，订单ID：{}", voucherOrder.getId());
        try {
            // 调用 Service 层的方法，此时 Service 是由 Spring 注入的代理对象，事务会生效
            voucherOrderService.createVoucherOrder(voucherOrder);
        } catch (Exception e) {
            log.error("处理秒杀订单异常", e);
            // 注意：如果配置了手动确认，这里需要根据业务决定是否 nack 重新入队
            // 如果不手动确认，Spring AMQP 默认会根据配置决定是否重试
        }
    }
}
