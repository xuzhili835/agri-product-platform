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
        List<Product> list = searchBy(keyword);
        if ((list == null || list.isEmpty()) && keyword != null && keyword.length() >= 4 && keyword.length() <= 20) {
            list = fallbackSearch(keyword);
        }
        if (list == null || list.isEmpty()) return "未搜到相关商品";
        // 搜索结果编号记入会话白名单:place_order 只接受白名单编号,杜绝模型编造/混淆编号
        list.forEach(pr -> searchedCache.record(ctx.getSessionId(), pr.getOrderId()));
        return masker.mask(list.stream()
                .map(pr -> "商品#" + pr.getOrderId() + " " + ("demand".equals(pr.getType()) ? "[求购]" : "[供应]")
                        + " " + pr.getTitle() + " ¥" + pr.getPrice() + " 卖家:" + pr.getOwnName())
                .collect(Collectors.joining("\n")));
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }

    private List<Product> searchBy(String kw) {
        Page<Product> p = productService.getProductPage(1, 10, null, kw == null ? "" : kw);
        return p.getRecords();
    }

    /**
     * 组合词回退:整串 LIKE 搜不到在售商品(如"攀枝花芒果"匹配不上"攀枝花天然有机芒果")时,
     * ①按拆分点把关键词拆两半,两边都命中的取交集(保精确);
     * ②仍为空则取片段单独搜,长片段优先(保召回,如"新鲜的芒果"能靠"芒果"命中)。
     */
    private List<Product> fallbackSearch(String keyword) {
        // ①拆两半求交集:左右片段均 >=2 字
        for (int i = 2; i <= keyword.length() - 2; i++) {
            List<Product> left = searchBy(keyword.substring(0, i));
            if (left == null || left.isEmpty()) continue;
            List<Product> right = searchBy(keyword.substring(i));
            if (right == null || right.isEmpty()) continue;
            List<Product> hit = left.stream()
                    .filter(l -> right.stream().anyMatch(r -> r.getOrderId().equals(l.getOrderId())))
                    .collect(Collectors.toList());
            if (!hit.isEmpty()) return hit;
        }
        // ②片段单独搜,长片段优先
        for (int len = keyword.length() - 2; len >= 2; len--) {
            for (int i = 0; i + len <= keyword.length(); i++) {
                List<Product> hit = searchBy(keyword.substring(i, i + len));
                if (hit != null && !hit.isEmpty()) return hit;
            }
        }
        return List.of();
    }
}
