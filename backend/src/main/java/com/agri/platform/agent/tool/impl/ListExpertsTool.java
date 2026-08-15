package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.entity.Expert;
import com.agri.platform.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 只读工具:列出平台可预约的专家(账号/姓名/专业)。预约必须把这里返回的真实账号传给
 * reserve_expert 的 expertName,杜绝模型编造专家名导致预约落到不存在的账号上。
 */
@Component
@RequiredArgsConstructor
public class ListExpertsTool implements Tool {
    private final ExpertService expertService;

    public String name() { return "list_experts"; }

    public String role() { return "farmer"; }

    public boolean isWrite() { return false; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("列出平台可预约的专家,返回专家账号(userName)/姓名/专业。"
                        + "预约时 reserve_expert 的 expertName 必须用这里返回的账号。")
                .parameters(Map.of()).build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        List<Expert> list = expertService.getAllExperts();
        if (list == null || list.isEmpty()) return "暂无可预约的专家";
        return list.stream()
                .map(e -> StrUtil.format("专家账号:{} 姓名:{} 专业:{}",
                        e.getUserName(), e.getRealName(), e.getProfession()))
                .collect(Collectors.joining("\n"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
