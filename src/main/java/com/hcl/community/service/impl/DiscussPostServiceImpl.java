package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcl.community.entity.DiscussPost;
import com.hcl.community.mapper.DiscussPostMapper;
import com.hcl.community.service.DiscussPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
@Service
public class DiscussPostServiceImpl extends ServiceImpl<DiscussPostMapper, DiscussPost> implements DiscussPostService {

    @Autowired
    DiscussPostService discussPostService;
    @Override
    public List<DiscussPost> selectDiscussPosts(int userId, int cur, int limit) {
        QueryWrapper<DiscussPost> queryWrapper=new QueryWrapper<>();
        if (userId!=0){
            queryWrapper.eq("user_id",userId);
        }
        queryWrapper.orderByDesc("type","create_time");
        queryWrapper.ne("status",2);   //状态2就是被拉黑的帖子
        Page<DiscussPost> page = new Page<>(cur,limit);
        Page<DiscussPost> discussPostPage = discussPostService.page(page, queryWrapper);
        List<DiscussPost> records = discussPostPage.getRecords();
        return records;
    }

    @Override
    public int findDiscussPostRows(int userId) {
        QueryWrapper<DiscussPost> queryWrapper=new QueryWrapper<>();
        queryWrapper.ne("status",2);
        if (userId!=0){
            queryWrapper.eq("user_id",userId);
        }
        int count = discussPostService.count(queryWrapper);
        return count;
    }
}
