package com.postion.airlineorderbackend.config; // 确保包名正确

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 这个规则会匹配所有不包含"."的路径（例如 /dashboard, /users/1, 但不会匹配 /styles.css）
        // 并将它们全部转发给 index.html，以便Angular路由能够接管。
        // 它实际上已经隐式地处理了深层路径，无需再添加 /** 的规则。
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");

        // 我们不再需要下面这行非法的规则：
        // registry.addViewController("/**/{path:[^\\.]*}")
        //         .setViewName("forward:/index.html");
    }
}