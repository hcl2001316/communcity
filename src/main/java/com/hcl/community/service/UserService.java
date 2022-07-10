package com.hcl.community.service;

import com.hcl.community.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
public interface UserService extends IService<User> {
    public User findUserById(String id);

    public Map<String, Object> register(User user);

    public int activation(int userId, String code);
}
