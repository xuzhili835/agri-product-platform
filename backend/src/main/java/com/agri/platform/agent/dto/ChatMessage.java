package com.agri.platform.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;        // system / user / assistant / tool
    private String content;     // 文本(assistant 带 tool_calls 时可为空)
    private String toolCallsJson;   // 仅 assistant 角色:原始 tool_calls JSON 数组字符串
    private String toolCallId;      // 仅 role=tool:对应的 tool_call_id
}
