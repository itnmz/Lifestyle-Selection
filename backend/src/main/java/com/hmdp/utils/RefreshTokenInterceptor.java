package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 刷新token拦截器
 * 拦截一切请求路径
 * 1.获取请求头中的token
 * 2.查询redis中的用户
 * 3.保存用户信息到ThreadLocal
 * 4.刷新token有效期
 * 5.放行
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token
        String token = request.getHeader("authorization");

//        StrUtil.isEmpty(token)：只判断 null 或 ""，不处理纯空格的情况。
//        StrUtil.isBlank(token)：更严格，连 " "（纯空格）也会被认为是空。
        if(StrUtil.isBlank(token)){
            return true;
        }
        //2.从token中获取用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash()
                .entries(RedisConstants.LOGIN_USER_KEY + token);
        //3.判断用户是否存在
        if(userMap.isEmpty()){
            return true;
        }

        //5.将token中的hash数据类型转换成UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);

        //6.存在则将用户保存到ThreadLocal
        UserHolder.saveUser(userDTO);

        //7.重置token过期时间，30分钟
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //8.放行
        return true;
    }

//    在一次 HTTP 请求完成之后，清理 ThreadLocal 中保存的用户信息，防止线程复用导致用户数据泄露
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
