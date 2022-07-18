package com.hcl.community.service;

import com.hcl.community.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpSession;
import java.util.Collection;
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
     User findUserById(Integer id);

     Map<String, Object> register(User user);

     int activation(int userId, String code);

     User selectByName(String username);

     Map<String, Object> login(String username, String password, long expiredSeconds);

    void logout(String ticket);

    void updateHeader(Integer id, String headerUrl);

    Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword, String confirmPassword);

    User getByEmail(String email);

    Map<String, Object> getCode(String email);

    Map<String, Object> forget(String email, String verifycode, String password, HttpSession session);

    Collection<? extends GrantedAuthority> getAuthorities(int userId);
}
