package com.agri.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare Turnstile 人机验证配置
 * - enabled: 是否启用（本地断网调试可置 false 旁路）
 * - secretKey: Cloudflare Secret Key，由环境变量 TURNSTILE_SECRET_KEY 注入，不写进代码/配置文件
 */
@Data
@ConfigurationProperties("turnstile")
public class TurnstileProperties {
    private boolean enabled = true;
    private String secretKey = "";
}
