package com.agri.platform.agent.dto;

import lombok.Data;

@Data
public class ToolCall {
    private String id;          // tool_call_id
    private String name;        // function.name
    private String arguments;   // function.arguments(JSON 字符串)
}
