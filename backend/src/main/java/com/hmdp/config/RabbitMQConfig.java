package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 * 声明交换机和队列
 */
@Configuration
public class RabbitMQConfig {
    public static final String SECKIL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKIL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKIL_ORDER_ROUTING_KEY = "seckill.order";

    // 创建交换机
    @Bean
    public DirectExchange seckillOrderExchange(){
        return new DirectExchange(SECKIL_ORDER_EXCHANGE);
    }

    // 创建队列
    @Bean
    public Queue seckillOrderQueue(){
        return new Queue(SECKIL_ORDER_QUEUE,true);
    }

    // 绑定队列和交换机
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SECKIL_ORDER_ROUTING_KEY);
    }
}
