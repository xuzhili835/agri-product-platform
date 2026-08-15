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
        // tb_agent_tool_log.session_id NOT NULL:confirm 时 sessionId 可能校验失败被置 null,
        // 插 null 会 SQL 报错把整个 confirm 打成 500——而那时写操作可能已经执行完
        l.setSessionId(sessionId == null ? "" : sessionId);
        l.setUserName(userName == null ? "" : userName);
        l.setToolName(toolName == null ? "" : toolName);
        l.setArguments(args); l.setResult(result); l.setStatus(status);
        l.setDurationMs(durationMs); l.setConfirmed(confirmed ? 1 : 0);
        mapper.insert(l);
    }
}
