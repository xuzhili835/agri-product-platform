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
            "你是农融汇平台的智能助手,服务农户和买家。规则:\n"
            + "1) 只能用提供的工具获取数据或发起操作,禁止编造额度/利率/价格等。\n"
            + "2) 信用分只影响审批通过率,不影响额度(额度由银行套餐决定)。\n"
            + "3) 涉及写操作(融资申请/预约/下单)必须先调用对应工具生成预览,由用户确认。\n"
            + "4) 不清楚或无数据时如实说'暂无该数据',不要编。\n"
            + "5) 用中文简洁回答。";

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
        var session = sessionService.getOrCreate(u.getUserName(), u.getRole(), req.getSessionId());

        // 构造上下文:system + 最近 N 轮 + 本轮 user
        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(new ChatMessage("system", SYSTEM_PROMPT, null, null));
        msgs.addAll(messageService.recentAsChat(session.getSessionId(), 6));   // 滑窗 6 轮
        msgs.add(new ChatMessage("user", req.getMessage(), null, null));

        OrchestratorResult out = orchestrator.run(msgs, u.getRole(), u.getUserName(), session.getSessionId());

        // 持久化可见消息(档2)
        messageService.save(session.getSessionId(), u.getUserName(), "user", req.getMessage(), null);
        String assistantText = out.needsConfirm() ? ("🔧 待确认操作:\n" + out.getDraft()) : out.getFinalText();
        messageService.save(session.getSessionId(), u.getUserName(), "assistant", assistantText,
                out.needsConfirm() ? "confirm:" + out.getPendingId() : null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", session.getSessionId());
        resp.put("reply", assistantText);
        resp.put("pendingId", out.getPendingId());
        resp.put("needsConfirm", out.needsConfirm());
        if (out.needsConfirm()) {
            toolLogService.log(session.getSessionId(), u.getUserName(), "pending", null, out.getDraft(), "pending", null, false);
        }
        return Result.success(resp);
    }

    /** 确认/取消写操作。 */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestHeader("Authorization") String token,
                                               @RequestBody ConfirmRequest req) {
        User u = currentUser(token);
        long t0 = System.currentTimeMillis();
        String result = orchestrator.confirmAndExecute(req.getPendingId(), req.isAccept());
        String sessionId = req.getSessionId();
        if (sessionId != null) {
            messageService.save(sessionId, u.getUserName(), "user", req.isAccept() ? "确认" : "取消", null);
            messageService.save(sessionId, u.getUserName(), "assistant", result, null);
        }
        toolLogService.log(sessionId, u.getUserName(), "write-tool", null, result, req.isAccept() ? "ok" : "cancelled",
                (int)(System.currentTimeMillis() - t0), req.isAccept());
        Map<String, Object> resp = new HashMap<>();
        resp.put("reply", result);
        return Result.success(resp);
    }

    /** 历史消息(档2 回看)。 */
    @GetMapping("/history/{sessionId}")
    public Result<List<AgentMessage>> history(@RequestHeader("Authorization") String token,
                                              @PathVariable String sessionId) {
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
