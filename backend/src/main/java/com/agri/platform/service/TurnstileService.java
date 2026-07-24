package com.agri.platform.service;

import com.agri.platform.config.TurnstileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cloudflare Turnstile 人机验证
 * <p>
 * 前端拿到一次性 token 后随注册请求带回，后端用 Secret Key 向 Cloudflare siteverify 二次校验。
 * 真正的校验在后端，前端那步可被绕过、不算数。
 */
@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Autowired
    private TurnstileProperties properties;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 校验 token 是否有效
     * - 开关关闭（enabled=false）：直接放行（本地断网调试旁路）
     * - token 为空：拒绝
     * - 调用 Cloudflare 校验：返回 success；调用异常一律拒绝（fail-closed）
     */
    public boolean verify(String token) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        // 国内访问 challenges.cloudflare.com 偶发抖动：仅在网络异常时重试一次以渡过瞬时故障，
        // 仍失败则 fail-closed 拒绝。（Cloudflare 正常返回 success=false 时不会抛异常，故不会重试。）
        Exception lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("secret", properties.getSecretKey());
                form.add("response", token);
                Map<?, ?> resp = restTemplate.postForObject(SITEVERIFY_URL, form, Map.class);
                return resp != null && Boolean.TRUE.equals(resp.get("success"));
            } catch (Exception e) {
                lastError = e;
                if (attempt == 0) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.warn("Turnstile siteverify 连续失败（fail-closed 拒绝）: {}", lastError == null ? "" : lastError.getMessage());
        return false;
    }
}
