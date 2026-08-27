package com.agri.platform.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * 编排循环输出。
 * <p>正常完成:finalText 非空、pendingId=null。</p>
 * <p>停在写工具待确认:finalText=draft、pendingId 非空、draft 非空(needsConfirm()==true);
 * 表单卡工具另带 form 字段列表(前端渲染可编辑表单,预填已提取的槽位)。</p>
 * <p>到达 max-iter:finalText="抱歉,这个问题处理步骤太多…"、pendingId=null。</p>
 */
@Data
public class OrchestratorResult {
    private String finalText;       // 给用户的文本(含流式)
    private String pendingId;       // 若循环停在等确认,这里非空
    private String draft;           // 待确认的操作预览
    private List<FormField> form;   // 表单卡字段(写工具可选;null=纯文字确认卡)
    public boolean needsConfirm() { return pendingId != null; }
}
