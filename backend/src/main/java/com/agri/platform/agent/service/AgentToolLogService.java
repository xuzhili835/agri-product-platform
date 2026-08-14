package com.agri.platform.agent.service;

import com.agri.platform.entity.AgentToolLog;
import com.agri.platform.mapper.AgentToolLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 工具调用审计。
 * create_time 由 DB DEFAULT CURRENT_TIMESTAMP 自动填充(DatabaseMigrationRunner DDL)。
 */
@Service
@RequiredArgsConstructor
public class AgentToolLogService {
    private final AgentToolLogMapper mapper;

    public void log(String sessionId, String userName, String toolName, String args, String result, String status, Integer durationMs, boolean confirmed) {
        AgentToolLog l = new AgentToolLog();
        l.setSessionId(sessionId); l.setUserName(userName); l.setToolName(toolName);
        l.setArguments(args); l.setResult(result); l.setStatus(status);
        l.setDurationMs(durationMs); l.setConfirmed(confirmed ? 1 : 0);
        mapper.insert(l);
    }
}
