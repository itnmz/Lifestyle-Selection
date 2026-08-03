package com.hmdp.Listener;

import com.hmdp.config.RabbitMQConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;

import javax.annotation.Resource;
import com.rabbitmq.client.Channel;

import java.io.IOException;

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
    public void handleSeckillOrder(VoucherOrder voucherOrder, Message message,
                                   Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("收到秒杀订单消息，订单ID：{}", voucherOrder.getId());

        try {
            // 调用 Service 层的方法，此时 Service 是由 Spring 注入的代理对象，事务会生效
            voucherOrderService.createVoucherOrder(voucherOrder);

            // 1. 业务处理成功，手动确认消息
            // 第二个参数 multiple: false 表示仅确认当前这条消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理秒杀订单异常，订单ID：{}", voucherOrder.getId(), e);
            // 注意：如果配置了手动确认，这里需要根据业务决定是否 nack 重新入队
            // 如果不手动确认，Spring AMQP 默认会根据配置决定是否重试

            // 2. 业务处理失败，拒绝消息
            // 第三个参数 requeue: false 表示不重新入队（直接丢弃或进入死信队列）
            // 如果设置为 true，消息会重新回到队列头部继续消费，如果代码依然报错会导致死循环！
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
