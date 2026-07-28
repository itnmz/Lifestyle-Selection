package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.impl.FollowServiceImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author nmz
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private FollowServiceImpl followService;


    @PutMapping("/{followUserId}/{isFollow}")
    public Result follow(@PathVariable("followUserId") Long followUserId, @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    private Result isFollow(@PathVariable("id") Long id){
        return followService.isFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long id){
        return followService.common(id);
    }
}
