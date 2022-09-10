package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hcl.community.entity.DiscussPost;
import com.hcl.community.mapper.DiscussPostMapper;
import com.hcl.community.service.DiscussPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcl.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

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


    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        System.out.println("init==========================================================================");
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override

                    //写的是如何去数据库查数据
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {      //key是拼接的
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int cur = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存: Redis -> mysql

                        return findDiscussPosts(0, cur, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        return selectDiscussPostRows(key);
                    }
                });
    }


    public List<DiscussPost> findDiscussPosts(int userId, int cur, int limit,int orderMode){
        QueryWrapper<DiscussPost> queryWrapper=new QueryWrapper<>();
        if (userId!=0){
            queryWrapper.eq("user_id",userId);
        }
        if (orderMode==0){
            queryWrapper.orderByDesc("type","create_time");
        }else if(orderMode==1){
            queryWrapper.orderByDesc("type","score","create_time");
        }
        queryWrapper.ne("status",2);   //状态2就是被拉黑的帖子
        Page<DiscussPost> page = new Page<>(cur,limit);
        Page<DiscussPost> discussPostPage = discussPostService.page(page, queryWrapper);
        List<DiscussPost> records = discussPostPage.getRecords();
        return records;
    }

    @Override
    //orderMode为1代表热门的帖子
    public List<DiscussPost> selectDiscussPosts(int userId, int cur, int limit,int orderMode) {
        if (userId == 0 && orderMode == 1) {      //userId为0表示不是访问的自己发布的帖子 而是首页的热门帖子
            return postListCache.get(cur + ":" + limit);
        }
        return findDiscussPosts(userId, cur, limit, orderMode);
    }

    public int selectDiscussPostRows(int userId){
        QueryWrapper<DiscussPost> queryWrapper=new QueryWrapper<>();
        queryWrapper.ne("status",2);
        if (userId!=0){
            queryWrapper.eq("user_id",userId);
        }
        int count = discussPostService.count(queryWrapper);
        return count;
    }

    @Override
    public int findDiscussPostRows(int userId) {     //如果userId为0表示不是用户访问自己的帖子数量 就可以去缓存里面去找 否则去数据库里面找
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        return selectDiscussPostRows(userId);
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

    @Override
    public void updateScore(int postId, double score) {
        DiscussPost discussPost = baseMapper.selectById(postId);
        if (discussPost!=null){
            discussPost.setScore(score);
            baseMapper.updateById(discussPost);
        }
    }


}
