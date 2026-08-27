package com.agri.platform.controller;

import com.agri.platform.agent.core.AgentOrchestrator;
import com.agri.platform.agent.dto.*;
import com.agri.platform.agent.service.*;
import com.agri.platform.common.Result;
import com.agri.platform.entity.AgentMessage;
import com.agri.platform.entity.User;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 农融汇 AI 智能助手 REST 端点。
 * <ul>
 *   <li>所有端点走默认 JwtInterceptor(需登录);/agent/** 不在 WebConfig 放行列表。</li>
 *   <li>chat 仅 farmer/buyer 可用(角色运行时校验);admin/toggle 仅 admin。</li>
 *   <li>System Prompt 硬编码于此;token→userName 复用 userService.getUserByUserNameFromToken(内部剥 "Bearer ")。</li>
 * </ul>
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final AgentSessionService sessionService;
    private final AgentMessageService messageService;
    private final AgentToolLogService toolLogService;
    private final SystemConfigService systemConfigService;
    private final UserService userService;

    private static final String SYSTEM_PROMPT =
            "你是农融汇平台的智能助手。严格遵守以下规则:\n"
            + "1) 所有数据(价格/额度/利率/商品编号/专家名单)只能来自工具返回,禁止凭空编造。\n"
            + "2) 用户请求超出当前可用工具范围时,如实告知无法办理并简要说明原因,禁止用文字虚构交易流程或执行结果。\n"
            + "3) 写操作(下单/预约/融资申请)只能通过调用对应工具发起,由用户点'确认执行'后才真正生效。"
            + "未调用工具时,禁止声称'已下单/已预约/已确认'等任何执行结果。\n"
            + "4) 下单前必须先用 search_market 查到真实商品编号,并把工具返回的真实编号传给 place_order,禁止自己编编号;"
            + "下单地址只能用'默认地址'、'地址簿#编号'(来自 list_addresses)或'新地址'(本会话刚通过 add_address 新增的那条);"
            + "用户提到具体地址(城市/街道)时,必须先调 list_addresses 找到对应编号,禁止直接传'默认地址';"
            + "用户说'用新地址/最新地址/刚加的地址下单'或'用新地址帮我买'时,只要本会话已新增过地址就传'新地址',"
            + "系统自动引用,不要再调 add_address、更不要索要地址信息;用户刚新增地址后说'继续'下单也默认传'新地址'"
            + "(除非用户明确要求默认地址);只有用户要用一个从未提供过的新地址时,"
            + "才让用户提供 省/市/区/详细地址 并调 add_address(收件人/手机号系统自动取注册资料),然后传'新地址'下单;"
            + "用户说'换成/改成/不要刚才那个'时,先用 list_my_orders 查待付款订单,如实告知旧订单还在,"
            + "询问是否取消(cancel_order 需用户确认),处理完旧单再帮用户买新的,不要默默新下单;"
            + "用户消息里没提任何地址时,address 一律直接传'默认地址'出卡,严禁反问'用哪个地址'或让用户选地址。\n"
            + "5) 预约专家前必须由你自己调用 list_experts 查真实专家账号,reserve_expert 的 expertName 必须用返回的账号,禁止编造;"
            + "用户不可能知道专家账号,严禁向用户索要'账号/账号名',你负责把用户说的姓名换算成账号。\n"
            + "6) 信用分只影响审批通过率,不影响额度(额度由银行套餐决定)。\n"
            + "7) 不清楚或工具未返回数据时,如实说'暂无该数据',不要编。\n"
            + "8) 用中文简洁回答,语气平实,禁止装饰符号(如✿、【】)、表情符号和角色扮演式输出。\n"
            + "9) '待确认操作:…确认执行?'确认卡和'下单成功/预约已提交/融资申请已提交'执行结果只能由系统工具产生,"
            + "你严禁在文字回复中模仿这些格式、伪造确认卡或声称任何执行结果——没弹出确认按钮的操作等于没有发生。\n"
            + "10) 用户表示同意/确认时,若你还没有生成确认卡,必须先调用对应写工具生成真确认卡,而不是直接宣称完成。\n"
            + "11) 用户的账号资料(真实姓名/手机号/默认地址)系统会按注册信息自动获取,不要向用户索要这些信息:"
            + "下单/新增地址需要收件人和手机号而用户未提供时,直接调用工具留空即可,系统自动用注册资料补齐,"
            + "只有工具报错提示资料缺失时才让用户补充。\n"
            + "12) 用户分多条消息提供信息时(如先说金额、后补还款来源),必须合并会话中已提供的全部信息直接推进,"
            + "严禁要求用户重复任何已提供过的信息——回复前先回看上文,'请提供/请告诉我金额、原因'等话术"
            + "在上文已有答案时绝对禁止;只允许追问真正缺失的必填字段,且一次只列缺的;"
            + "可选字段(联合贷款人、土壤条件、留言、问题细节等)一律不追问,必填信息齐全就直接出确认卡。\n"
            + "13) 搜索类请求(找商品/看行情/查求购)收到后立即调用 search_market 直接给出结果,"
            + "不要先反问偏好(品种/价格区间/产地);给出结果后可以再问是否需要缩小范围。\n"
            + "14) 写操作的必填信息一旦齐全立即调用工具出确认卡,禁止追加非必填的细化追问"
            + "(如'具体几点''再详细描述一下');时间给到'周三下午/周六上午'粒度即可直接出卡,"
            + "商品/地址/病情用户已说明的就不再确认、不再复述提问。\n"
            + "15) reserve_expert/add_address/apply_finance/ask_question 是表单卡工具:调用后系统会弹出可编辑表单,"
            + "你提取到的信息自动预填,缺失的槽位由用户在表单里自己补——因此信息不全也必须直接调用,"
            + "哪怕只提取到一两个字段(如只说了专家姓名)也要调用,严禁向用户追问这些工具的任何字段"
            + "(时间/面积/农作物/土壤/原因/还款来源/省市区/详细地址/标题等都不要问),"
            + "融资的申请金额由套餐额度固定决定,不需要也无法提供金额;"
            + "联合贷款人若用户指名道姓,也只原样带姓名即可(系统表单里选联系人),严禁编造对方账号;"
            + "预约专家时用户提过地名就拆进 province/city/area(区县),种植面积用 plantArea,"
            + "用户没提的地址/电话表单会按账号资料预填;"
            + "ask_question(线上提问,异步问答)与 reserve_expert(约时间一对一)分工:用户说'问问题/请教'"
            + "用 ask_question,'约时间/见面/上门'才用 reserve_expert,严禁把问问题当预约提交;"
            + "调用时缺失的字段直接省略不传,严禁填'周几上午''多少亩''当前状况''暂无'等占位文字,"
            + "把会话中已知的信息全部带上调工具即可。";

    /** 在通用规则后追加当前角色的能力边界:模型对越权请求(如农户要求下单)直接说明,而非编造流程。 */
    private static String systemPromptFor(String role) {
        String caps = "farmer".equals(role)
                ? "当前用户是农户。可用:查信用分、查融资套餐、查融资通过前景、申请融资、查/预约专家、向专家提问、查市场行情、查知识库。"
                  + "下单购买是买家功能,当前没有下单工具;用户要求下单时,直接说明农户账号无法下单。"
                : "当前用户是买家。可用:查市场行情、查知识库、下单购买商品、管理收货地址(查询/新增)、"
                  + "查/取消自己的待付款订单、查/预约专家、向专家提问。"
                  + "申请融资是农户功能,当前没有相应工具;用户要求时,直接说明买家账号无法办理。";
        return SYSTEM_PROMPT + "\n\n" + caps;
    }

    /** 总开关状态(前端小精灵挂载先查)。 */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestHeader("Authorization") String token) {
        User u = currentUser(token);
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", systemConfigService.agentEnabled());
        m.put("role", u.getRole());
        return Result.success(m);
    }

    /** 对话(同步)。 */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestHeader("Authorization") String token,
                                            @RequestBody AgentChatRequest req) {
        User u = currentUser(token);
        if (!"farmer".equals(u.getRole()) && !"buyer".equals(u.getRole())) {
            return Result.error("当前角色暂不支持智能助手");
        }
        if (!systemConfigService.agentEnabled()) {
            return Result.error("智能助手已停用");
        }
        // 入口校验:空 message 会以"无 content 的 user 消息"打到硅基 API 触发 400(与 bridge 消息 400 同族);
        // 超长 message 既拖慢 LLM 也可能超出模型上下文
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            return Result.error("消息内容不能为空");
        }
        if (req.getMessage().length() > 2000) {
            return Result.error("消息过长,请精简后重试(最多2000字)");
        }
        var session = sessionService.getOrCreate(u.getUserName(), u.getRole(), req.getSessionId());

        // 构造上下文:system + 最近 N 轮 + 本轮 user
        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(new ChatMessage("system", systemPromptFor(u.getRole()), null, null));
        msgs.addAll(messageService.recentAsChat(session.getSessionId(), 6));   // 滑窗 6 轮
        msgs.add(new ChatMessage("user", req.getMessage(), null, null));

        OrchestratorResult out = orchestrator.run(msgs, u.getRole(), u.getUserName(), session.getSessionId());

        // 持久化可见消息(档2)。前缀不加装饰符号(system prompt 第8条自己先遵守)
        messageService.save(session.getSessionId(), u.getUserName(), "user", req.getMessage(), null);
        String assistantText = out.needsConfirm() ? ("待确认操作:\n" + out.getDraft()) : out.getFinalText();
        messageService.save(session.getSessionId(), u.getUserName(), "assistant", assistantText,
                out.needsConfirm() ? "confirm:" + out.getPendingId() : null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", session.getSessionId());
        resp.put("reply", assistantText);
        resp.put("pendingId", out.getPendingId());
        resp.put("needsConfirm", out.needsConfirm());
        if (out.needsConfirm()) {
            // 表单卡:字段列表(含预填值/选项)一并下发,前端渲染可编辑表单;纯文字卡 form=null
            resp.put("form", out.getForm());
            toolLogService.log(session.getSessionId(), u.getUserName(), "pending", null, out.getDraft(), "pending", null, false);
        }
        return Result.success(resp);
    }

    /**
     * 确认/取消写操作。响应含结构化 status(executed/cancelled/timeout/rejected/error/disabled),
     * 前端据此渲染真实结果——此前只回文本,超时/异常也被前端显示成"已执行"。
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestHeader("Authorization") String token,
                                               @RequestBody ConfirmRequest req) {
        User u = currentUser(token);
        long t0 = System.currentTimeMillis();
        ConfirmOutcome outcome;
        if (!systemConfigService.agentEnabled()) {
            // 开关关闭时不再执行已挂起的写操作(pending 保留至超时,不消费)
            outcome = ConfirmOutcome.of(ConfirmOutcome.DISABLED, "智能助手已停用,该操作未执行");
        } else {
            // args:表单卡提交的编辑值,非空项覆盖挂起参数(纯文字卡不传,走 null)
            outcome = orchestrator.confirmAndExecute(req.getPendingId(), req.isAccept(), u.getUserName(), req.getArgs());
        }
        String sessionId = req.getSessionId();
        // 历史只写进属于当前用户的会话:pendingId 属主过了,但 sessionId 是前端另传的,
        // 不校验就能把"确认/结果"消息写进他人会话(污染对方历史)
        if (sessionId != null && sessionService.getOwned(sessionId, u.getUserName()) == null) {
            sessionId = null;
        }
        if (sessionId != null) {
            messageService.save(sessionId, u.getUserName(), "user", req.isAccept() ? "确认" : "取消", null);
            // tool-result 标记:recentAsChat 据此给模型加"系统注入的真实执行结果"标注,防其模仿伪造
            messageService.save(sessionId, u.getUserName(), "assistant", outcome.getText(), "tool-result");
        }
        String logStatus = ConfirmOutcome.EXECUTED.equals(outcome.getStatus()) ? "ok" : outcome.getStatus();
        toolLogService.log(sessionId, u.getUserName(), "write-tool", null, outcome.getText(), logStatus,
                (int)(System.currentTimeMillis() - t0), outcome.isExecuted());
        Map<String, Object> resp = new HashMap<>();
        resp.put("reply", outcome.getText());
        resp.put("status", outcome.getStatus());
        resp.put("success", outcome.isExecuted());
        return Result.success(resp);
    }

    /** 历史消息(档2 回看)。仅会话属主可读,防跨用户读取他人对话(含确认卡里的联系方式/地址)。 */
    @GetMapping("/history/{sessionId}")
    public Result<List<AgentMessage>> history(@RequestHeader("Authorization") String token,
                                              @PathVariable String sessionId) {
        User u = currentUser(token);
        if (sessionService.getOwned(sessionId, u.getUserName()) == null) {
            return Result.error("会话不存在或无权访问");
        }
        return Result.success(messageService.history(sessionId));
    }

    /** 管理员切换总开关。 */
    @PostMapping("/admin/toggle")
    public Result<Map<String, Object>> toggle(@RequestHeader("Authorization") String token,
                                              @RequestParam boolean enabled) {
        User u = currentUser(token);
        if (!"admin".equals(u.getRole())) return Result.error("无权限");
        systemConfigService.setAgentEnabled(enabled);
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", enabled);
        return Result.success(m);
    }

    private User currentUser(String token) {
        String userName = userService.getUserByUserNameFromToken(token);
        return userService.getUserByUserName(userName);
    }
}
