package com.agri.platform.agent.provider;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.agent.dto.ChatResponse;
import com.agri.platform.agent.dto.ToolSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SiliconFlowChatProviderTest {

    private final SiliconFlowChatProvider p = new SiliconFlowChatProvider(null);

    @Test
    void parseFinalText_noToolCalls() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"你好\"}}]}";
        ChatResponse r = p.parseResponse(json);
        assertEquals("你好", r.getText());
        assertFalse(r.hasToolCalls());
    }

    @Test
    void parseToolCalls() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                + "\"tool_calls\":[{\"id\":\"call_1\",\"function\":{\"name\":\"query_credit\","
                + "\"arguments\":\"{\\\"userName\\\":\\\"farmer01\\\"}\"}}]}}]}";
        ChatResponse r = p.parseResponse(json);
        assertTrue(r.hasToolCalls());
        assertEquals("query_credit", r.getToolCalls().get(0).getName());
        assertEquals("call_1", r.getToolCalls().get(0).getId());
        assertTrue(r.getToolCalls().get(0).getArguments().contains("farmer01"));
    }

    @Test
    void buildBodyContainsTools() {
        ToolSpec t = ToolSpec.builder().name("query_credit")
                .description("查信用分").parameters(Map.of("userName", "string")).build();
        String body = p.buildRequestBody(
                List.of(new ChatMessage("user", "我信用多少", null, null)),
                List.of(t), "Qwen/Qwen3-30B-A3B");
        assertTrue(body.contains("\"tools\""));
        assertTrue(body.contains("query_credit"));
        assertTrue(body.contains("Qwen/Qwen3-30B-A3B"));
    }
}
