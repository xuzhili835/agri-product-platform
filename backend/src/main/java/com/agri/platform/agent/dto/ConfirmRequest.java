package com.agri.platform.agent.dto;
import lombok.Data;

import java.util.Map;

@Data
public class ConfirmRequest {
    private String pendingId;
    private boolean accept;
    private String sessionId;
    /** 表单卡提交的编辑值(可选):非空覆盖挂起时存的参数,空值不传。 */
    private Map<String, Object> args;
}
