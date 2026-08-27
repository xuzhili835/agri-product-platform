package com.agri.platform.agent.tool;

import com.agri.platform.agent.dto.FormField;
import com.agri.platform.agent.dto.ToolSpec;

import java.util.List;
import java.util.Map;

/** agent 工具。读工具直接 execute;写工具先 preview 生成 draft,确认后 execute。 */
public interface Tool {
    String name();
    ToolSpec spec();
    /** 适用角色(如 "farmer" / "buyer" / "common")。 */
    String role();
    boolean isWrite();
    /** 读工具:直接执行返回结果文本;写工具:返回预览 draft 文本(不落库)。 */
    String previewOrExecute(ToolContext ctx, java.util.Map<String, Object> args);
    /** 写工具专用:用户确认后真正落库。读工具可抛 UnsupportedOperationException。 */
    String execute(ToolContext ctx, java.util.Map<String, Object> args);

    /**
     * 表单卡字段(写工具可选):返回 null 走纯文字确认卡;返回字段列表时前端渲染
     * 可编辑表单——模型提取到的值预填,缺失槽位由用户补,模型无需追问。
     */
    default List<FormField> formFields(ToolContext ctx, Map<String, Object> args) { return null; }

    /**
     * 确认时校验(写工具可选):preview 只管出卡不卡槽位,必填/格式校验挪到这里,
     * 在 confirm 合并表单值后、execute 前执行;不合法直接抛异常,pending 不被消费可改后重试。
     */
    default void validate(Map<String, Object> args) {}
}
