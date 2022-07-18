package com.hcl.community.service;

import com.hcl.community.entity.LoginTicket;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-11
 */
public interface LoginTicketService extends IService<LoginTicket> {
    //插入一条登录凭证
    int insertLoginTicket(LoginTicket loginTicket);

    //根据凭证查询
    LoginTicket selectByTicket(String ticket);

    //修改状态
    int updateStatus(String ticket,int status);
}
