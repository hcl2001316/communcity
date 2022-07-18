package com.hcl.community.interceptor;
import com.hcl.community.entity.User;
import com.hcl.community.service.DataService;
import com.hcl.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 统计UV
        String ip = request.getRemoteHost();    //获取用户登录的主机名
        dataService.recordUV(ip);

        // 统计DAU
        User user = hostHolder.getUser();     //用户登录的状态
        if (user != null) {
            dataService.recordDAU(user.getId());
        }

        return true;
    }
}
