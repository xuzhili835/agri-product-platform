package com.agri.platform.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ToolContext {
    private String userName;     // 当前用户(从 JWT 解出)
    private String role;         // farmer / buyer
    private String sessionId;
}
