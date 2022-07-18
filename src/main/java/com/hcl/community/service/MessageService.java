package com.hcl.community.service;

import com.hcl.community.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-13
 */
public interface MessageService extends IService<Message> {
    // 查询当前用户的会话列表,针对每个会话只返回一条最新的私信.
    List<Message> selectConversations(int userId, int offset, int limit);

    // 查询当前用户的会话数量.
    int selectConversationCount(int userId);

    // 查询某个会话所包含的私信列表.
    List<Message> selectLetters(String conversationId, int offset, int limit);

    // 查询某个会话所包含的私信数量.
    int selectLetterCount(String conversationId);

    // 查询未读私信的数量
    int selectLetterUnreadCount(int userId, String conversationId);

    //发送私信
    int addMessage(Message message);

    //用户点击未读消息之后修改为已读
    void readMessage(List<Integer> ids);

    //查看最后的通知
     Message findLatestNotice(int userId, String topic);

     //查看你通知的数量 方便做分页
     int findNoticeCount(int userId, String topic);

     //查询用户还未读的通知数量
     int findNoticeUnreadCount(int userId, String topic);

     //查询某个主题的所有的通知
     List<Message> findNotices(int userId, String topic, int offset, int limit);
}
