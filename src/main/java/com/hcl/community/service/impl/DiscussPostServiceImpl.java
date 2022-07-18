package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcl.community.entity.Comment;
import com.hcl.community.entity.DiscussPost;
import com.hcl.community.mapper.DiscussPostMapper;
import com.hcl.community.service.DiscussPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcl.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

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

    @Autowired
    SensitiveFilter sensitiveFilter;
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

    @Override
    public DiscussPost selectDiscussPostById(int id) {
        DiscussPost discussPost = baseMapper.selectById(id);
        return discussPost;
    }

    @Override
    public int insertDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        // 转义HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        // 过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        int insert = baseMapper.insert(discussPost);
        return insert;
    }

    @Override
    public boolean updateCommentCount(int id, int commentCount) {
        QueryWrapper<DiscussPost> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("entity_id",id);
        queryWrapper.eq("comment_count",commentCount);
        boolean update = discussPostService.update(queryWrapper);
        return update;
    }

    @Override
    public void updateType(int id, int type) {
        DiscussPost discussPost = baseMapper.selectById(id);
        if (discussPost!=null){
            discussPost.setType(type);
            baseMapper.updateById(discussPost);
        }
    }

    @Override
    public void updateStatus(int id, int status) {
        DiscussPost discussPost = baseMapper.selectById(id);
        if (discussPost!=null){
            discussPost.setStatus(status);
            baseMapper.updateById(discussPost);
        }
    }


}
