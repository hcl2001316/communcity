package com.hcl.community.service;

import com.hcl.community.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-12
 */
public interface CommentService extends IService<Comment> {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int cur, int limit);

    int selectCountByEntity(int entityType, int entityId);

    Comment selectCommentById(Integer entityId);
}
