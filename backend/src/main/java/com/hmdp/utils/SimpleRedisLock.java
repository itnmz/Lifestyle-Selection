package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 自己基于Redis的setnx实现锁
 */
public class SimpleRedisLock implements ILock{

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String KEY_PREFIX = "lock:";
    private static final DefaultRedisScript UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT = new DefaultRedisScript();
        // lua脚本位置
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        // 设置返回值类型为Long
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    // 这个 ID_PREFIX 通常用于后续实现可重入锁或锁的身份标识，
    // 确保只有持有锁的线程才能解锁，防止误删其他线程的锁。
    // 配合线程 ID 使用，可以构建唯一的锁标识符。
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    // 有参构造
    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }




    @Override
    public boolean tryLock(long timeoutSec) {
        // 通常用于后续实现可重入锁或锁的身份标识     UUID + 线程ID：保证在
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        // key：前缀 + 业务名称
        // value：UUID + 线程ID
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        //拆线过程中可能出现空指针
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        // 为解决在判断是否要释放锁与释放锁之间(锁的线程和释放锁的线程不一致)，
        // 发生业务阻塞导致锁过期，线程并行执行从而导致误删锁的问题，使用lua脚本保证程序原子性。
        //调用lua脚本(Redis执行lua脚本是原子的，在执行脚本期间，不会有其他指令插入)
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
                );

    }

    /**
     *  public void unLock() {
     *         //在释放锁时，需要判断当前线程是否持有锁，只有持有锁的线程才能解锁，防止误删
     *         // 1.获取当前线程标识
     *         String threadId = ID_PREFIX + Thread.currentThread().getId();
     *
     *         // 2.获取当前锁的标识
     *         String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
     *
     *         //3.进行判断是否释放锁
     *         if (threadId.equals(id)){
     *             stringRedisTemplate.delete(KEY_PREFIX + name);
     *         }
     *     }
     */
}
