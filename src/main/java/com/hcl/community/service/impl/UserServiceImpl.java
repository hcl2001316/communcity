package com.hcl.community.service.impl;

import com.hcl.community.entity.User;
import com.hcl.community.mapper.UserMapper;
import com.hcl.community.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findUserById(String  id) {
        return baseMapper.selectById(id);
    }
}
