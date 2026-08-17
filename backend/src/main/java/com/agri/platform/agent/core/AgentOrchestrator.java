package com.agri.platform.agent.core;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ReAct 编排循环。每轮:模型选答(无 tool_calls → 返回)或选工具;工具分读写:
 * <ul>
 *   <li>读工具:直接 previewOrExecute,异常文本作为 observation 回灌,模型续聊。</li>
 *   <li>写工具:只生成 draft,挂起到 {@link PendingActionStore},立即返回 pendingId 等用户确认。
 *       挂起前先清掉同 session 的旧 pending——同一会话任何时刻至多一张有效确认卡,
 *       杜绝"先问买苹果、再问买梨、两张卡都点"的顺序双写下单。</li>
 * </ul>
 * 用户确认后调 {@link #confirmAndExecute};该步做<strong>原子消费</strong>(remove 即返回 removed),
 * 杜绝并发确认的双写(双融资申请 / 双订单),并校验 pending 属主,防越权替他人确认。
 * <p>到达 {@code maxIterations} 或总耗时超 {@code DEADLINE_MS} 仍无终稿时,返回固定提示。</p>
 * <p><strong>假执行拦截</strong>:实测发现模型会用纯文字模仿确认卡/执行结果格式
 * ("待确认操作:…确认执行?" / "下单成功,订单号:14"),工具并未调用、什么都没落库,
 * 用户却以为下单成功。prompt 约束会被违反,故在代码层对<strong>无工具调用轮</strong>的
 * 终稿做话术检测,命中即替换为安全提示。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    /**
     * 系统专用话术,模型在纯文字轮(未调任何工具)说出即判定为假执行:
     * <ul>
     *   <li>"待确认操作:"——确认卡 draft 的系统前缀,只会由本类拼装,模型说了必是模仿;</li>
     *   <li>"即将下单/预约/提交融资 … 确认执行?"——写工具 preview 的固定句式;</li>
     *   <li>"下单成功,订单号"/"预约已提交"/"融资申请已提交"——写工具 execute 的固定句式,
     *       真结果只经 confirmAndExecute 返回,不会出现在 chat 终稿里。</li>
     * </ul>
     */
    static final Pattern FAKE_EXECUTION = Pattern.compile(
            "待确认操作|即将下单.*确认执行\\?|即将预约.*确认执行\\?|即将提交融资.*确认执行\\?"
                    + "|下单成功[,，]订单号|预约已提交|融资申请已提交"
                    // 现在时承诺:"为您下单"却没调工具——用户以为正在执行。
                    // 注意:不拦"我将/接下来…提交"类将来时预告——实测误伤率高(模型查完套餐
                    // 习惯说"我将为您提交"再调工具),拦了反而触发兜底循环;完成式伪造仍被拦,底线不破
                    + "|(现在|正在|马上)?为您(下单|购买|下单购买)", Pattern.DOTALL);

    /** 系统内部标注泄露:回灌历史用的 [系统确认卡:…]/[系统:…]/[用户对…] 标注是给模型的,
     *  出现在最终回复里既泄露内部机制也很难看(实测模型原样抄出过)。 */
    static final Pattern SYSTEM_LEAK = Pattern.compile("\\[(系统确认卡|系统:|用户对)");

    /** 向用户索要/询问账号已有资料:注册姓名/手机号系统会自动获取,纯文字索要或反复问
     *  "需要指定吗"都违反第 11 条规则(实测两种变体都出现过:直接索要、委婉询问),一律拦截。 */
    static final Pattern PII_ASK = Pattern.compile(
            "请提供[^。！？\\n]{0,20}(手机号|收件人|真实姓名|您的姓名)"
                    + "|(收件人|手机号|姓名)[^。！？\\n]{0,15}(需要|要不要|是否|想)[^。！？\\n]{0,10}(指定|提供|填写|用)"
                    + "|需要[^。！？\\n]{0,12}(指定|提供|填写)[^。！？\\n]{0,15}(收件人|手机号|姓名)");

    private static final String FAKE_EXECUTION_REPLY =
            "抱歉,我刚才的回复方式有误,请忽略。您的请求还没有执行——请把需求再完整说一遍,"
                    + "我会调用对应的工具并弹出真正的确认按钮。";

    /** 拦截假话术后给模型的纠偏指令:给一次改正机会,多数模型会改调工具,用户无感。 */
    private static final String FAKE_CORRECTION_PROMPT =
            "你上一条回复被系统拦截,用户没有看到。原因可能是:伪造确认卡/执行结果格式、把系统标注原文透给用户、"
            + "或向用户索要系统会自动获取的账号资料。请重新回答本次问题:"
            + "需要执行写操作就必须调用对应工具(如 place_order/add_address),工具会生成真确认卡;"
            + "需要收件人/手机号等账号资料时不要向用户索要,直接调用工具留空,系统自动用注册资料补齐,"
            + "只有工具报错提示资料缺失时才让用户补充;信息不足就用普通文字向用户追问其他必要信息;"
            + "禁止输出'待确认操作'、'确认执行?'、'下单成功'等系统专用格式文本,也不要把方括号系统标注透给用户。";


    private final ChatProvider chatProvider;
    private final ToolRegistry toolRegistry;
    private final PendingActionStore pendingStore;
    private final SiliconFlowProperties props;

    /**
     * 单次 run 的总耗时预算。前端 agentChat 超时 60s,后端必须在此前返回,
     * 否则前端报错后端却仍在跑并落库,用户重试造成重复消息/重复 pending。
     * 最坏单轮 = chat 40s + fallback 40s 已逼近预算,deadline 在每轮 chat 前拦截。
     */
    static final long DEADLINE_MS = 55_000L;

    /**
     * 运行编排循环。
     * @param history  已有消息(system+历史 user/assistant)
     * @param role     当前角色(farmer / buyer)
     * @param userName 当前用户
     * @param sessionId 会话 ID(同时作为 pending 的 session 维度)
     * @return 终稿文本,或在写工具待确认时返回 draft+pendingId
     */
    public OrchestratorResult run(List<ChatMessage> history, String role, String userName, String sessionId) {
        List<ChatMessage> messages = new ArrayList<>(history);
        List<ToolSpec> tools = toolRegistry.specsForRole(role);
        ToolContext ctx = new ToolContext(userName, role, sessionId);
        int maxIter = props.getMaxIterations();
        long deadline = System.currentTimeMillis() + DEADLINE_MS;

        for (int iter = 0; iter < maxIter; iter++) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("[agent] run 超过 {}ms 预算,提前终止", DEADLINE_MS);
                OrchestratorResult out = new OrchestratorResult();
                out.setFinalText("抱歉,这次处理时间过长,请稍后重试或把问题拆分得更具体一些。");
                return out;
            }
            ChatResponse resp = chatProvider.chat(messages, tools, props.getChatModel());
            if (!resp.hasToolCalls()) {
                String text = resp.getText() == null ? "" : resp.getText();
                if (FAKE_EXECUTION.matcher(text).find() || SYSTEM_LEAK.matcher(text).find()
                        || PII_ASK.matcher(text).find()) {
                    // 模型在未调工具的轮次用文字模仿系统格式(假确认卡/假执行结果)——拦截,
                    // 否则用户以为已下单,实际什么都没发生(实测复现过编造"海南新鲜芒果¥35"的假卡)。
                    // 拦截后带纠偏指令自动重试一轮:模型多数会改调工具,用户无感;
                    // 重试仍违规才返回兜底文案。
                    log.warn("[agent] 拦截假执行话术(纯文字轮),纠偏重试: {}", text.replaceAll("\n", " "));
                    messages.add(new ChatMessage("assistant", text, null, null));
                    messages.add(new ChatMessage("system", FAKE_CORRECTION_PROMPT, null, null));
                    ChatResponse retry = chatProvider.chat(messages, tools, props.getChatModel());
                    if (!retry.hasToolCalls()) {
                        String t2 = retry.getText() == null ? "" : retry.getText();
                        if (FAKE_EXECUTION.matcher(t2).find() || SYSTEM_LEAK.matcher(t2).find() || PII_ASK.matcher(t2).find()) {
                            log.warn("[agent] 纠偏重试仍输出假话术,返回兜底提示");
                            OrchestratorResult out = new OrchestratorResult();
                            out.setFinalText(FAKE_EXECUTION_REPLY);
                            return out;
                        }
                        OrchestratorResult out = new OrchestratorResult();
                        out.setFinalText(t2);
                        return out;
                    }
                    resp = retry;   // 重试后愿意调工具了,接回主流程处理 tool_calls
                } else {
                    OrchestratorResult out = new OrchestratorResult();
                    out.setFinalText(text);
                    return out;
                }
            }
            // 补回 assistant 桥接消息:OpenAI 兼容 API 要求 role=tool 消息前必须有带 tool_calls 的 assistant 消息,
            // 否则下一次 /chat/completions 返回 400。toolCallsJson 由 provider 序列化为 tool_calls。
            messages.add(new ChatMessage("assistant", resp.getText(),
                    serializeToolCallsForAssistant(resp.getToolCalls()), null));
            // 处理每个 tool_call
            for (ToolCall call : resp.getToolCalls()) {
                Map<String, Object> args = parseArgs(call.getArguments());
                Tool tool = toolRegistry.get(call.getName());

                if (tool == null) {
                    messages.add(toolMsg(call.getId(), "[工具异常] 未找到工具:" + call.getName()));
                    continue;
                }
                if (tool.isWrite()) {
                    // 生成 draft,挂起等确认,停止循环。预览抛异常(商品不存在/缺必填槽位等)
                    // → 作为 observation 回灌,让模型追问补槽或重新检索,不挂起无效确认卡、不抛 500。
                    String draft;
                    try {
                        draft = tool.previewOrExecute(ctx, args);
                    } catch (Exception e) {
                        log.warn("[agent] 写工具 {} 预览异常:{}", call.getName(), e.getMessage());
                        messages.add(toolMsg(call.getId(), "[工具异常] " + e.getMessage()));
                        continue;
                    }
                    // 同一会话至多一张有效确认卡:旧 pending 未消费则作废(前端旧卡再点只会拿到 timeout)
                    pendingStore.removeBySession(sessionId);
                    PendingActionStore.Pending p = pendingStore.put(sessionId, tool, ctx, args, draft);
                    OrchestratorResult out = new OrchestratorResult();
                    out.setFinalText(draft);
                    out.setPendingId(p.getId());
                    out.setDraft(draft);
                    return out;
                }
                // 读工具:执行(异常即观察)
                String result;
                try {
                    result = tool.previewOrExecute(ctx, args);
                } catch (Exception e) {
                    result = "[工具异常] " + e.getMessage();
                    log.warn("[agent] 工具 {} 异常:{}", call.getName(), e.getMessage());
                }
                messages.add(toolMsg(call.getId(), result));
            }
        }
        // 到达上限
        OrchestratorResult out = new OrchestratorResult();
        out.setFinalText("抱歉,这个问题处理步骤太多,请尝试拆分成更具体的问题。");
        return out;
    }

    /**
     * 用户确认 pending 后,执行写工具,返回结构化结果(供 controller 再灌进上下文续聊)。
     * <p><strong>属主校验</strong>:pending 的 ctx.userName 必须等于当前登录用户,否则 rejected——
     * 防止拿到他人 pendingId(如从历史消息泄露)后替他人确认下单/融资。校验用 get(不消费),
     * 不影响原用户后续确认。</p>
     * <p><strong>原子消费</strong>:属主校验通过后 {@link PendingActionStore#remove(String)} 取走。
     * 两个并发确认只有一个拿到非 null,另一个必得 timeout——关闭 get→remove→execute 三步的 TOCTOU 双写。</p>
     */
    public ConfirmOutcome confirmAndExecute(String pendingId, boolean accept, String userName) {
        PendingActionStore.Pending existing = pendingStore.get(pendingId);
        if (existing != null && !existing.getCtx().getUserName().equals(userName)) {
            log.warn("[agent] 用户 {} 试图确认不属于自己的 pending(属主:{})", userName, existing.getCtx().getUserName());
            return ConfirmOutcome.of(ConfirmOutcome.REJECTED, "无权确认该操作");
        }
        PendingActionStore.Pending p = pendingStore.remove(pendingId);   // atomic consume — closes the double-confirm race
        if (p == null) return ConfirmOutcome.of(ConfirmOutcome.TIMEOUT, "该操作已超时或不存在,请重新发起");
        if (!accept) return ConfirmOutcome.of(ConfirmOutcome.CANCELLED, "已取消该操作");
        try {
            return ConfirmOutcome.of(ConfirmOutcome.EXECUTED, p.getTool().execute(p.getCtx(), p.getArgs()));
        } catch (Exception e) {
            log.warn("[agent] 确认执行异常:{}", e.getMessage());
            return ConfirmOutcome.of(ConfirmOutcome.ERROR, "[执行失败] " + e.getMessage());
        }
    }

    private Map<String, Object> parseArgs(String arguments) {
        if (arguments == null || arguments.isBlank()) return Map.of();
        try {
            JSONObject o = JSONUtil.parseObj(arguments);
            Map<String, Object> m = new java.util.HashMap<>();
            o.forEach(m::put);
            return m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private ChatMessage toolMsg(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }

    /** 把本轮 tool_calls 序列化成 OpenAI 格式 JSON 串,塞进 assistant 桥接消息的 toolCallsJson。 */
    private String serializeToolCallsForAssistant(java.util.List<com.agri.platform.agent.dto.ToolCall> calls) {
        cn.hutool.json.JSONArray arr = new cn.hutool.json.JSONArray();
        for (com.agri.platform.agent.dto.ToolCall c : calls) {
            cn.hutool.json.JSONObject fn = new cn.hutool.json.JSONObject();
            fn.set("id", c.getId());
            fn.set("type", "function");
            cn.hutool.json.JSONObject func = new cn.hutool.json.JSONObject();
            func.set("name", c.getName());
            func.set("arguments", c.getArguments() == null ? "" : c.getArguments());
            fn.set("function", func);
            arr.add(fn);
        }
        return arr.toString();
    }
}
