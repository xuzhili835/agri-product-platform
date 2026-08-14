package com.agri.platform.agent.tool.impl;

import cn.hutool.core.util.StrUtil;
import com.agri.platform.agent.dto.ToolSpec;
import com.agri.platform.agent.tool.Tool;
import com.agri.platform.agent.tool.ToolContext;
import com.agri.platform.entity.FinanceProduct;
import com.agri.platform.service.FinanceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QueryFinancingProductsTool implements Tool {
    private final FinanceService financeService;
    public String name() { return "query_financing_products"; }
    public String role() { return "farmer"; }
    public boolean isWrite() { return false; }
    public ToolSpec spec() {
        return ToolSpec.builder().name(name())
                .description("列出当前可申请的融资套餐(银行发布),含额度上限、利率、期限。额度由套餐决定。")
                .parameters(Map.of()).build();
    }
    public String previewOrExecute(ToolContext ctx, Map<String, Object> args) {
        Page<FinanceProduct> p = financeService.getProductList(1, 20);
        List<FinanceProduct> all = p.getRecords();
        if (all == null || all.isEmpty()) return "暂无可申请的融资套餐";
        // 仅展示在售(status=0 或 null);暂停(1)的不可申请。字段已核对:productName/bankName/money/rate/repayment/status。
        List<FinanceProduct> list = all.stream()
                .filter(fp -> fp.getStatus() == null || fp.getStatus() == 0)
                .collect(Collectors.toList());
        if (list.isEmpty()) return "暂无可申请的融资套餐";
        return list.stream().map(fp -> StrUtil.format("套餐#{} {}({}) 额度上限{}元 利率{} 期限{}期",
                fp.getProductId(), fp.getProductName(), fp.getBankName(), fp.getMoney(), fp.getRate(), fp.getRepayment()))
                .collect(Collectors.joining("\n"));
    }
    public String execute(ToolContext ctx, Map<String, Object> args) { throw new UnsupportedOperationException(); }
}
