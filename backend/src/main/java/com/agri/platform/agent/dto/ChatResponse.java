package com.agri.platform.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String text;                // 模型最终文本(无工具调用时)
    private List<ToolCall> toolCalls;   // 模型要调的工具(可能多个)
    public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
}
