package com.pancm.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NettyClientApp {

    public static void main(String[] args) {
        // 启动嵌入式的 Tomcat 并初始化 Spring 环境及其各 Spring 组件
        // ApplicationContext context = SpringApplication.run(NettyClientApp.class, args);
        // NettyClient nettyClient = context.getBean(NettyClient.class);
        new NettyClient().run();
    }

}
