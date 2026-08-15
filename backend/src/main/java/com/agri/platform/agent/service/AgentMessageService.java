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
        // 次级按 id 排序:create_time 是秒级精度,chat 里 user/assistant 连续插入常常同秒,
        // 只按时间排序顺序不稳定——前端会显示"回答在提问前",recentAsChat 回灌也会把
        // 上下文顺序打乱让模型困惑
        return mapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId)
                .orderByAsc(AgentMessage::getCreateTime)
                .orderByAsc(AgentMessage::getId));
    }

    /**
     * 给编排循环的上下文:把历史消息转成 ChatMessage(只取最近 N 轮=滑窗)。
     * <p><strong>防模仿污染</strong>:确认卡 draft 与写工具执行结果是系统拼装的格式化文本,
     * 原样回灌等于给模型提供"照这样说话"的样本——实测模型会学着用纯文字伪造
     * "待确认操作:…确认执行?"/"下单成功,订单号:14"(工具并未调用)。回灌时加来源标注,
     * 明示这些是系统产物、禁止模仿。</p>
     */
    public List<ChatMessage> recentAsChat(String sessionId, int n) {
        List<AgentMessage> all = history(sessionId);
        int start = Math.max(0, all.size() - n * 2);   // 每轮 user+assistant
        List<ChatMessage> out = new ArrayList<>();
        for (AgentMessage m : all.subList(start, all.size())) {
            String content = m.getContent();
            if (m.getToolEvent() != null && m.getToolEvent().startsWith("confirm:")) {
                // 确认卡回灌只给抽象摘要、不透出格式原文:实测模型会把历史确认卡
                // (连同换行格式"待确认操作:…确认执行?")原样抄进回复伪造新卡
                content = "[系统确认卡:已向用户展示一个写操作的预览并等待其点击确认/取消,"
                        + "卡片格式与细节不对你开放。严禁在回复中模仿确认卡格式或声称用户已确认。]";
            } else if ("tool-result".equals(m.getToolEvent())) {
                content = "[以下是用户确认后写工具的真实执行结果,由系统注入。你不得在未调用工具时声称类似结果]"
                        + content;
            } else if ("user".equals(m.getDirection())
                    && ("确认".equals(trim(m.getContent())) || "取消".equals(trim(m.getContent())))) {
                content = "[用户对上一张系统确认卡的点击操作]" + content;
            }
            out.add(new ChatMessage(m.getDirection(), content, null, null));
        }
        return out;
    }

    private String trim(String s) { return s == null ? "" : s.trim(); }
}
