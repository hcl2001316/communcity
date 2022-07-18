package com.hcl.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hcl.community.entity.LoginTicket;
import com.hcl.community.entity.User;
import com.hcl.community.mapper.UserMapper;
import com.hcl.community.service.LoginTicketService;
import com.hcl.community.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hcl.community.util.CommunityConstant;
import com.hcl.community.util.CommunityUtil;
import com.hcl.community.util.MailClient;
import com.hcl.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService,CommunityConstant{


    @Autowired
    LoginTicketService loginTicketService;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private UserService userService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("username",user.getUsername());
        User u= baseMapper.selectOne(queryWrapper);
        // 验证账号
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        // 验证邮箱
        User u1= baseMapper.selectOne(queryWrapper);
        u1 = baseMapper.selectOne(new QueryWrapper<User>().eq("email",user.getEmail()));
        if (u1 != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }
        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        //普通用户
        user.setType(0);
        //刚注册的时候都是未激活
        user.setStatus(0);
        //生成一个激活码
        user.setActivationCode(CommunityUtil.generateUUID());
        //随机生成一个头像
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        baseMapper.insert(user);

        // 激活邮件
        Context context = new Context();
        //携带参数 用户的邮箱地址
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }
    @Override
    public User findUserById(Integer  id) {
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public int activation(int userId, String code) {
        User user = baseMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return CommunityConstant.ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            user.setStatus(1);
            baseMapper.updateById(user);
            return CommunityConstant.ACTIVATION_SUCCESS;
        } else {
            return CommunityConstant.ACTIVATION_FAILURE;
        }
    }

    @Override
    public User selectByName(String username) {
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("username",username);
        User user = baseMapper.selectOne(queryWrapper);
        return user;
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = this.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    @Override
    public void updateHeader(Integer userId, String headerUrl) {
        User user = this.findUserById(userId);
        user.setHeaderUrl(headerUrl);
        int update = baseMapper.updateById(user);
        if (update!=0){
            clearCache(userId);
        }
    }

    //忘记密码之后给邮箱发送验证码
    public Map<String, Object> getCode(String email) {
        Map<String,Object> map = new HashMap<>();
        //空值判断
        if (StringUtils.isBlank(email)){
            map.put("emailMsg","请输入邮箱！");
            return map;
        }
        //邮箱是否正确
        User user = this.getByEmail(email);
        if (user == null){
            map.put("emailMsg","该邮箱还未注册过，请注册后再使用！");
            return map;
        }
        //该用户还未激活
        if (user.getStatus() == 0){
            map.put("emailMsg","该邮箱还未激活，请到邮箱中激活后再使用！");
            return map;
        }
        //邮箱正确的情况下，发送验证码到邮箱
        Context context = new Context();
        context.setVariable("email",email);
        String code = CommunityUtil.generateUUID().substring(0,5);
        context.setVariable("code",code);
        String content = templateEngine.process("mail/forget", context);
        mailClient.sendMail(email, "验证码", content);
        map.put("code",code);//map中存放一份，为了之后和用户输入的验证码进行对比
        map.put("expirationTime", LocalDateTime.now().plusMinutes(5L));//过期时间
        return map;
    }

    //忘记密码
    @Override
    public Map<String, Object> forget(String email, String verifycode, String password, HttpSession session){
        Map<String,Object> map = new HashMap<>();
        //空值处理
        if (StringUtils.isBlank(email)){
            map.put("emailMsg","请输入邮箱！");
            return map;
        }
        if (StringUtils.isBlank(verifycode)){
            map.put("codeMsg","请输入验证码！");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","请输入新密码！");
            return map;
        }
        //邮箱在获取验证码那一步已经验证过了，是有效的邮箱，且验证码也有效
        User user = this.getByEmail(email);
        password = CommunityUtil.md5(password + user.getSalt());
        user.setPassword(password);
        baseMapper.updateById(user);
        return map;
    }



    // 修改密码
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword, String confirmPassword) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }
        if (!newPassword.equals(confirmPassword)){
            map.put("confirmPasswordMsg","两次输入的密码不一致");
            return map;
        }
        // 验证原始密码
        User user = userService.findUserById(userId);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "原密码输入有误!");
            return map;
        }
        // 更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        user.setPassword(newPassword);
        userService.updateById(user);
        return map;
    }

         public User getByEmail(String email){
        User user=baseMapper.selectOne(new QueryWrapper<User>().eq("email",email));
         return user;
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = baseMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
