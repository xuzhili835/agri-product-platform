package com.agri.platform.agent.dto;

import lombok.Data;

/**
 * 编排循环输出。
 * <p>正常完成:finalText 非空、pendingId=null。</p>
 * <p>停在写工具待确认:finalText=draft、pendingId 非空、draft 非空(needsConfirm()==true)。</p>
 * <p>到达 max-iter:finalText="抱歉,这个问题处理步骤太多…"、pendingId=null。</p>
 */
@Data
public class OrchestratorResult {
    private String finalText;       // 给用户的文本(含流式)
    private String pendingId;       // 若循环停在等确认,这里非空
    private String draft;           // 待确认的操作预览
    public boolean needsConfirm() { return pendingId != null; }
}
