package com.agri.platform.agent.service;

import com.agri.platform.agent.dto.ChatMessage;
import com.agri.platform.entity.AgentMessage;
import com.agri.platform.mapper.AgentMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * 档2:存用户可见消息 + 读历史。
 * create_time 由 DB DEFAULT CURRENT_TIMESTAMP 自动填充(DatabaseMigrationRunner DDL)。
 */
@Service
@RequiredArgsConstructor
public class AgentMessageService {
    private final AgentMessageMapper mapper;

    public void save(String sessionId, String userName, String direction, String content, String toolEvent) {
        AgentMessage m = new AgentMessage();
        m.setSessionId(sessionId); m.setUserName(userName);
        m.setDirection(direction); m.setContent(content); m.setToolEvent(toolEvent);
        mapper.insert(m);
    }

    public List<AgentMessage> history(String sessionId) {
        return mapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId).orderByAsc(AgentMessage::getCreateTime));
    }

    /** 给编排循环的上下文:把历史消息转成 ChatMessage(只取最近 N 轮=滑窗)。 */
    public List<ChatMessage> recentAsChat(String sessionId, int n) {
        List<AgentMessage> all = history(sessionId);
        int start = Math.max(0, all.size() - n * 2);   // 每轮 user+assistant
        List<ChatMessage> out = new ArrayList<>();
        for (AgentMessage m : all.subList(start, all.size())) {
            out.add(new ChatMessage(m.getDirection(), m.getContent(), null, null));
        }
        return out;
    }
}
