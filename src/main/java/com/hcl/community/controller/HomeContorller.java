package com.hcl.community.controller;

import com.hcl.community.entity.DiscussPost;
import com.hcl.community.entity.Page;
import com.hcl.community.entity.User;
import com.hcl.community.service.DiscussPostService;
import com.hcl.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 黄小爷
 * @description
 */
@Controller
public class HomeContorller {
    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    @GetMapping("/index")
    public String getIndexPage(Model model, Page page){
        System.out.println(discussPostService.findDiscussPostRows(0));
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");
        System.out.println(page.getOffset());
        List<DiscussPost> discussPostList = discussPostService.selectDiscussPosts(0, page.getCurrent(), page.getLimit());
        List<Map<String,Object>> list=new ArrayList<>();
        if (null!=discussPostList){
            for (DiscussPost discussPost : discussPostList) {
                Map<String,Object> map=new HashMap();
                User user = userService.findUserById(discussPost.getUserId());
                map.put("post",discussPost);
                map.put("user",user);
                list.add(map);
            }
        }
        System.out.println(list.size());
        model.addAttribute("discussPosts",list);
        model.addAttribute("page",page);
        return "/index";
    }

}
