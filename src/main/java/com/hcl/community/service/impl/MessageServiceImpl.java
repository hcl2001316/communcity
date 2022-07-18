package com.hcl.community.service.impl;

import com.hcl.community.entity.Message;
import com.hcl.community.mapper.MessageMapper;
import com.hcl.community.service.MessageService;
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
 * @since 2022-07-13
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Autowired
    MessageMapper messageMapper;

    @Autowired
    SensitiveFilter sensitiveFilter;

    @Override
    public List<Message> selectConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    @Override
    public int selectConversationCount(int userId) {
       return messageMapper.selectConversationCount(userId);
    }

    @Override
    public List<Message> selectLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId,offset,limit);
    }

    @Override
    public int selectLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    @Override
    public int selectLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId,conversationId);
    }

    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));  //过滤掉标签和敏感字符
        message.setContent(sensitiveFilter.filter(message.getContent()));
        int insert = baseMapper.insert(message);
        return insert;
    }

    public void readMessage(List<Integer> ids) {
        for (Integer id : ids) {
            Message message = baseMapper.selectById(id);
            if (message!=null){
                message.setStatus(1);

            }
            baseMapper.updateById(message);
        }
    }

    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }

    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
}
