package com.agri.platform.agent.core;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.agent.dto.ChatResponse;
import com.agri.platform.agent.dto.OrchestratorResult;
import com.agri.platform.agent.dto.ToolCall;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.provider.ChatProvider;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.tool.ToolRegistry;
import com.agri.platform.config.SiliconFlowProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编排循环单测:脚本化 ChatProvider 驱动多场景。覆盖:
 * <ol>
 *   <li>directAnswer_noTool:无 tool_calls → 直接返回文本。</li>
 *   <li>readToolThenAnswer:读工具 → observation → 模型续答(用无依赖 fake Tool,不用 QueryCreditTool)。</li>
 *   <li>maxIterationStops:死循环工具调用到 max-iter 后停,不抛异常。</li>
 *   <li>unknownToolBecomesErrorObservation:registry.get 返 null → 错误 observation → 模型答"功能不可用"。</li>
 *   <li>writeToolConfirmsThenExecutes:写工具→挂起→确认执行;且二次确认拿超时(证明 remove 原子消费防双写)。</li>
 * </ol>
 */
class AgentOrchestratorTest {

    /** 脚本化 ChatProvider:按入队顺序返回预设响应。 */
    static class ScriptedProvider implements ChatProvider {
        final Deque<ChatResponse> q = new ArrayDeque<>();
        /** 捕获每次 chat() 收到的消息列表副本,供回归断言验证编排循环发送给 provider 的消息形状。 */
        final java.util.List<java.util.List<ChatMessage>> allCalls = new java.util.ArrayList<>();
        void add(ChatResponse r) { q.add(r); }
        @Override
        public ChatResponse chat(List<ChatMessage> m, List<ToolSpec> t, String model) {
            allCalls.add(new java.util.ArrayList<>(m));
            return q.isEmpty() ? new ChatResponse() : q.pop();
        }
    }

    /** 无依赖的假读工具:不引 UserService/PiiMasker。 */
    static class FakeReadTool implements Tool {
        private final String toolName;
        private final String preview;
        FakeReadTool(String toolName, String preview) { this.toolName = toolName; this.preview = preview; }
        @Override public String name() { return toolName; }
        @Override public String role() { return "farmer"; }
        @Override public boolean isWrite() { return false; }
        @Override public ToolSpec spec() {
            return ToolSpec.builder().name(toolName).description("fake read").parameters(Map.of()).build();
        }
        @Override public String previewOrExecute(ToolContext ctx, Map<String, Object> args) { return preview; }
        @Override public String execute(ToolContext ctx, Map<String, Object> args) {
            throw new UnsupportedOperationException();
        }
    }

    /** 无依赖的假写工具:previewOrExecute 出 draft、execute 出最终文案。 */
    static class FakeWriteTool implements Tool {
        private final String toolName;
        private final String draft;
        private final String executed;
        FakeWriteTool(String toolName, String draft, String executed) {
            this.toolName = toolName; this.draft = draft; this.executed = executed;
        }
        @Override public String name() { return toolName; }
        @Override public String role() { return "farmer"; }
        @Override public boolean isWrite() { return true; }
        @Override public ToolSpec spec() {
            return ToolSpec.builder().name(toolName).description("fake write").parameters(Map.of()).build();
        }
        @Override public String previewOrExecute(ToolContext ctx, Map<String, Object> args) { return draft; }
        @Override public String execute(ToolContext ctx, Map<String, Object> args) { return executed; }
    }

    SiliconFlowProperties props() {
        SiliconFlowProperties p = new SiliconFlowProperties();
        p.setMaxIterations(6);
        return p;
    }

