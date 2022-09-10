package com.hcl.community.controller;


import com.hcl.community.annotation.LoginRequired;
import com.hcl.community.entity.User;
import com.hcl.community.service.FollowService;
import com.hcl.community.service.LikeService;
import com.hcl.community.service.UserService;
import com.hcl.community.util.CommunityConstant;
import com.hcl.community.util.CommunityUtil;
import com.hcl.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 黄成龙
 * @since 2022-07-10
 */
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    LikeService likeService;

    @Autowired
    FollowService followService;

    @LoginRequired
    @GetMapping(path = "/setting")
    public String getSettingPage() {
        return "/site/setting";
    }

    @GetMapping("/forget")
    public String getForgetPage(){
        return "site/forget";
    }

    @LoginRequired
    @PostMapping(path = "/upload")
    //上传头像
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @GetMapping(path = "/header/{fileName}")
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            throw  new RuntimeException("读取头像失败: " + e.getMessage());
        }
    }

    // 修改密码
    @PostMapping(path = "/updatePassword")
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword,confirmPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            model.addAttribute("confirmPasswordMsg", map.get("confirmPasswordMsg"));
            return "/site/setting";
        }
    }

    //忘记密码
    @PostMapping(path = "/forget")
    public String forget(Model model, String email, String verifycode, String password, HttpSession session) {
        //验证验证码是否正确
        if (!verifycode.equals(session.getAttribute("code"))) {
            model.addAttribute("codeMsg", "输入的验证码不正确！");
            return "site/forget";
        }
        //验证码是否过期
        if (LocalDateTime.now().isAfter((LocalDateTime) session.getAttribute("expirationTime"))) {
            model.addAttribute("codeMsg", "输入的验证码已过期，请重新获取验证码！");
            return "site/forget";
        }
        Map<String, Object> map = userService.forget(email, verifycode, password,session);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "密码修改成功，可以使用新密码登录了!");
            model.addAttribute("target", "/login");
            return "site/operate-result";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("codeMsg", map.get("codeMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }


    //忘记密码之后获取验证码
    @GetMapping(path = "/getCode")
    public String getCode(String email, Model model, HttpSession session) {
        Map<String, Object> map = userService.getCode(email);
        if (map.containsKey("emailMsg")) {//有错误的情况下
            model.addAttribute("emailMsg", map.get("emailMsg"));
        } else {//正确的情况下，向邮箱发送了验证码
            model.addAttribute("msg", "验证码已经发送到您的邮箱，5分钟内有效！");
            //将验证码存放在 session 中，后序和用户输入的信息进行比较
            session.setAttribute("code",map.get("code"));
            //后序判断用户输入验证码的时候验证码是否已经过期
            session.setAttribute("expirationTime",map.get("expirationTime"));
        }
        return "site/forget";
    }

    // 个人主页
    @GetMapping(path = "/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }





}

