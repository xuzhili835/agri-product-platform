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

    public AgentSession getOrCreate(String userName, String role, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            AgentSession s = mapper.selectById(sessionId);
            if (s != null) { touch(s); return s; }
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

    public void touch(AgentSession s) {
        s.setLastActiveTime(LocalDateTime.now());
        mapper.updateById(s);
    }
}
