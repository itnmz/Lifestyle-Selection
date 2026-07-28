package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 查询热门博客
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 查询博客详情
     * @param id
     * @return
     */
    Result queryBlogById(Long id);

    /**
     * 修改点赞数量
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 查询点赞TOP5用户
     * @param id
     * @return
     */
    Result queryLikeById(Long id);

    /**
     * 发布博客
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * 实现滚动分页查询
     */
    Result queryBlogOfFollow(Long max, Integer offset);

    /**
     * 查询当前登录用户博客
     * @param current
     * @return
     */
    Result queryMyBlog(Integer current);

    /**
     * 查询指定用户博客
     * @param current
     * @param id
     * @return
     */
    Result queryBlogUserId(Integer current, Long id);
}
