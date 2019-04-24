package com.pancm.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NettyServerApp {

    public static void main(String[] args) {
        // 启动嵌入式的 Tomcat 并初始化 Spring 环境及其各 Spring 组件
		// ApplicationContext context = SpringApplication.run(NettyServerApp.class, args);
		// NettyServer nettyServer = context.getBean(NettyServer.class);
        new NettyServer().run();
    }

}
