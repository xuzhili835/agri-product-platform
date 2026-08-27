package com.agri.platform.agent.core;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.agent.dto.ChatResponse;
import com.agri.platform.agent.dto.ConfirmOutcome;
import com.agri.platform.agent.dto.FormField;
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
                        && m.getContent() != null && m.getContent().contains("被系统拦截")),
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

    @Test
    void narratedIntentNudgedIntoRealToolCall() {
        // 回归(实测):模型槽位集齐后说"我将直接为您提交预约申请"却不发工具调用,把宣告当终稿。
        // 编排器应注入催促再跑一轮;催完真调工具 → 出确认卡;催了仍只说不动 → 原文放行(单轮只催一次)
        FakeWriteTool reserve = new FakeWriteTool("reserve_expert", "即将预约:王教授", "预约已提交");
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse narrate = new ChatResponse();
        narrate.setText("农作物:芒果 种植面积:50亩。我将直接为您提交预约申请。");
        prov.add(narrate);
        ChatResponse realCall = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("reserve_expert");
        tc.setArguments("{}");
        realCall.setToolCalls(List.of(tc));
        prov.add(realCall);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(reserve)),
                new PendingActionStore(props()), props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "预约王教授", null, null)), "farmer", "u1", "s1");
        assertTrue(out.needsConfirm(), "催促后模型调了工具,应出确认卡");
        assertEquals("即将预约:王教授", out.getFinalText());
        assertEquals(2, prov.allCalls.size(), "催促只多跑一轮");

        // 催了仍宣告(不调工具):最多再催一次,第二次催后仍宣告则原文放行(不进假执行兜底)
        ScriptedProvider prov2 = new ScriptedProvider();
        ChatResponse narrate2 = new ChatResponse();
        narrate2.setText("我将直接为您提交预约申请。");
        prov2.add(narrate2);
        ChatResponse narrate3 = new ChatResponse();
        narrate3.setText("请稍候,我将直接为您提交预约申请。");
        prov2.add(narrate3);
        ChatResponse narrate4 = new ChatResponse();
        narrate4.setText("我将直接为您提交预约申请,请稍等。");
        prov2.add(narrate4);
        AgentOrchestrator orch2 = new AgentOrchestrator(prov2, new ToolRegistry(List.of(reserve)),
                new PendingActionStore(props()), props());
        OrchestratorResult out2 = orch2.run(List.of(new ChatMessage("user", "预约王教授", null, null)), "farmer", "u1", "s1");
        assertFalse(out2.needsConfirm());
        assertEquals(3, prov2.allCalls.size(), "催两次后放行,不再无限催");
    }

    /** 表单卡假写工具:preview 宽松出卡(缺必填不抛)、validate 校验必填 b、execute 记录收到的参数。 */
    static class FakeFormTool implements Tool {
        final java.util.List<Map<String, Object>> executedArgs = new java.util.ArrayList<>();
        @Override public String name() { return "reserve_expert"; }
        @Override public String role() { return "common"; }
        @Override public boolean isWrite() { return true; }
        @Override public ToolSpec spec() {
            return ToolSpec.builder().name("reserve_expert").description("fake form").parameters(Map.of()).build();
        }
        @Override public String previewOrExecute(ToolContext c, Map<String, Object> a) {
            return "即将预约专家:" + a.getOrDefault("a", "未选择") + " 时间:" + a.getOrDefault("b", "未填写");
        }
        @Override public void validate(Map<String, Object> args) {
            Object b = args.get("b");
            if (b == null || b.toString().isBlank()) throw new RuntimeException("请填写期望时间");
        }
        @Override public List<FormField> formFields(ToolContext c, Map<String, Object> a) {
            return List.of(FormField.builder().key("b").label("期望时间").type("text")
                    .value(a.get("b") == null ? null : a.get("b").toString()).required(true).build());
        }
        @Override public String execute(ToolContext c, Map<String, Object> a) {
            executedArgs.add(a);
            return "预约已提交";
        }
    }

    @Test
    void formCardExposedWithPrefilledArgs() {
        // 表单卡工具:部分参数(缺必填 b)也能出卡,且 form 字段列表随卡透出;
        // 非表单写工具(未实现 formFields)form 保持 null,走纯文字卡
        PendingActionStore store = new PendingActionStore(props());
        FakeFormTool formTool = new FakeFormTool();
        FakeWriteTool plainTool = new FakeWriteTool("place_order", "draft", "done");
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("reserve_expert");
        tc.setArguments("{\"a\":\"王教授\"}");   // 只给了 a,缺必填 b——preview 不抛、出表单卡
        r1.setToolCalls(List.of(tc));
        prov.add(r1);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(formTool, plainTool)), store, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "预约王教授", null, null)), "farmer", "u1", "s1");
        assertTrue(out.needsConfirm(), "部分参数也应出卡(槽位由表单收集)");
        assertNotNull(out.getForm(), "表单卡工具必须透出 form 字段列表");
        assertEquals(1, out.getForm().size());
        assertEquals("b", out.getForm().get(0).getKey());
        assertTrue(out.getForm().get(0).isRequired());
        assertNull(out.getForm().get(0).getValue(), "未提供的槽位预填值应为空");

        // 非表单写工具:form 为 null(纯文字卡,协议不变)
        ChatResponse r2 = new ChatResponse();
        ToolCall tc2 = new ToolCall();
        tc2.setId("c2");
        tc2.setName("place_order");
        tc2.setArguments("{}");
        r2.setToolCalls(List.of(tc2));
        prov.add(r2);
        OrchestratorResult out2 = orch.run(List.of(new ChatMessage("user", "下单", null, null)), "buyer", "u1", "s1");
        assertTrue(out2.needsConfirm());
        assertNull(out2.getForm(), "未实现 formFields 的写工具走纯文字卡");
    }

    @Test
    void confirmWithOverrideArgsMergesAndExecutes() {
        // 表单提交值合并:非空覆盖存量(且去首尾空白)、空串删键、未涉及键保留;
        // execute 收到的是合并后的参数
        PendingActionStore store = new PendingActionStore(props());
        FakeFormTool tool = new FakeFormTool();
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("reserve_expert");
        tc.setArguments("{\"a\":\"王教授\",\"b\":\"周三上午\",\"c\":\"多余\"}");
        r1.setToolCalls(List.of(tc));
        prov.add(r1);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(tool)), store, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "预约王教授", null, null)), "farmer", "u1", "s1");
        assertTrue(out.needsConfirm());

        ConfirmOutcome ok = orch.confirmAndExecute(out.getPendingId(), true, "u1",
                Map.of("b", " 下周三下午 ", "c", ""));   // b 用户改了时间,c 清空
        assertEquals(ConfirmOutcome.EXECUTED, ok.getStatus());
        assertEquals(1, tool.executedArgs.size());
        Map<String, Object> merged = tool.executedArgs.get(0);
        assertEquals("王教授", merged.get("a"), "未编辑的存量参数保留");
        assertEquals("下周三下午", merged.get("b"), "用户编辑值覆盖存量且去空白");
        assertFalse(merged.containsKey("c"), "空串提交应删除该键");
    }

    @Test
    void confirmPrecheckFailureKeepsPendingRetryable() {
        // 预检(validate/preview)失败 → ERROR 且 pending 不消费,表单可改后重试;
        // 重试带全参数 → EXECUTED。这是"表单保持可编辑"的后端契约
        PendingActionStore store = new PendingActionStore(props());
        FakeFormTool tool = new FakeFormTool();
        ScriptedProvider prov = new ScriptedProvider();
        ChatResponse r1 = new ChatResponse();
        ToolCall tc = new ToolCall();
        tc.setId("c1");
        tc.setName("reserve_expert");
        tc.setArguments("{\"a\":\"王教授\"}");   // 缺必填 b
        r1.setToolCalls(List.of(tc));
        prov.add(r1);

        AgentOrchestrator orch = new AgentOrchestrator(prov, new ToolRegistry(List.of(tool)), store, props());
        OrchestratorResult out = orch.run(List.of(new ChatMessage("user", "预约王教授", null, null)), "farmer", "u1", "s1");
        assertTrue(out.needsConfirm());

        ConfirmOutcome bad = orch.confirmAndExecute(out.getPendingId(), true, "u1", null);
        assertEquals(ConfirmOutcome.ERROR, bad.getStatus());
        assertTrue(bad.getText().contains("请填写期望时间"), "错误消息应含校验原因:" + bad.getText());
        assertTrue(tool.executedArgs.isEmpty(), "预检失败不得执行");

        // pending 未被消费:补全后同一张卡重试成功
        ConfirmOutcome retry = orch.confirmAndExecute(out.getPendingId(), true, "u1", Map.of("b", "下周三下午"));
        assertEquals(ConfirmOutcome.EXECUTED, retry.getStatus());
        assertEquals(1, tool.executedArgs.size());
        assertEquals("下周三下午", tool.executedArgs.get(0).get("b"));
    }
}
