package com.agri.platform.agent.dto;
import lombok.Data;

@Data
public class AgentChatRequest {
    private String message;     // 用户这轮说的话
    private String sessionId;   // 可选,首次不传
}
