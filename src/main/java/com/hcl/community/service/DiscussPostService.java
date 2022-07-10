package com.hcl.community.service;

import com.hcl.community.entity.DiscussPost;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
public interface DiscussPostService extends IService<DiscussPost> {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    public int findDiscussPostRows(int userId);
}
