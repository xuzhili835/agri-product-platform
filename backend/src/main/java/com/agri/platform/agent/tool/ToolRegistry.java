package com.agri.platform.agent.tool;

import com.agri.platform.agent.dto.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** 启动时收集所有 Tool Bean,按名/角色查。 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> all) {
        for (Tool t : all) tools.put(t.name(), t);
    }

    public Tool get(String name) { return tools.get(name); }

    /** 给模型看的工具清单(按当前角色过滤)。 */
    public List<ToolSpec> specsForRole(String role) {
        return tools.values().stream()
                .filter(t -> "common".equals(t.role()) || t.role().equals(role))
                .map(Tool::spec)
                .collect(Collectors.toList());
    }
}
