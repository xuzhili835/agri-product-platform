package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 写工具:取消待付款订单(走确认门)。cancelOrder 内部校验属主与"仅待付款可取消"。
 * 配合"换成/改成"语义:模型先 list_my_orders 告知旧单,用户同意后经本工具取消,再走新购买。
 */
@Component
@RequiredArgsConstructor
public class CancelOrderTool implements Tool {
    private final OrderService orderService;

    public String name() { return "cancel_order"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("取消一笔待付款订单(需用户确认)。orderId 必须是 list_my_orders 返回的真实订单号。"
                        + "用户要'换成别的/不要刚才那个'时:先取消旧订单,再帮用户买新的。")
                .parameters(Map.of("orderId", "integer"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        Integer orderId = Args.toInt(args.get("orderId"));
        if (orderId == null) throw new RuntimeException("缺少订单号,请先用 list_my_orders 查询待付款订单");
        var o = orderService.getOrderDetailWithDetails(orderId, ctx.getUserName());   // 内部校验属主,不存在/非本人会抛
        if (o.getPurchaseStatus() == null || o.getPurchaseStatus() != 1) {
            throw new RuntimeException("订单#" + orderId + " 不是待付款状态,无法取消");
        }
        return StrUtil.format("即将取消订单#{}(合计¥{})。确认执行?", orderId, o.getTotalPrice());
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        Integer orderId = Args.toInt(args.get("orderId"));
        orderService.cancelOrder(orderId, ctx.getUserName());
        return StrUtil.format("订单#{}已取消。", orderId);
    }
}
