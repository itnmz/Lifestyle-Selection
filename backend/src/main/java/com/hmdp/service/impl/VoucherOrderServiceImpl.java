package com.hmdp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.config.RabbitMQConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {

    @Resource
    private SeckillVoucherServiceImpl seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient; // 使用Redisson实现分布式锁（可重入、可重试、超时机制）
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ObjectMapper objectMapper;

    // 执行lua脚本
    private static final DefaultRedisScript SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript();
        // lua脚本位置，文件名
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        // 设置返回类型 0 1 2
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /*
     * //定义阻塞队列
     * private static final BlockingQueue<VoucherOrder> orderTasks = new
     * ArrayBlockingQueue<>(1024);
     * //创建线程池
     * private static final ExecutorService SECKILL_ORDER_EXECUTOR =
     * Executors.newSingleThreadExecutor();
     * 
     * @PostConstruct
     * private void into(){
     * SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
     * }
     * 
     * //线程任务
     * private static class VoucherOrderHandler implements Runnable{
     * 
     * @Override
     * public void run() {
     * 
     * }
     * }
     */

    /**
     * 创建优惠劵订单
     * 
     * @param voucherId
     * @return
     */
    /**
     * public Result saveVoucher(Long voucherId) {
     * //1.查询秒杀优惠劵信息q
     * SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
     * 
     * //2.判断秒杀是否开始
     * if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
     * //秒杀尚未开始
     * return Result.fail("秒杀尚未开始");
     * }
     * 
     * //3.判断秒杀是否结束
     * if(voucher.getEndTime().isBefore(LocalDateTime.now())){
     * //秒杀已经结束
     * return Result.fail("秒杀已经结束");
     * }
     * 
     * //4.判断库存是否充足
     * if(voucher.getStock() < 1){
     * return Result.fail("库存不足");
     * }
     * 
     * 
     * 
     * Long userId = UserHolder.getUser().getId();
     * //使用用户id作为锁
     * // 等事务提交完再释放锁
     * 
     * // * 当在集群中使用 synchronized 锁时,会出现问题
     * // * synchronized (userId.toString().intern()) {
     * // * //获取代理对象（事务）
     * // * // Spring 的 @Transactional 是通过 AOP 动态代理实现的
     * // * // 当外部调用 saveVoucher() 时,会经过代理对象,事务正常
     * // * // 但在类内部使用 this.createVoucherOrder() 时,绕过了代理对象,直接调用目标方法,导致事务注解失效
     * // SimpleRedisLock lock = new SimpleRedisLock("order" + userId,
     * stringRedisTemplate);
     * //使用Redisson创建锁
     * RLock lock = redissonClient.getLock("lock:order" + userId);
     * boolean b = lock.tryLock();//等待时长（默认是0），超时释放时间（默认30s），单位
     * //判断是否获取锁成功
     * if(!b){
     * //获取锁失败，返回错误信息
     * return Result.fail("一人只能购买一张优惠劵");
     * }
     * 
     * try {
     * //获取代理对象（事务）
     * IVoucherOrderService proxy = (IVoucherOrderService)
     * AopContext.currentProxy();
     * return proxy.createVoucherOrder(voucherId, voucher);
     * } finally {
     * lock.unlock();
     * }
     * }
     */


    /**
     * 秒杀优惠劵
     * @param voucherId
     * @return
     */
    public Result saveVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 生成订单全局唯一ID
        long orderId = redisIdWorker.nextId("order");

        // 1.执行lua脚本
        Long result = (Long) stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                // 参数 key
                Collections.emptyList(),
                // 参数 value (优惠劵id、用户id、订单id)
                voucherId.toString(), userId.toString(), String.valueOf(orderId));
        // 返回值0：代表抢购成功，1：代表库存不足，2：代表重复下单
        int r = result.intValue();

        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        // 2.2 为0，代表可以下单，将订单信息保存到阻塞队列
        // 2.3 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.4 订单id，使用全局唯一id生成器
        voucherOrder.setId(orderId);
        // 2.5 下单用户
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        // //2.6 将订单信息放入阻塞队列中
        // orderTasks.add(voucherOrder);

        try {
            // 2.6 将订单信息放入rabbitmq中
            // 参数：交换机，routingKey，订单对象
            /**
             * TODO 目前的 saveVoucher 方法中，如果 convertAndSend 执行完，
             * TODO 但 RabbitMQ 宕机或网络抖动导致消息没发成功，用户收到了成功提示，但数据库最终没订单。
             * TODO 完善： 开启 RabbitMQ 的 publisher-confirm（发布确认）和 publisher-returns（回退机制）。
             */
            // 自动转换消息类型(通过配置的消息转换器)，并发送消息
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKIL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKIL_ORDER_ROUTING_KEY,
                    voucherOrder// 要传输的对象
            );
            // 3.返回订单id
            return Result.ok(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("下单失败");
        }
    }

    /**
     * 监听mq队列消息，执行数据库操作
     * 提取到单独的监听类中 SeckillVoucherListener
     * /**
     * TODO 事务失效的原因，如果方法调用没有经过这个“代理对象”(Spring对当前类做动态代理)，事务就会彻底失效
     * 内部调用：listenerSeckillVoucherOder 方法调用 createVoucherOrder
     * 是在同一个类内部调用，而不是通过Spring代理对象调用
     * TODO AOP代理机制：Spring的事务管理基于AOP代理，只有通过代理对象调用带 @Transactional 注解的方法才会触发事务管理
     */
    // @RabbitListener(queues = RabbitMQConfig.SECKIL_ORDER_QUEUE)
    // public void listenerSeckillVoucherOder(VoucherOrder voucherOrder){
    // try {
    // //获取代理对象（事务）
    // IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
    // proxy.createVoucherOrder(voucherOrder);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

    /**
     * 创建优惠劵订单
     * @param voucherOrder
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 没必要再检查一人一单了
        /*
         * Long count = lambdaQuery()
         * .eq(VoucherOrder::getUserId, userId)
         * .eq(VoucherOrder::getVoucherId, voucherId)
         * .count();
         * 
         * if (count > 0) {
         * return;
         * }
         */

        // SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        boolean success = seckillVoucherService.lambdaUpdate()
                // .set(SeckillVoucher::getStock, seckillVoucher.getStock() - 1)
                .setSql("stock = stock - 1")
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .gt(SeckillVoucher::getStock, 0)// 乐观锁
                .update();

        if (!success) {
            throw new RuntimeException("库存不足");
        }
        // 添加订单
        save(voucherOrder);

    }

    /*
     * @Transactional
     * public Result createVoucherOrder(Long voucherId, Seck illVoucher voucher) {
     * 
     * Long userId = UserHolder.getUser().getId();
     * //5.一人一单
     * // 当一个账号多人操作时会出现并发安全问题
     * Long count = lambdaQuery()
     * .eq(VoucherOrder::getUserId, userId)
     * .eq(VoucherOrder::getVoucherId, voucherId)
     * .count();
     * 
     * if (count > 0) {
     * return Result.fail("用户已经购买一次");
     * }
     * 
     * //6.扣减库存，(防止并发安全问题需要加锁)
     * //gt：大于、ge：大于等于、lt：小于、le：小于等于、ne：不等于
     * boolean success = seckillVoucherService.lambdaUpdate()
     * .set(SeckillVoucher::getStock, voucher.getStock() - 1)
     * .eq(SeckillVoucher::getVoucherId, voucherId)
     * .gt(SeckillVoucher::getStock, 0)//乐观锁
     * .update();
     * if (!success) {
     * //扣减库存失败
     * return Result.fail("库存不足");
     * }
     * 
     * //6.创建订单
     * VoucherOrder voucherOrder = new VoucherOrder();
     * //订单id，使用全局唯一id生成器
     * long orderId = redisIdWorker.nextId("order");
     * voucherOrder.setId(orderId);
     * //下单用户
     * voucherOrder.setUserId(userId);
     * voucherOrder.setVoucherId(voucherId);
     * save(voucherOrder);
     * 
     * //7.返回订单id
     * return Result.ok(orderId);
     * }
     */
}
