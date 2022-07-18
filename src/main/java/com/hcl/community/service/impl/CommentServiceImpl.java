package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcl.community.entity.Comment;
import com.hcl.community.mapper.CommentMapper;
import com.hcl.community.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcl.community.service.DiscussPostService;
import com.hcl.community.util.CommunityConstant;
import com.hcl.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-12
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService, CommunityConstant {
    @Autowired
    SensitiveFilter sensitiveFilter;

    @Autowired
    DiscussPostService discussPostService;

    @Override
    public List<Comment> selectCommentsByEntity(int entityType, int entityId, int cur, int limit) {
        Page<Comment> page=new Page<>(cur,limit);
        QueryWrapper<Comment> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("entity_type",entityType);
        queryWrapper.eq("entity_id",entityId);
        queryWrapper.eq("status",0);
        queryWrapper.orderByAsc("create_time");
        List<Comment> comments = baseMapper.selectPage(page,queryWrapper).getRecords();
        return comments;
    }

    @Override
    public int selectCountByEntity(int entityType, int entityId) {
        QueryWrapper<Comment> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("entity_type",entityType);
        queryWrapper.eq("entity_id",entityId);
        queryWrapper.eq("status",0);
        Integer count = baseMapper.selectCount(queryWrapper);
        return count;
    }

    @Override
    public Comment selectCommentById(Integer entityId) {
        return baseMapper.selectById(entityId);
    }


    //添加评论
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        // 添加评论  过滤敏感字符
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = baseMapper.insert(comment);
        // 更新帖子评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = this.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }
}
