package com.agri.platform.agent.tool.impl;

import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.agent.util.PiiMasker;
import com.agri.platform.entity.Product;
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
    public String name() { return "search_market"; }
    public String role() { return "common"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("搜索市场行情/商品比价。参数:keyword(品类或商品名,可选)。")
                .parameters(Map.of("keyword", "string")).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        String keyword = args.getOrDefault("keyword", "").toString();
        Page<Product> p = productService.getProductPage(1, 10, "goods", keyword);
        List<Product> list = p.getRecords();
        if (list == null || list.isEmpty()) return "未搜到相关商品";
        return masker.mask(list.stream()
                .map(pr -> pr.getTitle() + " ¥" + pr.getPrice() + " 卖家:" + pr.getOwnName())
                .collect(Collectors.joining("\n")));
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
