package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.dto.OrderPageResponse;
import com.agri.platform.dto.OrderResponse;
import com.agri.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 只读工具:查买家的待付款订单。用户说"换成/改成/不要刚才那个"时,模型必须先查本工具——
 * 旧订单不会因为用户改口自动消失(实测:用户"改成苹果"结果芒果旧单还挂着待付款),
 * 模型要如实告知并引导取消(list_my_orders → 用户确认 → cancel_order)。
 */
@Component
@RequiredArgsConstructor
public class ListMyOrdersTool implements Tool {
    private final OrderService orderService;

    public String name() { return "list_my_orders"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return false; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("查询当前买家最近的待付款订单,返回 订单号+商品明细+合计。"
                        + "用户说'换成/改成/不要刚才买的/取消订单'时,先调用本工具查看待付款订单再继续;"
                        + "取消订单用 cancel_order(传这里的订单号)。")
                .parameters(Map.of())
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        OrderPageResponse page = orderService.getOrderListWithDetailsPaged(ctx.getUserName(), 1, 5, 1);
        if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
            return "当前没有待付款订单";
        }
        return page.getRecords().stream()
                .map(o -> StrUtil.format("订单#{} {} 合计¥{} ({})",
                        o.getPurchaseId(),
                        o.getItems() == null ? "" : o.getItems().stream()
                                .map(i -> i.getProductName() + "x" + i.getCount())
                                .collect(Collectors.joining(",")),
                        o.getTotalPrice(),
                        o.getCreateTime()))
                .collect(Collectors.joining("\n"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
