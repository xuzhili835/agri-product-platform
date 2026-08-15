package com.agri.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 硅基流动（SiliconFlow）LLM 服务配置
 * - apiKey:            API 密钥，由环境变量 SILICONFLOW_API_KEY 或 application-local.yml 注入，不写进代码/提交配置
 * - baseUrl:           SiliconFlow OpenAI 兼容网关
 * - chatModel:         主聊天模型
 * - chatModelFallback: 主模型不可用时的兜底聊天模型
 * - embedModel:        文本向量化模型
 * - maxIterations:     单次会话最大工具循环次数
 * - pendingTtlSeconds: 挂起确认最长存活秒数
 */
@Data
@ConfigurationProperties("siliconflow")
public class SiliconFlowProperties {
    private String apiKey;
    private String baseUrl = "https://api.siliconflow.cn/v1";
    private String chatModel = "Qwen/Qwen2.5-72B-Instruct";
    private String chatModelFallback = "Qwen/Qwen2.5-7B-Instruct";
    private String embedModel = "BAAI/bge-m3";
    /** 单次会话最大工具循环次数 */
    private int maxIterations = 6;
    /** 挂起确认最长存活秒数 */
    private int pendingTtlSeconds = 300;
}
