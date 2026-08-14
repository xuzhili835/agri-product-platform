package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.entity.User;
import com.agri.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueryCreditTool implements Tool {
    private final UserService userService;
    private final PiiMasker masker;

    public String name() { return "query_credit"; }
    public String role() { return "farmer"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("查询当前农户的信用分(1-5)与积分。额度与信用分无关。")
                .parameters(Map.of()).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        User u = userService.getUserByUserName(ctx.getUserName());
        if (u == null) return "未找到用户";
        String raw = StrUtil.format("用户:{} 信用分:{} 积分:{}",
                u.getRealName(), u.getCredit(), u.getIntegral());
        return masker.mask(raw);
    }
    public String execute(ToolContext ctx, Map<String, Object> args) {
        throw new UnsupportedOperationException();
    }
}