    @Test
    void directAnswer_noTool() {
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r = new ChatResponse();
        r.setText("你好");
        prov.add(r);
        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "在吗", null, null)), "farmer", "u1", "s1");
        assertEquals("你好", out.getFinalText());
        assertFalse(out.needsConfirm());
    }

    @Test
    void readToolThenAnswer() {
        // 用无依赖 fake 读工具替代 brief 里的匿名 QueryCreditTool
        FakeReadTool credit = new FakeReadTool("query_credit", "信用4");
        ScriptedProvider prov = new ScriptedProvider();
        // r1:模型要求调 query_credit
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("query_credit");
        tc.setArguments("{}");
        r1.setToolCalls(List.of(tc));
        prov.add(r1);
        // r2:模型读观察后给出最终文本
        ChatResponse r2 = new ChatResponse();
        r2.setText("您的信用分为4级");
        prov.add(r2);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(credit)), null, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "查信用", null, null)), "farmer", "u1", "s1");
        assertEquals("您的信用分为4级", out.getFinalText());
        assertFalse(out.needsConfirm());

        // 回归守卫:第 2 次 chat (tool 执行后) 收到的消息里必须有带 tool_calls 的 assistant 桥接消息
        List<ChatMessage> secondCall = prov.allCalls.get(1);
        assertTrue(secondCall.stream().anyMatch(m ->
                "assistant".equals(m.getRole()) && m.getToolCallsJson() != null
                        && m.getToolCallsJson().contains("query_credit")),
                "orchestrator 必须在 role=tool 消息前补回 assistant tool_calls 桥接消息");
    }

    @Test
    void maxIterationStops() {
        ScriptedProvider prov = new ScriptedProvider();
        // 一直返回工具调用,永不停
        for (int i = 0; i < 20; i++) {
            ChatResponse r = new ChatResponse();
            ToolCall tc = new ToolCall();
            tc.setId("c" + i);
            tc.setName("no_such_tool");
            tc.setArguments("{}");
            r.setToolCalls(List.of(tc));
            prov.add(r);
        }
        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "x", null, null)), "farmer", "u1", "s1");
        // 到 max=6 次后停止,不抛异常、不死循环
        assertNotNull(out.getFinalText());
    }

    @Test
    void unknownToolBecomesErrorObservation() {
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("not_exist");
        tc.setArguments("{}");
        r1.setToolCalls(List.of(tc));
        prov.add(r1);
        ChatResponse r2 = new ChatResponse();
        r2.setText("抱歉,该功能暂不可用");
        prov.add(r2);
        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "x", null, null)), "farmer", "u1", "s1");
        assertEquals("抱歉,该功能暂不可用", out.getFinalText());
    }

    @Test
    void writeToolConfirmsThenExecutes() {
        // 真 PendingActionStore,验证写工具→挂起→确认→执行;二次确认必须超时(原子消费防双写)
        PendingActionStore store = new PendingActionStore();
        FakeWriteTool apply = new FakeWriteTool(
                "apply_finance",
                "即将提交融资申请:套餐#1",
                "融资申请已提交"
        );
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("apply_finance");
        tc.setArguments("{\"productId\":1}");
        r1.setToolCalls(List.of(tc));
        prov.add(r1);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(apply)), store, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "申请融资", null, null)), "farmer", "u1", "s1");

        // 写工具停在确认门
        assertTrue(out.needsConfirm());
        assertNotNull(out.getPendingId());
        assertEquals("即将提交融资申请:套餐#1", out.getDraft());
        assertEquals(out.getDraft(), out.getFinalText());

        // 用户确认 → 真执行
        String result = orch.confirmAndExecute(out.getPendingId(), true);
        assertEquals("融资申请已提交", result);

        // 二次确认同一个 pendingId → 必须超时(原子消费已取走,杜绝双写)
        String secondConfirm = orch.confirmAndExecute(out.getPendingId(), true);
        assertEquals("该操作已超时或不存在,请重新发起", secondConfirm);

        // 拒绝路径:先挂一个新的,再取消。先入队第二个 apply_finance 调用供第二次 run 取用。
        ChatResponse r2 = new ChatResponse();
        ToolCall tc2 = new ToolCall();
        tc2.setId("c2");
        tc2.setName("apply_finance");
        tc2.setArguments("{\"productId\":2}");
        r2.setToolCalls(List.of(tc2));
        prov.add(r2);

        OrchestratorResult out2 = orch.run(List.of(new ChatMessage("user", "再申请一次", null, null)), "farmer", "u1", "s2");
        assertTrue(out2.needsConfirm());
        String cancelled = orch.confirmAndExecute(out2.getPendingId(), false);
        assertEquals("已取消该操作", cancelled);
    }
}
