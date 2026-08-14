package com.agri.platform.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ToolSpec {
    private String name;
    private String description;     // 模型靠它选工具
    /** 参数 JSON Schema(简化:字段名->类型描述);构建 provider 请求时拼成 JSON Schema */
    private Map<String, String> parameters;
}
