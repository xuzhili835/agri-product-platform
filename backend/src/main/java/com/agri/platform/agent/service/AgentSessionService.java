package com.agri.platform.agent.service;

import com.agri.platform.entity.AgentSession;
import com.agri.platform.mapper.AgentSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSessionService {
    private final AgentSessionMapper mapper;

    /**
     * 按 sessionId 取会话;不存在或<strong>不属于该用户</strong>时新建。
     * <p>属主校验:此前直接 selectById 返回,用户 A 传 B 的 sessionId 即可把消息写进 B 的会话
     * (配合 /agent/history 越权读)。归属不符视同不存在,静默新建,不泄露他人会话存在性。</p>
     */
    public AgentSession getOrCreate(String userName, String role, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            AgentSession s = mapper.selectById(sessionId);
            if (s != null && userName.equals(s.getUserName())) { touch(s); return s; }
        }
        AgentSession s = new AgentSession();
        s.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        s.setUserName(userName);
        s.setRole(role);
        s.setStatus(1);
        s.setCreateTime(LocalDateTime.now());
        s.setLastActiveTime(LocalDateTime.now());
        mapper.insert(s);
        return s;
    }

    /** 取属于该用户的会话,不属于(或不存在)返回 null——history 端点据此拒绝越权读取。 */
    public AgentSession getOwned(String sessionId, String userName) {
        if (sessionId == null || sessionId.isBlank()) return null;
        AgentSession s = mapper.selectById(sessionId);
        return (s != null && userName.equals(s.getUserName())) ? s : null;
    }

    public void touch(AgentSession s) {
        s.setLastActiveTime(LocalDateTime.now());
        mapper.updateById(s);
    }
}
