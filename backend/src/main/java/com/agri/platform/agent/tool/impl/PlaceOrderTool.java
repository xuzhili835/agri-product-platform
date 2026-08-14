package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.dto.OrderRequest;
import com.agri.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 写工具:下单购买商品(直接购买)。previewOrExecute 生成确认 draft;execute 调 OrderService.submitOrder 落库,
 * 返回 purchaseId。
 *
 * <p>purchaseType=2 表直接购买(1 表购物车,不在此工具支持范围)。items 为单元素列表——本工具是「单挂单下单」,
 * 购物车合并下单不在 agent 范围内(避免与已有购物车流程抢锁)。</p>
 */
@Component
@RequiredArgsConstructor
public class PlaceOrderTool implements Tool {
    private final OrderService orderService;

    public String name() { return "place_order"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("下单购买商品(直接购买)。参数:orderId(商品/挂单ID)、count(数量)、address(收货地址)。需用户确认。")
                .parameters(Map.of(
                        "orderId", "integer",
                        "count", "integer",
                        "address", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        return StrUtil.format("即将下单:商品#{} 数量:{} 收货:{}。确认?",
                args.get("orderId"), args.get("count"), args.get("address"));
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        // OrderRequest 真实结构:{purchaseType, address, items:[{orderId,count}]}。submitOrder 返回 purchaseId(Integer)。
        OrderRequest req = new OrderRequest();
        req.setPurchaseType(2);     // 2=直接购买
        req.setAddress(String.valueOf(args.get("address")));
        OrderRequest.OrderItem item = new OrderRequest.OrderItem();
        item.setOrderId(toInt(args.get("orderId")));
        item.setCount(toInt(args.get("count")));
        req.setItems(List.of(item));
        Integer purchaseId = orderService.submitOrder(ctx.getUserName(), req);
        return "下单成功,订单号:" + purchaseId;
    }

    private Integer toInt(Object o) {
        return o == null ? null : Integer.valueOf(o.toString());
    }
}
