package com.agri.platform.agent.core;

import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.config.SiliconFlowProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 写工具挂起态:存「工具+上下文+参数+draft」,等用户确认。内存态、TTL(默认5min)超时回收(档2)。
 *
 * <p>线程安全:{@link ConcurrentHashMap} 单字段兜底;单个 Pending 写入后不修改(createdAt 等 final 语义),
 * 故 get 返回后读改无需额外锁。{@link #sweep()} 的 removeIf 由 ConcurrentHashMap 支持并发遍历。</p>
 *
 * <p>说明:本组件不负责确认态机的并发仲裁——同一 sessionId 只允许一个 pending,
 * 由编排层在挂起新 pending 前调 {@link #removeBySession(String)} 清旧保证。</p>
 */
@Component
@RequiredArgsConstructor
public class PendingActionStore {

    @Data
    public static class Pending {
        private String id;              // pendingId(UUID 去横线)
        private String sessionId;
        private Tool tool;
        private ToolContext ctx;
        private Map<String, Object> args;
        private String draft;           // 给用户看的预览文本
        private long createdAt;
    }

    private final ConcurrentHashMap<String, Pending> map = new ConcurrentHashMap<>();
    private final SiliconFlowProperties props;

    public Pending put(String sessionId, Tool tool, ToolContext ctx, Map<String, Object> args, String draft) {
        Pending p = new Pending();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setSessionId(sessionId);
        p.setTool(tool);
        p.setCtx(ctx);
        p.setArgs(args);
        p.setDraft(draft);
        p.setCreatedAt(System.currentTimeMillis());
        map.put(p.getId(), p);
        return p;
    }

    public Pending get(String pendingId) {
        return pendingId == null ? null : map.get(pendingId);
    }

    /**
     * 原子消费:移除并返回该 pending。返回值为 null 即「已被消费/不存在」。
     * <p>用 {@code ConcurrentHashMap.remove(key)} 的返回值实现原子 consume,
     * 杜绝 confirmAndExecute 里 get→remove→execute 三步的 TOCTOU 双写。</p>
     */
    public Pending remove(String pendingId) {
        return pendingId == null ? null : map.remove(pendingId);
    }

    /**
     * 清掉该会话所有挂起 pending(编排层挂起新 pending 前调用)。
     * 关闭"同一会话累积多张确认卡、顺序双确认都生效"的双写下单口子——
     * 旧卡即使残留在前端,点确认时也只会拿到 timeout。
     */
    public void removeBySession(String sessionId) {
        if (sessionId == null) return;
        map.values().removeIf(p -> sessionId.equals(p.getSessionId()));
    }

    /**
     * 每 60s 扫一次,清掉超过 TTL(pendingTtlSeconds,默认300s)未确认的。
     * {@link org.springframework.scheduling.annotation.EnableScheduling}
     * 在 PlatformApplication 上启用;单测直接 new 本类不触发调度,行为由 put/get/remove 自验。
     */
    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        long now = System.currentTimeMillis();
        long ttlMs = props.getPendingTtlSeconds() * 1000L;
        map.entrySet().removeIf(e -> now - e.getValue().getCreatedAt() > ttlMs);
    }
}
