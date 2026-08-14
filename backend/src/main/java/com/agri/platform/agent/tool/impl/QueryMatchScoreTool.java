package com.agri.platform.agent.tool.impl;

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
public class QueryMatchScoreTool implements Tool {
    private final UserService userService;
    private final PiiMasker masker;
    public String name() { return "query_match_score"; }
    public String role() { return "farmer"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("查询我的信用分(1-5)与融资通过前景的定性解读。不返回具体匹配分(精确分由银行在申请后综合信用/联合贷款人/交易活跃度/借贷负担计算)。")
                .parameters(Map.of()).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        User u = userService.getUserByUserName(ctx.getUserName());
        if (u == null) return "未找到用户";
        int credit = u.getCredit() == null ? 5 : u.getCredit();
        String level = credit >= 4 ? "良好,通过审批概率较高" : credit >= 3 ? "一般,建议加联合贷款人提高通过率" : "偏低,建议先提升信用或加联合贷款人";
        return masker.mask("信用分:" + credit + "/5," + level + "(精确匹配分由银行在申请后综合计算;额度由套餐决定)");
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
