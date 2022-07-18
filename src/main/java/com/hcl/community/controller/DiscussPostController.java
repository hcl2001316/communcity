package com.hcl.community.controller;


import com.hcl.community.entity.*;
import com.hcl.community.event.EventProducer;
import com.hcl.community.service.CommentService;
import com.hcl.community.service.DiscussPostService;
import com.hcl.community.service.LikeService;
import com.hcl.community.service.UserService;
import com.hcl.community.util.CommunityConstant;
import com.hcl.community.util.CommunityUtil;
import com.hcl.community.util.HostHolder;
import com.hcl.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
@Controller
@RequestMapping("discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    UserService userService;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    CommentService commentService;

    @Autowired
    LikeService likeService;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping(path = "/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {


        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.insertDiscussPost(post);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);


        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());


        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "发布成功!");
    }



    /*
    * 根据帖子id查询帖子的信息 包括了帖子的评论 和评论的评论
    * */
    @Transactional
    @GetMapping(path = "/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        // 根据帖子id查询出当前的帖子和帖子的作者
        DiscussPost post = discussPostService.selectDiscussPostById(discussPostId);
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("post", post);
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论: 给帖子的评论
        // 回复: 给评论的评论
        //首先查询给帖子的评论 就是根据帖子的id和entityTpye=1来查  comment里面的entityId就是评论表中的id
        List<Comment> commentList = commentService.selectCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getCurrent(), page.getLimit());

        // 封装评论以及评论的评论的集合
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        //遍历上面的帖子的评论 存入每一个评论里面的评论
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 存放帖子的每一个评论和归属其的子评论
                Map<String, Object> commentVo = new HashMap<>();
                // 将每一个帖子的评论存放 包括作者信息
                commentVo.put("comment", comment);
                // 作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);


                // 查询当前评论的子评论entityType=2代表评论的评论， comment.getId()是父评论的id
                List<Comment> replyList = commentService.selectCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 存放每一个评论的子评论的集合
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        // 将子评论和子评论的作者信息以及被评论的作者信息存放 reply.getTargetId()就是被评论的作者的信息
                        replyVo.put("reply", reply);
                        // 作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        //将子评论放入父评论
                        replyVoList.add(replyVo);
                    }
                }
                //将帖子中的某一个评论和子评论都加入到这个集合中
                commentVo.put("replys", replyVoList);
                // 回复数量，在帖子表中有当前帖子的回复数量，根据帖子的id去comment表中查询有多少条数据就是帖子的回复数量
                int replyCount = commentService.selectCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                //将这些封装好的数据加入到集合中去
                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVoList);
        model.addAttribute("page", page);
        return "/site/discuss-detail";
    }

    // 置顶、取消置顶
    @PostMapping(path = "/top")
    @ResponseBody
    public String setTop(int id) {
        DiscussPost discussPostById = discussPostService.selectDiscussPostById(id);
        // 获取置顶状态，1为置顶，0为正常状态,1^1=0 0^1=1
        int type = discussPostById.getType()^1;
        discussPostService.updateType(id, type);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);

        // 触发发帖事件(更改帖子状态)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 加精、取消加精
    @PostMapping(path = "/wonderful")
    @ResponseBody
    public String setWonderful(int id) {
        DiscussPost discussPostById = discussPostService.selectDiscussPostById(id);
        int status = discussPostById.getStatus()^1;
        // 1为加精，0为正常， 1^1=0, 0^1=1
        discussPostService.updateStatus(id, status);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);

        // 触发发帖事件(更改帖子类型)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);


        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);
        return CommunityUtil.getJSONString(0, null, map);
    }

    // 删除
    @PostMapping(path = "/delete")
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

}

