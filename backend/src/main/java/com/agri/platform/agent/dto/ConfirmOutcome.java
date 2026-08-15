package com.agri.platform.agent.dto;

import lombok.Data;

/**
 * 确认端点的结构化结果。此前 confirm 只返回文本,前端无法区分"执行成功/超时/异常",
 * 曾把超时也渲染成"已执行"。status 枚举:
 * <ul>
 *   <li>executed:用户同意且真正落库成功。</li>
 *   <li>cancelled:用户主动取消。</li>
 *   <li>timeout:pending 不存在/已被消费/已过期,本次未执行。</li>
 *   <li>rejected:pending 不属于当前用户(越权确认),本次未执行。</li>
 *   <li>error:用户同意但执行抛异常,未落库或部分落库(以工具返回为准)。</li>
 * </ul>
 */
@Data
public class ConfirmOutcome {
    public static final String EXECUTED = "executed";
    public static final String CANCELLED = "cancelled";
    public static final String TIMEOUT = "timeout";
    public static final String REJECTED = "rejected";
    public static final String ERROR = "error";
    /** 助手总开关已关:pending 未消费,操作未执行。 */
    public static final String DISABLED = "disabled";

    private String status;
    private String text;

    public static ConfirmOutcome of(String status, String text) {
        ConfirmOutcome o = new ConfirmOutcome();
        o.setStatus(status);
        o.setText(text);
        return o;
    }

    /** 前端据此渲染成功/失败样式:只有 executed 算"已执行"。 */
    public boolean isExecuted() { return EXECUTED.equals(status); }
}
