package com.agri.platform.agent.tool;

import com.agri.platform.agent.dto.ToolSpec;

/** agent 工具。读工具直接 execute;写工具先 preview 生成 draft,确认后 execute。 */
public interface Tool {
    String name();
    ToolSpec spec();
    /** 适用角色(如 "farmer" / "buyer" / "common")。 */
    String role();
    boolean isWrite();
    /** 读工具:直接执行返回结果文本;写工具:返回预览 draft 文本(不落库)。 */
    String previewOrExecute(ToolContext ctx, java.util.Map<String, Object> args);
    /** 写工具专用:用户确认后真正落库。读工具可抛 UnsupportedOperationException。 */
    String execute(ToolContext ctx, java.util.Map<String, Object> args);
}
