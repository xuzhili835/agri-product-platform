package com.agri.platform.agent.core;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.agent.dto.ChatResponse;
import com.agri.platform.agent.dto.ConfirmOutcome;
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
    void writeToolPreviewExceptionBecomesObservation() {
        // 写工具预览抛业务异常(如商品编号不存在)→ 不挂起、不抛 500,作为 observation 回灌,模型可恢复
        Tool flaky = new Tool() {
            @Override public String name() { return "place_order"; }
            @Override public String role() { return "buyer"; }
            @Override public boolean isWrite() { return true; }
            @Override public ToolSpec spec() {
                return ToolSpec.builder().name("place_order").description("").parameters(Map.of()).build();
            }
            @Override public String previewOrExecute(ToolContext c, Map<String, Object> a) {
                throw new RuntimeException("商品#999 不存在或已下架,请用 search_market 重新选择");
            }
            @Override public String execute(ToolContext c, Map<String, Object> a) { return "下单成功"; }
        };
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("place_order");
        tc.setArguments("{}");
        r1.setToolCalls(List.of(tc));
        prov.add(r1);
        ChatResponse r2 = new ChatResponse();
        r2.setText("该商品不存在,我帮您重新搜索");
        prov.add(r2);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(flaky)), new PendingActionStore(props()), props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "买芒果", null, null)), "buyer", "u1", "s1");

        // 预览失败:无挂起,模型基于异常 observation 给出恢复性回复
        assertFalse(out.needsConfirm());
        assertNull(out.getPendingId());
        assertEquals("该商品不存在,我帮您重新搜索", out.getFinalText());
        // 回归:第 2 次 chat 收到的消息里有 tool 观察消息(且前面有 assistant tool_calls 桥接)
        List<ChatMessage> secondCall = prov.allCalls.get(1);
        assertTrue(secondCall.stream().anyMatch(m ->
                "tool".equals(m.getRole()) && m.getContent().contains("商品#999 不存在")));
        assertTrue(secondCall.stream().anyMatch(m ->
                "assistant".equals(m.getRole()) && m.getToolCallsJson() != null));
    }

    @Test
    void writeToolConfirmsThenExecutes() {
        // 真 PendingActionStore,验证写工具→挂起→确认→执行;二次确认必须超时(原子消费防双写)
        PendingActionStore store = new PendingActionStore(props());
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

        // 他人确认 → rejected,pending 未被消费,属主仍可确认
        ConfirmOutcome stranger = orch.confirmAndExecute(out.getPendingId(), true, "attacker");
        assertEquals(ConfirmOutcome.REJECTED, stranger.getStatus());

        // 属主确认 → 真执行
        ConfirmOutcome result = orch.confirmAndExecute(out.getPendingId(), true, "u1");
        assertEquals(ConfirmOutcome.EXECUTED, result.getStatus());
        assertEquals("融资申请已提交", result.getText());

        // 二次确认同一个 pendingId → 必须超时(原子消费已取走,杜绝双写)
        ConfirmOutcome secondConfirm = orch.confirmAndExecute(out.getPendingId(), true, "u1");
        assertEquals(ConfirmOutcome.TIMEOUT, secondConfirm.getStatus());
        assertEquals("该操作已超时或不存在,请重新发起", secondConfirm.getText());

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
        ConfirmOutcome cancelled = orch.confirmAndExecute(out2.getPendingId(), false, "u1");
        assertEquals(ConfirmOutcome.CANCELLED, cancelled.getStatus());
        assertEquals("已取消该操作", cancelled.getText());
    }

    @Test
    void newPendingInvalidatesOldOneInSameSession() {
        // 回归:同 session 第二次挂起前必须作废旧 pending——
        // 此前旧卡仍有效,"先问买苹果再问买梨、两张卡都点"会造成顺序双写下单
        PendingActionStore store = new PendingActionStore(props());
        FakeWriteTool apply = new FakeWriteTool("apply_finance", "draft", "done");
        ScriptedProvider prov = new ScriptedProvider();
        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(apply)), store, props());

        for (int i = 0; i < 2; i++) {
            ChatResponse r = new ChatResponse();
            ToolCall tc = new ToolCall();
            tc.setId("c" + i);
            tc.setName("apply_finance");
            tc.setArguments("{}");
            r.setToolCalls(List.of(tc));
            prov.add(r);
        }

        OrchestratorResult first = orch.run(List.of(new ChatMessage("user", "买苹果", null, null)), "farmer", "u1", "s1");
        assertTrue(first.needsConfirm());
        OrchestratorResult second = orch.run(List.of(new ChatMessage("user", "买梨", null, null)), "farmer", "u1", "s1");
        assertTrue(second.needsConfirm());

        // 旧 pending 已被作废:确认旧卡只能拿到 timeout,新卡才真正执行
        assertEquals(ConfirmOutcome.TIMEOUT,
                orch.confirmAndExecute(first.getPendingId(), true, "u1").getStatus());
        assertEquals(ConfirmOutcome.EXECUTED,
                orch.confirmAndExecute(second.getPendingId(), true, "u1").getStatus());
    }

    @Test
    void fakeExecutionTextInPureTextTurnIsIntercepted() {
        // 回归(实测事故):模型在未调工具的轮次用文字伪造确认卡/执行结果
        // ("待确认操作:…确认执行?" / "下单成功,订单号:14"),用户以为下单了实际什么都没发生。
        // 拦截后会带纠偏指令自动重试一轮:重试正常→透出重试文本;重试仍假→兜底文案
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse fake = new ChatResponse();
        fake.setText("待确认操作:\n即将下单:海南新鲜芒果 ¥35.00 x 1 = ¥35.00\n收货地址:北京市西城区\n确认执行?");
        prov.add(fake);
        ChatResponse recovered = new ChatResponse();
        recovered.setText("抱歉,刚才的回复有误。请问您想购买什么商品?我先搜索再给您确认按钮。");
        prov.add(recovered);
        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "买芒果", null, null)), "buyer", "u1", "s1");
        assertFalse(out.needsConfirm(), "纯文字轮不得产生 pendingId");
        // 重试正常 → 透出重试文本(且纠偏消息确实发给了模型)
        assertEquals("抱歉,刚才的回复有误。请问您想购买什么商品?我先搜索再给您确认按钮。", out.getFinalText());
        assertEquals(2, prov.allCalls.size(), "拦截后应带纠偏指令重试一次");
        assertTrue(prov.allCalls.get(1).stream().anyMatch(m -> "system".equals(m.getRole())
                        && m.getContent() != null && m.getContent().contains("已被系统拦截")),
                "重试请求里应包含纠偏 system 消息");

        // 重试仍输出假话术 → 返回兜底文案,绝不透出伪造内容
        ScriptedProvider prov2 = new ScriptedProvider();
        ChatResponse fake2 = new ChatResponse();
        fake2.setText("下单成功,订单号:14,合计:¥35.00。请在订单列表完成支付。");
        prov2.add(fake2);
        ChatResponse fake2Again = new ChatResponse();
        fake2Again.setText("下单成功,订单号:15,合计:¥35.00。");
        prov2.add(fake2Again);
        AgentOrchestrator orch2 = new AgentOrchestrator(prov2, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out2 = orch2.run(List.of(new ChatMessage("user", "确认", null, null)), "buyer", "u1", "s1");
        assertFalse(out2.getFinalText().contains("订单号:1"), "伪造的执行结果不得透出");

        // 正常解释性文字不受误伤(无重试)
        ScriptedProvider prov3 = new ScriptedProvider();
        ChatResponse normal = new ChatResponse();
        normal.setText("下单需要先搜索商品,选择后我会给您弹出确认按钮,点击确认才会真正提交订单。");
        prov3.add(normal);
        AgentOrchestrator orch3 = new AgentOrchestrator(prov3, new ToolRegistry(List.of()), null, props());
        OrchestratorResult out3 = orch3.run(List.of(new ChatMessage("user", "怎么下单?", null, null)), "buyer", "u1", "s1");
        assertEquals("下单需要先搜索商品,选择后我会给您弹出确认按钮,点击确认才会真正提交订单。", out3.getFinalText());
        assertEquals(1, prov3.allCalls.size(), "正常文本不应触发重试");
    }
}
