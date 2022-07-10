package com.hcl.community;

import com.hcl.community.entity.DiscussPost;
import com.hcl.community.service.DiscussPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class CommunityApplicationTests {

    @Autowired
    DiscussPostService discussPostService;

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

}
