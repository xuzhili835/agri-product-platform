package com.agri.platform.agent.tool.impl;

import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.Args;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.entity.Product;
import com.agri.platform.agent.tool.SearchedProductCache;
import com.agri.platform.service.ProductService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchMarketTool implements Tool {
    private final ProductService productService;
    private final PiiMasker masker;
    private final SearchedProductCache searchedCache;
    public String name() { return "search_market"; }
    public String role() { return "common"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("搜索市场行情/商品比价,返回 商品#编号+[供应/求购]+标题+价格+卖家。下单只能买[供应]商品,下单时必须把返回的真实商品编号传给 place_order。参数:keyword(品类或商品名,可选)。")
                .parameters(Map.of("keyword", "string")).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        String keyword = Args.str(args.get("keyword"));
        // 不按 type 过滤:此前硬编码 "goods",农户发布的求购(demand)信息永远搜不到
        Page<Product> p = productService.getProductPage(1, 10, null, keyword == null ? "" : keyword);
        List<Product> list = p.getRecords();
        if (list == null || list.isEmpty()) return "未搜到相关商品";
        // 搜索结果编号记入会话白名单:place_order 只接受白名单编号,杜绝模型编造/混淆编号
        list.forEach(pr -> searchedCache.record(ctx.getSessionId(), pr.getOrderId()));
        return masker.mask(list.stream()
                .map(pr -> "商品#" + pr.getOrderId() + " " + ("demand".equals(pr.getType()) ? "[求购]" : "[供应]")
                        + " " + pr.getTitle() + " ¥" + pr.getPrice() + " 卖家:" + pr.getOwnName())
                .collect(Collectors.joining("\n")));
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
