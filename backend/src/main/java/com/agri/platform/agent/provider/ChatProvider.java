package com.agri.platform.agent.provider;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.agent.dto.ChatResponse;
import com.agri.platform.agent.dto.ToolSpec;
import java.util.List;

public interface ChatProvider {
    /** 带工具的对话:返回最终文本或要调的工具。 */
    ChatResponse chat(List<ChatMessage> messages, List<ToolSpec> tools, String model);
}
