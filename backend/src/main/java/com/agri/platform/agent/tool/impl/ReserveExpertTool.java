package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.dto.ReserveRequest;
import com.agri.platform.service.ReserveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 写工具:预约专家咨询。previewOrExecute 生成确认 draft;execute 调 ReserveService.makeReservation 落库。
 */
@Component
@RequiredArgsConstructor
public class ReserveExpertTool implements Tool {
    private final ReserveService reserveService;

    public String name() { return "reserve_expert"; }

    public String role() { return "farmer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("预约专家咨询。参数:expertName(专家姓名/账号)、preferredTime(期望时间段,如'工作日上午')、message(咨询内容/留言)。需用户确认。")
                .parameters(Map.of(
                        "expertName", "string",
                        "preferredTime", "string",
                        "message", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        return StrUtil.format("即将预约专家 {} 时间:{} 咨询内容:{}。确认?",
                args.get("expertName"), args.get("preferredTime"), args.get("message"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        ReserveRequest req = new ReserveRequest();
        req.setExpertName(String.valueOf(args.get("expertName")));
        req.setPreferredTime(String.valueOf(args.get("preferredTime")));
        req.setMessage(String.valueOf(args.get("message")));
        reserveService.makeReservation(ctx.getUserName(), req);
        return "专家预约已提交";
    }
}
