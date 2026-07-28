package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOW_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserServiceImpl userService;

    /**
     * 关注或取关
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + userId;
        //1.查询是否关注
        if(isFollow){
            //2.关注，插入数据
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean bool = save(follow);
            if(bool){
                //将关注用户添加到Redis的set集合中，从而判断共同关注
                //key，followUserId
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }

        }else {
            //3.取关，删除数据
            boolean remove = lambdaUpdate()
                    .eq(Follow::getFollowUserId, followUserId)
                    .eq(Follow::getUserId, userId)
                    .remove();
            if (remove){
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注
     * @param id
     * @return
     */
    @Override
    public Result isFollow(Long id) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();

        Long count = lambdaQuery()
                .eq(Follow::getFollowUserId, id)
                .eq(Follow::getUserId, userId)
                .count();

        //判断结果
        if(count != null && count > 0){
            return Result.ok(true);
        }
        return Result.ok(false);
    }

    @Override
    public Result common(Long id) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key1 = FOLLOW_KEY + userId;//当前用户
        String key2 = FOLLOW_KEY + id;//关注用户

        //1.查询交集
        Set<String> intersect = stringRedisTemplate.opsForSet()
                .intersect(key1, key2);
        if(intersect == null || intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        //2.解析id集合
        List<Long> ids = intersect.stream()
                //它调用 Long 类的静态方法 valueOf(String s)，
                // 将 Redis 查出来的字符串（String）解析成包装类 Long
                .map(Long::valueOf)// 对流中的每一个元素，执行一次转换操作
                .collect(Collectors.toList());// 关闭流，并将处理后的元素装进一个新的 List 容器里
        //3.返回数据
        List<UserDTO> userDTOList = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }
}
