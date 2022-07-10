package com.hcl.community;

import com.hcl.community.entity.DiscussPost;
import com.hcl.community.service.DiscussPostService;
import com.hcl.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@SpringBootTest
class CommunityApplicationTests {

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    TemplateEngine templateEngine;
    @Autowired
    MailClient mailClient;
    @Test
    void contextLoads() {
    }

    @Test
    void selectDiscussPosts(){
//        List<DiscussPost> discussPosts = discussPostService.selectDiscussPosts(0, 1, 10);
        List<DiscussPost> discussPosts = discussPostService.selectDiscussPosts(149, 1, 10);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
    }

    @Test
    void testHtmlMessae(){
        Context context=new Context();
        context.setVariable("username","你好呀！");
        String process = templateEngine.process("mail/demo", context);
        mailClient.sendMail("1774347909@qq.com","嚯嚯嚯",process);
    }

    @Test
    void testMessage(){
        mailClient.sendMail("1774347909@qq.com","hahh","hahahha");
    }

}
