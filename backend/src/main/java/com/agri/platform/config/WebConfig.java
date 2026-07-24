package com.agri.platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(
                        "/user/login",       // 登录接口
                        "/user/register",    // 注册接口
                        "/home/**",          // 首页
                        "/banner/**",        // 轮播图
                        "/product/**",       // 商品相关（公开）
                        "/finance/product/**",  // 融资产品（公开）
                        "/expert/list",      // 专家列表（公开）
                        "/expert/*",         // 专家详情（公开，单段路径；/expert/my/info 与 /expert/reserve/* 受保护）
                        "/knowledge/list",   // 知识列表（公开）
                        "/knowledge/*",      // 知识详情（公开）
                        "/knowledge/*/discuss/list", // 知识评论列表（公开）
                        "/question/list",    // 问答列表（公开）
                        "/upload",           // 文件上传（公开，前端 el-upload 直接调用）
                        "/uploads/**",       // 已上传文件的访问（公开）
                        "/alipay/notify"     // 支付宝异步回调（无 JWT，需放行）
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 映射到运行目录下的 uploads/ 文件夹，供访问上传的图片/材料
        String abs = new File("uploads").getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + abs + "/");
    }

    /**
     * HTTP 客户端（供 Turnstile siteverify 等出站调用复用）
     */
    @Bean
    public RestTemplate restTemplate() {
        // 设连接/读取超时，避免出站调用（如 Turnstile siteverify）因对端无响应而长时间挂起
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5s
        factory.setReadTimeout(8000);     // 8s
        return new RestTemplate(factory);
    }
}
