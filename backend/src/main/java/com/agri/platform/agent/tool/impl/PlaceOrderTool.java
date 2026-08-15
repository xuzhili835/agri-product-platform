package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.SearchedProductCache;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.dto.OrderRequest;
import com.agri.platform.entity.Address;
import com.agri.platform.entity.Product;
import com.agri.platform.mapper.ProductMapper;
import com.agri.platform.service.AddressService;
import com.agri.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 写工具:下单购买商品(直接购买)。previewOrExecute 校验商品存在、解析收货地址、算总价生成 draft;
 * execute 复检后调 OrderService.submitOrder 落库,返回 purchaseId。
 *
 * <p>参数校验在 preview 阶段抛业务异常(商品不存在/数量非法/无默认地址),由编排层把异常文本
 * 作为 observation 回灌给模型,模型追问用户或重新 search_market——避免把编造的商品编号
 * 落成 0 元订单(曾导致支付报"订单金额超过限额")。</p>
 *
 * <p>address 传"默认地址"/"默认"/空时,自动取买家默认收货地址(tb_address isDefault=1)的完整串;
 * 传具体地址文本时原样使用。</p>
 *
 * <p>purchaseType=2 表直接购买;items 为单元素列表——购物车合并下单不在 agent 范围内。</p>
 */
@Component
@RequiredArgsConstructor
public class PlaceOrderTool implements Tool {
    private final OrderService orderService;
    private final ProductMapper productMapper;
    private final AddressService addressService;
    private final SearchedProductCache searchedCache;
    private final SearchMarketTool searchMarketTool;

    public String name() { return "place_order"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("下单购买商品(直接购买)。用户已表达购买意向并给出商品和数量时立即调用本工具;"
                        + "调用后系统会自动向用户展示确认卡,不需要你等待用户口头确认,也不要在文字里说'现在为您下单'之类的话。"
                        + "orderId 必须是本次会话 search_market 返回或在售列表中的真实商品编号;订单号不是商品编号。"
                        + "count 为数量;address 为收货地址,用户说默认地址时原样传默认地址即可。")
                .parameters(Map.of(
                        "orderId", "integer",
                        "count", "integer",
                        "address", "string"))
                .build();
    }

    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        Integer orderId = toInt(args.get("orderId"));
        Integer count = toInt(args.get("count"));
        if (orderId == null) throw new RuntimeException("缺少商品编号,请先用 search_market 搜索商品");
        if (count == null || count <= 0) throw new RuntimeException("购买数量必须大于0");
        // 数量上限:submitOrder 无库存概念也不限数量,不设上限会落成 0.01 元商品 × 99999 的天价订单
        if (count > 999) throw new RuntimeException("单次购买数量不能超过999,如需更多请分批下单");
        // 编号白名单:必须是本会话搜索结果——实测模型会跳过搜索编造编号(把历史"订单号:14"当商品编号),
        // 编造编号要么撞错要么撞上用户根本不想买的商品。
        // 未命中时按该编号反查标题做精准代搜:商品真实存在则必命中(空 keyword 全量搜索会被
        // 分页/时间排序截断,目标商品可能不在前10),命中即放行——确认卡上用户还会亲眼核对
        // 商品名/价格,双保险;商品不存在则退全量列表引导模型重新选择。
        if (!searchedCache.contains(ctx.getSessionId(), orderId)) {
            Product probe = productMapper.selectById(orderId);
            String kw = probe == null || probe.getTitle() == null ? "" : probe.getTitle();
            String listing = searchMarketTool.previewOrExecute(ctx, Map.of("keyword", kw));
            if (!searchedCache.contains(ctx.getSessionId(), orderId)) {
                throw new RuntimeException("商品#" + orderId + " 不存在或已下架(订单号不能当商品编号)。"
                        + "请从以下在售商品中选择正确编号重新调用:\n" + listing);
            }
        }
        Product product = productMapper.selectById(orderId);
        if (product == null) throw new RuntimeException("商品#" + orderId + " 不存在或已下架,请用 search_market 重新选择");
        String address = resolveAddress(ctx, args);
        BigDecimal total = product.getPrice().multiply(new BigDecimal(count));
        return StrUtil.format("即将下单:{} ¥{} x {} = ¥{}\n收货地址:{}\n确认执行?",
                product.getTitle(), product.getPrice(), count, total, address);
    }

    public String execute(ToolContext ctx, Map<String, Object> args) {
        Integer orderId = toInt(args.get("orderId"));
        // 确认与执行之间商品可能被删除,复检一次,避免落成 0 元订单
        Product product = productMapper.selectById(orderId);
        if (product == null) throw new RuntimeException("商品#" + orderId + " 不存在或已下架,下单失败");
        String address = resolveAddress(ctx, args);

        OrderRequest req = new OrderRequest();
        req.setPurchaseType(2);     // 2=直接购买
        req.setAddress(address);
        OrderRequest.OrderItem item = new OrderRequest.OrderItem();
        item.setOrderId(orderId);
        item.setCount(toInt(args.get("count")));
        req.setItems(List.of(item));
        Integer purchaseId = orderService.submitOrder(ctx.getUserName(), req);
        return StrUtil.format("下单成功,订单号:{},合计:¥{}。请在订单列表完成支付。",
                purchaseId, product.getPrice().multiply(new BigDecimal(item.getCount())));
    }

    /** address 缺省或明确说"默认地址"时取买家默认收货地址;否则用用户给的地址文本。 */
    private String resolveAddress(ToolContext ctx, Map<String, Object> args) {
        Object a = args.get("address");
        String addr = a == null ? null : a.toString().trim();
        boolean useDefault = addr == null || addr.isEmpty()
                || "默认地址".equals(addr) || "默认".equals(addr) || "用默认地址".equals(addr);
        if (!useDefault) return addr;
        Address def = addressService.getDefaultAddress(ctx.getUserName());
        if (def != null) return def.getFullAddress();
        throw new RuntimeException("您还没有默认收货地址,请直接告诉我详细地址,或先在收货地址管理中添加");
    }

    /** 模型可能把整数参数回成 "3.0"(hutool 解析为 Double/BigDecimal),统一走 Args 健壮转换。 */
    private Integer toInt(Object o) {
        return Args.toInt(o);
    }
}
