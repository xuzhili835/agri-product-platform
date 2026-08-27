package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.LatestAddressCache;
import com.agri.platform.agent.tool.SearchedProductCache;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.dto.OrderRequest;
import com.agri.platform.dto.OrderResponse;
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
    private final LatestAddressCache latestAddressCache;

    public String name() { return "place_order"; }

    public String role() { return "buyer"; }

    public boolean isWrite() { return true; }

    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("下单购买商品(直接购买)。用户已表达购买意向并给出商品和数量时立即调用本工具;"
                        + "调用后系统会自动向用户展示确认卡,不需要你等待用户口头确认,也不要在文字里说'现在为您下单'之类的话,"
                        + "更不要征询式反问('需要我为您下单吗''要买2斤吗''您用哪个地址')——用户说买就是意向,确认卡本身就是征询;"
                        + "用户没提地址时 address 一律传'默认地址'(确认卡上用户可核对修改),不要反问选哪个地址;"
                        + "'一份/一个/一斤/两份'等量词即数量(换算成数字)直接出卡,禁止追问数量;"
                        + "仅当计价单位与用户说法无法换算(如按斤计价却说'一箱')时才允许追问换算数量;"
                        + "下单过程中禁止询问与商品/收货无关的问题。"
                        + "orderId 必须是本次会话 search_market 返回或在售列表中的真实商品编号;订单号不是商品编号。"
                        + "count 为数量。address 只能是'默认地址'、'新地址'(指本会话刚通过 add_address 新增的那条)或'地址簿#N'"
                        + "(N 为 list_addresses 返回的地址编号)。"
                        + "用户提到'新地址/最新地址/刚加的地址'且本会话已新增过地址时,address 直接传'新地址',系统会自动引用,"
                        + "不要再调 add_address、更不要向用户索要地址信息;只有用户要用一个从未提供过的地址时,"
                        + "才让用户提供 省/市/区/详细地址 并调 add_address(收件人/手机号系统自动取注册资料,不要索要),"
                        + "然后 address 传'新地址'下单。")
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
        // 换购自查(服务端兜底,不依赖模型记得先查单):同商品已有待付款订单时在确认卡上附提醒
        String reorderHint = pendingSameProductHint(ctx.getUserName(), orderId);
        return StrUtil.format("即将下单:{} ¥{} x {} = ¥{}\n收货地址:{}\n{}确认执行?",
                product.getTitle(), product.getPrice(), count, total, address, reorderHint);
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

    /** 换购提醒:查本人待付款订单中是否已含该商品,有则返回提醒文案(尽力而为,查询失败不阻塞下单)。 */
    private String pendingSameProductHint(String userName, Integer orderId) {
        try {
            var page = orderService.getOrderListWithDetailsPaged(userName, 1, 50, 1);
            if (page == null || page.getRecords() == null) return "";
            for (OrderResponse o : page.getRecords()) {
                if (o.getItems() == null) continue;
                for (OrderResponse.OrderItemResponse it : o.getItems()) {
                    if (orderId.equals(it.getProductId())) {
                        return StrUtil.format("[提醒] 您已有该商品的待付款订单#{}({} x {},合计¥{})。"
                                        + "如需换购,可对我说\"取消订单#{}\"先取消旧单;直接确认将再下一张新单。\n",
                                o.getPurchaseId(), it.getProductName(), it.getCount(), o.getTotalPrice(), o.getPurchaseId());
                    }
                }
            }
        } catch (Exception ignore) {
            // 提醒是增值信息,查不到不影响下单主流程
        }
        return "";
    }

    /**
     * address 只接受"默认地址"(取买家默认收货地址)或"地址簿#N"(N 为 list_addresses 返回的地址编号)。
     * 自由文本地址一律拒绝——用户实测踩过"随口一句'长沙流通县'直接进订单":既不落地址簿、
     * 也不是平台的三级区域(省/市/区)结构。新地址必须走 add_address 先落地址簿。
     */
    private String resolveAddress(ToolContext ctx, Map<String, Object> args) {
        Object a = args.get("address");
        String addr = a == null ? null : a.toString().trim();
        // '新地址'快捷引用:取本会话刚通过 add_address 新增的地址——用户说"用新地址下单"时
        // 模型无需翻地址簿找编号(实测会偷懒传'默认地址'导致新地址被无视)
        if (addr != null && (addr.contains("新地址") || addr.contains("最新地址") || addr.contains("刚新增"))) {
            Integer latestId = latestAddressCache.get(ctx.getSessionId());
            if (latestId == null) {
                throw new RuntimeException("本会话还没有新增过地址。请先让用户提供 省/市/区/详细地址 调 add_address,"
                        + "或改用'默认地址'/'地址簿#编号'");
            }
            Address hit = addressService.getAddressList(ctx.getUserName()).stream()
                    .filter(x -> latestId.equals(x.getId())).findFirst().orElse(null);
            if (hit != null) return hit.getFullAddress();
            throw new RuntimeException("刚新增的地址已不存在,请用 list_addresses 查看地址列表后重试");
        }
        boolean useDefault = addr == null || addr.isEmpty()
                || "默认地址".equals(addr) || "默认".equals(addr) || "用默认地址".equals(addr);
        if (useDefault) {
            Address def = addressService.getDefaultAddress(ctx.getUserName());
            if (def != null) return def.getFullAddress();
            throw new RuntimeException("您还没有默认收货地址。请提供 省/市/区/详细地址/收件人/手机号,"
                    + "我帮您新增到地址簿;或先在收货地址管理中添加");
        }
        // 地址簿#N / 地址N / 纯数字N → 地址簿第 N 条
        Integer id = parseAddressId(addr);
        if (id != null) {
            Address hit = addressService.getAddressList(ctx.getUserName()).stream()
                    .filter(x -> id.equals(x.getId())).findFirst().orElse(null);
            if (hit != null) return hit.getFullAddress();
            throw new RuntimeException("地址簿中没有编号为 " + id + " 的地址,请先调用 list_addresses 查看地址列表");
        }
        throw new RuntimeException("下单地址必须来自地址簿:请传'默认地址'或'地址簿#编号'(来自 list_addresses)。"
                + "用户要用新地址时,先收集 省/市/区/详细地址/收件人/手机号 调 add_address 新增后再下单,"
                + "不要把自由文本地址直接用于下单");
    }

    /** 解析"地址簿#N"/"地址#N"/"地址N"/纯数字"N",返回地址编号;不是编号格式返回 null。 */
    private Integer parseAddressId(String addr) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(?:地址簿?|#)\\s*#?\\s*(\\d+)$").matcher(addr);
        if (m.find()) return Integer.valueOf(m.group(1));
        return addr.matches("\\d+") ? Integer.valueOf(addr) : null;
    }

    /** 模型可能把整数参数回成 "3.0"(hutool 解析为 Double/BigDecimal),统一走 Args 健壮转换。 */
    private Integer toInt(Object o) {
        return Args.toInt(o);
    }
}
