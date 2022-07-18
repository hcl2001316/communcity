package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hcl.community.entity.LoginTicket;
import com.hcl.community.mapper.LoginTicketMapper;
import com.hcl.community.service.LoginTicketService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcl.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-11
 */
@Service
public class LoginTicketServiceImpl extends ServiceImpl<LoginTicketMapper, LoginTicket> implements LoginTicketService {


    @Autowired
    RedisTemplate redisTemplate;
    @Override
    public int insertLoginTicket(LoginTicket loginTicket) {
        int insert = baseMapper.insert(loginTicket);
        return insert;
    }

    @Override
    public LoginTicket selectByTicket(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    @Override
    public int updateStatus(String ticket, int status) {
        LoginTicket loginTicket = this.selectByTicket(ticket);
        loginTicket.setStatus(status);
        int update = baseMapper.updateById(loginTicket);
        return update;
    }
}
