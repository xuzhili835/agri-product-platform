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
                    // 完成式/现在时承诺:"已/正在/马上为您下单"却没调工具——用户以为已执行或正在执行。
                    // 注意:不拦"我将/接下来…提交"类将来时预告,也不拦"需要我为您下单吗?"征询句
                    // (实测误伤率高:模型习惯先征询再调工具,拦了反而触发兜底循环);完成式伪造仍被拦,底线不破
                    + "|(已|已经|现在|正在|马上)为您(下单|购买|下单购买)(?![吗么？?])", Pattern.DOTALL);

    /** 系统内部标注泄露:回灌历史用的 [系统确认卡:…]/[系统:…]/[用户对…] 标注是给模型的,
     *  出现在最终回复里既泄露内部机制也很难看(实测模型原样抄出过)。 */
    static final Pattern SYSTEM_LEAK = Pattern.compile("\\[(系统确认卡|系统:|用户对)");

    /** 纯文字轮"宣告将执行"却没发工具调用:模型槽位集齐、复述完计划("我将直接为您提交预约申请"
     *  /工具报错后"我将再次尝试提交")后把宣告当终稿。不算假执行(未声称已完成),但用户等不来确认卡——
     *  注入催促再跑一轮,多数情况模型会真正调工具。排除问句("需要我为您下单吗?"是征询,不是宣告)。 */
    static final Pattern NARRATED_WRITE_INTENT =
            Pattern.compile("我[^。！？\\n]{0,15}(为|帮)(您|你)[^。！？\\n]{0,8}(提交|预约|下单|新增|办理|购买)(?![^。！？\\n]{0,4}[吗呢？?])"
                    + "|我[^。！？\\n]{0,8}再次[^。！？\\n]{0,4}尝试[^。！？\\n]{0,8}(提交|申请|预约|下单)");

    /** 时间已给还在追问("上午还是下午/几点")——用户消息里已有"周X/上午/下午"等时间表达,
     *  模型仍把时间当缺失槽位追问(实测:"下周三下午"被问"上午还是下午")。 */
    static final Pattern TIME_ASK = Pattern.compile("(上午还是下午|下午还是上午|几点|具体时间|期望时间|哪一天|周几)");
    static final Pattern TIME_GIVEN = Pattern.compile("(周[一二三四五六日天]|星期[一二三四五六日天]|今天|明天|后天|上午|下午|傍晚|晚上)");

    /** 表单工具槽位追问:reserve_expert/add_address/apply_finance 调用后系统弹表单收集缺失字段,
     *  模型却还在文字里要槽位——实测三种句式:陈述式索取("请提供省、市、区和详细地址")、
     *  问句式("金额是多少？""周几上午或下午?")、空口宣告后等用户补充。与用户消息的写意图联合判定,
     *  避免误伤咨询类问答。 */
    static final Pattern FORM_SLOT_ASK = Pattern.compile(
            "(请|想)(您|你)?(告诉我|提供|填写|补充|说明|选择|输入)[^。！？\\n]{0,24}"
                    + "(金额|原因|还款来源|时间|面积|省|市|区|详细地址|街道|门牌|套餐|农作物|状况|联合贷款人)"
                    + "|(金额|原因|还款来源|具体时间|时间|周几|几点|上午还是下午|种植面积|面积"
                    + "|什么农作物|农作物|生长情况|土壤条件|省|市|区|详细地址|套餐)"
                    + "[^。！？\\n]{0,8}[?？]");
    /** 用户消息里的写请求意图(用于 FORM_SLOT_ASK 的联合判定)。 */
    static final Pattern WRITE_REQUEST = Pattern.compile(
            "(预约|申请[^。！？\\n]{0,6}(贷|款|融资)|贷款|融资|加[^。！？\\n]{0,4}地址|新增地址|添加[^。！？\\n]{0,4}地址|下单|帮我买|取消订单)");

    /** 表单工具(预约/融资/地址)的用户写意图——比 WRITE_REQUEST 窄,不含下单/取消:
     *  这三个工具的槽位一律由系统表单收集,模型检索完(如 list_experts)停下来用文字索要任何信息都不该发生。 */
    static final Pattern FORM_INTENT = Pattern.compile(
            "(预约|申请|贷款|融资|加[^。！？\\n]{0,4}地址|新增地址|添加[^。！？\\n]{0,4}地址)");

    /** 宣告催促:告诉模型计划说了/在追问表单槽位但工具没调——表单卡工具缺槽也直接调,系统表单会收集。
     *  实测模型可能连续两轮空宣告("请稍等,我将为您预约")才真正调工具,故允许催两次。 */
    private static final String NARRATED_INTENT_NUDGE =
            "你上一条回复在提问或空口宣告,而不是行动。reserve_expert/add_address/apply_finance 是表单卡工具:"
                    + "调用后系统会自动弹出可编辑表单,已提取的信息预填、缺失的槽位由用户自己在表单里补——"
                    + "因此这几个工具严禁向用户追问任何字段(时间/面积/农作物/状况/金额/原因/还款来源/省市区/详细地址都不要问),"
                    + "把已知信息全部带上直接调用工具,哪怕只提取到一两个字段。"
                    + "预约专家时先调 list_experts 把用户说的姓名换算成真实账号,再调 reserve_expert;"
                    + "融资时无法确定套餐就直接调用 apply_finance 并省略 productId,用户会在表单下拉里自己选;"
                    + "其他写工具(place_order/cancel_order)必填信息能从会话中获得时立即调用,确实缺失才追问且只问缺的;"
                    + "用户消息里的时间(如'下周三下午')原样使用,禁止问'上午还是下午/几点'。"
                    + "你的下一条回复不允许再出现纯文字宣告或追问:必须立即发出工具调用(function call)。";;

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
        int nudges = 0;          // 宣告/槽位追问催促次数(实测模型可能连续空宣告,允许催两次)
        int readToolCalls = 0;   // 本次 run 已执行的读工具轮数(表单意图"检索完停下来追问"判定用)

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
                    // 宣告"我将执行"却没调工具 / 时间已给还在追问 / 表单工具槽位追问:最多催两次,
                    // 催完真调工具就出卡,仍不调就把文字放行。
                    // formIntentStop:表单工具写意图的用户,模型检索完读工具(list_experts/query_financing_products)
                    // 却停下来用文字索要信息(措辞变体无穷:请告知/请提供/请问…,无法逐词枚举)——
                    // 按"已做读工具+文本含请或问号"宽判定,这三类工具缺槽也该直接调,由表单收集
                    String curUserMsg = currentUserName(messages);
                    boolean asksGivenTime = TIME_ASK.matcher(text).find()
                            && curUserMsg != null && TIME_GIVEN.matcher(curUserMsg).find();
                    boolean asksFormSlots = FORM_SLOT_ASK.matcher(text).find()
                            && curUserMsg != null && WRITE_REQUEST.matcher(curUserMsg).find();
                    boolean formIntentStop = readToolCalls > 0 && curUserMsg != null
                            && FORM_INTENT.matcher(curUserMsg).find()
                            && (text.contains("请") || text.contains("？") || text.contains("?"));
                    if (nudges < 2 && (NARRATED_WRITE_INTENT.matcher(text).find() || asksGivenTime
                            || asksFormSlots || formIntentStop)) {
                        nudges++;
                        log.warn("[agent] 纯文字轮宣告未执行/追问槽位,注入催促续跑: {}", text.replaceAll("\n", " "));
                        messages.add(new ChatMessage("assistant", text, null, null));
                        messages.add(new ChatMessage("system", NARRATED_INTENT_NUDGE, null, null));
                        continue;
                    }
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
                    // 表单卡:字段列表随卡下发,前端预填渲染,缺失槽位用户在表单里补(模型无需追问)
                    try {
                        out.setForm(tool.formFields(ctx, args));
                    } catch (Exception e) {
                        log.warn("[agent] 工具 {} formFields 异常,降级为文字卡:{}", call.getName(), e.getMessage());
                    }
                    return out;
                }
                // 读工具:执行(异常即观察)
                readToolCalls++;
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

    /** 取消息列表中最后一条 user 消息内容(当前轮用户输入),供"时间已给还追问"判定。 */
    private String currentUserName(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if ("user".equals(m.getRole())) return m.getContent();
        }
        return null;
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
        return confirmAndExecute(pendingId, accept, userName, null);
    }

    /**
     * 带表单值的确认重载:表单卡提交时用户编辑/补全的字段以 overrideArgs 传入——
     * 非空值覆盖挂起时存的 args,空串/null 视为清掉该键(前端空值本来就不传,这里双保险)。
     * <p>合并后先 validate+preview 预检,失败返回 ERROR 且<strong>不消费 pending</strong>——
     * 前端表单保持可编辑,用户改完可再次提交;预检通过才原子 remove 并 execute。</p>
     */
    public ConfirmOutcome confirmAndExecute(String pendingId, boolean accept, String userName,
                                            Map<String, Object> overrideArgs) {
        PendingActionStore.Pending existing = pendingStore.get(pendingId);
        if (existing != null && !existing.getCtx().getUserName().equals(userName)) {
            log.warn("[agent] 用户 {} 试图确认不属于自己的 pending(属主:{})", userName, existing.getCtx().getUserName());
            return ConfirmOutcome.of(ConfirmOutcome.REJECTED, "无权确认该操作");
        }
        if (!accept) {
            PendingActionStore.Pending p = pendingStore.remove(pendingId);   // 取消也消费掉,防旧卡复活
            if (p == null) return ConfirmOutcome.of(ConfirmOutcome.TIMEOUT, "该操作已超时或不存在,请重新发起");
            return ConfirmOutcome.of(ConfirmOutcome.CANCELLED, "已取消该操作");
        }
        PendingActionStore.Pending p = pendingStore.get(pendingId);
        if (p == null) return ConfirmOutcome.of(ConfirmOutcome.TIMEOUT, "该操作已超时或不存在,请重新发起");
        Map<String, Object> merged = mergeArgs(p.getArgs(), overrideArgs);
        try {
            // 预检(不消费 pending):必填校验 + 业务校验/在售复验,失败时表单保持可编辑可重试
            p.getTool().validate(merged);
            p.getTool().previewOrExecute(p.getCtx(), merged);
        } catch (Exception e) {
            log.warn("[agent] 确认预检失败(pending 保留可重试):{}", e.getMessage());
            return ConfirmOutcome.of(ConfirmOutcome.ERROR, "[校验未通过] " + e.getMessage());
        }
        PendingActionStore.Pending consumed = pendingStore.remove(pendingId);   // atomic consume — closes the double-confirm race
        if (consumed == null) return ConfirmOutcome.of(ConfirmOutcome.TIMEOUT, "该操作已超时或不存在,请重新发起");
        try {
            return ConfirmOutcome.of(ConfirmOutcome.EXECUTED, consumed.getTool().execute(consumed.getCtx(), merged));
        } catch (Exception e) {
            log.warn("[agent] 确认执行异常:{}", e.getMessage());
            return ConfirmOutcome.of(ConfirmOutcome.ERROR, "[执行失败] " + e.getMessage());
        }
    }

    /** 表单值合并:非空覆盖存量,空值删除该键(让工具的自动兜底/注册资料回填生效)。 */
    private Map<String, Object> mergeArgs(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new java.util.HashMap<>();
        if (base != null) merged.putAll(base);
        if (override != null) {
            for (Map.Entry<String, Object> e : override.entrySet()) {
                Object v = e.getValue();
                if (v == null || (v instanceof String s && s.trim().isEmpty())) {
                    merged.remove(e.getKey());
                } else {
                    merged.put(e.getKey(), v instanceof String s ? s.trim() : v);
                }
            }
        }
        return merged;
    }

    /** 模型塞进参数的占位文字:该追问没追问时,模型会把追问措辞本身当值传("周几上午""多少亩"
     *  "当前状况""暂无"),以及空串/JSON null 占位——解析参数时统一清掉,视同未提供。 */
    static final Pattern PLACEHOLDER_VALUE = Pattern.compile(
            "周几|星期几|周[XXx]|星期[XXx]|上午还是下午|几点|多少|待填|待补充|待定|未填写|未提供|不知道|不确定|暂无|没有");

    private Map<String, Object> parseArgs(String arguments) {
        if (arguments == null || arguments.isBlank()) return Map.of();
        try {
            JSONObject o = JSONUtil.parseObj(arguments);
            Map<String, Object> m = new java.util.HashMap<>();
            o.forEach((k, v) -> {
                // hutool 把 JSON null 解析成 JSONNull 对象(非 null 非 String),toString 还原成 "null" 字符串——一并清掉
                if (v == null || v instanceof cn.hutool.json.JSONNull) return;
                if (v instanceof String s) {
                    String t = s.trim();
                    if (t.isEmpty() || "null".equalsIgnoreCase(t) || "none".equalsIgnoreCase(t)
                            || PLACEHOLDER_VALUE.matcher(t).find()) return;
                }
                m.put(k, v);
            });
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
